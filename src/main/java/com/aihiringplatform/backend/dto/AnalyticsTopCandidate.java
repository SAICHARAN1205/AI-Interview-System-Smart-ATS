package com.aihiringplatform.backend.dto;

public class AnalyticsTopCandidate {
    private String candidateName;
    private String candidateEmail;
    private String jobTitle;
    private String status;
    private Integer atsScore;
    private Integer matchScore;
    private String appliedAt;

    public AnalyticsTopCandidate() {}

    public AnalyticsTopCandidate(String candidateName, String candidateEmail, String jobTitle, String status, Integer atsScore, Integer matchScore, String appliedAt) {
        this.candidateName = candidateName;
        this.candidateEmail = candidateEmail;
        this.jobTitle = jobTitle;
        this.status = status;
        this.atsScore = atsScore;
        this.matchScore = matchScore;
        this.appliedAt = appliedAt;
    }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String candidateEmail) { this.candidateEmail = candidateEmail; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getAtsScore() { return atsScore; }
    public void setAtsScore(Integer atsScore) { this.atsScore = atsScore; }

    public Integer getMatchScore() { return matchScore; }
    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }

    public String getAppliedAt() { return appliedAt; }
    public void setAppliedAt(String appliedAt) { this.appliedAt = appliedAt; }
}
