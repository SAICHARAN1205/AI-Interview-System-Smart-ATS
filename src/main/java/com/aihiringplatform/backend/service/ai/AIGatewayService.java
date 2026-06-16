package com.aihiringplatform.backend.service.ai;

import com.aihiringplatform.backend.service.PromptBuilderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class AIGatewayService {

    private static final Logger logger = LoggerFactory.getLogger(AIGatewayService.class);

    private static final String GEMINI = "Gemini";
    private static final String OPENAI = "OpenAI";
    private static final String DEEPSEEK = "DeepSeek";
    private static final String GROQ = "Groq";
    private static final String TOGETHER = "Together AI";
    private static final String OPENROUTER = "OpenRouter";
    private static final String LOCAL_FALLBACK = "SmartATS local fallback";

    private static final List<String> DEFAULT_PROVIDER_ORDER = List.of(
            GEMINI,
            OPENAI,
            DEEPSEEK,
            GROQ,
            TOGETHER,
            OPENROUTER,
            LOCAL_FALLBACK
    );

    private final Map<String, AIProvider> providerByName;
    private final Map<String, List<String>> providerOrderByOperation;
    private final com.aihiringplatform.backend.repository.AIProviderConfigRepository aiProviderConfigRepository;

    public AIGatewayService(AIProvider[] providers, com.aihiringplatform.backend.repository.AIProviderConfigRepository aiProviderConfigRepository) {
        this.aiProviderConfigRepository = aiProviderConfigRepository;
        this.providerByName = new LinkedHashMap<>();
        Arrays.stream(providers)
                .filter(provider -> provider != null && provider.getName() != null && !provider.getName().isBlank())
                .forEach(provider -> providerByName.put(provider.getName(), provider));

        this.providerOrderByOperation = Map.of(
                "question-generation", List.of(GEMINI, GROQ, OPENAI, DEEPSEEK, TOGETHER, OPENROUTER, LOCAL_FALLBACK),
                "answer-evaluation", List.of(OPENAI, DEEPSEEK, GEMINI, GROQ, TOGETHER, OPENROUTER, LOCAL_FALLBACK),
                "interview-evaluation", List.of(OPENAI, DEEPSEEK, GEMINI, GROQ, TOGETHER, OPENROUTER, LOCAL_FALLBACK),
                "ats-analysis", List.of(GEMINI, TOGETHER, OPENAI, DEEPSEEK, GROQ, OPENROUTER, LOCAL_FALLBACK),
                "job-match", List.of(GEMINI, OPENAI, DEEPSEEK, TOGETHER, GROQ, OPENROUTER, LOCAL_FALLBACK)
        );
    }

    public <T> AIGatewayResult<T> generateStructuredResponse(
            PromptBuilderService.PromptDefinition prompt,
            Class<T> responseType,
            Supplier<T> localFallbackSupplier
    ) {
        List<AIProvider> providers = orderedProvidersFor(prompt == null ? null : prompt.operation());
        logger.info(
                "AI gateway request started for {}. Providers in order: {}",
                prompt.operation(),
                providers.stream().map(AIProvider::getName).toList()
        );
        List<String> fallbackReasons = new ArrayList<>();
        long gatewayStartedAt = System.currentTimeMillis();

        for (int index = 0; index < providers.size(); index++) {
            AIProvider provider = providers.get(index);

            if (!provider.isEnabled()) {
                logger.info(
                        "AI gateway skipping {} for {} because it is disabled",
                        provider.getName(),
                        prompt.operation()
                );
                continue;
            }

            if (!provider.isAvailable()) {
                String cooldownReason = provider.getName() + " cooldown=" + provider.getRemainingCooldownMillis() + "ms";
                fallbackReasons.add(cooldownReason);
                logger.warn(
                        "{} cooling down for {} ms -> switched to {} for {}",
                        provider.getName(),
                        provider.getRemainingCooldownMillis(),
                        nextProviderName(providers, index),
                        prompt.operation()
                );
                continue;
            }

            long startedAt = System.currentTimeMillis();
            try {
                logger.info("AI gateway selected {} for {}", provider.getName(), prompt.operation());
                T payload = provider.generateStructuredResponse(prompt, responseType, localFallbackSupplier);
                long elapsedMillis = System.currentTimeMillis() - startedAt;
                logger.info(
                        "AI gateway used {} for {} in {} ms (gateway elapsed={} ms, fallbackReason={})",
                        provider.getName(),
                        prompt.operation(),
                        elapsedMillis,
                        System.currentTimeMillis() - gatewayStartedAt,
                        fallbackReasons.isEmpty() ? "none" : String.join(" | ", fallbackReasons)
                );
                return new AIGatewayResult<>(payload, provider.getName(), provider.isLocalFallback());
            } catch (AIProviderException exception) {
                fallbackReasons.add(provider.getName() + "=" + exception.getFailureReason());
                logger.warn(
                        "{} unavailable -> switched to {} for {} after {} ms ({})",
                        provider.getName(),
                        nextProviderName(providers, index),
                        prompt.operation(),
                        System.currentTimeMillis() - startedAt,
                        exception.getFailureReason()
                );
            }
        }

        logger.error(
                "AI gateway exhausted configured providers for {} after {} ms. Reasons: {}. Forcing local fallback.",
                prompt.operation(),
                System.currentTimeMillis() - gatewayStartedAt,
                fallbackReasons.isEmpty() ? "none" : String.join(" | ", fallbackReasons)
        );
        T payload = localFallbackSupplier == null ? null : localFallbackSupplier.get();
        return new AIGatewayResult<>(payload, LOCAL_FALLBACK, true);
    }

    public List<ProviderStatus> getProviderDiagnostics() {
        return orderedProvidersFor(null).stream()
                .map(provider -> new ProviderStatus(
                        provider.getName(),
                        provider.isEnabled(),
                        provider.isAvailable(),
                        provider.isLocalFallback(),
                        provider.getRemainingCooldownMillis()
                ))
                .toList();
    }

    private List<AIProvider> orderedProvidersFor(String operation) {
        List<com.aihiringplatform.backend.entity.AIProviderConfig> dbConfigs = aiProviderConfigRepository.findAllByOrderByPriorityOrderAsc();
        if (dbConfigs.isEmpty()) {
            List<String> configuredOrder = operation == null ? DEFAULT_PROVIDER_ORDER : providerOrderByOperation.getOrDefault(operation, DEFAULT_PROVIDER_ORDER);
            return configuredOrder.stream()
                    .map(providerByName::get)
                    .filter(provider -> provider != null)
                    .toList();
        } else {
            return dbConfigs.stream()
                    .filter(com.aihiringplatform.backend.entity.AIProviderConfig::isEnabled)
                    .map(config -> providerByName.get(config.getProviderName()))
                    .filter(provider -> provider != null)
                    .toList();
        }
    }

    private String nextProviderName(List<AIProvider> providers, int currentIndex) {
        for (int index = currentIndex + 1; index < providers.size(); index++) {
            AIProvider provider = providers.get(index);
            if (provider.isEnabled()) {
                return provider.getName();
            }
        }
        return "no provider";
    }

    public record ProviderStatus(
            String name,
            boolean enabled,
            boolean available,
            boolean localFallback,
            long cooldownMillis
    ) {
    }
}
