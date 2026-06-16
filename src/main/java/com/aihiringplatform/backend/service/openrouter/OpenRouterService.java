package com.aihiringplatform.backend.service.openrouter;

import com.aihiringplatform.backend.config.OpenRouterProperties;
import com.aihiringplatform.backend.service.AIResponseParser;
import com.aihiringplatform.backend.service.PromptBuilderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class OpenRouterService {

    private static final Logger logger = LoggerFactory.getLogger(OpenRouterService.class);

    private final RestClient restClient;
    private final OpenRouterProperties properties;
    private final OpenRouterResponseParser responseParser;
    private final AIResponseParser structuredParser;
    private final Object requestQueueMonitor = new Object();
    private long nextRequestAtMillis = 0L;
    private long cooldownUntilMillis = 0L;

    public OpenRouterService(
            @Qualifier("openRouterRestClient") RestClient openRouterRestClient,
            OpenRouterProperties properties,
            OpenRouterResponseParser responseParser,
            AIResponseParser structuredParser
    ) {
        this.restClient = openRouterRestClient;
        this.properties = properties;
        this.responseParser = responseParser;
        this.structuredParser = structuredParser;
    }

    public <T> T generateStructuredResponse(
            PromptBuilderService.PromptDefinition prompt,
            Class<T> responseType
    ) {
        ensureConfigured();

        RuntimeException lastFailure = null;
        for (String model : configuredModels()) {
            int attempts = Math.max(1, properties.getMaxRetries() + 1);

            for (int attempt = 1; attempt <= attempts; attempt++) {
                waitForQueueTurn(prompt.operation(), model);
                long startedAt = System.currentTimeMillis();

                try {
                    logger.info(
                            "OpenRouter request started for {} using model {} against {}",
                            prompt.operation(),
                            model,
                            properties.getBaseUrl() + "/api/v1/chat/completions"
                    );

                    String rawResponse = executeRequest(prompt, model, buildRequestBody(prompt, model));
                    String parsedText = responseParser.extractStructuredText(rawResponse);
                    logger.info(
                            "OpenRouter parsed output for {} using model {}: {}",
                            prompt.operation(),
                            model,
                            preview(parsedText)
                    );

                    T parsed = structuredParser.parseStructuredPayload(
                            parsedText,
                            responseType
                    );

                    logger.info(
                        "OpenRouter {} succeeded via model {} in {} ms on attempt {}",
                            prompt.operation(),
                            model,
                            System.currentTimeMillis() - startedAt,
                            attempt
                    );
                    return parsed;
                } catch (RestClientResponseException exception) {
                    lastFailure = exception;
                    int statusCode = exception.getStatusCode().value();
                    logger.warn(
                            "OpenRouter {} failed via model {} in {} ms on attempt {}/{} with status {} and reason {}. Body: {}",
                            prompt.operation(),
                            model,
                            System.currentTimeMillis() - startedAt,
                            attempt,
                            attempts,
                            statusCode,
                            exception.getMessage(),
                            preview(exception.getResponseBodyAsString())
                    );

                    if (statusCode == 429) {
                        long cooldownMillis = applyCooldown(exception);
                        logger.warn(
                                "OpenRouter {} rate-limited on model {}. Cooling down requests for {} ms",
                                prompt.operation(),
                                model,
                                cooldownMillis
                        );
                    }

                    if (!shouldRetry(statusCode, attempt, attempts)) {
                        break;
                    }
                } catch (RuntimeException exception) {
                    lastFailure = exception;
                    logger.warn(
                            "OpenRouter {} failed via model {} in {} ms on attempt {}/{}: {} ({})",
                            prompt.operation(),
                            model,
                            System.currentTimeMillis() - startedAt,
                            attempt,
                            attempts,
                            exception.getClass().getSimpleName(),
                            preview(exception.getMessage())
                    );

                    if (attempt >= attempts) {
                        break;
                    }
                }

                sleepBeforeRetry(attempt, lastFailure instanceof RestClientResponseException responseException
                        && responseException.getStatusCode().value() == 429);
            }
        }

        throw lastFailure == null
                ? new IllegalStateException("OpenRouter request failed.")
                : lastFailure;
    }

    public OpenRouterConnectivityResult runConnectivityCheck() {
        boolean enabled = properties.isEnabled();
        boolean apiKeyConfigured = properties.getApiKey() != null && !properties.getApiKey().isBlank();
        List<OpenRouterAttemptResult> attempts = new ArrayList<>();

        if (!enabled || !apiKeyConfigured) {
            return new OpenRouterConnectivityResult(
                    enabled,
                    apiKeyConfigured,
                    properties.getBaseUrl(),
                    properties.getHttpReferer(),
                    properties.getTitle(),
                    configuredModels(),
                    attempts,
                    false,
                    "OpenRouter is not configured.",
                    null
            );
        }

        PromptBuilderService.PromptDefinition diagnosticPrompt = new PromptBuilderService.PromptDefinition(
                "openrouter-connectivity-test",
                "You are a backend connectivity check. Return valid JSON only.",
                "Return a JSON object confirming OpenRouter connectivity for SmartATS.",
                Map.of(
                        "type", "object",
                        "additionalProperties", false,
                        "properties", Map.of(
                                "status", Map.of("type", "string"),
                                "message", Map.of("type", "string")
                        ),
                        "required", List.of("status", "message")
                ),
                120,
                0.0
        );

        for (String model : configuredModels()) {
            try {
                String rawResponse = executeRequest(diagnosticPrompt, model, buildRequestBody(diagnosticPrompt, model));
                String parsedText = responseParser.extractStructuredText(rawResponse);
                attempts.add(new OpenRouterAttemptResult(model, 200, true, "Success", preview(parsedText)));
                return new OpenRouterConnectivityResult(
                        true,
                        true,
                        properties.getBaseUrl(),
                        properties.getHttpReferer(),
                        properties.getTitle(),
                        configuredModels(),
                        attempts,
                        true,
                        "OpenRouter connectivity check succeeded.",
                        parsedText
                );
            } catch (RestClientResponseException exception) {
                attempts.add(new OpenRouterAttemptResult(
                        model,
                        exception.getStatusCode().value(),
                        false,
                        exception.getMessage(),
                        preview(exception.getResponseBodyAsString())
                ));
            } catch (RuntimeException exception) {
                attempts.add(new OpenRouterAttemptResult(
                        model,
                        null,
                        false,
                        exception.getClass().getSimpleName(),
                        preview(exception.getMessage())
                ));
            }
        }

        return new OpenRouterConnectivityResult(
                true,
                true,
                properties.getBaseUrl(),
                properties.getHttpReferer(),
                properties.getTitle(),
                configuredModels(),
                attempts,
                false,
                "OpenRouter connectivity check failed.",
                null
        );
    }

    private void ensureConfigured() {
        if (!properties.isEnabled() || properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("OpenRouter API is not configured.");
        }
    }

    private List<String> configuredModels() {
        Set<String> models = new LinkedHashSet<>();
        if (properties.getPrimaryModel() != null && !properties.getPrimaryModel().isBlank()) {
            models.add(properties.getPrimaryModel().trim());
        }
        if (properties.getSecondaryModel() != null && !properties.getSecondaryModel().isBlank()) {
            models.add(properties.getSecondaryModel().trim());
        }
        if (properties.getRuntimeFallbackModels() != null) {
            for (String fallbackModel : properties.getRuntimeFallbackModels()) {
                if (fallbackModel != null && !fallbackModel.isBlank()) {
                    models.add(fallbackModel.trim());
                }
            }
        }
        int maxModelAttempts = Math.max(1, properties.getMaxModelAttempts());
        List<String> configured = new ArrayList<>(models);
        if (configured.size() > maxModelAttempts) {
            logger.info(
                    "OpenRouter model attempts capped at {}. Using {} and skipping {} additional configured fallback model(s).",
                    maxModelAttempts,
                    configured.subList(0, maxModelAttempts),
                    configured.size() - maxModelAttempts
            );
        }
        return configured.stream().limit(maxModelAttempts).toList();
    }

    private Map<String, Object> buildRequestBody(PromptBuilderService.PromptDefinition prompt, String model) {
        return Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", prompt.systemInstruction()),
                        Map.of("role", "user", "content", prompt.userPrompt())
                ),
                "response_format", Map.of(
                        "type", "json_schema",
                        "json_schema", Map.of(
                                "name", schemaName(prompt.operation()),
                                "strict", true,
                                "schema", prompt.responseSchema()
                        )
                ),
                "provider", Map.of(
                        "allow_fallbacks", true,
                        "require_parameters", true
                ),
                "stream", false,
                "max_tokens", prompt.maxOutputTokens(),
                "temperature", prompt.temperature()
        );
    }

    private String executeRequest(
            PromptBuilderService.PromptDefinition prompt,
            String model,
            Map<String, Object> requestBody
    ) {
        return restClient.post()
                .uri("/api/v1/chat/completions")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", properties.getHttpReferer())
                .header("X-Title", properties.getTitle())
                .body(requestBody)
                .exchange((request, response) -> {
                    String responseBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    int statusCode = response.getStatusCode().value();
                    logger.info(
                            "OpenRouter response status {} for {} using model {}",
                            statusCode,
                            prompt.operation(),
                            model
                    );

                    if (statusCode >= 400) {
                        throw new RestClientResponseException(
                                "OpenRouter returned HTTP " + statusCode,
                                statusCode,
                                "",
                                HttpHeaders.writableHttpHeaders(response.getHeaders()),
                                responseBody.getBytes(StandardCharsets.UTF_8),
                                StandardCharsets.UTF_8
                        );
                    }

                    return responseBody;
                });
    }

    private String schemaName(String operation) {
        if (operation == null || operation.isBlank()) {
            return "smartats_response";
        }
        return operation.trim().replaceAll("[^A-Za-z0-9_]+", "_");
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

    private void waitForQueueTurn(String operation, String model) {
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

                logger.debug("OpenRouter {} waiting {} ms for next request slot on model {}", operation, waitMillis, model);
                try {
                    requestQueueMonitor.wait(waitMillis);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for OpenRouter request slot.", interruptedException);
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
        logger.debug("OpenRouter backing off for {} ms before retry attempt {}", computedDelay, attempt + 1);
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
        return condensed.length() <= 240
                ? condensed
                : condensed.substring(0, 240) + "...";
    }

    public record OpenRouterConnectivityResult(
            boolean enabled,
            boolean apiKeyConfigured,
            String baseUrl,
            String httpReferer,
            String title,
            List<String> configuredModels,
            List<OpenRouterAttemptResult> attempts,
            boolean success,
            String message,
            String parsedOutput
    ) {
    }

    public record OpenRouterAttemptResult(
            String model,
            Integer statusCode,
            boolean success,
            String message,
            String detail
    ) {
    }
}
