package com.aihiringplatform.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AtsAnalysisHistoryItem {

    private Long id;
    private String sourceFileName;
    private String targetRole;
    private Integer atsScore;
    private Integer keywordCoverageScore;
    private Integer formattingScore;
    private Integer projectScore;
    private String atsCompatibility;
    private String formattingQuality;
    private String projectQuality;
    private String summary;
    private Boolean fallbackUsed;
    private String message;
    private List<String> matchedKeywords;
    private List<String> missingKeywords;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> optimizationTips;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public Integer getAtsScore() {
        return atsScore;
    }

    public void setAtsScore(Integer atsScore) {
        this.atsScore = atsScore;
    }

    public Integer getKeywordCoverageScore() {
        return keywordCoverageScore;
    }

    public void setKeywordCoverageScore(Integer keywordCoverageScore) {
        this.keywordCoverageScore = keywordCoverageScore;
    }

    public Integer getFormattingScore() {
        return formattingScore;
    }

    public void setFormattingScore(Integer formattingScore) {
        this.formattingScore = formattingScore;
    }

    public Integer getProjectScore() {
        return projectScore;
    }

    public void setProjectScore(Integer projectScore) {
        this.projectScore = projectScore;
    }

    public String getAtsCompatibility() {
        return atsCompatibility;
    }

    public void setAtsCompatibility(String atsCompatibility) {
        this.atsCompatibility = atsCompatibility;
    }

    public String getFormattingQuality() {
        return formattingQuality;
    }

    public void setFormattingQuality(String formattingQuality) {
        this.formattingQuality = formattingQuality;
    }

    public String getProjectQuality() {
        return projectQuality;
    }

    public void setProjectQuality(String projectQuality) {
        this.projectQuality = projectQuality;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
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

    public List<String> getMatchedKeywords() {
        return matchedKeywords;
    }

    public void setMatchedKeywords(List<String> matchedKeywords) {
        this.matchedKeywords = matchedKeywords;
    }

    public List<String> getMissingKeywords() {
        return missingKeywords;
    }

    public void setMissingKeywords(List<String> missingKeywords) {
        this.missingKeywords = missingKeywords;
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

    public List<String> getOptimizationTips() {
        return optimizationTips;
    }

    public void setOptimizationTips(List<String> optimizationTips) {
        this.optimizationTips = optimizationTips;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
