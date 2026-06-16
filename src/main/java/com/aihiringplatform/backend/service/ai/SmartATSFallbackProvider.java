package com.aihiringplatform.backend.service.ai;

import com.aihiringplatform.backend.service.PromptBuilderService;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class SmartATSFallbackProvider implements AIProvider {

    private static final String PROVIDER_NAME = "SmartATS local fallback";

    @Override
    public String getName() {
        return PROVIDER_NAME;
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
        return localFallbackSupplier == null ? null : localFallbackSupplier.get();
    }
}
