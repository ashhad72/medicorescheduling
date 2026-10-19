package com.medicoreschedulingengine.os.scheduler;

import com.medicoreschedulingengine.os.model.dto.AlgorithmResultDTO;
import com.medicoreschedulingengine.os.model.dto.GanttEntryDTO;
import com.medicoreschedulingengine.os.model.dto.PatientMetricDTO;
import com.medicoreschedulingengine.os.model.entity.Patient;
import com.medicoreschedulingengine.os.model.enums.PatientStatus;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Shortest Remaining Time First (SRTF) — Preemptive
 *
 * OS Concept  : At every time unit, the process with the shortest remaining
 *               burst time runs. A new arrival can preempt the current process.
 * Hospital Use: ICU — quickly stabilise patients needing only minor intervention.
 *               A patient requiring less treatment always gets priority.
 *
 * Logic (unit-step simulation):
 *   1. At each time unit, pick the available patient with lowest remainingTime.
 *   2. Run it for 1 unit.
 *   3. If a different patient takes over, record a Gantt segment for the switch.
 *   4. Merge consecutive Gantt entries for the same patient into one block.
 */
@Component("SRTF")
public class SRTFStrategy implements SchedulingStrategy {

    private static final String ALGORITHM_NAME = "SRTF";

    @Override
    public String getAlgorithmName() {
        return ALGORITHM_NAME;
    }

    @Override
    public AlgorithmResultDTO execute(List<Patient> patients) {

        // Reset remaining times
        patients.forEach(p -> p.setRemainingTime(p.getBurstTime()));

        List<Patient> remaining = new ArrayList<>(patients);
        List<GanttEntryDTO>    ganttEntries   = new ArrayList<>();
        List<PatientMetricDTO> patientMetrics = new ArrayList<>();

        Map<Long, Integer> firstExecution = new HashMap<>();
        Map<Long, Integer> completionTime = new HashMap<>();

        int currentTime  = 0;
        int totalTime    = patients.stream().mapToInt(Patient::getBurstTime).sum()
                + patients.stream().mapToInt(Patient::getArrivalTime).max().orElse(0);

        Patient lastPatient = null;
        int     segStart    = 0;

        while (!remaining.isEmpty()) {

            // ── Available patients at currentTime ─────────────
            final int t = currentTime;
            List<Patient> available = new ArrayList<>();
            for (Patient p : remaining) {
                if (p.getArrivalTime() <= t) available.add(p);
            }

            if (available.isEmpty()) {
                // CPU idle
                if (lastPatient != null) {
                    ganttEntries.add(new GanttEntryDTO(
                            lastPatient.getId(), lastPatient.getName(),
                            segStart, currentTime, ALGORITHM_NAME));
                    lastPatient = null;
                }
                currentTime++;
                continue;
            }

            // ── Pick patient with shortest remaining time ─────
            Patient current = available.stream()
                    .min(Comparator.comparingInt(Patient::getRemainingTime)
                            .thenComparingInt(Patient::getArrivalTime))
                    .orElseThrow();

            // ── Track context switches for Gantt ──────────────
            if (lastPatient == null) {
                segStart = currentTime;
            } else if (!lastPatient.getId().equals(current.getId())) {
                ganttEntries.add(new GanttEntryDTO(
                        lastPatient.getId(), lastPatient.getName(),
                        segStart, currentTime, ALGORITHM_NAME));
                segStart = currentTime;
            }

            // Record first execution
            firstExecution.putIfAbsent(current.getId(), currentTime);

            // ── Execute 1 time unit ───────────────────────────
            current.setRemainingTime(current.getRemainingTime() - 1);
            currentTime++;
            lastPatient = current;

            if (current.getRemainingTime() == 0) {
                // Patient finished
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