package com.aihiringplatform.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai.gemini")
public class GeminiProperties {

    private boolean enabled = true;
    private String apiKey;
    private String model = "gemini-2.5-flash";
    private String baseUrl = "https://generativelanguage.googleapis.com";
    private int connectTimeoutMillis = 4000;
    private int readTimeoutMillis = 10000;
    private int maxRetries = 1;
    private long retryDelayMillis = 1000;
    private long queueSpacingMillis = 1200;
    private long rateLimitCooldownMillis = 10000;
    private long maxBackoffMillis = 12000;
    private int maxInputChars = 6000;
    private int maxResumeChars = 24000;
    private int maxJobDescriptionChars = 12000;
    private int maxSkills = 12;
    private int requestsPerMinute = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getRetryDelayMillis() {
        return retryDelayMillis;
    }

    public void setRetryDelayMillis(long retryDelayMillis) {
        this.retryDelayMillis = retryDelayMillis;
    }

    public long getQueueSpacingMillis() {
        return queueSpacingMillis;
    }

    public void setQueueSpacingMillis(long queueSpacingMillis) {
        this.queueSpacingMillis = queueSpacingMillis;
    }

    public long getRateLimitCooldownMillis() {
        return rateLimitCooldownMillis;
    }

    public void setRateLimitCooldownMillis(long rateLimitCooldownMillis) {
        this.rateLimitCooldownMillis = rateLimitCooldownMillis;
    }

    public long getMaxBackoffMillis() {
        return maxBackoffMillis;
    }

    public void setMaxBackoffMillis(long maxBackoffMillis) {
        this.maxBackoffMillis = maxBackoffMillis;
    }

    public int getMaxInputChars() {
        return maxInputChars;
    }

    public void setMaxInputChars(int maxInputChars) {
        this.maxInputChars = maxInputChars;
    }

    public int getMaxResumeChars() {
        return maxResumeChars;
    }

    public void setMaxResumeChars(int maxResumeChars) {
        this.maxResumeChars = maxResumeChars;
    }

    public int getMaxJobDescriptionChars() {
        return maxJobDescriptionChars;
    }

    public void setMaxJobDescriptionChars(int maxJobDescriptionChars) {
        this.maxJobDescriptionChars = maxJobDescriptionChars;
    }

    public int getMaxSkills() {
        return maxSkills;
    }

    public void setMaxSkills(int maxSkills) {
        this.maxSkills = maxSkills;
    }

    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }

    public void setRequestsPerMinute(int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }
}
