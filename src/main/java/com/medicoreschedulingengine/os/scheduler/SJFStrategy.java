package com.medicoreschedulingengine.os.scheduler;

import com.medicoreschedulingengine.os.model.dto.AlgorithmResultDTO;
import com.medicoreschedulingengine.os.model.dto.GanttEntryDTO;
import com.medicoreschedulingengine.os.model.dto.PatientMetricDTO;
import com.medicoreschedulingengine.os.model.entity.Patient;
import com.medicoreschedulingengine.os.model.enums.PatientStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Shortest Job First (SJF) — Non-Preemptive
 *
 * OS Concept  : Among all available processes, the one with the shortest
 *               burst time is selected next. Once started, it runs to completion.
 * Hospital Use: Surgery — shortest procedures completed first to maximise throughput.
 *
 * Logic:
 *   1. At each scheduling point, collect all patients that have arrived.
 *   2. Pick the one with the smallest burst time.
 *   3. Run it to completion.
 *   4. Repeat.
 */
@Component("SJF")
public class SJFStrategy implements SchedulingStrategy {

    private static final String ALGORITHM_NAME = "SJF";

    @Override
    public String getAlgorithmName() {
        return ALGORITHM_NAME;
    }

    @Override
    public AlgorithmResultDTO execute(List<Patient> patients) {

        List<Patient> remaining    = new ArrayList<>(patients);
        List<Patient> completed    = new ArrayList<>();
        List<GanttEntryDTO>    ganttEntries   = new ArrayList<>();
        List<PatientMetricDTO> patientMetrics = new ArrayList<>();

        int currentTime     = 0;
        int totalWaiting    = 0;
        int totalTurnaround = 0;
        int totalResponse   = 0;

        while (!remaining.isEmpty()) {

            // ── Collect all arrived patients ──────────────────
            final int time = currentTime;
            List<Patient> available = remaining.stream()
                    .filter(p -> p.getArrivalTime() <= time)
                    .collect(java.util.stream.Collectors.toList());

            if (available.isEmpty()) {
                // CPU idle — jump to nearest arrival
                currentTime = remaining.stream()
                        .mapToInt(Patient::getArrivalTime)
                        .min()
                        .orElse(currentTime);
                continue;
            }

            // ── Pick shortest burst time ──────────────────────
            // Tie-break: earlier arrival time wins
            Patient selected = available.stream()
                    .min(Comparator.comparingInt(Patient::getBurstTime)
                            .thenComparingInt(Patient::getArrivalTime))
                    .orElseThrow();

            int startTime      = currentTime;
            int endTime        = startTime + selected.getBurstTime();
            int waitingTime    = startTime - selected.getArrivalTime();
            int turnaroundTime = endTime   - selected.getArrivalTime();
            int responseTime   = waitingTime; // non-preemptive: first response = start

            totalWaiting    += waitingTime;
            totalTurnaround += turnaroundTime;
            totalResponse   += responseTime;

            selected.setStatus(PatientStatus.COMPLETED);
            selected.setRemainingTime(0);

            ganttEntries.add(new GanttEntryDTO(
                    selected.getId(), selected.getName(),
                    startTime, endTime, ALGORITHM_NAME
            ));

            patientMetrics.add(new PatientMetricDTO(
                    selected.getId(), selected.getName(),
                    selected.getArrivalTime(), selected.getBurstTime(), selected.getPriority(),
                    endTime, waitingTime, turnaroundTime, responseTime
            ));

            currentTime = endTime;
            remaining.remove(selected);
            completed.add(selected);
        }

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
}