package com.medicoreschedulingengine.os.scheduler;

import com.medicoreschedulingengine.os.model.dto.AlgorithmResultDTO;
import com.medicoreschedulingengine.os.model.dto.GanttEntryDTO;
import com.medicoreschedulingengine.os.model.dto.PatientMetricDTO;
import com.medicoreschedulingengine.os.model.entity.Patient;
import com.medicoreschedulingengine.os.model.enums.PatientStatus;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Round Robin (RR) — Preemptive
 *
 * OS Concept  : Each process gets a fixed time quantum on the CPU.
 *               If not finished, it goes back to the end of the ready queue.
 * Hospital Use: OPD — doctor spends limited time with each patient before moving on.
 *
 * Logic:
 *   1. Sort patients by arrival time.
 *   2. Maintain a ready queue. Add patients as they arrive.
 *   3. Each patient runs for min(remainingTime, quantum) time units.
 *   4. If remaining time > 0 after quantum, re-add to end of queue.
 *   5. Track first execution time per patient for response time.
 */
@Component("ROUND_ROBIN")
public class RoundRobinStrategy implements SchedulingStrategy {

    private static final String ALGORITHM_NAME = "ROUND_ROBIN";
    private static final int    DEFAULT_QUANTUM = 3; // configurable time quantum

    private final int timeQuantum;

    public RoundRobinStrategy() {
        this.timeQuantum = DEFAULT_QUANTUM;
    }

    public RoundRobinStrategy(int timeQuantum) {
        this.timeQuantum = timeQuantum;
    }

    @Override
    public String getAlgorithmName() {
        return ALGORITHM_NAME;
    }

    @Override
    public AlgorithmResultDTO execute(List<Patient> patients) {

        // ── Step 1: Sort by arrival time ──────────────────────
        List<Patient> sorted = new ArrayList<>(patients);
        sorted.sort(Comparator.comparingInt(Patient::getArrivalTime));

        // Reset remaining times
        sorted.forEach(p -> p.setRemainingTime(p.getBurstTime()));

        List<GanttEntryDTO>    ganttEntries   = new ArrayList<>();
        Map<Long, Integer>     firstExecution = new HashMap<>();  // patientId -> first start time
        Map<Long, Integer>     completionTime = new HashMap<>();
        Queue<Patient>         readyQueue     = new LinkedList<>();

        int currentTime = 0;
        int index       = 0; // pointer into sorted list for arrivals

        // ── Step 2: Main scheduling loop ─────────────────────
        while (index < sorted.size() || !readyQueue.isEmpty()) {

            // Add all patients that have arrived by currentTime
            while (index < sorted.size()
                    && sorted.get(index).getArrivalTime() <= currentTime) {
                readyQueue.add(sorted.get(index));
                index++;
            }

            if (readyQueue.isEmpty()) {
                // CPU idle — jump to next arrival
                currentTime = sorted.get(index).getArrivalTime();
                continue;
            }

            Patient current = readyQueue.poll();

            // Record first execution time (for response time)
            firstExecution.putIfAbsent(current.getId(), currentTime);

            int executeFor = Math.min(current.getRemainingTime(), timeQuantum);
            int startTime  = currentTime;
            int endTime    = currentTime + executeFor;

            // Add Gantt entry for this slice
            ganttEntries.add(new GanttEntryDTO(
                    current.getId(), current.getName(),
                    startTime, endTime, ALGORITHM_NAME
            ));

            current.setRemainingTime(current.getRemainingTime() - executeFor);
            currentTime = endTime;

            // Add newly arrived patients before re-queuing current
            while (index < sorted.size()
                    && sorted.get(index).getArrivalTime() <= currentTime) {
                readyQueue.add(sorted.get(index));
                index++;
            }

            if (current.getRemainingTime() > 0) {
                // Not finished — send back to end of queue
                readyQueue.add(current);
            } else {
                // Finished
                current.setStatus(PatientStatus.COMPLETED);
                completionTime.put(current.getId(), endTime);
            }
        }

        // ── Step 3: Calculate metrics ─────────────────────────
        List<PatientMetricDTO> patientMetrics = new ArrayList<>();
        int totalWaiting    = 0;
        int totalTurnaround = 0;
        int totalResponse   = 0;

        for (Patient patient : sorted) {
            int ct  = completionTime.getOrDefault(patient.getId(), 0);
            int tt  = ct - patient.getArrivalTime();
            int wt  = tt - patient.getBurstTime();
            int rt  = firstExecution.getOrDefault(patient.getId(), 0)
                    - patient.getArrivalTime();

            totalWaiting    += wt;
            totalTurnaround += tt;
            totalResponse   += rt;

            patientMetrics.add(new PatientMetricDTO(
                    patient.getId(), patient.getName(),
                    patient.getArrivalTime(), patient.getBurstTime(), patient.getPriority(),
                    ct, wt, tt, rt
            ));
        }

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