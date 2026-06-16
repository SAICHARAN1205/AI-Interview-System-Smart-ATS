package com.aihiringplatform.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai.stability")
public class AiStabilityProperties {

    private long requestCooldownMillis = 4000L;
    private long cacheTtlMillis = 120000L;
    private int cacheMaxEntries = 200;
    private int providerFailureThreshold = 2;
    private long providerCooldownMillis = 180000L;
    private long providerMaxCooldownMillis = 300000L;

    public long getRequestCooldownMillis() {
        return requestCooldownMillis;
    }

    public void setRequestCooldownMillis(long requestCooldownMillis) {
        this.requestCooldownMillis = requestCooldownMillis;
    }

    public long getCacheTtlMillis() {
        return cacheTtlMillis;
    }

    public void setCacheTtlMillis(long cacheTtlMillis) {
        this.cacheTtlMillis = cacheTtlMillis;
    }

    public int getCacheMaxEntries() {
        return cacheMaxEntries;
    }

    public void setCacheMaxEntries(int cacheMaxEntries) {
        this.cacheMaxEntries = cacheMaxEntries;
    }

    public int getProviderFailureThreshold() {
        return providerFailureThreshold;
    }

    public void setProviderFailureThreshold(int providerFailureThreshold) {
        this.providerFailureThreshold = providerFailureThreshold;
    }

    public long getProviderCooldownMillis() {
        return providerCooldownMillis;
    }

    public void setProviderCooldownMillis(long providerCooldownMillis) {
        this.providerCooldownMillis = providerCooldownMillis;
    }

    public long getProviderMaxCooldownMillis() {
        return providerMaxCooldownMillis;
    }

    public void setProviderMaxCooldownMillis(long providerMaxCooldownMillis) {
        this.providerMaxCooldownMillis = providerMaxCooldownMillis;
    }
}
