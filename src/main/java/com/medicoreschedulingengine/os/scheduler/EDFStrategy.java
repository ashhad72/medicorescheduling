package com.medicoreschedulingengine.os.scheduler;

import com.medicoreschedulingengine.os.model.dto.AlgorithmResultDTO;
import com.medicoreschedulingengine.os.model.dto.GanttEntryDTO;
import com.medicoreschedulingengine.os.model.dto.PatientMetricDTO;
import com.medicoreschedulingengine.os.model.entity.Patient;
import com.medicoreschedulingengine.os.model.enums.PatientStatus;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Earliest Deadline First (EDF) — Preemptive
 *
 * OS Concept  : At every scheduling point, the process with the earliest
 *               deadline gets the CPU. A newly arrived process with a closer
 *               deadline immediately preempts the current process.
 *
 * Hospital Use: ICU — patients whose condition is deteriorating rapidly
 *               (closest deadline) receive immediate attention.
 *               A patient with 2 hours left before critical threshold
 *               preempts one with 5 hours remaining.
 *
 * Deadline Calculation:
 *   Since our Patient entity does not have an explicit deadline field,
 *   we derive it as:
 *
 *       deadline = arrivalTime + burstTime + priority
 *
 *   Reasoning:
 *     - arrivalTime : when the patient entered the system
 *     - burstTime   : how long treatment takes
 *     - priority    : urgency factor — lower priority number = tighter deadline
 *                     (priority 1 patient gets a tighter deadline than priority 5)
 *
 *   This produces a meaningful deadline without adding a new DB field,
 *   and maps cleanly to the OS EDF concept for the simulation.
 *
 * Logic (unit-step simulation):
 *   1. At each time unit, collect all arrived patients with remaining time > 0.
 *   2. Select the one with the earliest (smallest) deadline.
 *   3. Run for 1 time unit.
 *   4. Track context switches and merge Gantt segments.
 */
@Component("EDF")
public class EDFStrategy implements SchedulingStrategy {

    private static final String ALGORITHM_NAME = "EDF";

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
        Map<Long, Integer> deadlineMap    = new HashMap<>();

        // Pre-calculate deadlines for all patients
        for (Patient p : patients) {
            deadlineMap.put(p.getId(), calculateDeadline(p));
        }

        int     currentTime = 0;
        Patient lastPatient = null;
        int     segStart    = 0;

        while (!remaining.isEmpty()) {

            // ── Collect available patients ────────────────────
            final int t = currentTime;
            List<Patient> available = new ArrayList<>();
            for (Patient p : remaining) {
                if (p.getArrivalTime() <= t) available.add(p);
            }

            if (available.isEmpty()) {
                // CPU idle — close any open Gantt segment
                if (lastPatient != null) {
                    ganttEntries.add(new GanttEntryDTO(
                            lastPatient.getId(), lastPatient.getName(),
                            segStart, currentTime, ALGORITHM_NAME));
                    lastPatient = null;
                }
                currentTime++;
                continue;
            }

            // ── Pick patient with earliest deadline ───────────
            // Tie-break 1: shorter remaining time (closer to finishing)
            // Tie-break 2: earlier arrival time
            Patient current = available.stream()
                    .min(Comparator
                            .comparingInt((Patient p) -> deadlineMap.get(p.getId()))
                            .thenComparingInt(Patient::getRemainingTime)
                            .thenComparingInt(Patient::getArrivalTime))
                    .orElseThrow();

            // ── Context switch tracking ───────────────────────
            if (lastPatient == null) {
                segStart = currentTime;
            } else if (!lastPatient.getId().equals(current.getId())) {
                // Different patient taking over — close previous segment
                ganttEntries.add(new GanttEntryDTO(
                        lastPatient.getId(), lastPatient.getName(),
                        segStart, currentTime, ALGORITHM_NAME));
                segStart = currentTime;
            }

            // Record first time this patient touched the CPU
            firstExecution.putIfAbsent(current.getId(), currentTime);

            // ── Execute 1 time unit ───────────────────────────
            current.setRemainingTime(current.getRemainingTime() - 1);
            currentTime++;
            lastPatient = current;

            // ── Check if patient finished ─────────────────────
            if (current.getRemainingTime() == 0) {
                // Close the Gantt segment
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

            // Guard against negative values
            wt = Math.max(0, wt);
            rt = Math.max(0, rt);

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

    // ── Deadline Formula ──────────────────────────────────────

    /**
     * Derives a deadline for each patient from existing fields.
     *
     * deadline = arrivalTime + burstTime + priority
     *
     * Lower priority number = more critical = tighter deadline.
     * Example:
     *   Patient A: arrival=0, burst=5, priority=1 → deadline = 6
     *   Patient B: arrival=0, burst=5, priority=4 → deadline = 9
     *   EDF picks Patient A first (deadline 6 < 9).
     *
     * @param patient the patient to compute deadline for
     * @return        integer deadline value
     */
    private int calculateDeadline(Patient patient) {
        return patient.getArrivalTime() + patient.getBurstTime() + patient.getPriority();
    }
}