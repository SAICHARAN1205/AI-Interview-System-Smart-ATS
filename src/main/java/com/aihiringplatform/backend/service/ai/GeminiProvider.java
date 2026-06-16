package com.aihiringplatform.backend.service.ai;

import com.aihiringplatform.backend.config.AiStabilityProperties;
import com.aihiringplatform.backend.config.GeminiProperties;
import com.aihiringplatform.backend.service.PromptBuilderService;
import com.aihiringplatform.backend.service.gemini.GeminiService;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class GeminiProvider implements AIProvider {

    private static final String PROVIDER_NAME = "Gemini";

    private final GeminiService geminiService;
    private final GeminiProperties properties;
    private final AIProviderHealthTracker healthTracker;

    public GeminiProvider(
            GeminiService geminiService,
            GeminiProperties properties,
            AiStabilityProperties stabilityProperties
    ) {
        this.geminiService = geminiService;
        this.properties = properties;
        this.healthTracker = new AIProviderHealthTracker(
                stabilityProperties.getProviderCooldownMillis(),
                stabilityProperties.getProviderMaxCooldownMillis(),
                stabilityProperties.getProviderFailureThreshold()
        );
    }

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled() && properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    @Override
    public boolean isAvailable() {
        return isEnabled() && healthTracker.isAvailable();
    }

    @Override
    public long getRemainingCooldownMillis() {
        return healthTracker.getRemainingCooldownMillis();
    }

    @Override
    public <T> T generateStructuredResponse(
            PromptBuilderService.PromptDefinition prompt,
            Class<T> responseType,
            Supplier<T> localFallbackSupplier
    ) {
        try {
            T payload = geminiService.generateStructuredResponse(prompt, responseType);
            healthTracker.recordSuccess();
            return payload;
        } catch (RuntimeException exception) {
            AIProviderException providerException = AIProviderException.from(PROVIDER_NAME, exception);
            healthTracker.recordFailure(providerException.isHealthImpacting());
            throw providerException;
        }
    }
}
