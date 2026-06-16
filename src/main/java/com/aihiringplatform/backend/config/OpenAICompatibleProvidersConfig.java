package com.aihiringplatform.backend.config;

import com.aihiringplatform.backend.service.AIResponseParser;
import com.aihiringplatform.backend.service.ai.AIProvider;
import com.aihiringplatform.backend.service.ai.ManagedExternalAIProvider;
import com.aihiringplatform.backend.service.chat.OpenAICompatibleService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAICompatibleProvidersConfig {

    @Bean
    public RestClient openAiRestClient(OpenAIProperties properties) {
        return buildRestClient(properties);
    }

    @Bean
    public RestClient groqRestClient(GroqProperties properties) {
        return buildRestClient(properties);
    }

    @Bean
    public RestClient deepSeekRestClient(DeepSeekProperties properties) {
        return buildRestClient(properties);
    }

    @Bean
    public RestClient togetherAiRestClient(TogetherAIProperties properties) {
        return buildRestClient(properties);
    }

    @Bean
    public OpenAICompatibleService openAiChatService(
            @Qualifier("openAiRestClient") RestClient restClient,
            OpenAIProperties properties,
            AIResponseParser responseParser
    ) {
        return new OpenAICompatibleService("OpenAI", restClient, properties, responseParser);
    }

    @Bean
    public OpenAICompatibleService groqChatService(
            @Qualifier("groqRestClient") RestClient restClient,
            GroqProperties properties,
            AIResponseParser responseParser
    ) {
        return new OpenAICompatibleService("Groq", restClient, properties, responseParser);
    }

    @Bean
    public OpenAICompatibleService deepSeekChatService(
            @Qualifier("deepSeekRestClient") RestClient restClient,
            DeepSeekProperties properties,
            AIResponseParser responseParser
    ) {
        return new OpenAICompatibleService("DeepSeek", restClient, properties, responseParser);
    }

    @Bean
    public OpenAICompatibleService togetherAiChatService(
            @Qualifier("togetherAiRestClient") RestClient restClient,
            TogetherAIProperties properties,
            AIResponseParser responseParser
    ) {
        return new OpenAICompatibleService("Together AI", restClient, properties, responseParser);
    }

    @Bean
    public AIProvider openAIProvider(
            @Qualifier("openAiChatService") OpenAICompatibleService service,
            AiStabilityProperties stabilityProperties
    ) {
        return new ManagedExternalAIProvider(
                "OpenAI",
                service::isConfigured,
                service,
                stabilityProperties
        );
    }

    @Bean
    public AIProvider groqProvider(
            @Qualifier("groqChatService") OpenAICompatibleService service,
            AiStabilityProperties stabilityProperties
    ) {
        return new ManagedExternalAIProvider(
                "Groq",
                service::isConfigured,
                service,
                stabilityProperties
        );
    }

    @Bean
    public AIProvider deepSeekProvider(
            @Qualifier("deepSeekChatService") OpenAICompatibleService service,
            AiStabilityProperties stabilityProperties
    ) {
        return new ManagedExternalAIProvider(
                "DeepSeek",
                service::isConfigured,
                service,
                stabilityProperties
        );
    }

    @Bean
    public AIProvider togetherAIProvider(
            @Qualifier("togetherAiChatService") OpenAICompatibleService service,
            AiStabilityProperties stabilityProperties
    ) {
        return new ManagedExternalAIProvider(
                "Together AI",
                service::isConfigured,
                service,
                stabilityProperties
        );
    }

    private RestClient buildRestClient(ChatCompletionProviderProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMillis());
        requestFactory.setReadTimeout(properties.getReadTimeoutMillis());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(properties.getBaseUrl())
                .build();
    }
}
