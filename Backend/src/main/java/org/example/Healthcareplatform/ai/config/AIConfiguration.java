package org.example.Healthcareplatform.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.ai.provider.AIProvider;
import org.example.Healthcareplatform.ai.provider.MockProvider;
import org.example.Healthcareplatform.ai.provider.OpenRouterProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * AI module configuration.
 * <p>
 * Provider selection logic:
 * <ul>
 *   <li>{@code ai.provider=openrouter} → {@link OpenRouterProvider} (default)</li>
 *   <li>{@code ai.provider=mock}        → {@link MockProvider}</li>
 *   <li>If neither can be created       → falls back to {@link MockProvider}</li>
 * </ul>
 */
@Slf4j
@Configuration
public class AIConfiguration {

    @Value("${ai.openrouter.api-key:}")
    private String openRouterApiKey;

    @Value("${ai.openrouter.base-url:https://openrouter.ai/api/v1}")
    private String openRouterBaseUrl;

    @Value("${ai.openrouter.model:google/gemini-2.0-flash-001}")
    private String openRouterModel;

    @Value("${ai.openrouter.timeout-seconds:60}")
    private int openRouterTimeout;

    // ---- Provider beans ----

    /**
     * Creates the OpenRouter provider when {@code ai.provider=openrouter} (the default).
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "ai.provider", havingValue = "openrouter", matchIfMissing = true)
    public AIProvider openRouterProvider() {
        log.info("Activating OpenRouter provider — model={}, base-url={}", openRouterModel, openRouterBaseUrl);
        return new OpenRouterProvider(openRouterBaseUrl, openRouterApiKey, openRouterModel);
    }

    /**
     * Creates the Mock provider when {@code ai.provider=mock}.
     */
    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "mock")
    public AIProvider mockAiProvider() {
        log.info("Activating Mock AI provider");
        return new MockProvider();
    }

    /**
     * Ultimate fallback — if neither mode produced an AIProvider, use Mock.
     */
    @Bean
    @ConditionalOnMissingBean(AIProvider.class)
    public AIProvider fallbackProvider() {
        log.warn("No AI provider configured — falling back to MockProvider");
        return new MockProvider();
    }
}
