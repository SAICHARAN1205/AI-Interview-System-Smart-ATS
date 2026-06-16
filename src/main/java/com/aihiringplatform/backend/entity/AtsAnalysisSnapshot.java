package com.aihiringplatform.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "ats_analysis_snapshots")
public class AtsAnalysisSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    private String sourceFileName;
    private String targetRole;
    private Integer atsScore;
    private Integer keywordCoverageScore;
    private Integer formattingScore;
    private Integer projectScore;
    private Boolean fallbackUsed;
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String atsCompatibility;

    @Column(columnDefinition = "TEXT")
    private String formattingQuality;

    @Column(columnDefinition = "TEXT")
    private String projectQuality;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String matchedKeywordsJson;

    @Column(columnDefinition = "TEXT")
    private String missingKeywordsJson;

    @Column(columnDefinition = "TEXT")
    private String strengthsJson;

    @Column(columnDefinition = "TEXT")
    private String weaknessesJson;

    @Column(columnDefinition = "TEXT")
    private String optimizationTipsJson;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getCandidate() {
        return candidate;
    }

    public void setCandidate(User candidate) {
        this.candidate = candidate;
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

    public Boolean getFallbackUsed() {
        return fallbackUsed;
    }

    public void setFallbackUsed(Boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMatchedKeywordsJson() {
        return matchedKeywordsJson;
    }

    public void setMatchedKeywordsJson(String matchedKeywordsJson) {
        this.matchedKeywordsJson = matchedKeywordsJson;
    }

    public String getMissingKeywordsJson() {
        return missingKeywordsJson;
    }

    public void setMissingKeywordsJson(String missingKeywordsJson) {
        this.missingKeywordsJson = missingKeywordsJson;
    }

    public String getStrengthsJson() {
        return strengthsJson;
    }

    public void setStrengthsJson(String strengthsJson) {
        this.strengthsJson = strengthsJson;
    }

    public String getWeaknessesJson() {
        return weaknessesJson;
    }

    public void setWeaknessesJson(String weaknessesJson) {
        this.weaknessesJson = weaknessesJson;
    }

    public String getOptimizationTipsJson() {
        return optimizationTipsJson;
    }

    public void setOptimizationTipsJson(String optimizationTipsJson) {
        this.optimizationTipsJson = optimizationTipsJson;
    }
}
