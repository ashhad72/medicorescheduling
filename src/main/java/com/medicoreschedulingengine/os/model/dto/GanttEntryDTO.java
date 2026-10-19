package com.medicoreschedulingengine.os.model.dto;

public class GanttEntryDTO {

    private Long patientId;
    private String patientName;
    private int startTime;
    private int endTime;
    private int duration;
    private String algorithmUsed;

    // ── Constructors ──────────────────────────────────────────

    public GanttEntryDTO() {}

    public GanttEntryDTO(Long patientId, String patientName,
                         int startTime, int endTime, String algorithmUsed) {
        this.patientId    = patientId;
        this.patientName  = patientName;
        this.startTime    = startTime;
        this.endTime      = endTime;
        this.duration     = endTime - startTime;
        this.algorithmUsed = algorithmUsed;
    }

    // ── Getters & Setters ─────────────────────────────────────

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public int getStartTime() { return startTime; }
    public void setStartTime(int startTime) { this.startTime = startTime; }

    public int getEndTime() { return endTime; }
    public void setEndTime(int endTime) { this.endTime = endTime; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getAlgorithmUsed() { return algorithmUsed; }
    public void setAlgorithmUsed(String algorithmUsed) { this.algorithmUsed = algorithmUsed; }
}