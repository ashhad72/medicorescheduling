package com.medicoreschedulingengine.os.model.dto;

import com.medicoreschedulingengine.os.model.enums.DepartmentType;

import java.util.List;

public class AlgorithmResultDTO {

    private Long simulationRunId;
    private String algorithmName;
    private DepartmentType departmentType;

    // ── Aggregate metrics ─────────────────────────────────────
    private double avgWaitingTime;
    private double avgTurnaroundTime;
    private double avgResponseTime;
    private int totalPatients;

    // ── Per-patient detail ────────────────────────────────────
    private List<PatientMetricDTO> patientMetrics;

    // ── Gantt chart data ──────────────────────────────────────
    private List<GanttEntryDTO> ganttEntries;

    // ── Constructors ──────────────────────────────────────────

    public AlgorithmResultDTO() {}

    // ── Getters & Setters ─────────────────────────────────────

    public Long getSimulationRunId() { return simulationRunId; }
    public void setSimulationRunId(Long simulationRunId) { this.simulationRunId = simulationRunId; }

    public String getAlgorithmName() { return algorithmName; }
    public void setAlgorithmName(String algorithmName) { this.algorithmName = algorithmName; }

    public DepartmentType getDepartmentType() { return departmentType; }
    public void setDepartmentType(DepartmentType departmentType) { this.departmentType = departmentType; }

    public double getAvgWaitingTime() { return avgWaitingTime; }
    public void setAvgWaitingTime(double avgWaitingTime) { this.avgWaitingTime = avgWaitingTime; }

    public double getAvgTurnaroundTime() { return avgTurnaroundTime; }
    public void setAvgTurnaroundTime(double avgTurnaroundTime) { this.avgTurnaroundTime = avgTurnaroundTime; }

    public double getAvgResponseTime() { return avgResponseTime; }
    public void setAvgResponseTime(double avgResponseTime) { this.avgResponseTime = avgResponseTime; }

    public int getTotalPatients() { return totalPatients; }
    public void setTotalPatients(int totalPatients) { this.totalPatients = totalPatients; }

    public List<PatientMetricDTO> getPatientMetrics() { return patientMetrics; }
    public void setPatientMetrics(List<PatientMetricDTO> patientMetrics) { this.patientMetrics = patientMetrics; }

    public List<GanttEntryDTO> getGanttEntries() { return ganttEntries; }
    public void setGanttEntries(List<GanttEntryDTO> ganttEntries) { this.ganttEntries = ganttEntries; }
}