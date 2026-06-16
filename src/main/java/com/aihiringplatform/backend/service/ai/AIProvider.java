package com.aihiringplatform.backend.service.ai;

import com.aihiringplatform.backend.service.PromptBuilderService;

import java.util.function.Supplier;

public interface AIProvider {

    String getName();

    boolean isEnabled();

    boolean isAvailable();

    default boolean isLocalFallback() {
        return false;
    }

    default long getRemainingCooldownMillis() {
        return 0L;
    }

    <T> T generateStructuredResponse(
            PromptBuilderService.PromptDefinition prompt,
            Class<T> responseType,
            Supplier<T> localFallbackSupplier
    );
}
