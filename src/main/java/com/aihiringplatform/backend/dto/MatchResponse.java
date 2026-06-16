package com.aihiringplatform.backend.dto;

import java.util.List;

public class MatchResponse {
    private Long jobId;
    private int score;
    private Integer matchPercentage;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private String recruiterSummary;
    private String targetRole;
    private Boolean fallbackUsed;
    private String message;

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public List<String> getMatchedSkills() { return matchedSkills; }
    public void setMatchedSkills(List<String> matchedSkills) { this.matchedSkills = matchedSkills; }

    public Integer getMatchPercentage() {
        return matchPercentage;
    }

    public void setMatchPercentage(Integer matchPercentage) {
        this.matchPercentage = matchPercentage;
    }

    public List<String> getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(List<String> missingSkills) {
        this.missingSkills = missingSkills;
    }

    public String getRecruiterSummary() {
        return recruiterSummary;
    }

    public void setRecruiterSummary(String recruiterSummary) {
        this.recruiterSummary = recruiterSummary;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
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
}
