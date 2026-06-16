package com.aihiringplatform.backend.dto;

public class AnalyticsJobPerformance {

    private Long jobId;
    private String jobTitle;
    private Integer applicants;
    private Integer shortlisted;
    private Integer rejected;
    private Integer interviews;
    private Integer successRate;
    private Integer qualityScore;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public Integer getApplicants() {
        return applicants;
    }

    public void setApplicants(Integer applicants) {
        this.applicants = applicants;
    }

    public Integer getShortlisted() {
        return shortlisted;
    }

    public void setShortlisted(Integer shortlisted) {
        this.shortlisted = shortlisted;
    }

    public Integer getRejected() {
        return rejected;
    }

    public void setRejected(Integer rejected) {
        this.rejected = rejected;
    }

    public Integer getInterviews() {
        return interviews;
    }

    public void setInterviews(Integer interviews) {
        this.interviews = interviews;
    }

    public Integer getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Integer successRate) {
        this.successRate = successRate;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = qualityScore;
    }
}
