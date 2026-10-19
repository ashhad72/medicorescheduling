package com.medicoreschedulingengine.os.model.dto;

import com.medicoreschedulingengine.os.model.enums.DepartmentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PatientRequestDTO {

    @NotBlank(message = "Patient name is required")
    private String name;

    @Min(value = 0, message = "Arrival time cannot be negative")
    private int arrivalTime;

    @Min(value = 1, message = "Burst time must be at least 1")
    private int burstTime;

    @Min(value = 1, message = "Priority must be at least 1")
    private int priority;

    @NotNull(message = "Department type is required")
    private DepartmentType departmentType;

    // ── Constructors ──────────────────────────────────────────

    public PatientRequestDTO() {}

    public PatientRequestDTO(String name, int arrivalTime, int burstTime,
                             int priority, DepartmentType departmentType) {
        this.name           = name;
        this.arrivalTime    = arrivalTime;
        this.burstTime      = burstTime;
        this.priority       = priority;
        this.departmentType = departmentType;
    }

    // ── Getters & Setters ─────────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(int arrivalTime) { this.arrivalTime = arrivalTime; }

    public int getBurstTime() { return burstTime; }
    public void setBurstTime(int burstTime) { this.burstTime = burstTime; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public DepartmentType getDepartmentType() { return departmentType; }
    public void setDepartmentType(DepartmentType departmentType) { this.departmentType = departmentType; }
}