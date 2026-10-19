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
 * First Come First Serve (FCFS) — Non-Preemptive
 *
 * OS Concept  : Processes are executed in the order they arrive in the ready queue.
 * Hospital Use: OPD — patients served by token/arrival order.
 *
 * Logic:
 *   1. Sort patients by arrival time (ascending).
 *   2. Process each patient to completion without interruption.
 *   3. If CPU is idle (current time < next patient arrival), advance time.
 */
@Component("FCFS")
public class FCFSStrategy implements SchedulingStrategy {

    private static final String ALGORITHM_NAME = "FCFS";

    @Override
    public String getAlgorithmName() {
        return ALGORITHM_NAME;
    }

    @Override
    public AlgorithmResultDTO execute(List<Patient> patients) {

        // ── Step 1: Sort by arrival time ──────────────────────
        List<Patient> sorted = new ArrayList<>(patients);
        sorted.sort(Comparator.comparingInt(Patient::getArrivalTime));

        List<GanttEntryDTO>    ganttEntries   = new ArrayList<>();
        List<PatientMetricDTO> patientMetrics = new ArrayList<>();

        int currentTime        = 0;
        int totalWaiting       = 0;
        int totalTurnaround    = 0;
        int totalResponse      = 0;

        // ── Step 2: Process each patient in order ─────────────
        for (Patient patient : sorted) {

            // If CPU is idle, jump to patient arrival
            if (currentTime < patient.getArrivalTime()) {
                currentTime = patient.getArrivalTime();
            }

            int startTime      = currentTime;
            int endTime        = startTime + patient.getBurstTime();

            // Metrics
            int waitingTime    = startTime - patient.getArrivalTime();
            int turnaroundTime = endTime   - patient.getArrivalTime();
            int responseTime   = waitingTime; // FCFS: first response = start of execution

            totalWaiting    += waitingTime;
            totalTurnaround += turnaroundTime;
            totalResponse   += responseTime;

            // Update patient state
            patient.setStatus(PatientStatus.COMPLETED);
            patient.setRemainingTime(0);

            // Gantt entry
            ganttEntries.add(new GanttEntryDTO(
                    patient.getId(), patient.getName(),
                    startTime, endTime, ALGORITHM_NAME
            ));

            // Per-patient metrics
            patientMetrics.add(new PatientMetricDTO(
                    patient.getId(), patient.getName(),
                    patient.getArrivalTime(), patient.getBurstTime(), patient.getPriority(),
                    endTime, waitingTime, turnaroundTime, responseTime
            ));

            currentTime = endTime;
        }

        // ── Step 3: Build result ──────────────────────────────
        int n = sorted.size();

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