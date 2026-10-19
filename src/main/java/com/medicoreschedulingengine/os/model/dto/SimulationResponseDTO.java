package com.medicoreschedulingengine.os.model.dto;

import com.medicoreschedulingengine.os.model.enums.DepartmentType;

import java.time.LocalDateTime;

public class SimulationResponseDTO {

    private DepartmentType departmentType;
    private String departmentName;
    private LocalDateTime simulatedAt;

    // ── The full comparison (contains both AlgorithmResultDTOs) ──
    private ComparisonResponseDTO comparison;

    // ── Constructors ──────────────────────────────────────────

    public SimulationResponseDTO() {}

    public SimulationResponseDTO(DepartmentType departmentType, String departmentName,
                                 ComparisonResponseDTO comparison) {
        this.departmentType = departmentType;
        this.departmentName = departmentName;
        this.comparison     = comparison;
        this.simulatedAt    = LocalDateTime.now();
    }

    // ── Getters & Setters ─────────────────────────────────────

    public DepartmentType getDepartmentType() { return departmentType; }
    public void setDepartmentType(DepartmentType departmentType) { this.departmentType = departmentType; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public LocalDateTime getSimulatedAt() { return simulatedAt; }
    public void setSimulatedAt(LocalDateTime simulatedAt) { this.simulatedAt = simulatedAt; }

    public ComparisonResponseDTO getComparison() { return comparison; }
    public void setComparison(ComparisonResponseDTO comparison) { this.comparison = comparison; }
}