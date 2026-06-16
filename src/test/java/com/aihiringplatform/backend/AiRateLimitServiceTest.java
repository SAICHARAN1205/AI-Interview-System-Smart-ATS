package com.aihiringplatform.backend;

import com.aihiringplatform.backend.config.AiStabilityProperties;
import com.aihiringplatform.backend.config.GeminiProperties;
import com.aihiringplatform.backend.service.AiRateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiRateLimitServiceTest {

    @Test
    void blocksRepeatedRequestDuringShortCooldown() {
        GeminiProperties geminiProperties = new GeminiProperties();
        geminiProperties.setRequestsPerMinute(30);

        AiStabilityProperties stabilityProperties = new AiStabilityProperties();
        stabilityProperties.setRequestCooldownMillis(150L);

        AiRateLimitService service = new AiRateLimitService(geminiProperties, stabilityProperties);

        assertDoesNotThrow(() -> service.assertAllowed("ai-ats-analyze", "candidate@example.com"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.assertAllowed("ai-ats-analyze", "candidate@example.com")
        );

        assertEquals(429, exception.getStatusCode().value());
        assertEquals(
                "Please wait 1 more second(s) before requesting another AI analysis.",
                exception.getReason()
        );
    }

    @Test
    void allowsRequestAgainAfterCooldownExpires() throws InterruptedException {
        GeminiProperties geminiProperties = new GeminiProperties();
        geminiProperties.setRequestsPerMinute(30);

        AiStabilityProperties stabilityProperties = new AiStabilityProperties();
        stabilityProperties.setRequestCooldownMillis(80L);

        AiRateLimitService service = new AiRateLimitService(geminiProperties, stabilityProperties);

        service.assertAllowed("ai-match-score", "candidate@example.com");
        Thread.sleep(100L);

        assertDoesNotThrow(() -> service.assertAllowed("ai-match-score", "candidate@example.com"));
    }
}
