package com.aihiringplatform.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AIResponseParser {

    private static final Logger logger = LoggerFactory.getLogger(AIResponseParser.class);

    private final ObjectMapper objectMapper;

    public AIResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String extractGeminiText(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode blockReason = root.path("promptFeedback").path("blockReason");
            if (!blockReason.isMissingNode() && !blockReason.asText("").isBlank()) {
                throw new IllegalStateException("Gemini blocked the prompt.");
            }

            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new IllegalStateException("Gemini returned no candidates.");
            }

            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                throw new IllegalStateException("Gemini returned no content parts.");
            }

            String text = parts.get(0).path("text").asText("");
            if (text.isBlank()) {
                throw new IllegalStateException("Gemini returned an empty response.");
            }

            return text;
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse Gemini response.", exception);
        }
    }

    public String extractChatCompletionText(String providerName, String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode errorMessage = root.path("error").path("message");
            if (!errorMessage.isMissingNode() && !errorMessage.asText("").isBlank()) {
                throw new IllegalStateException(providerName + " returned an error response.");
            }

            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new IllegalStateException(providerName + " returned no choices.");
            }

            JsonNode messageContent = choices.get(0).path("message").path("content");
            if (messageContent.isTextual()) {
                String text = sanitizeModelOutput(messageContent.asText(""));
                if (text.isBlank()) {
                    throw new IllegalStateException(providerName + " returned an empty response.");
                }
                return text;
            }

            if (messageContent.isArray()) {
                StringBuilder combined = new StringBuilder();
                for (JsonNode item : messageContent) {
                    if (item.isTextual()) {
                        combined.append(item.asText(""));
                    } else if (item.has("text")) {
                        combined.append(item.path("text").asText(""));
                    }
                }

                String text = sanitizeModelOutput(combined.toString());
                if (text.isBlank()) {
                    throw new IllegalStateException(providerName + " returned an empty response.");
                }
                return text;
            }

            throw new IllegalStateException(providerName + " returned no usable content.");
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse " + providerName + " response.", exception);
        }
    }

    public <T> T parseStructuredPayload(String rawText, Class<T> responseType) {
        try {
            return parseStructuredPayloadFromJson(extractJsonFromResponse(rawText), responseType);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse structured AI response.", exception);
        }
    }

    public <T> T parseStructuredPayloadFromJson(String json, Class<T> responseType) {
        try {
            return objectMapper.readValue(json, responseType);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse structured AI response.", exception);
        }
    }

    public String extractJsonFromResponse(String raw) {
        String normalized = sanitizeModelOutput(stripCodeFences(raw));
        if (normalized.isBlank()) {
            throw new IllegalStateException("AI response was empty after cleanup.");
        }

        int startIndex = findFirstJsonStart(normalized);
        if (startIndex < 0) {
            throw new IllegalStateException("No JSON object found in AI response.");
        }

        String extracted = extractBalancedJson(normalized, startIndex);
        if (extracted == null || extracted.isBlank()) {
            throw new IllegalStateException("Unable to extract valid JSON from AI response.");
        }
        return extracted.trim();
    }

    public boolean isParsingFailure(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        String message = throwable.getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("parse")
                || normalized.contains("json")
                || normalized.contains("object found")
                || normalized.contains("empty after cleanup")
                || normalized.contains("no usable content")
                || normalized.contains("balanced json");
    }

    public String sanitizeModelOutput(String value) {
        if (value == null) {
            return "";
        }

        String sanitized = value
                .replace('\u0000', ' ')
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\u2028", "\n")
                .replace("\u2029", "\n")
                .replace('\uFEFF', ' ')
                .trim();

        return sanitized.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", " ");
    }

    private String stripCodeFences(String value) {
        if (value == null) {
            return "";
        }

        String trimmed = value.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7).trim();
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3).trim();
        }

        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
        }

        if (trimmed.startsWith("\uFEFF")) {
            trimmed = trimmed.substring(1);
        }

        return trimmed;
    }

    private int findFirstJsonStart(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '{' || current == '[') {
                return index;
            }
        }
        return -1;
    }

    private String extractBalancedJson(String value, int startIndex) {
        char opening = value.charAt(startIndex);
        char closing = opening == '{' ? '}' : ']';
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int index = startIndex; index < value.length(); index++) {
            char current = value.charAt(index);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (current == '\\') {
                escaped = true;
                continue;
            }

            if (current == '"') {
                inString = !inString;
                continue;
            }

            if (inString) {
                continue;
            }

            if (current == opening) {
                depth++;
            } else if (current == closing) {
                depth--;
                if (depth == 0) {
                    String candidate = value.substring(startIndex, index + 1);
                    try {
                        objectMapper.readTree(candidate);
                        return candidate;
                    } catch (Exception exception) {
                        logger.debug("Discarded non-parseable JSON candidate: {}", exception.getMessage());
                    }
                }
            }
        }

        throw new IllegalStateException("Unable to find a balanced JSON object in AI response.");
    }
}
