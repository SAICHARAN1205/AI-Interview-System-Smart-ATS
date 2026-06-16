package com.aihiringplatform.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ResumeFileResponse {

    private Long id;
    private Long userId;
    private String fileName;
    private String mimeType;
    private String filePath;
    private LocalDateTime uploadedAt;
    private boolean hasResume;
    private Integer resumeConfidenceScore;
    private String resumeValidationStatus;
    private List<String> detectedResumeSignals;
    private String message;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public boolean isHasResume() {
        return hasResume;
    }

    public void setHasResume(boolean hasResume) {
        this.hasResume = hasResume;
    }

    public Integer getResumeConfidenceScore() {
        return resumeConfidenceScore;
    }

    public void setResumeConfidenceScore(Integer resumeConfidenceScore) {
        this.resumeConfidenceScore = resumeConfidenceScore;
    }

    public String getResumeValidationStatus() {
        return resumeValidationStatus;
    }

    public void setResumeValidationStatus(String resumeValidationStatus) {
        this.resumeValidationStatus = resumeValidationStatus;
    }

    public List<String> getDetectedResumeSignals() {
        return detectedResumeSignals;
    }

    public void setDetectedResumeSignals(List<String> detectedResumeSignals) {
        this.detectedResumeSignals = detectedResumeSignals;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
