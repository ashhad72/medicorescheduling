package com.medicoreschedulingengine.os.model.dto;

import com.medicoreschedulingengine.os.model.entity.Patient;
import com.medicoreschedulingengine.os.model.enums.DepartmentType;
import com.medicoreschedulingengine.os.model.enums.PatientStatus;

public class PatientResponseDTO {

    private Long id;
    private String name;
    private int arrivalTime;
    private int burstTime;
    private int priority;
    private int remainingTime;
    private PatientStatus status;
    private DepartmentType departmentType;
    private String departmentName;

    // ── Static factory — converts entity to DTO ───────────────

    public static PatientResponseDTO from(Patient patient) {
        PatientResponseDTO dto = new PatientResponseDTO();
        dto.id             = patient.getId();
        dto.name           = patient.getName();
        dto.arrivalTime    = patient.getArrivalTime();
        dto.burstTime      = patient.getBurstTime();
        dto.priority       = patient.getPriority();
        dto.remainingTime  = patient.getRemainingTime();
        dto.status         = patient.getStatus();
        dto.departmentType = patient.getDepartment().getDepartmentType();
        dto.departmentName = patient.getDepartment().getName();
        return dto;
    }

    // ── Constructors ──────────────────────────────────────────

    public PatientResponseDTO() {}

    // ── Getters & Setters ─────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(int arrivalTime) { this.arrivalTime = arrivalTime; }

    public int getBurstTime() { return burstTime; }
    public void setBurstTime(int burstTime) { this.burstTime = burstTime; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public int getRemainingTime() { return remainingTime; }
    public void setRemainingTime(int remainingTime) { this.remainingTime = remainingTime; }

    public PatientStatus getStatus() { return status; }
    public void setStatus(PatientStatus status) { this.status = status; }

    public DepartmentType getDepartmentType() { return departmentType; }
    public void setDepartmentType(DepartmentType departmentType) { this.departmentType = departmentType; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
}