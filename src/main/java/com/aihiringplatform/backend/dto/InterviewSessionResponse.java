package com.aihiringplatform.backend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class InterviewSessionResponse {
    private Long id;
    private Long jobId;
    private Long applicationId;
    private String jobRole;
    private String difficulty;
    private List<String> skills;
    private String interviewType;
    private String monitoringMode;
    private Integer estimatedDurationMinutes;
    private String status;
    private List<String> questions;
    private Map<Integer, String> answers;
    private Integer currentQuestionIndex;
    private Integer elapsedSeconds;
    private LocalDateTime startedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private Boolean fallbackUsed;
    private String message;
    private InterviewResultResponse result;
    private Integer tabSwitchCount;
    private Integer faceWarningCount;
    private Integer suspiciousActivityCount;
    private Boolean cameraUsed;
    private Boolean microphoneUsed;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getQuestions() {
        return questions;
    }

    public void setQuestions(List<String> questions) {
        this.questions = questions;
    }

    public Map<Integer, String> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<Integer, String> answers) {
        this.answers = answers;
    }

    public Integer getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public void setCurrentQuestionIndex(Integer currentQuestionIndex) {
        this.currentQuestionIndex = currentQuestionIndex;
    }

    public Integer getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void setElapsedSeconds(Integer elapsedSeconds) {
        this.elapsedSeconds = elapsedSeconds;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Boolean getFallbackUsed() {
        return fallbackUsed;
    }

    public void setFallbackUsed(Boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public InterviewResultResponse getResult() {
        return result;
    }

    public void setResult(InterviewResultResponse result) {
        this.result = result;
    }

    public Integer getTabSwitchCount() {
        return tabSwitchCount;
    }

    public void setTabSwitchCount(Integer tabSwitchCount) {
        this.tabSwitchCount = tabSwitchCount;
    }

    public Integer getFaceWarningCount() {
        return faceWarningCount;
    }

    public void setFaceWarningCount(Integer faceWarningCount) {
        this.faceWarningCount = faceWarningCount;
    }

    public Integer getSuspiciousActivityCount() {
        return suspiciousActivityCount;
    }

    public void setSuspiciousActivityCount(Integer suspiciousActivityCount) {
        this.suspiciousActivityCount = suspiciousActivityCount;
    }

    public Boolean getCameraUsed() {
        return cameraUsed;
    }

    public void setCameraUsed(Boolean cameraUsed) {
        this.cameraUsed = cameraUsed;
    }

    public Boolean getMicrophoneUsed() {
        return microphoneUsed;
    }

    public void setMicrophoneUsed(Boolean microphoneUsed) {
        this.microphoneUsed = microphoneUsed;
    }
}
