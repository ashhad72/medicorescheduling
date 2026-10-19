package com.medicoreschedulingengine.os.scheduler;

import com.medicoreschedulingengine.os.model.dto.AlgorithmResultDTO;
import com.medicoreschedulingengine.os.model.dto.GanttEntryDTO;
import com.medicoreschedulingengine.os.model.dto.PatientMetricDTO;
import com.medicoreschedulingengine.os.model.entity.Patient;
import com.medicoreschedulingengine.os.model.enums.PatientStatus;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Preemptive Priority Scheduling
 *
 * OS Concept  : The process with the highest priority (lowest priority number)
 *               always runs. A higher-priority arrival immediately preempts
 *               the current process.
 * Hospital Use: ER — a cardiac arrest patient (priority 1) instantly interrupts
 *               treatment of a lower-priority patient.
 *
 * Convention  : priority 1 = most critical, higher number = less critical.
 *
 * Logic (unit-step simulation):
 *   1. At each time unit, pick the available patient with the lowest priority number.
 *   2. Run for 1 unit.
 *   3. Track context switches and merge Gantt segments accordingly.
 */
@Component("PREEMPTIVE_PRIORITY")
public class PrioritySchedulingStrategy implements SchedulingStrategy {

    private static final String ALGORITHM_NAME = "PREEMPTIVE_PRIORITY";

    @Override
    public String getAlgorithmName() {
        return ALGORITHM_NAME;
    }

    @Override
    public AlgorithmResultDTO execute(List<Patient> patients) {

        // Reset remaining times
        patients.forEach(p -> p.setRemainingTime(p.getBurstTime()));

        List<Patient> remaining    = new ArrayList<>(patients);
        List<GanttEntryDTO>    ganttEntries   = new ArrayList<>();
        List<PatientMetricDTO> patientMetrics = new ArrayList<>();

        Map<Long, Integer> firstExecution = new HashMap<>();
        Map<Long, Integer> completionTime = new HashMap<>();

        int     currentTime = 0;
        Patient lastPatient = null;
        int     segStart    = 0;

        while (!remaining.isEmpty()) {

            // ── Available patients ────────────────────────────
            final int t = currentTime;
            List<Patient> available = new ArrayList<>();
            for (Patient p : remaining) {
                if (p.getArrivalTime() <= t) available.add(p);
            }

            if (available.isEmpty()) {
                if (lastPatient != null) {
                    ganttEntries.add(new GanttEntryDTO(
                            lastPatient.getId(), lastPatient.getName(),
                            segStart, currentTime, ALGORITHM_NAME));
                    lastPatient = null;
                }
                currentTime++;
                continue;
            }

            // ── Pick highest priority (lowest number) ─────────
            // Tie-break: lower arrival time wins
            Patient current = available.stream()
                    .min(Comparator.comparingInt(Patient::getPriority)
                            .thenComparingInt(Patient::getArrivalTime))
                    .orElseThrow();

            // ── Context switch tracking ───────────────────────
            if (lastPatient == null) {
                segStart = currentTime;
            } else if (!lastPatient.getId().equals(current.getId())) {
                ganttEntries.add(new GanttEntryDTO(
                        lastPatient.getId(), lastPatient.getName(),
                        segStart, currentTime, ALGORITHM_NAME));
                segStart = currentTime;
            }

            firstExecution.putIfAbsent(current.getId(), currentTime);

            // ── Execute 1 time unit ───────────────────────────
            current.setRemainingTime(current.getRemainingTime() - 1);
            currentTime++;
            lastPatient = current;

            if (current.getRemainingTime() == 0) {
                ganttEntries.add(new GanttEntryDTO(
                        current.getId(), current.getName(),
                        segStart, currentTime, ALGORITHM_NAME));
                lastPatient = null;
                segStart    = currentTime;

                current.setStatus(PatientStatus.COMPLETED);
                completionTime.put(current.getId(), currentTime);
                remaining.remove(current);
            }
        }

        // ── Calculate metrics ─────────────────────────────────
        int totalWaiting    = 0;
        int totalTurnaround = 0;
        int totalResponse   = 0;

        for (Patient patient : patients) {
            int ct = completionTime.getOrDefault(patient.getId(), 0);
            int tt = ct - patient.getArrivalTime();
            int wt = tt - patient.getBurstTime();
            int rt = firstExecution.getOrDefault(patient.getId(), 0)
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