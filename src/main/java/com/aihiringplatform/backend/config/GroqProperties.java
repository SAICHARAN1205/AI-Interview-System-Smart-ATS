package com.aihiringplatform.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai.groq")
public class GroqProperties implements ChatCompletionProviderProperties {

    private boolean enabled = true;
    private String apiKey;
    private String model = "llama-3.3-70b-versatile";
    private String baseUrl = "https://api.groq.com/openai";
    private int connectTimeoutMillis = 3000;
    private int readTimeoutMillis = 8000;
    private int maxRetries = 0;
    private long retryDelayMillis = 750L;
    private long queueSpacingMillis = 250L;
    private long rateLimitCooldownMillis = 8000L;
    private long maxBackoffMillis = 10000L;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    @Override
    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    @Override
    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    @Override
    public long getRetryDelayMillis() {
        return retryDelayMillis;
    }

    public void setRetryDelayMillis(long retryDelayMillis) {
        this.retryDelayMillis = retryDelayMillis;
    }

    @Override
    public long getQueueSpacingMillis() {
        return queueSpacingMillis;
    }

    public void setQueueSpacingMillis(long queueSpacingMillis) {
        this.queueSpacingMillis = queueSpacingMillis;
    }

    @Override
    public long getRateLimitCooldownMillis() {
        return rateLimitCooldownMillis;
    }

    public void setRateLimitCooldownMillis(long rateLimitCooldownMillis) {
        this.rateLimitCooldownMillis = rateLimitCooldownMillis;
    }

    @Override
    public long getMaxBackoffMillis() {
        return maxBackoffMillis;
    }

    public void setMaxBackoffMillis(long maxBackoffMillis) {
        this.maxBackoffMillis = maxBackoffMillis;
    }
}
