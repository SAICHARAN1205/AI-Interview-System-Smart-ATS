package com.aihiringplatform.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class InterviewQuestionGenerationRequest {

    @NotBlank(message = "Job role is required")
    @Size(max = 100, message = "Job role must be 100 characters or less")
    private String jobRole;

    @Size(max = 30, message = "Difficulty must be 30 characters or less")
    private String difficulty;

    @Size(max = 12, message = "A maximum of 12 skills is allowed")
    private List<@NotBlank(message = "Skills cannot be blank") @Size(max = 60, message = "Each skill must be 60 characters or less") String> skills;

    @Size(max = 30, message = "Interview type must be 30 characters or less")
    private String interviewType;

    @Size(max = 12000, message = "Job description must be 12000 characters or less")
    private String jobDescription;

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

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }
}
