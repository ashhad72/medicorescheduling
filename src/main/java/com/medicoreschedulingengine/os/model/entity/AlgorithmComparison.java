package com.medicoreschedulingengine.os.model.entity;

import com.medicoreschedulingengine.os.model.enums.DepartmentType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "algorithm_comparisons", indexes = {
        @Index(name = "idx_comparison_department", columnList = "department_type"),
        @Index(name = "idx_comparison_at",         columnList = "compared_at")
})
public class AlgorithmComparison {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "simulation_run_id_1", nullable = false)
    private SimulationRun simulationRun1;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "simulation_run_id_2", nullable = false)
    private SimulationRun simulationRun2;

    @Enumerated(EnumType.STRING)
    @Column(name = "department_type", nullable = false, length = 20)
    private DepartmentType departmentType;

    /**
     * Overall winner — null if draw across all three metrics.
     */
    @Column(name = "winner_algorithm", length = 100)
    private String winnerAlgorithm;

    @Column(name = "winner_waiting_time", length = 100)
    private String winnerWaitingTime;

    @Column(name = "winner_turnaround_time", length = 100)
    private String winnerTurnaroundTime;

    @Column(name = "winner_response_time", length = 100)
    private String winnerResponseTime;

    @Column(name = "compared_at", nullable = false, updatable = false)
    private LocalDateTime comparedAt = LocalDateTime.now();

    // ── Constructors ──────────────────────────────────────────

    public AlgorithmComparison() {}

    public AlgorithmComparison(SimulationRun simulationRun1, SimulationRun simulationRun2,
                               DepartmentType departmentType) {
        this.simulationRun1 = simulationRun1;
        this.simulationRun2 = simulationRun2;
        this.departmentType = departmentType;
    }

    // ── Getters & Setters ─────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SimulationRun getSimulationRun1() { return simulationRun1; }
    public void setSimulationRun1(SimulationRun simulationRun1) { this.simulationRun1 = simulationRun1; }

    public SimulationRun getSimulationRun2() { return simulationRun2; }
    public void setSimulationRun2(SimulationRun simulationRun2) { this.simulationRun2 = simulationRun2; }

    public DepartmentType getDepartmentType() { return departmentType; }
    public void setDepartmentType(DepartmentType departmentType) { this.departmentType = departmentType; }

    public String getWinnerAlgorithm() { return winnerAlgorithm; }
    public void setWinnerAlgorithm(String winnerAlgorithm) { this.winnerAlgorithm = winnerAlgorithm; }

    public String getWinnerWaitingTime() { return winnerWaitingTime; }
    public void setWinnerWaitingTime(String winnerWaitingTime) { this.winnerWaitingTime = winnerWaitingTime; }

    public String getWinnerTurnaroundTime() { return winnerTurnaroundTime; }
    public void setWinnerTurnaroundTime(String winnerTurnaroundTime) { this.winnerTurnaroundTime = winnerTurnaroundTime; }

    public String getWinnerResponseTime() { return winnerResponseTime; }
    public void setWinnerResponseTime(String winnerResponseTime) { this.winnerResponseTime = winnerResponseTime; }

    public LocalDateTime getComparedAt() { return comparedAt; }
    public void setComparedAt(LocalDateTime comparedAt) { this.comparedAt = comparedAt; }

    @Override
    public String toString() {
        return "AlgorithmComparison{id=" + id + ", dept=" + departmentType
                + ", winner='" + winnerAlgorithm + "'}";
    }
}