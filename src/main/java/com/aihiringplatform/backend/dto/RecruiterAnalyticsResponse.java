package com.aihiringplatform.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class RecruiterAnalyticsResponse {

    private List<AnalyticsMetric> overview;
    private List<AnalyticsPoint> applicationsOverTime;
    private List<AnalyticsPoint> applicantGrowth;
    private List<AnalyticsSlice> shortlistBreakdown;
    private List<AnalyticsSlice> rejectionBreakdown;
    private List<AnalyticsJobPerformance> topPerformingJobs;
    private List<AnalyticsJobPerformance> hardestJobsToFill;
    private List<AnalyticsJobPerformance> mostSuccessfulPostings;
    private List<AnalyticsSlice> candidateQualityDistribution;
    private List<AnalyticsInsight> aiInsights;
    private List<AnalyticsMetric> adminPreviewMetrics;
    private Boolean adminPreviewVisible;
    private String emptyStateMessage;
    private LocalDateTime generatedAt;

    // New enterprise analytics fields
    private List<AnalyticsSlice> statusDistribution;
    private List<AnalyticsPoint> atsScoreDistribution;
    private List<AnalyticsPoint> applicationsPerJob;
    private List<AnalyticsPoint> hiringFunnel;
    private List<AnalyticsTopCandidate> topCandidates;

    public List<AnalyticsMetric> getOverview() {
        return overview;
    }

    public void setOverview(List<AnalyticsMetric> overview) {
        this.overview = overview;
    }

    public List<AnalyticsPoint> getApplicationsOverTime() {
        return applicationsOverTime;
    }

    public void setApplicationsOverTime(List<AnalyticsPoint> applicationsOverTime) {
        this.applicationsOverTime = applicationsOverTime;
    }

    public List<AnalyticsPoint> getApplicantGrowth() {
        return applicantGrowth;
    }

    public void setApplicantGrowth(List<AnalyticsPoint> applicantGrowth) {
        this.applicantGrowth = applicantGrowth;
    }

    public List<AnalyticsSlice> getShortlistBreakdown() {
        return shortlistBreakdown;
    }

    public void setShortlistBreakdown(List<AnalyticsSlice> shortlistBreakdown) {
        this.shortlistBreakdown = shortlistBreakdown;
    }

    public List<AnalyticsSlice> getRejectionBreakdown() {
        return rejectionBreakdown;
    }

    public void setRejectionBreakdown(List<AnalyticsSlice> rejectionBreakdown) {
        this.rejectionBreakdown = rejectionBreakdown;
    }

    public List<AnalyticsJobPerformance> getTopPerformingJobs() {
        return topPerformingJobs;
    }

    public void setTopPerformingJobs(List<AnalyticsJobPerformance> topPerformingJobs) {
        this.topPerformingJobs = topPerformingJobs;
    }

    public List<AnalyticsJobPerformance> getHardestJobsToFill() {
        return hardestJobsToFill;
    }

    public void setHardestJobsToFill(List<AnalyticsJobPerformance> hardestJobsToFill) {
        this.hardestJobsToFill = hardestJobsToFill;
    }

    public List<AnalyticsJobPerformance> getMostSuccessfulPostings() {
        return mostSuccessfulPostings;
    }

    public void setMostSuccessfulPostings(List<AnalyticsJobPerformance> mostSuccessfulPostings) {
        this.mostSuccessfulPostings = mostSuccessfulPostings;
    }

    public List<AnalyticsSlice> getCandidateQualityDistribution() {
        return candidateQualityDistribution;
    }

    public void setCandidateQualityDistribution(List<AnalyticsSlice> candidateQualityDistribution) {
        this.candidateQualityDistribution = candidateQualityDistribution;
    }

    public List<AnalyticsInsight> getAiInsights() {
        return aiInsights;
    }

    public void setAiInsights(List<AnalyticsInsight> aiInsights) {
        this.aiInsights = aiInsights;
    }

    public List<AnalyticsMetric> getAdminPreviewMetrics() {
        return adminPreviewMetrics;
    }

    public void setAdminPreviewMetrics(List<AnalyticsMetric> adminPreviewMetrics) {
        this.adminPreviewMetrics = adminPreviewMetrics;
    }

    public Boolean getAdminPreviewVisible() {
        return adminPreviewVisible;
    }

    public void setAdminPreviewVisible(Boolean adminPreviewVisible) {
        this.adminPreviewVisible = adminPreviewVisible;
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

    public List<AnalyticsSlice> getStatusDistribution() {
        return statusDistribution;
    }

    public void setStatusDistribution(List<AnalyticsSlice> statusDistribution) {
        this.statusDistribution = statusDistribution;
    }

    public List<AnalyticsPoint> getAtsScoreDistribution() {
        return atsScoreDistribution;
    }

    public void setAtsScoreDistribution(List<AnalyticsPoint> atsScoreDistribution) {
        this.atsScoreDistribution = atsScoreDistribution;
    }

    public List<AnalyticsPoint> getApplicationsPerJob() {
        return applicationsPerJob;
    }

    public void setApplicationsPerJob(List<AnalyticsPoint> applicationsPerJob) {
        this.applicationsPerJob = applicationsPerJob;
    }

    public List<AnalyticsPoint> getHiringFunnel() {
        return hiringFunnel;
    }

    public void setHiringFunnel(List<AnalyticsPoint> hiringFunnel) {
        this.hiringFunnel = hiringFunnel;
    }

    public List<AnalyticsTopCandidate> getTopCandidates() {
        return topCandidates;
    }

    public void setTopCandidates(List<AnalyticsTopCandidate> topCandidates) {
        this.topCandidates = topCandidates;
    }
}
