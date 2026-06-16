package com.aihiringplatform.backend.service.ai;

public record AIGatewayResult<T>(T payload, String providerUsed, String modelUsed, boolean localFallbackUsed) {

    public AIGatewayResult(T payload, String providerUsed, boolean localFallbackUsed) {
        this(payload, providerUsed, null, localFallbackUsed);
    }
}
