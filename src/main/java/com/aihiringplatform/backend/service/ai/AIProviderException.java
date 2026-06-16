package com.aihiringplatform.backend.service.ai;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

public class AIProviderException extends RuntimeException {

    private final String providerName;
    private final Integer statusCode;
    private final String failureReason;
    private final boolean healthImpacting;

    public AIProviderException(
            String providerName,
            Integer statusCode,
            String failureReason,
            boolean healthImpacting,
            Throwable cause
    ) {
        super(providerName + " failed: " + failureReason, cause);
        this.providerName = providerName;
        this.statusCode = statusCode;
        this.failureReason = failureReason;
        this.healthImpacting = healthImpacting;
    }

    public static AIProviderException from(String providerName, RuntimeException exception) {
        if (exception instanceof AIProviderException providerException) {
            return providerException;
        }

        if (exception instanceof RestClientResponseException responseException) {
            int statusCode = responseException.getStatusCode().value();
            return new AIProviderException(
                    providerName,
                    statusCode,
                    "HTTP " + statusCode,
                    statusCode == 408 || statusCode == 429 || statusCode >= 500,
                    exception
            );
        }

        if (exception instanceof ResourceAccessException) {
            String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase();
            String reason = message.contains("timed out") ? "timeout" : "connection failure";
            return new AIProviderException(providerName, null, reason, true, exception);
        }

        String normalizedMessage = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase();
        boolean parsingFailure = normalizedMessage.contains("parse")
                || normalizedMessage.contains("empty response")
                || normalizedMessage.contains("no choices")
                || normalizedMessage.contains("no candidates")
                || normalizedMessage.contains("no content")
                || normalizedMessage.contains("no usable content")
                || normalizedMessage.contains("json");

        return new AIProviderException(
                providerName,
                null,
                parsingFailure ? "parsing failure" : exception.getClass().getSimpleName(),
                parsingFailure,
                exception
        );
    }

    public String getProviderName() {
        return providerName;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public boolean isHealthImpacting() {
        return healthImpacting;
    }
}
