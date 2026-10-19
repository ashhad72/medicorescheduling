package com.medicoreschedulingengine.os.simulation;

import com.medicoreschedulingengine.os.model.dto.AlgorithmResultDTO;
import com.medicoreschedulingengine.os.model.entity.ExecutionLog;
import com.medicoreschedulingengine.os.model.entity.Patient;
import com.medicoreschedulingengine.os.model.entity.SimulationRun;
import com.medicoreschedulingengine.os.model.enums.DepartmentType;
import com.medicoreschedulingengine.os.repository.ExecutionLogRepository;
import com.medicoreschedulingengine.os.repository.SimulationRepository;
import com.medicoreschedulingengine.os.scheduler.SchedulingStrategy;
import com.medicoreschedulingengine.os.service.MetricsService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SimulationRunner
 *
 * Executes a single algorithm run end-to-end for one department.
 *
 * Responsibilities:
 *   1. Receive a cloned patient list and a scheduling strategy
 *   2. Execute the algorithm via the strategy interface
 *   3. Set the department type on the result
 *   4. Verify and recalculate metrics
 *   5. Persist the SimulationRun to the database
 *   6. Persist all ExecutionLog entries to the database
 *   7. Return the fully populated AlgorithmResultDTO
 *
 * This class handles ONE algorithm run at a time.
 * DepartmentSimulationEngine calls it TWICE per simulation
 * (once per algorithm) and then compares the two results.
 */
@Component
public class SimulationRunner {

    private final MetricsService           metricsService;
    private final SimulationRepository     simulationRepository;
    private final ExecutionLogRepository   executionLogRepository;
    private final GanttChartBuilder        ganttChartBuilder;

    public SimulationRunner(MetricsService metricsService,
                            SimulationRepository simulationRepository,
                            ExecutionLogRepository executionLogRepository,
                            GanttChartBuilder ganttChartBuilder) {
        this.metricsService         = metricsService;
        this.simulationRepository   = simulationRepository;
        this.executionLogRepository = executionLogRepository;
        this.ganttChartBuilder      = ganttChartBuilder;
    }

    // ── Main Run Method ───────────────────────────────────────

    /**
     * Executes a single scheduling algorithm against a cloned patient list,
     * persists the results to the database, and returns the populated DTO.
     *
     * @param strategy        The scheduling algorithm to run
     * @param clonedPatients  Independent clone of the patient dataset
     * @param originalPatients Original DB-backed patient entities (for FK references in logs)
     * @param departmentType  The department being simulated
     * @return                Fully populated AlgorithmResultDTO with DB IDs set
     */
    public AlgorithmResultDTO run(SchedulingStrategy strategy,
                                  List<Patient> clonedPatients,
                                  List<Patient> originalPatients,
                                  DepartmentType departmentType) {

        System.out.println("[SimulationRunner] Starting: " + strategy.getAlgorithmName()
                + " | Department: " + departmentType
                + " | Patients: " + clonedPatients.size());

        // ── Step 1: Execute the algorithm ─────────────────────
        AlgorithmResultDTO result = strategy.execute(clonedPatients);

        // ── Step 2: Set department type ───────────────────────
        result.setDepartmentType(departmentType);

        // ── Step 3: Verify and recalculate metrics ────────────
        result = metricsService.verifyAndRecalculateMetrics(result);

        // ── Step 4: Build and persist SimulationRun ───────────
        SimulationRun simulationRun = metricsService.buildSimulationRun(result);
        simulationRun = simulationRepository.save(simulationRun);
        result.setSimulationRunId(simulationRun.getId());

        System.out.println("[SimulationRunner] SimulationRun saved with id: "
                + simulationRun.getId());

        // ── Step 5: Build and persist ExecutionLogs ───────────
        List<ExecutionLog> logs = metricsService.buildExecutionLogs(
                result, simulationRun, originalPatients);
        executionLogRepository.saveAll(logs);

        System.out.println("[SimulationRunner] ExecutionLogs saved: " + logs.size() + " entries");

        // ── Step 6: Attach Gantt chart data ───────────────────
        GanttChartBuilder.GanttChartData ganttData =
                ganttChartBuilder.buildFromResult(result);

        // Merge consecutive entries for cleaner visualization
        result.setGanttEntries(
                ganttChartBuilder.mergeConsecutiveEntries(ganttData.getEntries())
        );

        System.out.println("[SimulationRunner] Completed: " + strategy.getAlgorithmName()
                + " | AvgWT=" + result.getAvgWaitingTime()
                + " | AvgTT=" + result.getAvgTurnaroundTime()
                + " | AvgRT=" + result.getAvgResponseTime());

        return result;
    }

    // ── Dry Run (no DB persistence) ───────────────────────────

    /**
     * Executes the algorithm without saving anything to the database.
     * Useful for testing, previewing, or frontend previews before
     * the user confirms they want to save the simulation.
     *
     * @param strategy        The scheduling algorithm to run
     * @param clonedPatients  Independent clone of the patient dataset
     * @param departmentType  The department being simulated
     * @return                AlgorithmResultDTO with metrics and Gantt data (no DB IDs)
     */
    public AlgorithmResultDTO dryRun(SchedulingStrategy strategy,
                                     List<Patient> clonedPatients,
                                     DepartmentType departmentType) {

        System.out.println("[SimulationRunner] DRY RUN: " + strategy.getAlgorithmName());

        AlgorithmResultDTO result = strategy.execute(clonedPatients);
        result.setDepartmentType(departmentType);
        result = metricsService.verifyAndRecalculateMetrics(result);

        GanttChartBuilder.GanttChartData ganttData =
                ganttChartBuilder.buildFromResult(result);
        result.setGanttEntries(
                ganttChartBuilder.mergeConsecutiveEntries(ganttData.getEntries())
        );

        return result;
    }
}