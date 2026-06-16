package com.aihiringplatform.backend.service.openrouter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class OpenRouterResponseParser {

    private final ObjectMapper objectMapper;

    public OpenRouterResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String extractStructuredText(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode errorMessage = root.path("error").path("message");
            if (!errorMessage.isMissingNode() && !errorMessage.asText("").isBlank()) {
                throw new IllegalStateException("OpenRouter returned an error response.");
            }

            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new IllegalStateException("OpenRouter returned no choices.");
            }

            JsonNode messageContent = choices.get(0).path("message").path("content");
            if (messageContent.isTextual()) {
                String text = messageContent.asText("");
                if (!text.isBlank()) {
                    return text;
                }
            }

            if (messageContent.isArray()) {
                StringBuilder combined = new StringBuilder();
                for (JsonNode item : messageContent) {
                    String text = item.path("text").asText("");
                    if (!text.isBlank()) {
                        if (combined.length() > 0) {
                            combined.append('\n');
                        }
                        combined.append(text);
                    }
                }
                if (combined.length() > 0) {
                    return combined.toString();
                }
            }

            throw new IllegalStateException("OpenRouter returned an empty response.");
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse OpenRouter response.", exception);
        }
    }
}
