package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.config.AiStabilityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class AiResponseCacheService {

    private static final Logger logger = LoggerFactory.getLogger(AiResponseCacheService.class);

    private final AiStabilityProperties properties;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public AiResponseCacheService(AiStabilityProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public <T> T getOrCompute(String operation, String cacheKey, Class<T> responseType, Supplier<T> supplier) {
        long ttlMillis = Math.max(0L, properties.getCacheTtlMillis());
        long now = Instant.now().toEpochMilli();
        String resolvedOperation = normalizeOperation(operation);
        String resolvedKey = resolvedOperation + "::" + cacheKey;

        if (ttlMillis > 0L) {
            CacheEntry cached = cache.get(resolvedKey);
            if (cached != null && cached.expiresAtMillis() > now) {
                logger.info(
                        "AI response cache hit for {} (age={} ms, expiresIn={} ms)",
                        resolvedOperation,
                        now - cached.createdAtMillis(),
                        cached.expiresAtMillis() - now
                );
                return deepCopy(cached.payload(), responseType);
            }
        }

        logger.info("AI response cache miss for {}", resolvedOperation);
        T computed = supplier.get();
        if (computed == null || ttlMillis <= 0L) {
            return computed;
        }

        cache.put(
                resolvedKey,
                new CacheEntry(
                        deepCopy(computed, responseType),
                        now,
                        now + ttlMillis
                )
        );
        trimCacheIfNeeded();
        return deepCopy(computed, responseType);
    }

    private void trimCacheIfNeeded() {
        int maxEntries = Math.max(1, properties.getCacheMaxEntries());
        if (cache.size() <= maxEntries) {
            return;
        }

        int removeCount = cache.size() - maxEntries;
        cache.entrySet().stream()
                .sorted(Comparator.comparingLong(entry -> entry.getValue().createdAtMillis()))
                .limit(removeCount)
                .map(Map.Entry::getKey)
                .toList()
                .forEach(cache::remove);
    }

    private String normalizeOperation(String operation) {
        return operation == null || operation.isBlank() ? "ai-operation" : operation.trim();
    }

    private <T> T deepCopy(Object value, Class<T> responseType) {
        return objectMapper.convertValue(value, responseType);
    }

    private record CacheEntry(
            Object payload,
            long createdAtMillis,
            long expiresAtMillis
    ) {
    }
}
