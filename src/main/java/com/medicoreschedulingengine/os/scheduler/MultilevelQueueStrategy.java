package com.medicoreschedulingengine.os.scheduler;

import com.medicoreschedulingengine.os.model.dto.AlgorithmResultDTO;
import com.medicoreschedulingengine.os.model.dto.GanttEntryDTO;
import com.medicoreschedulingengine.os.model.dto.PatientMetricDTO;
import com.medicoreschedulingengine.os.model.entity.Patient;
import com.medicoreschedulingengine.os.model.enums.PatientStatus;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Multilevel Queue Scheduling (MLQ)
 *
 * OS Concept  : Processes are permanently assigned to a queue based on a
 *               property (here: priority). Each queue has its own scheduling
 *               algorithm. Higher queues are always fully served before
 *               lower queues get any CPU time.
 *
 * Hospital Use: ER triage — separate zones for Critical, Urgent, and Normal
 *               patients. Critical zone is always cleared first.
 *
 * Queue Structure (3 levels):
 *   Level 1 — CRITICAL  (priority 1)     → Preemptive Priority (FCFS within level)
 *   Level 2 — URGENT    (priority 2-3)   → Round Robin (quantum = 3)
 *   Level 3 — NORMAL    (priority 4+)    → FCFS
 *
 * Rules:
 *   - A patient NEVER moves between queues once assigned.
 *   - Level 1 is always checked first. If any Level 1 patient is available,
 *     lower levels do NOT run.
 *   - Starvation of lower levels is acceptable in ER context (by design).
 *
 * Logic:
 *   1. Classify each patient into a queue level based on priority.
 *   2. At each scheduling point, pick the highest non-empty level with
 *      available patients.
 *   3. Run one time unit (unit-step simulation for accurate preemption).
 *   4. Track Gantt segments across context switches.
 */
@Component("MULTILEVEL_QUEUE")
public class MultilevelQueueStrategy implements SchedulingStrategy {

    private static final String ALGORITHM_NAME  = "MULTILEVEL_QUEUE";
    private static final int    RR_QUANTUM       = 3;

    // ── Queue level thresholds ────────────────────────────────
    private static final int CRITICAL_MAX_PRIORITY = 1;
    private static final int URGENT_MAX_PRIORITY   = 3;
    // priority 4+ → NORMAL

    @Override
    public String getAlgorithmName() {
        return ALGORITHM_NAME;
    }

    @Override
    public AlgorithmResultDTO execute(List<Patient> patients) {

        // Reset remaining times
        patients.forEach(p -> p.setRemainingTime(p.getBurstTime()));

        // ── Classify patients into three queues ───────────────
        // Each queue is a LinkedList to support FCFS and RR naturally.
        // We store patient + their individual RR quantum counter.
        List<Patient> criticalQueue = new ArrayList<>();
        List<Patient> urgentQueue   = new ArrayList<>();
        List<Patient> normalQueue   = new ArrayList<>();

        for (Patient p : patients) {
            if (p.getPriority() <= CRITICAL_MAX_PRIORITY) {
                criticalQueue.add(p);
            } else if (p.getPriority() <= URGENT_MAX_PRIORITY) {
                urgentQueue.add(p);
            } else {
                normalQueue.add(p);
            }
        }

        List<GanttEntryDTO>    ganttEntries   = new ArrayList<>();
        List<PatientMetricDTO> patientMetrics = new ArrayList<>();

        Map<Long, Integer> firstExecution  = new HashMap<>();
        Map<Long, Integer> completionTimes = new HashMap<>();

        // Remaining sets per level (patients not yet completed)
        List<Patient> criticalRemaining = new ArrayList<>(criticalQueue);
        List<Patient> urgentRemaining   = new ArrayList<>(urgentQueue);
        List<Patient> normalRemaining   = new ArrayList<>(normalQueue);

        // RR quantum tracker for urgent queue
        Map<Long, Integer> rrQuantumUsed = new HashMap<>();

        int     currentTime = 0;
        Patient lastPatient = null;
        int     segStart    = 0;

        int totalRemaining = patients.size();

        while (totalRemaining > 0) {

            final int t = currentTime;

            // ── Find available patients per level ─────────────
            List<Patient> availCritical = getAvailable(criticalRemaining, t);
            List<Patient> availUrgent   = getAvailable(urgentRemaining,   t);
            List<Patient> availNormal   = getAvailable(normalRemaining,   t);

            Patient current = null;
            int     level   = 0;

            if (!availCritical.isEmpty()) {
                // Level 1: FCFS within critical — earliest arrival
                current = availCritical.stream()
                        .min(Comparator.comparingInt(Patient::getArrivalTime))
                        .orElseThrow();
                level = 1;

            } else if (!availUrgent.isEmpty()) {
                // Level 2: Round Robin — pick patient whose RR slot is due
                // We simulate RR by tracking how many units each patient
                // has used in the current quantum.
                current = getRRPatient(availUrgent, rrQuantumUsed);
                level   = 2;

            } else if (!availNormal.isEmpty()) {
                // Level 3: FCFS within normal
                current = availNormal.stream()
                        .min(Comparator.comparingInt(Patient::getArrivalTime))
                        .orElseThrow();
                level = 3;

            } else {
                // All queues empty or no patient has arrived yet — CPU idle
                if (lastPatient != null) {
                    ganttEntries.add(new GanttEntryDTO(
                            lastPatient.getId(), lastPatient.getName(),
                            segStart, currentTime, ALGORITHM_NAME));
                    lastPatient = null;
                }
                currentTime++;
                continue;
            }

            // ── Context switch detection ──────────────────────
            if (lastPatient == null) {
                segStart = currentTime;
            } else if (!lastPatient.getId().equals(current.getId())) {
                ganttEntries.add(new GanttEntryDTO(
                        lastPatient.getId(), lastPatient.getName(),
                        segStart, currentTime, ALGORITHM_NAME));
                segStart = currentTime;

                // If switching away from an urgent patient mid-quantum, reset its counter
                if (level != 2) {
                    rrQuantumUsed.remove(lastPatient.getId());
                }
            }

            firstExecution.putIfAbsent(current.getId(), currentTime);

            // ── Execute 1 time unit ───────────────────────────
            current.setRemainingTime(current.getRemainingTime() - 1);
            currentTime++;
            lastPatient = current;

            // Track RR quantum usage for urgent level
            if (level == 2) {
                rrQuantumUsed.merge(current.getId(), 1, Integer::sum);
                // If quantum exhausted and patient still has work, rotate it
                if (rrQuantumUsed.get(current.getId()) >= RR_QUANTUM
                        && current.getRemainingTime() > 0) {
                    rrQuantumUsed.remove(current.getId());
                    // Move to end of urgent queue (rotate)
                    urgentRemaining.remove(current);
                    urgentRemaining.add(current);
                    // Force segment close on next iteration
                    ganttEntries.add(new GanttEntryDTO(
                            current.getId(), current.getName(),
                            segStart, currentTime, ALGORITHM_NAME));
                    segStart    = currentTime;
                    lastPatient = null;
                }
            }

            // ── Check completion ──────────────────────────────
            if (current.getRemainingTime() == 0) {
                if (lastPatient != null) {
                    ganttEntries.add(new GanttEntryDTO(
                            current.getId(), current.getName(),
                            segStart, currentTime, ALGORITHM_NAME));
                    lastPatient = null;
                    segStart    = currentTime;
                }

                current.setStatus(PatientStatus.COMPLETED);
                completionTimes.put(current.getId(), currentTime);
                rrQuantumUsed.remove(current.getId());

                criticalRemaining.remove(current);
                urgentRemaining.remove(current);
                normalRemaining.remove(current);

                totalRemaining--;
            }
        }

        // ── Calculate metrics ─────────────────────────────────
        int totalWaiting    = 0;
        int totalTurnaround = 0;
        int totalResponse   = 0;

        for (Patient patient : patients) {
            int ct = completionTimes.getOrDefault(patient.getId(), 0);
            int tt = ct - patient.getArrivalTime();
            int wt = tt - patient.getBurstTime();
            int rt = firstExecution.getOrDefault(patient.getId(), 0)
                    - patient.getArrivalTime();

            // Guard against negative values due to edge cases
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

    // ── Helpers ───────────────────────────────────────────────

    /**
     * Returns patients from a queue whose arrival time <= currentTime.
     */
    private List<Patient> getAvailable(List<Patient> queue, int currentTime) {
        List<Patient> available = new ArrayList<>();
        for (Patient p : queue) {
            if (p.getArrivalTime() <= currentTime) available.add(p);
        }
        return available;
    }

    /**
     * For the urgent (RR) queue, picks the patient who has used the
     * fewest quantum units in the current slice — simulating RR rotation.
     * If tied, earliest arrival wins.
     */
    private Patient getRRPatient(List<Patient> available, Map<Long, Integer> rrQuantumUsed) {
        return available.stream()
                .min(Comparator.comparingInt(
                                (Patient p) -> rrQuantumUsed.getOrDefault(p.getId(), 0))
                        .thenComparingInt(Patient::getArrivalTime))
                .orElseThrow();
    }
}