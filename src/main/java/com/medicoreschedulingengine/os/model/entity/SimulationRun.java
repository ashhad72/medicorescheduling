package com.medicoreschedulingengine.os.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.medicoreschedulingengine.os.model.enums.DepartmentType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "simulation_runs", indexes = {
        @Index(name = "idx_run_department", columnList = "department_type"),
        @Index(name = "idx_run_algorithm",  columnList = "algorithm_name"),
        @Index(name = "idx_run_at",         columnList = "run_at")
})
public class SimulationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "algorithm_name", nullable = false, length = 100)
    private String algorithmName;

    @Enumerated(EnumType.STRING)
    @Column(name = "department_type", nullable = false, length = 20)
    private DepartmentType departmentType;

    // ── Aggregate Metrics ─────────────────────────────────────

    @Column(name = "avg_waiting_time", nullable = false)
    private double avgWaitingTime;

    @Column(name = "avg_turnaround_time", nullable = false)
    private double avgTurnaroundTime;

    @Column(name = "avg_response_time", nullable = false)
    private double avgResponseTime;

    @Column(name = "total_patients", nullable = false)
    private int totalPatients;

    @Column(name = "run_at", nullable = false, updatable = false)
    private LocalDateTime runAt = LocalDateTime.now();

    // ── Relations ─────────────────────────────────────────────

    @JsonIgnore
    @OneToMany(mappedBy = "simulationRun", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ExecutionLog> executionLogs = new ArrayList<>();

    // ── Constructors ──────────────────────────────────────────

    public SimulationRun() {}

    public SimulationRun(String algorithmName, DepartmentType departmentType,
                         double avgWaitingTime, double avgTurnaroundTime,
                         double avgResponseTime, int totalPatients) {
        this.algorithmName    = algorithmName;
        this.departmentType   = departmentType;
        this.avgWaitingTime   = avgWaitingTime;
        this.avgTurnaroundTime = avgTurnaroundTime;
        this.avgResponseTime  = avgResponseTime;
        this.totalPatients    = totalPatients;
    }

    // ── Getters & Setters ─────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public LocalDateTime getRunAt() { return runAt; }
    public void setRunAt(LocalDateTime runAt) { this.runAt = runAt; }

    public List<ExecutionLog> getExecutionLogs() { return executionLogs; }
    public void setExecutionLogs(List<ExecutionLog> executionLogs) { this.executionLogs = executionLogs; }

    @Override
    public String toString() {
        return "SimulationRun{id=" + id + ", algorithm='" + algorithmName
                + "', dept=" + departmentType + ", avgWT=" + avgWaitingTime + "}";
    }
}