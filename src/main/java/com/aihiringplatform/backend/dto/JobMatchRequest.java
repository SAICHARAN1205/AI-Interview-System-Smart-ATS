package com.aihiringplatform.backend.dto;

import jakarta.validation.constraints.Size;

public class JobMatchRequest {

    @Size(max = 30000, message = "Resume text must be 30000 characters or less")
    private String resumeText;

    @Size(max = 12000, message = "Candidate profile must be 12000 characters or less")
    private String candidateProfile;

    @Size(max = 100, message = "Target role must be 100 characters or less")
    private String targetRole;

    @Size(max = 12000, message = "Job description must be 12000 characters or less")
    private String jobDescription;

    private Long jobId;

    public String getResumeText() {
        return resumeText;
    }

    public void setResumeText(String resumeText) {
        this.resumeText = resumeText;
    }

    public String getCandidateProfile() {
        return candidateProfile;
    }

    public void setCandidateProfile(String candidateProfile) {
        this.candidateProfile = candidateProfile;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }
}
