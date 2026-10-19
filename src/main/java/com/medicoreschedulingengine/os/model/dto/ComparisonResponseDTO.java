package com.medicoreschedulingengine.os.model.dto;

import com.medicoreschedulingengine.os.model.enums.DepartmentType;

public class ComparisonResponseDTO {

    private DepartmentType departmentType;

    // ── The two algorithm results being compared ───────────────
    private AlgorithmResultDTO result1;
    private AlgorithmResultDTO result2;

    // ── Winner summary ────────────────────────────────────────
    private String overallWinner;
    private String winnerWaitingTime;
    private String winnerTurnaroundTime;
    private String winnerResponseTime;

    // ── Comparison ID (saved to DB) ───────────────────────────
    private Long comparisonId;

    // ── Constructors ──────────────────────────────────────────

    public ComparisonResponseDTO() {}

    public ComparisonResponseDTO(DepartmentType departmentType,
                                 AlgorithmResultDTO result1,
                                 AlgorithmResultDTO result2) {
        this.departmentType = departmentType;
        this.result1        = result1;
        this.result2        = result2;
        determineWinners();
    }

    // ── Winner logic ──────────────────────────────────────────

    private void determineWinners() {
        this.winnerWaitingTime = result1.getAvgWaitingTime() <= result2.getAvgWaitingTime()
                ? result1.getAlgorithmName() : result2.getAlgorithmName();

        this.winnerTurnaroundTime = result1.getAvgTurnaroundTime() <= result2.getAvgTurnaroundTime()
                ? result1.getAlgorithmName() : result2.getAlgorithmName();

        this.winnerResponseTime = result1.getAvgResponseTime() <= result2.getAvgResponseTime()
                ? result1.getAlgorithmName() : result2.getAlgorithmName();

        // Overall winner: whoever wins at least 2 out of 3 metrics
        int score1 = 0;
        if (winnerWaitingTime.equals(result1.getAlgorithmName()))    score1++;
        if (winnerTurnaroundTime.equals(result1.getAlgorithmName())) score1++;
        if (winnerResponseTime.equals(result1.getAlgorithmName()))   score1++;

        if (score1 >= 2) {
            this.overallWinner = result1.getAlgorithmName();
        } else if (score1 <= 1) {
            this.overallWinner = result2.getAlgorithmName();
        } else {
            this.overallWinner = "DRAW";
        }
    }

    // ── Getters & Setters ─────────────────────────────────────

    public DepartmentType getDepartmentType() { return departmentType; }
    public void setDepartmentType(DepartmentType departmentType) { this.departmentType = departmentType; }

    public AlgorithmResultDTO getResult1() { return result1; }
    public void setResult1(AlgorithmResultDTO result1) { this.result1 = result1; }

    public AlgorithmResultDTO getResult2() { return result2; }
    public void setResult2(AlgorithmResultDTO result2) { this.result2 = result2; }

    public String getOverallWinner() { return overallWinner; }
    public void setOverallWinner(String overallWinner) { this.overallWinner = overallWinner; }

    public String getWinnerWaitingTime() { return winnerWaitingTime; }
    public void setWinnerWaitingTime(String winnerWaitingTime) { this.winnerWaitingTime = winnerWaitingTime; }

    public String getWinnerTurnaroundTime() { return winnerTurnaroundTime; }
    public void setWinnerTurnaroundTime(String winnerTurnaroundTime) { this.winnerTurnaroundTime = winnerTurnaroundTime; }

    public String getWinnerResponseTime() { return winnerResponseTime; }
    public void setWinnerResponseTime(String winnerResponseTime) { this.winnerResponseTime = winnerResponseTime; }

    public Long getComparisonId() { return comparisonId; }
    public void setComparisonId(Long comparisonId) { this.comparisonId = comparisonId; }
}