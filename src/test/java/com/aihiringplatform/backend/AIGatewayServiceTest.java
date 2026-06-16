package com.aihiringplatform.backend;

import com.aihiringplatform.backend.config.AiStabilityProperties;
import com.aihiringplatform.backend.config.GeminiProperties;
import com.aihiringplatform.backend.config.OpenRouterProperties;
import com.aihiringplatform.backend.service.PromptBuilderService;
import com.aihiringplatform.backend.service.ai.AIGatewayResult;
import com.aihiringplatform.backend.service.ai.AIGatewayService;
import com.aihiringplatform.backend.service.ai.GeminiProvider;
import com.aihiringplatform.backend.service.ai.OpenRouterProvider;
import com.aihiringplatform.backend.service.ai.SmartATSFallbackProvider;
import com.aihiringplatform.backend.service.gemini.GeminiService;
import com.aihiringplatform.backend.service.openrouter.OpenRouterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIGatewayServiceTest {

    @Mock
    private GeminiService geminiService;

    @Mock
    private OpenRouterService openRouterService;

    private AIGatewayService gatewayService;

    @BeforeEach
    void setUp() {
        GeminiProperties geminiProperties = new GeminiProperties();
        geminiProperties.setEnabled(true);
        geminiProperties.setApiKey("test-gemini-key");
        geminiProperties.setRateLimitCooldownMillis(25);
        geminiProperties.setMaxBackoffMillis(100);

        OpenRouterProperties openRouterProperties = new OpenRouterProperties();
        openRouterProperties.setEnabled(true);
        openRouterProperties.setApiKey("test-openrouter-key");
        openRouterProperties.setRateLimitCooldownMillis(25);
        openRouterProperties.setMaxBackoffMillis(100);
        openRouterProperties.setMaxModelAttempts(1);

        AiStabilityProperties stabilityProperties = new AiStabilityProperties();
        stabilityProperties.setProviderFailureThreshold(2);
        stabilityProperties.setProviderCooldownMillis(1_000L);
        stabilityProperties.setProviderMaxCooldownMillis(2_000L);

        gatewayService = new AIGatewayService(
                new com.aihiringplatform.backend.service.ai.AIProvider[]{
                        new GeminiProvider(geminiService, geminiProperties, stabilityProperties),
                        new OpenRouterProvider(openRouterService, openRouterProperties, stabilityProperties),
                        new SmartATSFallbackProvider()
                },
                org.mockito.Mockito.mock(com.aihiringplatform.backend.repository.AIProviderConfigRepository.class)
        );
    }

    @Test
    void geminiSuccessFlowUsesGemini() {
        when(geminiService.generateStructuredResponse(any(), eq(SampleResponse.class)))
                .thenReturn(sample("gemini"));

        AIGatewayResult<SampleResponse> result = gatewayService.generateStructuredResponse(
                prompt(),
                SampleResponse.class,
                () -> sample("local")
        );

        assertEquals("Gemini", result.providerUsed());
        assertFalse(result.localFallbackUsed());
        assertEquals("gemini", result.payload().getValue());
        verify(openRouterService, never()).generateStructuredResponse(any(), eq(SampleResponse.class));
    }

    @Test
    void gemini429FlowFallsBackToOpenRouter() {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Retry-After", List.of("0"));
        HttpClientErrorException tooManyRequests = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                headers,
                new byte[0],
                StandardCharsets.UTF_8
        );

        when(geminiService.generateStructuredResponse(any(), eq(SampleResponse.class)))
                .thenThrow(tooManyRequests);
        when(openRouterService.generateStructuredResponse(any(), eq(SampleResponse.class)))
                .thenReturn(sample("openrouter"));

        AIGatewayResult<SampleResponse> result = gatewayService.generateStructuredResponse(
                prompt(),
                SampleResponse.class,
                () -> sample("local")
        );

        assertEquals("OpenRouter", result.providerUsed());
        assertFalse(result.localFallbackUsed());
        assertEquals("openrouter", result.payload().getValue());
    }

    @Test
    void gemini503FlowFallsBackToOpenRouter() {
        HttpServerErrorException serviceUnavailable = HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
        );

        when(geminiService.generateStructuredResponse(any(), eq(SampleResponse.class)))
                .thenThrow(serviceUnavailable);
        when(openRouterService.generateStructuredResponse(any(), eq(SampleResponse.class)))
                .thenReturn(sample("openrouter"));

        AIGatewayResult<SampleResponse> result = gatewayService.generateStructuredResponse(
                prompt(),
                SampleResponse.class,
                () -> sample("local")
        );

        assertEquals("OpenRouter", result.providerUsed());
        assertEquals("openrouter", result.payload().getValue());
    }

    @Test
    void geminiTimeoutFlowFallsBackToOpenRouter() {
        when(geminiService.generateStructuredResponse(any(), eq(SampleResponse.class)))
                .thenThrow(new ResourceAccessException("Read timed out", new SocketTimeoutException("Read timed out")));
        when(openRouterService.generateStructuredResponse(any(), eq(SampleResponse.class)))
                .thenReturn(sample("openrouter"));

        AIGatewayResult<SampleResponse> result = gatewayService.generateStructuredResponse(
                prompt(),
                SampleResponse.class,
                () -> sample("local")
        );

        assertEquals("OpenRouter", result.providerUsed());
        assertEquals("openrouter", result.payload().getValue());
    }

    @Test
    void completeProviderFailureFlowUsesSmartAtsFallback() {
        HttpClientErrorException tooManyRequests = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
        );
        HttpServerErrorException serviceUnavailable = HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
        );

        when(geminiService.generateStructuredResponse(any(), eq(SampleResponse.class)))
                .thenThrow(tooManyRequests);
        when(openRouterService.generateStructuredResponse(any(), eq(SampleResponse.class)))
                .thenThrow(serviceUnavailable);

        AIGatewayResult<SampleResponse> result = gatewayService.generateStructuredResponse(
                prompt(),
                SampleResponse.class,
                () -> sample("local")
        );

        assertEquals("SmartATS local fallback", result.providerUsed());
        assertTrue(result.localFallbackUsed());
        assertEquals("local", result.payload().getValue());
    }

    @Test
    void repeatedGeminiFailuresTripCircuitBreakerAndSkipNextAttempt() {
        HttpServerErrorException serviceUnavailable = HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
        );

        when(geminiService.generateStructuredResponse(any(), eq(SampleResponse.class)))
                .thenThrow(serviceUnavailable);
        when(openRouterService.generateStructuredResponse(any(), eq(SampleResponse.class)))
                .thenReturn(sample("openrouter"));

        AIGatewayResult<SampleResponse> first = gatewayService.generateStructuredResponse(
                prompt(),
                SampleResponse.class,
                () -> sample("local")
        );
        AIGatewayResult<SampleResponse> second = gatewayService.generateStructuredResponse(
                prompt(),
                SampleResponse.class,
                () -> sample("local")
        );
        AIGatewayResult<SampleResponse> third = gatewayService.generateStructuredResponse(
                prompt(),
                SampleResponse.class,
                () -> sample("local")
        );

        assertEquals("OpenRouter", first.providerUsed());
        assertEquals("OpenRouter", second.providerUsed());
        assertEquals("OpenRouter", third.providerUsed());
        verify(geminiService, times(2)).generateStructuredResponse(any(), eq(SampleResponse.class));
        verify(openRouterService, times(3)).generateStructuredResponse(any(), eq(SampleResponse.class));
    }

    private PromptBuilderService.PromptDefinition prompt() {
        return new PromptBuilderService.PromptDefinition(
                "ats-analysis",
                "Return JSON only.",
                "Test prompt",
                Map.of(
                        "type", "object",
                        "additionalProperties", false,
                        "properties", Map.of("value", Map.of("type", "string")),
                        "required", List.of("value")
                ),
                200,
                0.1
        );
    }

    private SampleResponse sample(String value) {
        SampleResponse response = new SampleResponse();
        response.setValue(value);
        return response;
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
