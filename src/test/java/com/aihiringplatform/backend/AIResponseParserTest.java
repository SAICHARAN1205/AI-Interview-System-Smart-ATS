package com.aihiringplatform.backend;

import com.aihiringplatform.backend.service.AIResponseParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AIResponseParserTest {

    private final AIResponseParser parser = new AIResponseParser(new ObjectMapper());

    @Test
    void extractJsonFromResponseRemovesMarkdownAndExtraText() throws Exception {
        String raw = """
                Here is the evaluation:
                ```json
                {
                  "overallScore": 85,
                  "communication": 80,
                  "technical": 90,
                  "strengths": ["Good explanation"],
                  "weaknesses": ["Need concise answers"],
                  "feedback": "Strong technical understanding."
                }
                ```
                Extra trailing text
                """;

        String extracted = parser.extractJsonFromResponse(raw);

        var node = new ObjectMapper().readTree(extracted);
        assertEquals(85, node.get("overallScore").asInt());
        assertEquals(80, node.get("communication").asInt());
        assertFalse(extracted.contains("```"));
    }
}
