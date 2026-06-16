package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.config.AiStabilityProperties;
import com.aihiringplatform.backend.config.GeminiProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class AiRateLimitService {

    private final GeminiProperties properties;
    private final AiStabilityProperties stabilityProperties;
    private final ConcurrentHashMap<String, Deque<Long>> requestWindows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastRequestTimes = new ConcurrentHashMap<>();

    public AiRateLimitService(GeminiProperties properties, AiStabilityProperties stabilityProperties) {
        this.properties = properties;
        this.stabilityProperties = stabilityProperties;
    }

    public void assertAllowed(String action, String actor) {
        int limit = properties.getRequestsPerMinute();
        if (limit <= 0) {
            assertCooldown(action, actor);
            return;
        }

        long now = Instant.now().toEpochMilli();
        long windowStart = now - 60_000;
        String key = action + "::" + (actor == null || actor.isBlank() ? "anonymous" : actor);
        assertCooldown(key, now);
        Deque<Long> timestamps = requestWindows.computeIfAbsent(key, ignored -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= limit) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "AI rate limit reached. Please wait a minute and try again."
                );
            }

            timestamps.addLast(now);
        }
    }

    private void assertCooldown(String action, String actor) {
        String key = action + "::" + (actor == null || actor.isBlank() ? "anonymous" : actor);
        assertCooldown(key, Instant.now().toEpochMilli());
    }

    private void assertCooldown(String key, long now) {
        long cooldownMillis = Math.max(0L, stabilityProperties.getRequestCooldownMillis());
        if (cooldownMillis <= 0L) {
            return;
        }

        Long previousRequestAt = lastRequestTimes.put(key, now);
        if (previousRequestAt == null) {
            return;
        }

        long elapsedMillis = now - previousRequestAt;
        if (elapsedMillis >= cooldownMillis) {
            return;
        }

        lastRequestTimes.put(key, previousRequestAt);
        long waitSeconds = Math.max(1L, (long) Math.ceil((cooldownMillis - elapsedMillis) / 1000.0));
        throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Please wait " + waitSeconds + " more second(s) before requesting another AI analysis."
        );
    }
}
