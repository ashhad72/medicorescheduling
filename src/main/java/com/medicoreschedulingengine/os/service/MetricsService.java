package com.medicoreschedulingengine.os.service;

import com.medicoreschedulingengine.os.model.dto.AlgorithmResultDTO;
import com.medicoreschedulingengine.os.model.dto.PatientMetricDTO;
import com.medicoreschedulingengine.os.model.entity.ExecutionLog;
import com.medicoreschedulingengine.os.model.entity.Patient;
import com.medicoreschedulingengine.os.model.entity.SimulationRun;
import com.medicoreschedulingengine.os.util.TimeCalculator;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MetricsService
 *
 * Responsible for:
 *   1. Verifying and recalculating metrics from AlgorithmResultDTO
 *   2. Building SimulationRun entities from algorithm results
 *   3. Building ExecutionLog entities from algorithm results
 *   4. Comparing two algorithm results and determining the winner
 */
@Service
public class MetricsService {

    private final TimeCalculator timeCalculator;

    public MetricsService(TimeCalculator timeCalculator) {
        this.timeCalculator = timeCalculator;
    }

    // ── Metric Verification ───────────────────────────────────

    /**
     * Recalculates and validates aggregate metrics from patient-level data.
     * Overwrites the values already set in the DTO to ensure accuracy.
     *
     * Called by SimulationService after each algorithm execution
     * to guarantee metric consistency before saving to DB.
     *
     * @param result  AlgorithmResultDTO returned by a scheduling strategy
     * @return        The same DTO with verified aggregate metrics
     */
    public AlgorithmResultDTO verifyAndRecalculateMetrics(AlgorithmResultDTO result) {
        List<PatientMetricDTO> metrics = result.getPatientMetrics();

        if (metrics == null || metrics.isEmpty()) {
            result.setAvgWaitingTime(0.0);
            result.setAvgTurnaroundTime(0.0);
            result.setAvgResponseTime(0.0);
            return result;
        }

        double avgWT = timeCalculator.calculateAvgWaitingTime(metrics);
        double avgTT = timeCalculator.calculateAvgTurnaroundTime(metrics);
        double avgRT = timeCalculator.calculateAvgResponseTime(metrics);

        result.setAvgWaitingTime(round(avgWT));
        result.setAvgTurnaroundTime(round(avgTT));
        result.setAvgResponseTime(round(avgRT));

        return result;
    }

    // ── SimulationRun Builder ─────────────────────────────────

    /**
     * Builds a SimulationRun entity from an AlgorithmResultDTO.
     * This entity is persisted to the simulation_runs table.
     *
     * @param result  Verified AlgorithmResultDTO
     * @return        SimulationRun entity ready to be saved
     */
    public SimulationRun buildSimulationRun(AlgorithmResultDTO result) {
        SimulationRun run = new SimulationRun();
        run.setAlgorithmName(result.getAlgorithmName());
        run.setDepartmentType(result.getDepartmentType());
        run.setAvgWaitingTime(result.getAvgWaitingTime());
        run.setAvgTurnaroundTime(result.getAvgTurnaroundTime());
        run.setAvgResponseTime(result.getAvgResponseTime());
        run.setTotalPatients(result.getTotalPatients());
        return run;
    }

    // ── ExecutionLog Builder ──────────────────────────────────

    /**
     * Builds ExecutionLog entities from an AlgorithmResultDTO.
     * These are persisted to the execution_logs table and used
     * by GanttChartBuilder to render the timeline.
     *
     * Requires the SimulationRun to already be saved (needs its ID).
     *
     * @param result         Verified AlgorithmResultDTO
     * @param simulationRun  Persisted SimulationRun entity
     * @param patients       Original patient entities (for FK references)
     * @return               List of ExecutionLog entities ready to be saved
     */
    public List<ExecutionLog> buildExecutionLogs(AlgorithmResultDTO result,
                                                 SimulationRun simulationRun,
                                                 List<Patient> patients) {
        return result.getGanttEntries().stream()
                .map(ganttEntry -> {
                    // Find the matching patient entity by ID
                    Patient patient = patients.stream()
                            .filter(p -> p.getId().equals(ganttEntry.getPatientId()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException(
                                    "Patient not found for Gantt entry: "
                                            + ganttEntry.getPatientId()));

                    // Find matching per-patient metrics
                    PatientMetricDTO metric = result.getPatientMetrics().stream()
                            .filter(m -> m.getPatientId().equals(ganttEntry.getPatientId()))
                            .findFirst()
                            .orElse(null);

                    int wt = metric != null ? metric.getWaitingTime()    : 0;
                    int tt = metric != null ? metric.getTurnaroundTime() : 0;
                    int rt = metric != null ? metric.getResponseTime()   : 0;

                    return new ExecutionLog(
                            simulationRun,
                            patient,
                            ganttEntry.getStartTime(),
                            ganttEntry.getEndTime(),
                            result.getAlgorithmName(),
                            wt, tt, rt
                    );
                })
                .collect(java.util.stream.Collectors.toList());
    }

    // ── Comparison Helpers ────────────────────────────────────

    /**
     * Determines which algorithm performed better on waiting time.
     *
     * @return Algorithm name with lower average waiting time
     */
    public String compareWaitingTime(AlgorithmResultDTO r1, AlgorithmResultDTO r2) {
        return r1.getAvgWaitingTime() <= r2.getAvgWaitingTime()
                ? r1.getAlgorithmName()
                : r2.getAlgorithmName();
    }

    /**
     * Determines which algorithm performed better on turnaround time.
     *
     * @return Algorithm name with lower average turnaround time
     */
    public String compareTurnaroundTime(AlgorithmResultDTO r1, AlgorithmResultDTO r2) {
        return r1.getAvgTurnaroundTime() <= r2.getAvgTurnaroundTime()
                ? r1.getAlgorithmName()
                : r2.getAlgorithmName();
    }

    /**
     * Determines which algorithm performed better on response time.
     *
     * @return Algorithm name with lower average response time
     */
    public String compareResponseTime(AlgorithmResultDTO r1, AlgorithmResultDTO r2) {
        return r1.getAvgResponseTime() <= r2.getAvgResponseTime()
                ? r1.getAlgorithmName()
                : r2.getAlgorithmName();
    }

    /**
     * Determines the overall winner by majority vote across 3 metrics.
     * Returns "DRAW" if each algorithm wins exactly one metric and
     * the third is tied.
     *
     * @return Algorithm name of overall winner, or "DRAW"
     */
    public String determineOverallWinner(AlgorithmResultDTO r1, AlgorithmResultDTO r2) {
        int score1 = 0;
        int score2 = 0;

        // Waiting time
        if (r1.getAvgWaitingTime() < r2.getAvgWaitingTime())       score1++;
        else if (r2.getAvgWaitingTime() < r1.getAvgWaitingTime())   score2++;

        // Turnaround time
        if (r1.getAvgTurnaroundTime() < r2.getAvgTurnaroundTime())  score1++;
        else if (r2.getAvgTurnaroundTime() < r1.getAvgTurnaroundTime()) score2++;

        // Response time
        if (r1.getAvgResponseTime() < r2.getAvgResponseTime())      score1++;
        else if (r2.getAvgResponseTime() < r1.getAvgResponseTime()) score2++;

        if (score1 > score2) return r1.getAlgorithmName();
        if (score2 > score1) return r2.getAlgorithmName();
        return "DRAW";
    }

    /**
     * Prints a formatted metric summary to console — useful during
     * development and demos.
     *
     * @param result  AlgorithmResultDTO to print
     */
    public void printMetricsSummary(AlgorithmResultDTO result) {
        System.out.println("=================================================");
        System.out.println("Algorithm  : " + result.getAlgorithmName());
        System.out.println("Department : " + result.getDepartmentType());
        System.out.println("Patients   : " + result.getTotalPatients());
        System.out.printf ("Avg WT     : %.2f%n", result.getAvgWaitingTime());
        System.out.printf ("Avg TT     : %.2f%n", result.getAvgTurnaroundTime());
        System.out.printf ("Avg RT     : %.2f%n", result.getAvgResponseTime());
        System.out.println("=================================================");
    }

    // ── Utility ───────────────────────────────────────────────

    /**
     * Rounds a double to 2 decimal places.
     */
    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}