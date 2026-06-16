package com.aihiringplatform.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class CandidateAnalyticsResponse {

    private List<AnalyticsMetric> overview;
    private List<AnalyticsPoint> atsScoreHistory;
    private List<AnalyticsPoint> interviewScoreHistory;
    private List<AnalyticsSlice> applicationStatuses;
    private List<AnalyticsPoint> skillMatchTrends;
    private List<AnalyticsPoint> communicationTrends;
    private List<AnalyticsPoint> technicalTrends;
    private List<AnalyticsPoint> radarMetrics;
    private List<AnalyticsPoint> atsBreakdown;
    private List<AnalyticsPoint> atsHeatmap;
    private List<AtsAnalysisHistoryItem> atsHistory;
    private List<AnalyticsInterviewHistoryItem> recentInterviews;
    private List<AnalyticsInsight> improvementInsights;
    private String emptyStateMessage;
    private LocalDateTime generatedAt;

    public List<AnalyticsMetric> getOverview() {
        return overview;
    }

    public void setOverview(List<AnalyticsMetric> overview) {
        this.overview = overview;
    }

    public List<AnalyticsPoint> getAtsScoreHistory() {
        return atsScoreHistory;
    }

    public void setAtsScoreHistory(List<AnalyticsPoint> atsScoreHistory) {
        this.atsScoreHistory = atsScoreHistory;
    }

    public List<AnalyticsPoint> getInterviewScoreHistory() {
        return interviewScoreHistory;
    }

    public void setInterviewScoreHistory(List<AnalyticsPoint> interviewScoreHistory) {
        this.interviewScoreHistory = interviewScoreHistory;
    }

    public List<AnalyticsSlice> getApplicationStatuses() {
        return applicationStatuses;
    }

    public void setApplicationStatuses(List<AnalyticsSlice> applicationStatuses) {
        this.applicationStatuses = applicationStatuses;
    }

    public List<AnalyticsPoint> getSkillMatchTrends() {
        return skillMatchTrends;
    }

    public void setSkillMatchTrends(List<AnalyticsPoint> skillMatchTrends) {
        this.skillMatchTrends = skillMatchTrends;
    }

    public List<AnalyticsPoint> getCommunicationTrends() {
        return communicationTrends;
    }

    public void setCommunicationTrends(List<AnalyticsPoint> communicationTrends) {
        this.communicationTrends = communicationTrends;
    }

    public List<AnalyticsPoint> getTechnicalTrends() {
        return technicalTrends;
    }

    public void setTechnicalTrends(List<AnalyticsPoint> technicalTrends) {
        this.technicalTrends = technicalTrends;
    }

    public List<AnalyticsPoint> getRadarMetrics() {
        return radarMetrics;
    }

    public void setRadarMetrics(List<AnalyticsPoint> radarMetrics) {
        this.radarMetrics = radarMetrics;
    }

    public List<AnalyticsPoint> getAtsBreakdown() {
        return atsBreakdown;
    }

    public void setAtsBreakdown(List<AnalyticsPoint> atsBreakdown) {
        this.atsBreakdown = atsBreakdown;
    }

    public List<AnalyticsPoint> getAtsHeatmap() {
        return atsHeatmap;
    }

    public void setAtsHeatmap(List<AnalyticsPoint> atsHeatmap) {
        this.atsHeatmap = atsHeatmap;
    }

    public List<AtsAnalysisHistoryItem> getAtsHistory() {
        return atsHistory;
    }

    public void setAtsHistory(List<AtsAnalysisHistoryItem> atsHistory) {
        this.atsHistory = atsHistory;
    }

    public List<AnalyticsInterviewHistoryItem> getRecentInterviews() {
        return recentInterviews;
    }

    public void setRecentInterviews(List<AnalyticsInterviewHistoryItem> recentInterviews) {
        this.recentInterviews = recentInterviews;
    }

    public List<AnalyticsInsight> getImprovementInsights() {
        return improvementInsights;
    }

    public void setImprovementInsights(List<AnalyticsInsight> improvementInsights) {
        this.improvementInsights = improvementInsights;
    }

    public String getEmptyStateMessage() {
        return emptyStateMessage;
    }

    public void setEmptyStateMessage(String emptyStateMessage) {
        this.emptyStateMessage = emptyStateMessage;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
