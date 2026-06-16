package com.aihiringplatform.backend.service.gemini;

import com.aihiringplatform.backend.config.GeminiProperties;
import com.aihiringplatform.backend.service.AIResponseParser;
import com.aihiringplatform.backend.service.PromptBuilderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Component
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    private final RestClient restClient;
    private final GeminiProperties properties;
    private final AIResponseParser responseParser;
    private final Object requestQueueMonitor = new Object();
    private long nextRequestAtMillis = 0L;
    private long cooldownUntilMillis = 0L;

    public GeminiService(
            @Qualifier("geminiRestClient") RestClient geminiRestClient,
            GeminiProperties properties,
            AIResponseParser responseParser
    ) {
        this.restClient = geminiRestClient;
        this.properties = properties;
        this.responseParser = responseParser;
    }

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
                T parsed = executeRequest(prompt, responseType);

                logger.info(
                        "Gemini {} succeeded in {} ms on attempt {}",
                        prompt.operation(),
                        System.currentTimeMillis() - startedAt,
                        attempt
                );
                return parsed;
            } catch (RestClientResponseException exception) {
                lastFailure = exception;
                logger.warn(
                        "Gemini {} failed in {} ms on attempt {}/{} with status {}",
                        prompt.operation(),
                        System.currentTimeMillis() - startedAt,
                        attempt,
                        attempts,
                        exception.getStatusCode().value()
                );

                int statusCode = exception.getStatusCode().value();
                if (statusCode == 429) {
                    long cooldownMillis = applyCooldown(exception);
                    logger.warn(
                            "Gemini {} rate-limited. Cooling down requests for {} ms",
                            prompt.operation(),
                            cooldownMillis
                    );
                }

                if (!shouldRetry(statusCode, attempt, attempts)) {
                    break;
                }
            } catch (RuntimeException exception) {
                if (isInterviewEvaluation(prompt) && responseParser.isParsingFailure(exception)) {
                    logger.warn(
                            "Gemini {} parsing failed on attempt {}. Retrying once with stricter JSON prompt. Reason: {}",
                            prompt.operation(),
                            attempt,
                            exception.getMessage()
                    );

                    try {
                        T parsed = executeRequest(buildStricterInterviewEvaluationPrompt(prompt), responseType);
                        logger.info(
                                "Gemini {} succeeded after strict JSON retry in {} ms on attempt {}",
                                prompt.operation(),
                                System.currentTimeMillis() - startedAt,
                                attempt
                        );
                        return parsed;
                    } catch (RuntimeException retryException) {
                        lastFailure = retryException;
                        logger.warn(
                                "Gemini {} strict JSON retry failed. Falling back to next provider. Reason: {}",
                                prompt.operation(),
                                retryException.getMessage()
                        );
                        break;
                    }
                }

                lastFailure = exception;
                logger.warn(
                        "Gemini {} failed in {} ms on attempt {}/{}: {}",
                        prompt.operation(),
                        System.currentTimeMillis() - startedAt,
                        attempt,
                        attempts,
                        exception.getClass().getSimpleName()
                );

                if (attempt >= attempts) {
                    break;
                }
            }

            sleepBeforeRetry(attempt, lastFailure instanceof RestClientResponseException responseException
                    && responseException.getStatusCode().value() == 429);
        }

        throw lastFailure == null
                ? new IllegalStateException("Gemini request failed.")
                : lastFailure;
    }

    private <T> T executeRequest(PromptBuilderService.PromptDefinition prompt, Class<T> responseType) {
        String rawResponse = restClient.post()
                .uri("/v1beta/models/{model}:generateContent", properties.getModel())
                .header("x-goog-api-key", properties.getApiKey())
                .header("Content-Type", "application/json")
                .body(buildRequestBody(prompt))
                .retrieve()
                .body(String.class);

        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalStateException("Gemini returned a null or empty HTTP body.");
        }

        String rawText = responseParser.extractGeminiText(rawResponse);
        String extractedJson = responseParser.extractJsonFromResponse(rawText);

        if (isInterviewEvaluation(prompt)) {
            logger.info("Gemini raw AI response for {}: {}", prompt.operation(), rawText);
            logger.info("Gemini parsed JSON for {}: {}", prompt.operation(), extractedJson);
        }

        return responseParser.parseStructuredPayloadFromJson(extractedJson, responseType);
    }

    private boolean isInterviewEvaluation(PromptBuilderService.PromptDefinition prompt) {
        return prompt != null && "interview-evaluation".equals(prompt.operation());
    }

    private PromptBuilderService.PromptDefinition buildStricterInterviewEvaluationPrompt(
            PromptBuilderService.PromptDefinition original
    ) {
        return new PromptBuilderService.PromptDefinition(
                original.operation(),
                original.systemInstruction() + "\nReturn ONLY valid JSON. Do not include markdown, explanations, code fences, or any text before or after the JSON object.",
                original.userPrompt() + "\n\nFinal reminder: Return ONLY valid JSON. Do not include markdown, explanations, code fences, or extra text.",
                original.responseSchema(),
                original.maxOutputTokens(),
                0.0
        );
    }

    private void ensureConfigured() {
        if (!properties.isEnabled() || properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("Gemini API is not configured.");
        }
    }

    private Map<String, Object> buildRequestBody(PromptBuilderService.PromptDefinition prompt) {
        return Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", prompt.systemInstruction()))
                ),
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", prompt.userPrompt()))
                        )
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseJsonSchema", prompt.responseSchema(),
                        "maxOutputTokens", prompt.maxOutputTokens(),
                        "temperature", prompt.temperature()
                ),
                "safetySettings", List.of(
                        Map.of(
                                "category", "HARM_CATEGORY_HATE_SPEECH",
                                "threshold", "BLOCK_MEDIUM_AND_ABOVE"
                        ),
                        Map.of(
                                "category", "HARM_CATEGORY_HARASSMENT",
                                "threshold", "BLOCK_MEDIUM_AND_ABOVE"
                        ),
                        Map.of(
                                "category", "HARM_CATEGORY_DANGEROUS_CONTENT",
                                "threshold", "BLOCK_MEDIUM_AND_ABOVE"
                        ),
                        Map.of(
                                "category", "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                                "threshold", "BLOCK_MEDIUM_AND_ABOVE"
                        )
                )
        );
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

                if (waitMillis <= 0) {
                    nextRequestAtMillis = now + spacingMillis;
                    return;
                }

                logger.debug("Gemini {} waiting {} ms for next request slot", operation, waitMillis);
                try {
                    requestQueueMonitor.wait(waitMillis);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for Gemini request slot.", interruptedException);
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
        logger.debug("Gemini backing off for {} ms before retry attempt {}", computedDelay, attempt + 1);
        try {
            Thread.sleep(computedDelay);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}
