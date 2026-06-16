package com.aihiringplatform.backend;

import com.aihiringplatform.backend.config.GeminiProperties;
import com.aihiringplatform.backend.dto.InterviewEvaluationResponse;
import com.aihiringplatform.backend.service.AIResponseParser;
import com.aihiringplatform.backend.service.PromptBuilderService;
import com.aihiringplatform.backend.service.gemini.GeminiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        GeminiProperties properties = new GeminiProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setModel("gemini-2.5-flash");
        properties.setMaxRetries(3);
        properties.setRetryDelayMillis(5);
        properties.setQueueSpacingMillis(0);
        properties.setRateLimitCooldownMillis(40);
        properties.setMaxBackoffMillis(100);

        geminiService = new GeminiService(restClient, properties, new AIResponseParser(new ObjectMapper()));

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/v1beta/models/{model}:generateContent"), eq("gemini-2.5-flash")))
                .thenReturn(requestBodySpec);
        when(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyMap())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void rateLimitRetriesOnlyOnceAndHonorsCooldown() {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Retry-After", List.of("0"));
        HttpClientErrorException tooManyRequests = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                headers,
                new byte[0],
                StandardCharsets.UTF_8
        );

        when(responseSpec.body(String.class))
                .thenThrow(tooManyRequests)
                .thenThrow(tooManyRequests);

        long startedAt = System.currentTimeMillis();
        assertThrows(RuntimeException.class, () -> geminiService.generateStructuredResponse(prompt(), SampleResponse.class));
        long elapsedMillis = System.currentTimeMillis() - startedAt;

        verify(responseSpec, times(2)).body(String.class);
        assertTrue(elapsedMillis >= 35);
    }

    @Test
    void interviewEvaluationRetriesOnceWithStricterPromptAfterParsingFailure() {
        when(responseSpec.body(String.class))
                .thenReturn(geminiEnvelope("This is not valid JSON"))
                .thenReturn(geminiEnvelope("""
                        {
                          "overallScore": 85,
                          "communication": 80,
                          "technical": 90,
                          "strengths": ["Good explanation"],
                          "weaknesses": ["Need concise answers"],
                          "feedback": "Strong technical understanding."
                        }
                        """));

        InterviewEvaluationResponse response = geminiService.generateStructuredResponse(
                prompt(),
                InterviewEvaluationResponse.class
        );

        assertEquals(85, response.getOverallScore());
        assertEquals(80, response.getCommunicationScore());
        assertEquals(90, response.getTechnicalScore());
        assertEquals("Strong technical understanding.", response.getFinalFeedback());
        verify(responseSpec, times(2)).body(String.class);
    }

    private PromptBuilderService.PromptDefinition prompt() {
        return new PromptBuilderService.PromptDefinition(
                "interview-evaluation",
                "Return JSON only.",
                "Test prompt",
                Map.of(
                        "type", "object",
                        "additionalProperties", false,
                        "properties", Map.of(
                                "overallScore", Map.of("type", "integer"),
                                "communication", Map.of("type", "integer"),
                                "technical", Map.of("type", "integer"),
                                "strengths", Map.of("type", "array", "items", Map.of("type", "string")),
                                "weaknesses", Map.of("type", "array", "items", Map.of("type", "string")),
                                "feedback", Map.of("type", "string")
                        ),
                        "required", List.of("overallScore", "communication", "technical", "strengths", "weaknesses", "feedback")
                ),
                200,
                0.1
        );
    }

    private String geminiEnvelope(String text) {
        return """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": %s
                          }
                        ]
                      }
                    }
                  ]
                }
                """.formatted(new ObjectMapper().valueToTree(text).toString());
    }

    public static class SampleResponse {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
