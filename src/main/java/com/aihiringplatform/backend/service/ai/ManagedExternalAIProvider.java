package com.aihiringplatform.backend.service.ai;

import com.aihiringplatform.backend.config.AiStabilityProperties;
import com.aihiringplatform.backend.service.PromptBuilderService;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class ManagedExternalAIProvider implements AIProvider {

    private final String providerName;
    private final BooleanSupplier enabledSupplier;
    private final StructuredResponseExecutor executor;
    private final AIProviderHealthTracker healthTracker;

    public ManagedExternalAIProvider(
            String providerName,
            BooleanSupplier enabledSupplier,
            StructuredResponseExecutor executor,
            AiStabilityProperties stabilityProperties
    ) {
        this.providerName = providerName;
        this.enabledSupplier = enabledSupplier;
        this.executor = executor;
        this.healthTracker = new AIProviderHealthTracker(
                stabilityProperties.getProviderCooldownMillis(),
                stabilityProperties.getProviderMaxCooldownMillis(),
                stabilityProperties.getProviderFailureThreshold()
        );
    }

    @Override
    public String getName() {
        return providerName;
    }

    @Override
    public boolean isEnabled() {
        return enabledSupplier.getAsBoolean();
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
            T payload = executor.generateStructuredResponse(prompt, responseType);
            healthTracker.recordSuccess();
            return payload;
        } catch (RuntimeException exception) {
            AIProviderException providerException = AIProviderException.from(providerName, exception);
            healthTracker.recordFailure(providerException.isHealthImpacting());
            throw providerException;
        }
    }
}
