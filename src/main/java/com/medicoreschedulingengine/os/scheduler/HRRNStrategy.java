package com.medicoreschedulingengine.os.scheduler;

import com.medicoreschedulingengine.os.model.dto.AlgorithmResultDTO;
import com.medicoreschedulingengine.os.model.dto.GanttEntryDTO;
import com.medicoreschedulingengine.os.model.dto.PatientMetricDTO;
import com.medicoreschedulingengine.os.model.entity.Patient;
import com.medicoreschedulingengine.os.model.enums.PatientStatus;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Highest Response Ratio Next (HRRN) — Non-Preemptive
 *
 * OS Concept  : A non-preemptive algorithm that selects the process with the
 *               highest response ratio. This prevents starvation of long jobs
 *               because their ratio grows the longer they wait.
 *
 * Formula:
 *   Response Ratio = (Waiting Time + Burst Time) / Burst Time
 *                  = 1 + (Waiting Time / Burst Time)
 *
 * Hospital Use: Surgery — long-waiting surgical patients eventually gain
 *               priority over newly arrived shorter procedures.
 *               Minor surgeries still run first when waiting times are equal,
 *               but a long-waiting major surgery will eventually win.
 *
 * Logic:
 *   1. At each scheduling point, calculate the response ratio for every
 *      available (arrived) patient.
 *   2. Select the patient with the highest ratio.
 *   3. Run to completion (non-preemptive).
 *   4. Recalculate ratios at the next scheduling point.
 *
 * Note: When a patient just arrives (waitingTime = 0), ratio = 1.0.
 *       Short-burst patients naturally have higher ratios early on,
 *       but long-waiting patients catch up as their waiting time grows.
 */
@Component("HRRN")
public class HRRNStrategy implements SchedulingStrategy {

    private static final String ALGORITHM_NAME = "HRRN";

    @Override
    public String getAlgorithmName() {
        return ALGORITHM_NAME;
    }

    @Override
    public AlgorithmResultDTO execute(List<Patient> patients) {

        List<Patient> remaining    = new ArrayList<>(patients);
        List<GanttEntryDTO>    ganttEntries   = new ArrayList<>();
        List<PatientMetricDTO> patientMetrics = new ArrayList<>();

        int totalWaiting    = 0;
        int totalTurnaround = 0;
        int totalResponse   = 0;
        int currentTime     = 0;

        while (!remaining.isEmpty()) {

            // ── Collect arrived patients ──────────────────────
            final int t = currentTime;
            List<Patient> available = new ArrayList<>();
            for (Patient p : remaining) {
                if (p.getArrivalTime() <= t) available.add(p);
            }

            if (available.isEmpty()) {
                // CPU idle — jump to next arrival
                currentTime = remaining.stream()
                        .mapToInt(Patient::getArrivalTime)
                        .min()
                        .orElse(currentTime + 1);
                continue;
            }

            // ── Calculate response ratio for each available patient ──
            Patient selected    = null;
            double  highestRatio = -1.0;

            for (Patient p : available) {
                int    waitingTime    = currentTime - p.getArrivalTime();
                double responseRatio  = calculateResponseRatio(waitingTime, p.getBurstTime());

                if (responseRatio > highestRatio) {
                    highestRatio = responseRatio;
                    selected     = p;
                } else if (responseRatio == highestRatio && selected != null) {
                    // Tie-break: shorter burst time wins (favour throughput)
                    if (p.getBurstTime() < selected.getBurstTime()) {
                        selected = p;
                    }
                }
            }

            if (selected == null) continue;

            // ── Execute selected patient to completion ─────────
            int startTime      = currentTime;
            int endTime        = startTime + selected.getBurstTime();
            int waitingTime    = startTime - selected.getArrivalTime();
            int turnaroundTime = endTime   - selected.getArrivalTime();
            int responseTime   = waitingTime; // non-preemptive: first response = start

            double finalRatio  = calculateResponseRatio(waitingTime, selected.getBurstTime());

            totalWaiting    += waitingTime;
            totalTurnaround += turnaroundTime;
            totalResponse   += responseTime;

            selected.setStatus(PatientStatus.COMPLETED);
            selected.setRemainingTime(0);

            // ── Gantt entry ───────────────────────────────────
            ganttEntries.add(new GanttEntryDTO(
                    selected.getId(), selected.getName(),
                    startTime, endTime, ALGORITHM_NAME
            ));

            // ── Per-patient metrics ───────────────────────────
            patientMetrics.add(new PatientMetricDTO(
                    selected.getId(), selected.getName(),
                    selected.getArrivalTime(), selected.getBurstTime(), selected.getPriority(),
                    endTime, waitingTime, turnaroundTime, responseTime
            ));

            currentTime = endTime;
            remaining.remove(selected);
        }

        // ── Build result ──────────────────────────────────────
        int n = patients.size();

        AlgorithmResultDTO result = new AlgorithmResultDTO();
        result.setAlgorithmName(ALGORITHM_NAME);
        result.setTotalPatients(n);
        result.setAvgWaitingTime(n == 0 ? 0 : (double) totalWaiting    / n);
        result.setAvgTurnaroundTime(n == 0 ? 0 : (double) totalTurnaround / n);
        result.setAvgResponseTime(n == 0 ? 0 : (double) totalResponse   / n);
        result.setPatientMetrics(patientMetrics);
        result.setGanttEntries(ganttEntries);

        return result;
    }

    // ── Formula ───────────────────────────────────────────────

    /**
     * Calculates HRRN response ratio.
     *
     * Formula: (waitingTime + burstTime) / burstTime
     *
     * @param waitingTime  Time the patient has been waiting (currentTime - arrivalTime)
     * @param burstTime    Total treatment time required
     * @return             Response ratio (always >= 1.0)
     */
    private double calculateResponseRatio(int waitingTime, int burstTime) {
        if (burstTime <= 0) return 1.0;
        return (double) (waitingTime + burstTime) / burstTime;
    }
}