package com.aihiringplatform.backend.dto;

import java.util.List;

public class InterviewEvaluationRequest {

    private String jobRole;
    private String difficulty;
    private List<String> skills;
    private List<InterviewTranscriptItem> transcript;

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

    public List<InterviewTranscriptItem> getTranscript() {
        return transcript;
    }

    public void setTranscript(List<InterviewTranscriptItem> transcript) {
        this.transcript = transcript;
    }
}
