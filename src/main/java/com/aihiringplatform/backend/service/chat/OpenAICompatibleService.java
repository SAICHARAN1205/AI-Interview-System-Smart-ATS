package com.aihiringplatform.backend.service.chat;

import com.aihiringplatform.backend.config.ChatCompletionProviderProperties;
import com.aihiringplatform.backend.service.AIResponseParser;
import com.aihiringplatform.backend.service.PromptBuilderService;
import com.aihiringplatform.backend.service.ai.StructuredResponseExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

public class OpenAICompatibleService implements StructuredResponseExecutor {

    private static final Logger logger = LoggerFactory.getLogger(OpenAICompatibleService.class);

    private final String providerName;
    private final RestClient restClient;
    private final ChatCompletionProviderProperties properties;
    private final AIResponseParser responseParser;
    private final Object requestQueueMonitor = new Object();
    private long nextRequestAtMillis = 0L;
    private long cooldownUntilMillis = 0L;

    public OpenAICompatibleService(
            String providerName,
            RestClient restClient,
            ChatCompletionProviderProperties properties,
            AIResponseParser responseParser
    ) {
        this.providerName = providerName;
        this.restClient = restClient;
        this.properties = properties;
        this.responseParser = responseParser;
    }

    public boolean isConfigured() {
        return properties.isEnabled()
                && properties.getApiKey() != null
                && !properties.getApiKey().isBlank();
    }

    public String getModel() {
        return properties.getModel();
    }

    @Override
    public <T> T generateStructuredResponse(
            PromptBuilderService.PromptDefinition prompt,
            Class<T> responseType
    ) {
        ensureConfigured();

        int attempts = Math.max(1, properties.getMaxRetries() + 1);
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            waitForQueueTurn(prompt.operation());
            long startedAt = System.currentTimeMillis();

            try {
                logger.info(
                        "{} request started for {} using model {}",
                        providerName,
                        prompt.operation(),
                        properties.getModel()
                );

                T parsed = executeRequest(prompt, responseType);
                logger.info(
                        "{} {} succeeded using model {} in {} ms on attempt {}",
                        providerName,
                        prompt.operation(),
                        properties.getModel(),
                        System.currentTimeMillis() - startedAt,
                        attempt
                );
                return parsed;
            } catch (RestClientResponseException exception) {
                lastFailure = exception;
                int statusCode = exception.getStatusCode().value();
                logger.warn(
                        "{} {} failed using model {} in {} ms on attempt {}/{} with status {}",
                        providerName,
                        prompt.operation(),
                        properties.getModel(),
                        System.currentTimeMillis() - startedAt,
                        attempt,
                        attempts,
                        statusCode
                );

                if (statusCode == 429) {
                    long cooldownMillis = applyCooldown(exception);
                    logger.warn(
                            "{} {} rate-limited on model {}. Cooling down for {} ms",
                            providerName,
                            prompt.operation(),
                            properties.getModel(),
                            cooldownMillis
                    );
                }

                if (!shouldRetry(statusCode, attempt, attempts)) {
                    break;
                }
            } catch (RuntimeException exception) {
                lastFailure = exception;
                logger.warn(
                        "{} {} failed using model {} in {} ms on attempt {}/{}: {}",
                        providerName,
                        prompt.operation(),
                        properties.getModel(),
                        System.currentTimeMillis() - startedAt,
                        attempt,
                        attempts,
                        preview(exception.getMessage())
                );

                if (responseParser.isParsingFailure(exception) && attempt < attempts) {
                    sleepBeforeRetry(attempt, false);
                    continue;
                }

                if (attempt >= attempts) {
                    break;
                }
            }

            sleepBeforeRetry(attempt, lastFailure instanceof RestClientResponseException responseException
                    && responseException.getStatusCode().value() == 429);
        }

        throw lastFailure == null
                ? new IllegalStateException(providerName + " request failed.")
                : lastFailure;
    }

    private <T> T executeRequest(PromptBuilderService.PromptDefinition prompt, Class<T> responseType) {
        String rawResponse = restClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(buildRequestBody(prompt))
                .retrieve()
                .body(String.class);

        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalStateException(providerName + " returned a null or empty HTTP body.");
        }

        String rawText = responseParser.extractChatCompletionText(providerName, rawResponse);
        String extractedJson = responseParser.extractJsonFromResponse(rawText);

        logger.info(
                "{} parsed output for {} using model {}: {}",
                providerName,
                prompt.operation(),
                properties.getModel(),
                preview(extractedJson)
        );

        return responseParser.parseStructuredPayloadFromJson(extractedJson, responseType);
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException(providerName + " API is not configured.");
        }
    }

    private Map<String, Object> buildRequestBody(PromptBuilderService.PromptDefinition prompt) {
        return Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", strictJsonInstruction(prompt.systemInstruction())),
                        Map.of("role", "user", "content", strictJsonInstruction(prompt.userPrompt()))
                ),
                "stream", false,
                "max_tokens", prompt.maxOutputTokens(),
                "temperature", prompt.temperature()
        );
    }

    private String strictJsonInstruction(String value) {
        return (value == null ? "" : value.trim())
                + "\nReturn ONLY valid JSON. Do not include markdown, explanations, or code fences.";
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 408 || statusCode == 429 || statusCode >= 500;
    }

    private boolean shouldRetry(int statusCode, int attempt, int attempts) {
        if (!isRetryableStatus(statusCode) || attempt >= attempts) {
            return false;
        }

        if (statusCode == 429) {
            return attempt < Math.min(attempts, 2);
        }

        return true;
    }

    private void waitForQueueTurn(String operation) {
        long spacingMillis = Math.max(0L, properties.getQueueSpacingMillis());
        synchronized (requestQueueMonitor) {
            while (true) {
                long now = System.currentTimeMillis();
                long availableAt = Math.max(nextRequestAtMillis, cooldownUntilMillis);
                long waitMillis = availableAt - now;

                if (waitMillis <= 0L) {
                    nextRequestAtMillis = now + spacingMillis;
                    return;
                }

                logger.debug("{} {} waiting {} ms for next request slot", providerName, operation, waitMillis);
                try {
                    requestQueueMonitor.wait(waitMillis);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for " + providerName + " request slot.", interruptedException);
                }
            }
        }
    }

    private long applyCooldown(RestClientResponseException exception) {
        long retryAfterMillis = extractRetryAfterMillis(exception);
        long cooldownMillis = Math.max(properties.getRateLimitCooldownMillis(), retryAfterMillis);
        synchronized (requestQueueMonitor) {
            cooldownUntilMillis = Math.max(cooldownUntilMillis, System.currentTimeMillis() + cooldownMillis);
            requestQueueMonitor.notifyAll();
        }
        return cooldownMillis;
    }

    private long extractRetryAfterMillis(RestClientResponseException exception) {
        if (exception.getResponseHeaders() == null) {
            return 0L;
        }

        String retryAfter = exception.getResponseHeaders().getFirst("Retry-After");
        if (retryAfter == null || retryAfter.isBlank()) {
            return 0L;
        }

        try {
            return Math.max(0L, Long.parseLong(retryAfter.trim()) * 1000L);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private void sleepBeforeRetry(int attempt, boolean rateLimited) {
        long baseDelay = Math.max(0L, properties.getRetryDelayMillis());
        long computedDelay = rateLimited
                ? Math.min(
                Math.max(baseDelay, baseDelay * (attempt + 1L)),
                Math.max(baseDelay, properties.getMaxBackoffMillis())
        )
                : Math.min(
                Math.max(baseDelay, baseDelay * (1L << Math.max(0, attempt - 1))),
                Math.max(baseDelay, properties.getMaxBackoffMillis())
        );
        logger.debug("{} backing off for {} ms before retry attempt {}", providerName, computedDelay, attempt + 1);
        try {
            Thread.sleep(computedDelay);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private String preview(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String condensed = value.replaceAll("\\s+", " ").trim();
        return condensed.length() <= 220
                ? condensed
                : condensed.substring(0, 220) + "...";
    }
}
