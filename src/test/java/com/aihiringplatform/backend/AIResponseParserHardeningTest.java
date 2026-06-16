package com.aihiringplatform.backend;

import com.aihiringplatform.backend.service.AIResponseParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIResponseParserHardeningTest {

    private final AIResponseParser parser = new AIResponseParser(new ObjectMapper());

    @Test
    void extractChatCompletionTextSupportsArrayContentAndSanitizesOutput() {
        String rawResponse = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": [
                          { "type": "text", "text": "\\uFEFF```json\\n{\\n  \\"status\\": \\"ok\\"\\n}\\n```" }
                        ]
                      }
                    }
                  ]
                }
                """;

        String extracted = parser.extractChatCompletionText("OpenAI", rawResponse);
        String json = parser.extractJsonFromResponse(extracted);

        assertEquals("{\n  \"status\": \"ok\"\n}", json);
    }

    @Test
    void sanitizeModelOutputRemovesControlCharacters() {
        String sanitized = parser.sanitizeModelOutput("Hello\u0000World\u2028JSON");
        assertTrue(sanitized.contains("Hello World"));
        assertTrue(sanitized.contains("\nJSON"));
    }
}
