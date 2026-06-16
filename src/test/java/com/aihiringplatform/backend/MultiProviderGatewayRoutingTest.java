package com.aihiringplatform.backend;

import com.aihiringplatform.backend.service.PromptBuilderService;
import com.aihiringplatform.backend.service.ai.AIGatewayResult;
import com.aihiringplatform.backend.service.ai.AIGatewayService;
import com.aihiringplatform.backend.service.ai.AIProvider;
import com.aihiringplatform.backend.service.ai.AIProviderException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiProviderGatewayRoutingTest {

    @Test
    void answerEvaluationUsesDeepSeekWhenOpenAiTimesOut() {
        List<String> callOrder = new ArrayList<>();
        AIGatewayService gatewayService = new AIGatewayService(
                new AIProvider[]{
                    provider("OpenAI", callOrder, null,
                            new ResourceAccessException("Read timed out", new SocketTimeoutException("Read timed out"))),
                    provider("DeepSeek", callOrder, sample("deepseek"), null),
                    localFallback(callOrder)
                },
                org.mockito.Mockito.mock(com.aihiringplatform.backend.repository.AIProviderConfigRepository.class)
        );

        AIGatewayResult<SampleResponse> result = gatewayService.generateStructuredResponse(
                prompt("answer-evaluation"),
                SampleResponse.class,
                () -> sample("local")
        );

        assertEquals("DeepSeek", result.providerUsed());
        assertEquals("deepseek", result.payload().getValue());
        assertEquals(List.of("OpenAI", "DeepSeek"), callOrder);
    }

    @Test
    void atsAnalysisFallsBackToTogetherWhenGeminiFails() {
        List<String> callOrder = new ArrayList<>();
        AIGatewayService gatewayService = new AIGatewayService(
                new AIProvider[]{
                    provider("Gemini", callOrder, null, new IllegalStateException("Unable to parse structured AI response.")),
                    provider("Together AI", callOrder, sample("together"), null),
                    localFallback(callOrder)
                },
                org.mockito.Mockito.mock(com.aihiringplatform.backend.repository.AIProviderConfigRepository.class)
        );

        AIGatewayResult<SampleResponse> result = gatewayService.generateStructuredResponse(
                prompt("ats-analysis"),
                SampleResponse.class,
                () -> sample("local")
        );

        assertEquals("Together AI", result.providerUsed());
        assertEquals("together", result.payload().getValue());
        assertEquals(List.of("Gemini", "Together AI"), callOrder);
    }

    @Test
    void questionGenerationFallsBackToOpenRouterWhenGroqReturnsMalformedResponse() {
        List<String> callOrder = new ArrayList<>();
        AIGatewayService gatewayService = new AIGatewayService(
                new AIProvider[]{
                    provider("Gemini", callOrder, null, new IllegalStateException("Unable to parse structured AI response.")),
                    provider("Groq", callOrder, null, new IllegalStateException("No JSON object found in AI response.")),
                    provider("OpenRouter", callOrder, sample("openrouter"), null),
                    localFallback(callOrder)
                },
                org.mockito.Mockito.mock(com.aihiringplatform.backend.repository.AIProviderConfigRepository.class)
        );

        AIGatewayResult<SampleResponse> result = gatewayService.generateStructuredResponse(
                prompt("question-generation"),
                SampleResponse.class,
                () -> sample("local")
        );

        assertEquals("OpenRouter", result.providerUsed());
        assertEquals("openrouter", result.payload().getValue());
        assertEquals(List.of("Gemini", "Groq", "OpenRouter"), callOrder);
    }

    @Test
    void openRouter429FallsBackToLocalFailSafe() {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Retry-After", List.of("0"));
        HttpClientErrorException tooManyRequests = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                headers,
                new byte[0],
                StandardCharsets.UTF_8
        );

        List<String> callOrder = new ArrayList<>();
        AIGatewayService gatewayService = new AIGatewayService(
                new AIProvider[]{
                    provider("Gemini", callOrder, null, new IllegalStateException("Unable to parse structured AI response.")),
                    provider("OpenAI", callOrder, null, new ResourceAccessException("Read timed out")),
                    provider("DeepSeek", callOrder, null, new IllegalStateException("Unable to parse structured AI response.")),
                    provider("Together AI", callOrder, null, new IllegalStateException("Unable to parse structured AI response.")),
                    provider("Groq", callOrder, null, new IllegalStateException("Unable to parse structured AI response.")),
                    provider("OpenRouter", callOrder, null, tooManyRequests),
                    localFallback(callOrder)
                },
                org.mockito.Mockito.mock(com.aihiringplatform.backend.repository.AIProviderConfigRepository.class)
        );

        AIGatewayResult<SampleResponse> result = gatewayService.generateStructuredResponse(
                prompt("job-match"),
                SampleResponse.class,
                () -> sample("local")
        );

        assertEquals("SmartATS local fallback", result.providerUsed());
        assertTrue(result.localFallbackUsed());
        assertEquals("local", result.payload().getValue());
        assertTrue(callOrder.contains("OpenRouter"));
    }

    private AIProvider provider(
            String name,
            List<String> callOrder,
            SampleResponse payload,
            RuntimeException failure
    ) {
        return new AIProvider() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public <T> T generateStructuredResponse(
                    PromptBuilderService.PromptDefinition prompt,
                    Class<T> responseType,
                    Supplier<T> localFallbackSupplier
            ) {
                callOrder.add(name);
                if (failure != null) {
                    throw AIProviderException.from(name, failure);
                }
                return responseType.cast(payload);
            }
        };
    }

    private AIProvider localFallback(List<String> callOrder) {
        return new AIProvider() {
            @Override
            public String getName() {
                return "SmartATS local fallback";
            }

            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public boolean isLocalFallback() {
                return true;
            }

            @Override
            public <T> T generateStructuredResponse(
                    PromptBuilderService.PromptDefinition prompt,
                    Class<T> responseType,
                    Supplier<T> localFallbackSupplier
            ) {
                callOrder.add("SmartATS local fallback");
                return localFallbackSupplier.get();
            }
        };
    }

    private PromptBuilderService.PromptDefinition prompt(String operation) {
        return new PromptBuilderService.PromptDefinition(
                operation,
                "Return JSON only.",
                "Test prompt",
                Map.of(
                        "type", "object",
                        "additionalProperties", false,
                        "properties", Map.of("value", Map.of("type", "string")),
                        "required", List.of("value")
                ),
                200,
                0.0
        );
    }

    private SampleResponse sample(String value) {
        SampleResponse response = new SampleResponse();
        response.setValue(value);
        return response;
    }

    static class SampleResponse {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
