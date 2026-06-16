package com.aihiringplatform.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InterviewAnswerEvaluationResponse {

    @JsonAlias("overallScore")
    private Integer score;
    private Integer communicationScore;
    private Integer technicalScore;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> missingConcepts;
    private List<String> improvementSuggestions;
    private String communicationEvaluation;
    private String technicalRelevance;
    private String summary;
    private String finalFeedback;
    private Boolean fallbackUsed;
    private String message;

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getOverallScore() {
        return score;
    }

    public void setOverallScore(Integer overallScore) {
        this.score = overallScore;
    }

    public Integer getCommunicationScore() {
        return communicationScore;
    }

    public void setCommunicationScore(Integer communicationScore) {
        this.communicationScore = communicationScore;
    }

    public Integer getTechnicalScore() {
        return technicalScore;
    }

    public void setTechnicalScore(Integer technicalScore) {
        this.technicalScore = technicalScore;
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

    public List<String> getMissingConcepts() {
        return missingConcepts;
    }

    public void setMissingConcepts(List<String> missingConcepts) {
        this.missingConcepts = missingConcepts;
    }

    public List<String> getImprovementSuggestions() {
        return improvementSuggestions;
    }

    public void setImprovementSuggestions(List<String> improvementSuggestions) {
        this.improvementSuggestions = improvementSuggestions;
    }

    public String getCommunicationEvaluation() {
        return communicationEvaluation;
    }

    public void setCommunicationEvaluation(String communicationEvaluation) {
        this.communicationEvaluation = communicationEvaluation;
    }

    public String getTechnicalRelevance() {
        return technicalRelevance;
    }

    public void setTechnicalRelevance(String technicalRelevance) {
        this.technicalRelevance = technicalRelevance;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getFinalFeedback() {
        return finalFeedback;
    }

    public void setFinalFeedback(String finalFeedback) {
        this.finalFeedback = finalFeedback;
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
