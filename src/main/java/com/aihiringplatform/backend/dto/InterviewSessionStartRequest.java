package com.aihiringplatform.backend.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public class InterviewSessionStartRequest {
    private Long jobId;
    private Long applicationId;

    @Size(max = 100, message = "Job role must be 100 characters or less")
    private String jobRole;

    @Size(max = 30, message = "Difficulty must be 30 characters or less")
    private String difficulty;

    @Size(max = 12, message = "A maximum of 12 skills is allowed")
    private List<String> skills;

    private String interviewType;
    private String monitoringMode;
    private Integer estimatedDurationMinutes;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getJobRole() {
        return jobRole;
    }

    public void setJobRole(String jobRole) {
        this.jobRole = jobRole;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public String getInterviewType() {
        return interviewType;
    }

    public void setInterviewType(String interviewType) {
        this.interviewType = interviewType;
    }

    public String getMonitoringMode() {
        return monitoringMode;
    }

    public void setMonitoringMode(String monitoringMode) {
        this.monitoringMode = monitoringMode;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }
}
