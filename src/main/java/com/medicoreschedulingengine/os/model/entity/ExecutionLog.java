package com.medicoreschedulingengine.os.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "execution_logs", indexes = {
        @Index(name = "idx_log_run",     columnList = "simulation_run_id"),
        @Index(name = "idx_log_patient", columnList = "patient_id"),
        @Index(name = "idx_log_start",   columnList = "start_time")
})
public class ExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_run_id", nullable = false)
    private SimulationRun simulationRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /**
     * Denormalised for fast Gantt chart queries — avoids joins on every render.
     */
    @Column(name = "patient_name", nullable = false, length = 150)
    private String patientName;

    // ── Gantt Chart Fields ────────────────────────────────────

    @Column(name = "start_time", nullable = false)
    private int startTime;

    @Column(name = "end_time", nullable = false)
    private int endTime;

    @Column(name = "algorithm_used", nullable = false, length = 100)
    private String algorithmUsed;

    // ── Per-Patient Metrics ───────────────────────────────────

    @Column(name = "waiting_time", nullable = false)
    private int waitingTime;

    @Column(name = "turnaround_time", nullable = false)
    private int turnaroundTime;

    @Column(name = "response_time", nullable = false)
    private int responseTime;

    // ── Constructors ──────────────────────────────────────────

    public ExecutionLog() {}

    public ExecutionLog(SimulationRun simulationRun, Patient patient,
                        int startTime, int endTime, String algorithmUsed,
                        int waitingTime, int turnaroundTime, int responseTime) {
        this.simulationRun  = simulationRun;
        this.patient        = patient;
        this.patientName    = patient.getName();
        this.startTime      = startTime;
        this.endTime        = endTime;
        this.algorithmUsed  = algorithmUsed;
        this.waitingTime    = waitingTime;
        this.turnaroundTime = turnaroundTime;
        this.responseTime   = responseTime;
    }

    // ── Getters & Setters ─────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SimulationRun getSimulationRun() { return simulationRun; }
    public void setSimulationRun(SimulationRun simulationRun) { this.simulationRun = simulationRun; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public int getStartTime() { return startTime; }
    public void setStartTime(int startTime) { this.startTime = startTime; }

    public int getEndTime() { return endTime; }
    public void setEndTime(int endTime) { this.endTime = endTime; }

    public String getAlgorithmUsed() { return algorithmUsed; }
    public void setAlgorithmUsed(String algorithmUsed) { this.algorithmUsed = algorithmUsed; }

    public int getWaitingTime() { return waitingTime; }
    public void setWaitingTime(int waitingTime) { this.waitingTime = waitingTime; }

    public int getTurnaroundTime() { return turnaroundTime; }
    public void setTurnaroundTime(int turnaroundTime) { this.turnaroundTime = turnaroundTime; }

    public int getResponseTime() { return responseTime; }
    public void setResponseTime(int responseTime) { this.responseTime = responseTime; }

    @Override
    public String toString() {
        return "ExecutionLog{id=" + id + ", patient='" + patientName
                + "', start=" + startTime + ", end=" + endTime
                + ", algo='" + algorithmUsed + "'}";
    }
}