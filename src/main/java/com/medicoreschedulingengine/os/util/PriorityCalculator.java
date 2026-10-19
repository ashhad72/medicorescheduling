package com.medicoreschedulingengine.os.util;

import com.medicoreschedulingengine.os.model.entity.Patient;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PriorityCalculator
 *
 * Utility class for priority-related calculations used across:
 *   - PrioritySchedulingStrategy  (ER preemptive priority)
 *   - MultilevelQueueStrategy     (ER queue classification)
 *   - EDFStrategy                 (ICU deadline derivation)
 *
 * Priority Convention:
 *   Priority 1 = Most critical  (highest urgency)
 *   Priority 2 = High urgency
 *   Priority 3 = Moderate urgency
 *   Priority 4+ = Normal / low urgency
 *
 * Queue Level Classification (used by MLQ):
 *   CRITICAL  → priority == 1
 *   URGENT    → priority 2–3
 *   NORMAL    → priority 4+
 */
@Component
public class PriorityCalculator {

    // ── Queue Level Constants ─────────────────────────────────

    public static final int CRITICAL_PRIORITY_MAX = 1;
    public static final int URGENT_PRIORITY_MAX   = 3;
    // priority 4+ = NORMAL

    public static final String LEVEL_CRITICAL = "CRITICAL";
    public static final String LEVEL_URGENT   = "URGENT";
    public static final String LEVEL_NORMAL   = "NORMAL";

    // ── Queue Classification ──────────────────────────────────

    /**
     * Returns the MLQ queue level label for a given priority value.
     *
     * @param priority  Patient priority number (1 = most critical)
     * @return          "CRITICAL", "URGENT", or "NORMAL"
     */
    public String getQueueLevel(int priority) {
        if (priority <= CRITICAL_PRIORITY_MAX) return LEVEL_CRITICAL;
        if (priority <= URGENT_PRIORITY_MAX)   return LEVEL_URGENT;
        return LEVEL_NORMAL;
    }

    /**
     * Returns the MLQ queue level label for a patient.
     *
     * @param patient  The patient to classify
     * @return         "CRITICAL", "URGENT", or "NORMAL"
     */
    public String getQueueLevelForPatient(Patient patient) {
        return getQueueLevel(patient.getPriority());
    }

    /**
     * Returns true if the patient is classified as critical (priority 1).
     *
     * @param patient  The patient to check
     * @return         true if critical
     */
    public boolean isCritical(Patient patient) {
        return patient.getPriority() <= CRITICAL_PRIORITY_MAX;
    }

    /**
     * Returns true if the patient is classified as urgent (priority 2–3).
     *
     * @param patient  The patient to check
     * @return         true if urgent
     */
    public boolean isUrgent(Patient patient) {
        return patient.getPriority() > CRITICAL_PRIORITY_MAX
                && patient.getPriority() <= URGENT_PRIORITY_MAX;
    }

    /**
     * Returns true if the patient is classified as normal (priority 4+).
     *
     * @param patient  The patient to check
     * @return         true if normal
     */
    public boolean isNormal(Patient patient) {
        return patient.getPriority() > URGENT_PRIORITY_MAX;
    }

    // ── EDF Deadline Derivation ───────────────────────────────

    /**
     * Derives an EDF deadline for a patient from existing fields.
     *
     * Formula: deadline = arrivalTime + burstTime + priority
     *
     * Lower priority number = more critical = tighter deadline.
     *
     * Example:
     *   Patient A: arrival=0, burst=5, priority=1 → deadline=6  (critical)
     *   Patient B: arrival=0, burst=5, priority=4 → deadline=9  (normal)
     *   EDF picks Patient A first.
     *
     * @param patient  The patient to compute deadline for
     * @return         Integer deadline value
     */
    public int calculateEDFDeadline(Patient patient) {
        return patient.getArrivalTime() + patient.getBurstTime() + patient.getPriority();
    }

    // ── Sorting Helpers ───────────────────────────────────────

    /**
     * Sorts patients by priority ascending (most critical first).
     * Tie-break: earlier arrival time wins.
     *
     * @param patients  List of patients to sort
     * @return          New sorted list (original list unchanged)
     */
    public List<Patient> sortByPriority(List<Patient> patients) {
        return patients.stream()
                .sorted(Comparator
                        .comparingInt(Patient::getPriority)
                        .thenComparingInt(Patient::getArrivalTime))
                .collect(Collectors.toList());
    }

    /**
     * Sorts patients by EDF deadline ascending (earliest deadline first).
     * Tie-break: shorter remaining time, then earlier arrival.
     *
     * @param patients  List of patients to sort
     * @return          New sorted list (original list unchanged)
     */
    public List<Patient> sortByDeadline(List<Patient> patients) {
        return patients.stream()
                .sorted(Comparator
                        .comparingInt(this::calculateEDFDeadline)
                        .thenComparingInt(Patient::getRemainingTime)
                        .thenComparingInt(Patient::getArrivalTime))
                .collect(Collectors.toList());
    }

    /**
     * Filters patients from a list whose priority is at or above
     * a given threshold (i.e. priority number <= maxPriority).
     *
     * @param patients     Full patient list
     * @param maxPriority  Maximum priority number to include
     * @return             Filtered list
     */
    public List<Patient> filterByMaxPriority(List<Patient> patients, int maxPriority) {
        return patients.stream()
                .filter(p -> p.getPriority() <= maxPriority)
                .collect(Collectors.toList());
    }

    /**
     * Returns the most critical patient (lowest priority number) from a list.
     * Tie-break: earlier arrival time.
     *
     * @param patients  List of available patients
     * @return          Most critical patient
     */
    public Patient getMostCriticalPatient(List<Patient> patients) {
        if (patients == null || patients.isEmpty()) {
            throw new IllegalArgumentException("Patient list cannot be empty.");
        }
        return patients.stream()
                .min(Comparator
                        .comparingInt(Patient::getPriority)
                        .thenComparingInt(Patient::getArrivalTime))
                .orElseThrow(() -> new IllegalStateException("Could not determine most critical patient."));
    }

    // ── Priority Aging ────────────────────────────────────────

    /**
     * Applies priority aging to prevent starvation.
     *
     * In standard OS scheduling, a process that has been waiting too long
     * gets its priority boosted. Here, if a patient has been waiting more
     * than the agingThreshold time units, their priority number is decremented
     * by 1 (making them more critical), down to a minimum of 1.
     *
     * Used optionally by services that want dynamic priority behaviour.
     *
     * @param patient         The patient to apply aging to
     * @param currentTime     Current simulation time
     * @param agingThreshold  Number of time units before aging kicks in
     * @return                Updated priority value
     */
    public int applyAging(Patient patient, int currentTime, int agingThreshold) {
        int waitingTime = currentTime - patient.getArrivalTime();
        if (waitingTime > 0 && waitingTime % agingThreshold == 0) {
            int newPriority = Math.max(1, patient.getPriority() - 1);
            patient.setPriority(newPriority);
            return newPriority;
        }
        return patient.getPriority();
    }
}