package com.medicoreschedulingengine.os.model.dto;

public class PatientMetricDTO {

    private Long patientId;
    private String patientName;
    private int arrivalTime;
    private int burstTime;
    private int priority;
    private int completionTime;
    private int waitingTime;
    private int turnaroundTime;
    private int responseTime;

    // ── Constructors ──────────────────────────────────────────

    public PatientMetricDTO() {}

    public PatientMetricDTO(Long patientId, String patientName, int arrivalTime,
                            int burstTime, int priority, int completionTime,
                            int waitingTime, int turnaroundTime, int responseTime) {
        this.patientId      = patientId;
        this.patientName    = patientName;
        this.arrivalTime    = arrivalTime;
        this.burstTime      = burstTime;
        this.priority       = priority;
        this.completionTime = completionTime;
        this.waitingTime    = waitingTime;
        this.turnaroundTime = turnaroundTime;
        this.responseTime   = responseTime;
    }

    // ── Getters & Setters ─────────────────────────────────────

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public int getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(int arrivalTime) { this.arrivalTime = arrivalTime; }

    public int getBurstTime() { return burstTime; }
    public void setBurstTime(int burstTime) { this.burstTime = burstTime; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public int getCompletionTime() { return completionTime; }
    public void setCompletionTime(int completionTime) { this.completionTime = completionTime; }

    public int getWaitingTime() { return waitingTime; }
    public void setWaitingTime(int waitingTime) { this.waitingTime = waitingTime; }

    public int getTurnaroundTime() { return turnaroundTime; }
    public void setTurnaroundTime(int turnaroundTime) { this.turnaroundTime = turnaroundTime; }

    public int getResponseTime() { return responseTime; }
    public void setResponseTime(int responseTime) { this.responseTime = responseTime; }
}