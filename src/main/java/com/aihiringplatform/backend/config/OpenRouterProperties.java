package com.aihiringplatform.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.ai.openrouter")
public class OpenRouterProperties {

    private boolean enabled = true;
    private String apiKey;
    private String baseUrl = "https://openrouter.ai";
    private String primaryModel = "deepseek/deepseek-chat-v3-0324:free";
    private String secondaryModel = "qwen/qwen-2.5-72b-instruct:free";
    private List<String> runtimeFallbackModels = new ArrayList<>(List.of(
            "deepseek/deepseek-v4-flash:free",
            "qwen/qwen3-next-80b-a3b-instruct:free",
            "qwen/qwen3-coder:free"
    ));
    private String httpReferer = "http://localhost:8080";
    private String title = "SmartATS";
    private int connectTimeoutMillis = 4000;
    private int readTimeoutMillis = 12000;
    private int maxRetries = 0;
    private long retryDelayMillis = 1500;
    private long queueSpacingMillis = 500;
    private long rateLimitCooldownMillis = 12000;
    private long maxBackoffMillis = 15000;
    private int maxModelAttempts = 1;

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

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getPrimaryModel() {
        return primaryModel;
    }

    public void setPrimaryModel(String primaryModel) {
        this.primaryModel = primaryModel;
    }

    public String getSecondaryModel() {
        return secondaryModel;
    }

    public void setSecondaryModel(String secondaryModel) {
        this.secondaryModel = secondaryModel;
    }

    public List<String> getRuntimeFallbackModels() {
        return runtimeFallbackModels;
    }

    public void setRuntimeFallbackModels(List<String> runtimeFallbackModels) {
        this.runtimeFallbackModels = runtimeFallbackModels;
    }

    public String getHttpReferer() {
        return httpReferer;
    }

    public void setHttpReferer(String httpReferer) {
        this.httpReferer = httpReferer;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public int getMaxModelAttempts() {
        return maxModelAttempts;
    }

    public void setMaxModelAttempts(int maxModelAttempts) {
        this.maxModelAttempts = maxModelAttempts;
    }
}
