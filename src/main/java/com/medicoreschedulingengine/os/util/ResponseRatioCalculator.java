package com.medicoreschedulingengine.os.util;

import com.medicoreschedulingengine.os.model.entity.Patient;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * ResponseRatioCalculator
 *
 * Utility class for HRRN (Highest Response Ratio Next) formula calculations.
 *
 * Formula:
 *   Response Ratio = (Waiting Time + Burst Time) / Burst Time
 *                  = 1 + (Waiting Time / Burst Time)
 *
 * Properties:
 *   - Ratio is always >= 1.0
 *   - When waiting time = 0, ratio = 1.0 (just arrived)
 *   - Short burst patients naturally have a higher ratio
 *   - Long-waiting patients gain ratio over time preventing starvation
 *
 * Used by:
 *   - HRRNStrategy  (scheduling decisions)
 *   - MetricsService (optional ratio reporting)
 */
@Component
public class ResponseRatioCalculator {

    // ── Core Formula ──────────────────────────────────────────

    /**
     * Calculates the HRRN response ratio for a patient.
     *
     * @param waitingTime  Time patient has been waiting (currentTime - arrivalTime)
     * @param burstTime    Total treatment time required
     * @return             Response ratio (always >= 1.0)
     */
    public double calculateResponseRatio(int waitingTime, int burstTime) {
        if (burstTime <= 0) return 1.0;
        return (double) (waitingTime + burstTime) / burstTime;
    }

    /**
     * Calculates the HRRN response ratio directly from a Patient object
     * given the current simulation time.
     *
     * @param patient      The patient to calculate ratio for
     * @param currentTime  Current simulation time unit
     * @return             Response ratio (always >= 1.0)
     */
    public double calculateResponseRatioForPatient(Patient patient, int currentTime) {
        int waitingTime = Math.max(0, currentTime - patient.getArrivalTime());
        return calculateResponseRatio(waitingTime, patient.getBurstTime());
    }

    // ── Selection Helper ──────────────────────────────────────

    /**
     * Selects the patient with the highest response ratio from a list
     * of available (already arrived) patients.
     *
     * Tie-break: shorter burst time wins (favours throughput).
     * Second tie-break: earlier arrival time wins.
     *
     * @param availablePatients  Patients that have arrived and are waiting
     * @param currentTime        Current simulation time unit
     * @return                   Patient with highest response ratio
     */
    public Patient selectHighestRatioPatient(List<Patient> availablePatients, int currentTime) {
        if (availablePatients == null || availablePatients.isEmpty()) {
            throw new IllegalArgumentException("Available patients list cannot be empty.");
        }

        return availablePatients.stream()
                .max(Comparator
                        .comparingDouble((Patient p) ->
                                calculateResponseRatioForPatient(p, currentTime))
                        .thenComparingInt(p -> -p.getBurstTime())   // shorter burst wins on tie
                        .thenComparingInt(p -> -p.getArrivalTime())) // earlier arrival wins on tie
                .orElseThrow(() -> new IllegalStateException("Could not select patient."));
    }

    // ── Reporting Helpers ─────────────────────────────────────

    /**
     * Returns the response ratio for every patient in a list at a given time.
     * Useful for logging or frontend display of ratio evolution.
     *
     * @param patients     List of patients to evaluate
     * @param currentTime  Current simulation time unit
     * @return             List of ratio values in the same order as input
     */
    public List<Double> getAllRatios(List<Patient> patients, int currentTime) {
        return patients.stream()
                .map(p -> calculateResponseRatioForPatient(p, currentTime))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Formats a response ratio as a readable string rounded to 2 decimal places.
     *
     * @param ratio  The response ratio value
     * @return       Formatted string e.g. "2.50"
     */
    public String formatRatio(double ratio) {
        return String.format("%.2f", ratio);
    }
}