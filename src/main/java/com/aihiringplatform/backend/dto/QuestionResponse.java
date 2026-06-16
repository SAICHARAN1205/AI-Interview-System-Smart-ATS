package com.aihiringplatform.backend.dto;

import java.util.List;

public class QuestionResponse {
    private Long jobId;
    private String jobRole;
    private String difficulty;
    private String interviewType;
    private List<String> skills;
    private List<String> questions;
    private List<InterviewQuestionItem> structuredQuestions;
    private Boolean fallbackUsed;
    private String message;

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

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

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public List<String> getQuestions() { return questions; }
    public void setQuestions(List<String> questions) { this.questions = questions; }

    public List<InterviewQuestionItem> getStructuredQuestions() {
        return structuredQuestions;
    }

    public void setStructuredQuestions(List<InterviewQuestionItem> structuredQuestions) {
        this.structuredQuestions = structuredQuestions;
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
