package com.aihiringplatform.backend.service.ai;

import com.aihiringplatform.backend.config.DeepSeekProperties;
import com.aihiringplatform.backend.config.GeminiProperties;
import com.aihiringplatform.backend.config.GroqProperties;
import com.aihiringplatform.backend.config.OpenAIProperties;
import com.aihiringplatform.backend.config.OpenRouterProperties;
import com.aihiringplatform.backend.config.TogetherAIProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class AIProviderStartupDiagnostics implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AIProviderStartupDiagnostics.class);

    private final GeminiProperties geminiProperties;
    private final OpenAIProperties openAIProperties;
    private final DeepSeekProperties deepSeekProperties;
    private final GroqProperties groqProperties;
    private final TogetherAIProperties togetherAIProperties;
    private final OpenRouterProperties openRouterProperties;
    private final AIGatewayService gatewayService;

    public AIProviderStartupDiagnostics(
            GeminiProperties geminiProperties,
            OpenAIProperties openAIProperties,
            DeepSeekProperties deepSeekProperties,
            GroqProperties groqProperties,
            TogetherAIProperties togetherAIProperties,
            OpenRouterProperties openRouterProperties,
            AIGatewayService gatewayService
    ) {
        this.geminiProperties = geminiProperties;
        this.openAIProperties = openAIProperties;
        this.deepSeekProperties = deepSeekProperties;
        this.groqProperties = groqProperties;
        this.togetherAIProperties = togetherAIProperties;
        this.openRouterProperties = openRouterProperties;
        this.gatewayService = gatewayService;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean envFilePresent = Files.exists(Path.of(".env"));
        boolean geminiKeyConfigured = geminiProperties.getApiKey() != null && !geminiProperties.getApiKey().isBlank();
        boolean openAiKeyConfigured = openAIProperties.getApiKey() != null && !openAIProperties.getApiKey().isBlank();
        boolean deepSeekKeyConfigured = deepSeekProperties.getApiKey() != null && !deepSeekProperties.getApiKey().isBlank();
        boolean groqKeyConfigured = groqProperties.getApiKey() != null && !groqProperties.getApiKey().isBlank();
        boolean togetherKeyConfigured = togetherAIProperties.getApiKey() != null && !togetherAIProperties.getApiKey().isBlank();
        boolean openRouterKeyConfigured = openRouterProperties.getApiKey() != null && !openRouterProperties.getApiKey().isBlank();

        logger.info(
                "AI provider diagnostics at startup: Gemini enabled={} keyConfigured={} model={}; OpenAI enabled={} keyConfigured={} model={}; DeepSeek enabled={} keyConfigured={} model={}; Groq enabled={} keyConfigured={} model={}; TogetherAI enabled={} keyConfigured={} model={}; OpenRouter enabled={} keyConfigured={} primaryModel={} secondaryModel={} runtimeFallbackModels={} httpReferer={} title={}; SmartATS enabled={}; .env present={}",
                geminiProperties.isEnabled(),
                geminiKeyConfigured,
                geminiProperties.getModel(),
                openAIProperties.isEnabled(),
                openAiKeyConfigured,
                openAIProperties.getModel(),
                deepSeekProperties.isEnabled(),
                deepSeekKeyConfigured,
                deepSeekProperties.getModel(),
                groqProperties.isEnabled(),
                groqKeyConfigured,
                groqProperties.getModel(),
                togetherAIProperties.isEnabled(),
                togetherKeyConfigured,
                togetherAIProperties.getModel(),
                openRouterProperties.isEnabled(),
                openRouterKeyConfigured,
                openRouterProperties.getPrimaryModel(),
                openRouterProperties.getSecondaryModel(),
                openRouterProperties.getRuntimeFallbackModels(),
                openRouterProperties.getHttpReferer(),
                openRouterProperties.getTitle(),
                gatewayService.getProviderDiagnostics().stream().anyMatch(AIGatewayService.ProviderStatus::localFallback),
                envFilePresent
        );

        if (openRouterProperties.isEnabled() && !openRouterKeyConfigured) {
            logger.warn("OPENROUTER_API_KEY is missing. OpenRouter fallback is enabled in configuration but cannot be used.");
        }
        if (openAIProperties.isEnabled() && !openAiKeyConfigured) {
            logger.warn("OPENAI_API_KEY is missing. OpenAI fallback is enabled in configuration but cannot be used.");
        }
        if (deepSeekProperties.isEnabled() && !deepSeekKeyConfigured) {
            logger.warn("DEEPSEEK_API_KEY is missing. DeepSeek fallback is enabled in configuration but cannot be used.");
        }
        if (groqProperties.isEnabled() && !groqKeyConfigured) {
            logger.warn("GROQ_API_KEY is missing. Groq fallback is enabled in configuration but cannot be used.");
        }
        if (togetherAIProperties.isEnabled() && !togetherKeyConfigured) {
            logger.warn("TOGETHER_API_KEY is missing. Together AI fallback is enabled in configuration but cannot be used.");
        }
    }
}
