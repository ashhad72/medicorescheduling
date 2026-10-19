package com.medicoreschedulingengine.os.util;

import com.medicoreschedulingengine.os.model.dto.PatientMetricDTO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TimeCalculator
 *
 * Utility class responsible for computing the three core OS scheduling metrics:
 *
 *   1. Waiting Time (WT)
 *      Time a patient spends in the ready queue waiting for the CPU.
 *      Formula: WT = Turnaround Time - Burst Time
 *
 *   2. Turnaround Time (TT)
 *      Total time from arrival to completion.
 *      Formula: TT = Completion Time - Arrival Time
 *
 *   3. Response Time (RT)
 *      Time from arrival to first CPU access.
 *      Formula: RT = First Start Time - Arrival Time
 *      Note: For non-preemptive algorithms, RT == WT.
 *
 * These methods are used by MetricsService to compute aggregate averages
 * across all patients in a simulation run.
 */
@Component
public class TimeCalculator {

    // ── Per-Patient Calculations ──────────────────────────────

    /**
     * Calculates turnaround time for a single patient.
     *
     * @param completionTime  Time unit when patient finished treatment
     * @param arrivalTime     Time unit when patient arrived
     * @return                Turnaround time (>= 0)
     */
    public int calculateTurnaroundTime(int completionTime, int arrivalTime) {
        return Math.max(0, completionTime - arrivalTime);
    }

    /**
     * Calculates waiting time for a single patient.
     *
     * @param turnaroundTime  Already computed turnaround time
     * @param burstTime       Total treatment time required
     * @return                Waiting time (>= 0)
     */
    public int calculateWaitingTime(int turnaroundTime, int burstTime) {
        return Math.max(0, turnaroundTime - burstTime);
    }

    /**
     * Calculates waiting time directly from raw values.
     *
     * @param completionTime  Time unit when patient finished
     * @param arrivalTime     Time unit when patient arrived
     * @param burstTime       Total treatment time required
     * @return                Waiting time (>= 0)
     */
    public int calculateWaitingTimeDirect(int completionTime, int arrivalTime, int burstTime) {
        int turnaroundTime = calculateTurnaroundTime(completionTime, arrivalTime);
        return calculateWaitingTime(turnaroundTime, burstTime);
    }

    /**
     * Calculates response time for a single patient.
     *
     * @param firstStartTime  Time unit when patient first got CPU access
     * @param arrivalTime     Time unit when patient arrived
     * @return                Response time (>= 0)
     */
    public int calculateResponseTime(int firstStartTime, int arrivalTime) {
        return Math.max(0, firstStartTime - arrivalTime);
    }

    // ── Aggregate Calculations ────────────────────────────────

    /**
     * Calculates average waiting time across all patients in a simulation run.
     *
     * @param metrics  List of per-patient metrics
     * @return         Average waiting time, 0.0 if list is empty
     */
    public double calculateAvgWaitingTime(List<PatientMetricDTO> metrics) {
        if (metrics == null || metrics.isEmpty()) return 0.0;
        return metrics.stream()
                .mapToInt(PatientMetricDTO::getWaitingTime)
                .average()
                .orElse(0.0);
    }

    /**
     * Calculates average turnaround time across all patients in a simulation run.
     *
     * @param metrics  List of per-patient metrics
     * @return         Average turnaround time, 0.0 if list is empty
     */
    public double calculateAvgTurnaroundTime(List<PatientMetricDTO> metrics) {
        if (metrics == null || metrics.isEmpty()) return 0.0;
        return metrics.stream()
                .mapToInt(PatientMetricDTO::getTurnaroundTime)
                .average()
                .orElse(0.0);
    }

    /**
     * Calculates average response time across all patients in a simulation run.
     *
     * @param metrics  List of per-patient metrics
     * @return         Average response time, 0.0 if list is empty
     */
    public double calculateAvgResponseTime(List<PatientMetricDTO> metrics) {
        if (metrics == null || metrics.isEmpty()) return 0.0;
        return metrics.stream()
                .mapToInt(PatientMetricDTO::getResponseTime)
                .average()
                .orElse(0.0);
    }

    // ── CPU Utilization ───────────────────────────────────────

    /**
     * Calculates CPU utilization percentage.
     *
     * Formula: (Total Burst Time / Total Simulation Time) * 100
     *
     * @param totalBurstTime      Sum of all burst times
     * @param totalSimulationTime Total elapsed time from first arrival to last completion
     * @return                    CPU utilization percentage (0.0 - 100.0)
     */
    public double calculateCpuUtilization(int totalBurstTime, int totalSimulationTime) {
        if (totalSimulationTime <= 0) return 0.0;
        return Math.min(100.0, ((double) totalBurstTime / totalSimulationTime) * 100.0);
    }

    /**
     * Calculates throughput — number of patients completed per unit time.
     *
     * @param totalPatients       Number of patients completed
     * @param totalSimulationTime Total elapsed simulation time
     * @return                    Throughput value
     */
    public double calculateThroughput(int totalPatients, int totalSimulationTime) {
        if (totalSimulationTime <= 0) return 0.0;
        return (double) totalPatients / totalSimulationTime;
    }
}