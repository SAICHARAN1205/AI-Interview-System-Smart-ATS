package com.aihiringplatform.backend.config;

public interface ChatCompletionProviderProperties {

    boolean isEnabled();

    String getApiKey();

    String getModel();

    String getBaseUrl();

    int getConnectTimeoutMillis();

    int getReadTimeoutMillis();

    int getMaxRetries();

    long getRetryDelayMillis();

    long getQueueSpacingMillis();

    long getRateLimitCooldownMillis();

    long getMaxBackoffMillis();
}
