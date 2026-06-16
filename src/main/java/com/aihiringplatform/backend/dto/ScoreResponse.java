package com.aihiringplatform.backend.dto;

public class ScoreResponse {
    private Long applicationId;
    private double score;
    private String remark;

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
