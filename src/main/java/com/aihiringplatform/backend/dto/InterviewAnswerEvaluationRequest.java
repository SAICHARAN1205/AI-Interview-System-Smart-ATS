package com.aihiringplatform.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class InterviewAnswerEvaluationRequest {

    @NotBlank(message = "Interview question is required")
    @Size(max = 1200, message = "Interview question must be 1200 characters or less")
    private String question;

    @NotBlank(message = "Candidate answer is required")
    @Size(max = 6000, message = "Candidate answer must be 6000 characters or less")
    private String candidateAnswer;

    @NotBlank(message = "Job role is required")
    @Size(max = 100, message = "Job role must be 100 characters or less")
    private String jobRole;

    @Size(max = 12, message = "A maximum of 12 expected skills is allowed")
    private List<@NotBlank(message = "Expected skills cannot be blank") @Size(max = 60, message = "Each expected skill must be 60 characters or less") String> expectedSkills;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getCandidateAnswer() {
        return candidateAnswer;
    }

    public void setCandidateAnswer(String candidateAnswer) {
        this.candidateAnswer = candidateAnswer;
    }

    public String getJobRole() {
        return jobRole;
    }

    public void setJobRole(String jobRole) {
        this.jobRole = jobRole;
    }

    public List<String> getExpectedSkills() {
        return expectedSkills;
    }

    public void setExpectedSkills(List<String> expectedSkills) {
        this.expectedSkills = expectedSkills;
    }
}
