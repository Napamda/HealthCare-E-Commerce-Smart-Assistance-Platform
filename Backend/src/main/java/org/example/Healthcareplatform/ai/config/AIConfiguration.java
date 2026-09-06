package org.example.Healthcareplatform.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.ai.ocr.FallbackOCRProvider;
import org.example.Healthcareplatform.ai.ocr.OCRProvider;
import org.example.Healthcareplatform.ai.ocr.OpenRouterOCRProvider;
import org.example.Healthcareplatform.ai.provider.AIProvider;
import org.example.Healthcareplatform.ai.provider.FallbackAIProvider;
import org.example.Healthcareplatform.ai.provider.OpenRouterProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration
public class AIConfiguration {

    @Value("${ai.openrouter.api-key:}")
    private String openRouterApiKey;

    @Value("${ai.openrouter.base-url:https://openrouter.ai/api/v1}")
    private String openRouterBaseUrl;

    @Value("${ai.openrouter.model:openrouter/free}")
    private String openRouterModel;

    @Value("${ai.ocr.model:nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free}")
    private String ocrModel;

    @Value("${ai.provider:openrouter}")
    private String provider;

    private boolean keyConfigured() {
        return openRouterApiKey != null && !openRouterApiKey.isBlank();
    }


    @Bean
    @Primary
    public AIProvider openRouterProvider() {
        if ("mock".equalsIgnoreCase(provider)) {
            log.info("Activating deterministic mock AI provider");
            return new AIProvider() {
                @Override public String chat(String prompt) { return "I am your healthcare assistant. How can I help?"; }
                @Override public String providerName() { return "Mock"; }
                @Override public String modelName() { return "mock/v1"; }
            };
        }
        if (!keyConfigured()) {
            log.warn("OPENROUTER_API_KEY is not set — AI chat/recommendations disabled. Set OPENROUTER_API_KEY to enable.");
            return new FallbackAIProvider("OPENROUTER_API_KEY is not set — set it to enable AI features");
        }
        log.info("Activating OpenRouter provider — model={}, base-url={}", openRouterModel, openRouterBaseUrl);
        return new OpenRouterProvider(openRouterBaseUrl, openRouterApiKey, openRouterModel);
    }

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "ai.ocr.enabled", havingValue = "true", matchIfMissing = true)
    public OCRProvider openRouterOCRProvider(ObjectMapper objectMapper) {
        if (!keyConfigured()) {
            log.warn("OPENROUTER_API_KEY is not set — OCR disabled, using fallback extractor");
            return new FallbackOCRProvider();
        }
        log.info("Activating OpenRouter OCR provider — model={}", ocrModel);
        return new OpenRouterOCRProvider(openRouterBaseUrl, openRouterApiKey, ocrModel, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "ai.ocr.enabled", havingValue = "false")
    public OCRProvider disabledOCRProvider() {
        log.info("OCR disabled — using fallback only");
        return new FallbackOCRProvider();
    }

    @Bean
    @ConditionalOnMissingBean(OCRProvider.class)
    public OCRProvider fallbackOCRProvider() {
        log.warn("No OCR provider configured — falling back to FallbackOCRProvider");
        return new FallbackOCRProvider();
    }
}
