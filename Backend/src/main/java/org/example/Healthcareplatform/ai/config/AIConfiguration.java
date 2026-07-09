package org.example.Healthcareplatform.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.ai.ocr.FallbackOCRProvider;
import org.example.Healthcareplatform.ai.ocr.OCRProvider;
import org.example.Healthcareplatform.ai.ocr.OpenRouterOCRProvider;
import org.example.Healthcareplatform.ai.provider.AIProvider;
import org.example.Healthcareplatform.ai.provider.MockProvider;
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


    @Bean
    @Primary
    @ConditionalOnProperty(name = "ai.provider", havingValue = "openrouter", matchIfMissing = true)
    public AIProvider openRouterProvider() {
        log.info("Activating OpenRouter provider — model={}, base-url={}", openRouterModel, openRouterBaseUrl);
        return new OpenRouterProvider(openRouterBaseUrl, openRouterApiKey, openRouterModel);
    }

    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "mock")
    public AIProvider mockAiProvider() {
        log.info("Activating Mock AI provider");
        return new MockProvider();
    }


    @Bean
    @ConditionalOnMissingBean(AIProvider.class)
    public AIProvider fallbackProvider() {
        log.warn("No AI provider configured — falling back to MockProvider");
        return new MockProvider();
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
