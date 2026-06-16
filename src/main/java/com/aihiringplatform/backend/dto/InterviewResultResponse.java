package com.aihiringplatform.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class InterviewResultResponse {
    private Long sessionId;
    private String monitoringMode;
    private Integer overallScore;
    private Integer communicationScore;
    private Integer technicalScore;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> improvementSuggestions;
    private String communicationEvaluation;
    private String technicalRelevance;
    private String summary;
    private String finalFeedback;
    private Boolean fallbackUsed;
    private String message;
    private List<InterviewQuestionBreakdownItem> questionBreakdown;
    private Integer totalQuestions;
    private Integer answeredQuestions;
    private LocalDateTime completedAt;
    private Integer confidenceScore;
    private Integer fluencyScore;
    private Integer tabSwitchCount;
    private Integer faceWarningCount;
    private Integer suspiciousActivityCount;
    private String integrityStatus;
    private List<MonitoringEventDto> monitoringEvents;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getMonitoringMode() {
        return monitoringMode;
    }

    public void setMonitoringMode(String monitoringMode) {
        this.monitoringMode = monitoringMode;
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

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(List<String> weaknesses) {
        this.weaknesses = weaknesses;
    }

    public List<String> getImprovementSuggestions() {
        return improvementSuggestions;
    }

    public void setImprovementSuggestions(List<String> improvementSuggestions) {
        this.improvementSuggestions = improvementSuggestions;
    }

    public String getCommunicationEvaluation() {
        return communicationEvaluation;
    }

    public void setCommunicationEvaluation(String communicationEvaluation) {
        this.communicationEvaluation = communicationEvaluation;
    }

    public String getTechnicalRelevance() {
        return technicalRelevance;
    }

    public void setTechnicalRelevance(String technicalRelevance) {
        this.technicalRelevance = technicalRelevance;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getFinalFeedback() {
        return finalFeedback;
    }

    public void setFinalFeedback(String finalFeedback) {
        this.finalFeedback = finalFeedback;
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

    public List<InterviewQuestionBreakdownItem> getQuestionBreakdown() {
        return questionBreakdown;
    }

    public void setQuestionBreakdown(List<InterviewQuestionBreakdownItem> questionBreakdown) {
        this.questionBreakdown = questionBreakdown;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public Integer getAnsweredQuestions() {
        return answeredQuestions;
    }

    public void setAnsweredQuestions(Integer answeredQuestions) {
        this.answeredQuestions = answeredQuestions;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
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

    public String getIntegrityStatus() {
        return integrityStatus;
    }

    public void setIntegrityStatus(String integrityStatus) {
        this.integrityStatus = integrityStatus;
    }

    public List<MonitoringEventDto> getMonitoringEvents() {
        return monitoringEvents;
    }

    public void setMonitoringEvents(List<MonitoringEventDto> monitoringEvents) {
        this.monitoringEvents = monitoringEvents;
    }
}
