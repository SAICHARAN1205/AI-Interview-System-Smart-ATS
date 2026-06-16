package com.aihiringplatform.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResumeAnalysisResponse {

    private Integer atsScore;
    private Integer keywordMatch;
    private Integer resumeConfidenceScore;
    private String atsCompatibility;
    private String recruiterImpression;
    private String formattingQuality;
    private List<String> formattingIssues;
    private String projectQuality;
    private String resumeValidationStatus;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> missingSkills;
    private List<String> improvementSuggestions;
    private List<String> optimizationFeedback;
    private List<String> optimizationTips;
    private List<String> missingKeywords;
    private List<String> matchedKeywords;
    private List<String> detectedResumeSignals;
    private String summary;
    private String finalVerdict;
    private Boolean fallbackUsed;
    private String message;

    public Integer getAtsScore() {
        return atsScore;
    }

    public void setAtsScore(Integer atsScore) {
        this.atsScore = atsScore;
    }

    public Integer getKeywordMatch() {
        return keywordMatch;
    }

    public void setKeywordMatch(Integer keywordMatch) {
        this.keywordMatch = keywordMatch;
    }

    public Integer getResumeConfidenceScore() {
        return resumeConfidenceScore;
    }

    public void setResumeConfidenceScore(Integer resumeConfidenceScore) {
        this.resumeConfidenceScore = resumeConfidenceScore;
    }

    public String getAtsCompatibility() {
        return atsCompatibility;
    }

    public void setAtsCompatibility(String atsCompatibility) {
        this.atsCompatibility = atsCompatibility;
    }

    public String getRecruiterImpression() {
        return recruiterImpression;
    }

    public void setRecruiterImpression(String recruiterImpression) {
        this.recruiterImpression = recruiterImpression;
    }

    public String getFormattingQuality() {
        return formattingQuality;
    }

    public void setFormattingQuality(String formattingQuality) {
        this.formattingQuality = formattingQuality;
    }

    public List<String> getFormattingIssues() {
        return formattingIssues;
    }

    public void setFormattingIssues(List<String> formattingIssues) {
        this.formattingIssues = formattingIssues;
    }

    public String getProjectQuality() {
        return projectQuality;
    }

    public void setProjectQuality(String projectQuality) {
        this.projectQuality = projectQuality;
    }

    public String getResumeValidationStatus() {
        return resumeValidationStatus;
    }

    public void setResumeValidationStatus(String resumeValidationStatus) {
        this.resumeValidationStatus = resumeValidationStatus;
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

    public List<String> getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(List<String> missingSkills) {
        this.missingSkills = missingSkills;
    }

    public List<String> getImprovementSuggestions() {
        return improvementSuggestions;
    }

    public void setImprovementSuggestions(List<String> improvementSuggestions) {
        this.improvementSuggestions = improvementSuggestions;
    }

    public List<String> getOptimizationFeedback() {
        return optimizationFeedback;
    }

    public void setOptimizationFeedback(List<String> optimizationFeedback) {
        this.optimizationFeedback = optimizationFeedback;
    }

    public List<String> getOptimizationTips() {
        return optimizationTips;
    }

    public void setOptimizationTips(List<String> optimizationTips) {
        this.optimizationTips = optimizationTips;
    }

    public List<String> getMissingKeywords() {
        return missingKeywords;
    }

    public void setMissingKeywords(List<String> missingKeywords) {
        this.missingKeywords = missingKeywords;
    }

    public List<String> getMatchedKeywords() {
        return matchedKeywords;
    }

    public void setMatchedKeywords(List<String> matchedKeywords) {
        this.matchedKeywords = matchedKeywords;
    }

    public List<String> getDetectedResumeSignals() {
        return detectedResumeSignals;
    }

    public void setDetectedResumeSignals(List<String> detectedResumeSignals) {
        this.detectedResumeSignals = detectedResumeSignals;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getFinalVerdict() {
        return finalVerdict;
    }

    public void setFinalVerdict(String finalVerdict) {
        this.finalVerdict = finalVerdict;
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
