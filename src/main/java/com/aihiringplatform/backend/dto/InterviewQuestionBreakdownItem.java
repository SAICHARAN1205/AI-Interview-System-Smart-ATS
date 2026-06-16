package com.aihiringplatform.backend.dto;

import java.util.List;

public class InterviewQuestionBreakdownItem {

    private Integer questionNumber;
    private String question;
    private Integer score;
    private Integer technicalAccuracy;
    private Integer communication;
    private Integer confidence;
    private Integer clarity;
    private Integer completeness;
    private Integer roleRelevance;
    private Integer problemSolving;
    private String feedback;
    private List<String> strengths;
    private List<String> weaknesses;

    public Integer getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(Integer questionNumber) {
        this.questionNumber = questionNumber;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getTechnicalAccuracy() {
        return technicalAccuracy;
    }

    public void setTechnicalAccuracy(Integer technicalAccuracy) {
        this.technicalAccuracy = technicalAccuracy;
    }

    public Integer getCommunication() {
        return communication;
    }

    public void setCommunication(Integer communication) {
        this.communication = communication;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
    }

    public Integer getClarity() {
        return clarity;
    }

    public void setClarity(Integer clarity) {
        this.clarity = clarity;
    }

    public Integer getCompleteness() {
        return completeness;
    }

    public void setCompleteness(Integer completeness) {
        this.completeness = completeness;
    }

    public Integer getRoleRelevance() {
        return roleRelevance;
    }

    public void setRoleRelevance(Integer roleRelevance) {
        this.roleRelevance = roleRelevance;
    }

    public Integer getProblemSolving() {
        return problemSolving;
    }

    public void setProblemSolving(Integer problemSolving) {
        this.problemSolving = problemSolving;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(List<String> weaknesses) {
        this.weaknesses = weaknesses;
    }
}
