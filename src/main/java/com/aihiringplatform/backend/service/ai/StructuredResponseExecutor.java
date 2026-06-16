package com.aihiringplatform.backend.service.ai;

import com.aihiringplatform.backend.service.PromptBuilderService;

public interface StructuredResponseExecutor {

    <T> T generateStructuredResponse(
            PromptBuilderService.PromptDefinition prompt,
            Class<T> responseType
    );
}
