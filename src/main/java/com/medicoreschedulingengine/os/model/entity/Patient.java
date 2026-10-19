package com.medicoreschedulingengine.os.model.entity;

import com.medicoreschedulingengine.os.model.enums.PatientStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "patients", indexes = {
        @Index(name = "idx_patient_department", columnList = "department_id"),
        @Index(name = "idx_patient_status",     columnList = "status"),
        @Index(name = "idx_patient_arrival",    columnList = "arrival_time")
})
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    // ── OS Scheduling Fields ──────────────────────────────────

    @Column(name = "arrival_time", nullable = false)
    private int arrivalTime;

    @Column(name = "burst_time", nullable = false)
    private int burstTime;

    /**
     * Lower number = higher priority.
     * Priority 1 is most critical (used in ER Preemptive Priority & MLQ).
     */
    @Column(name = "priority", nullable = false)
    private int priority;

    /**
     * Tracks how much treatment time remains.
     * Initialised to burstTime; decremented during preemptive scheduling (SRTF, Priority).
     */
    @Column(name = "remaining_time", nullable = false)
    private int remainingTime;

    // ── Status & Relations ────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PatientStatus status = PatientStatus.WAITING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Constructors ──────────────────────────────────────────

    public Patient() {}

    public Patient(String name, int arrivalTime, int burstTime, int priority, Department department) {
        this.name          = name;
        this.arrivalTime   = arrivalTime;
        this.burstTime     = burstTime;
        this.priority      = priority;
        this.remainingTime = burstTime;   // always starts equal to burst time
        this.department    = department;
        this.status        = PatientStatus.WAITING;
    }

    // ── Convenience ───────────────────────────────────────────

    /**
     * Resets the patient back to its initial state.
     * Called before each algorithm run to ensure cloned datasets start fresh.
     */
    public void reset() {
        this.remainingTime = this.burstTime;
        this.status        = PatientStatus.WAITING;
    }

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

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Patient{id=" + id + ", name='" + name + "', burst=" + burstTime
                + ", priority=" + priority + ", status=" + status + "}";
    }
}