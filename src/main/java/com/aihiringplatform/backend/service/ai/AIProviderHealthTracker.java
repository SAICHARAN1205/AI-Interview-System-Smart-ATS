package com.aihiringplatform.backend.service.ai;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

final class AIProviderHealthTracker {

    private final long baseCooldownMillis;
    private final long maxCooldownMillis;
    private final int failureThreshold;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicLong cooldownUntilMillis = new AtomicLong();

    AIProviderHealthTracker(long baseCooldownMillis, long maxCooldownMillis, int failureThreshold) {
        this.baseCooldownMillis = Math.max(0L, baseCooldownMillis);
        this.maxCooldownMillis = Math.max(this.baseCooldownMillis, maxCooldownMillis);
        this.failureThreshold = Math.max(1, failureThreshold);
    }

    boolean isAvailable() {
        return getRemainingCooldownMillis() <= 0L;
    }

    long getRemainingCooldownMillis() {
        long remaining = cooldownUntilMillis.get() - System.currentTimeMillis();
        return Math.max(0L, remaining);
    }

    void recordSuccess() {
        consecutiveFailures.set(0);
        cooldownUntilMillis.set(0L);
    }

    void recordFailure(boolean healthImpacting) {
        if (!healthImpacting || baseCooldownMillis <= 0L) {
            return;
        }

        int failureCount = consecutiveFailures.incrementAndGet();
        if (failureCount < failureThreshold) {
            return;
        }

        int failuresPastThreshold = failureCount - failureThreshold + 1;
        long scaledCooldown = Math.min(
                Math.max(baseCooldownMillis, baseCooldownMillis * Math.max(1L, failuresPastThreshold)),
                maxCooldownMillis
        );
        long nextCooldownUntil = System.currentTimeMillis() + scaledCooldown;
        cooldownUntilMillis.updateAndGet(current -> Math.max(current, nextCooldownUntil));
    }

    int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }
}
