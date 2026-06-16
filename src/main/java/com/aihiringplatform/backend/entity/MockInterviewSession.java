package com.aihiringplatform.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mock_interview_sessions")
public class MockInterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne
    @JoinColumn(name = "application_id")
    private Application application;

    private String jobRole;
    private String difficulty;
    private String interviewType;
    private String monitoringMode;
    private Integer estimatedDurationMinutes;

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Enumerated(EnumType.STRING)
    private MockInterviewStatus status;

    @Column(columnDefinition = "TEXT")
    private String questionsJson;

    @Column(columnDefinition = "TEXT")
    private String answersJson;

    private Integer currentQuestionIndex;
    private Integer elapsedSeconds;
    private LocalDateTime startedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    private Integer overallScore;
    private Integer communicationScore;
    private Integer technicalScore;

    @Column(columnDefinition = "TEXT")
    private String strengthsJson;

    @Column(columnDefinition = "TEXT")
    private String weaknessesJson;

    @Column(columnDefinition = "TEXT")
    private String improvementSuggestionsJson;

    @Column(columnDefinition = "TEXT")
    private String communicationFeedback;

    @Column(columnDefinition = "TEXT")
    private String technicalFeedback;

    @Column(columnDefinition = "TEXT")
    private String resultSummary;

    @Column(columnDefinition = "TEXT")
    private String finalFeedback;

    @Column(columnDefinition = "TEXT")
    private String questionBreakdownJson;

    private Boolean fallbackUsed;

    @Column(columnDefinition = "TEXT")
    private String monitoringEventsJson;

    @Column(columnDefinition = "TEXT")
    private String transcriptJson;

    private Integer tabSwitchCount;
    private Integer faceWarningCount;
    private Integer suspiciousActivityCount;
    private Integer confidenceScore;
    private Integer fluencyScore;
    private Boolean cameraUsed;
    private Boolean microphoneUsed;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getCandidate() {
        return candidate;
    }

    public void setCandidate(User candidate) {
        this.candidate = candidate;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
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

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public MockInterviewStatus getStatus() {
        return status;
    }

    public void setStatus(MockInterviewStatus status) {
        this.status = status;
    }

    public String getQuestionsJson() {
        return questionsJson;
    }

    public void setQuestionsJson(String questionsJson) {
        this.questionsJson = questionsJson;
    }

    public String getAnswersJson() {
        return answersJson;
    }

    public void setAnswersJson(String answersJson) {
        this.answersJson = answersJson;
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

    public Integer getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Integer overallScore) {
        this.overallScore = overallScore;
    }

    public Integer getCommunicationScore() {
        return communicationScore;
    }

    public void setCommunicationScore(Integer communicationScore) {
        this.communicationScore = communicationScore;
    }

    public Integer getTechnicalScore() {
        return technicalScore;
    }

    public void setTechnicalScore(Integer technicalScore) {
        this.technicalScore = technicalScore;
    }

    public String getStrengthsJson() {
        return strengthsJson;
    }

    public void setStrengthsJson(String strengthsJson) {
        this.strengthsJson = strengthsJson;
    }

    public String getWeaknessesJson() {
        return weaknessesJson;
    }

    public void setWeaknessesJson(String weaknessesJson) {
        this.weaknessesJson = weaknessesJson;
    }

    public String getImprovementSuggestionsJson() {
        return improvementSuggestionsJson;
    }

    public void setImprovementSuggestionsJson(String improvementSuggestionsJson) {
        this.improvementSuggestionsJson = improvementSuggestionsJson;
    }

    public String getCommunicationFeedback() {
        return communicationFeedback;
    }

    public void setCommunicationFeedback(String communicationFeedback) {
        this.communicationFeedback = communicationFeedback;
    }

    public String getTechnicalFeedback() {
        return technicalFeedback;
    }

    public void setTechnicalFeedback(String technicalFeedback) {
        this.technicalFeedback = technicalFeedback;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    public String getFinalFeedback() {
        return finalFeedback;
    }

    public void setFinalFeedback(String finalFeedback) {
        this.finalFeedback = finalFeedback;
    }

    public String getQuestionBreakdownJson() {
        return questionBreakdownJson;
    }

    public void setQuestionBreakdownJson(String questionBreakdownJson) {
        this.questionBreakdownJson = questionBreakdownJson;
    }

    public Boolean getFallbackUsed() {
        return fallbackUsed;
    }

    public void setFallbackUsed(Boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
    }

    public String getMonitoringEventsJson() {
        return monitoringEventsJson;
    }

    public void setMonitoringEventsJson(String monitoringEventsJson) {
        this.monitoringEventsJson = monitoringEventsJson;
    }

    public String getTranscriptJson() {
        return transcriptJson;
    }

    public void setTranscriptJson(String transcriptJson) {
        this.transcriptJson = transcriptJson;
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

    public Integer getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Integer confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Integer getFluencyScore() {
        return fluencyScore;
    }

    public void setFluencyScore(Integer fluencyScore) {
        this.fluencyScore = fluencyScore;
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
