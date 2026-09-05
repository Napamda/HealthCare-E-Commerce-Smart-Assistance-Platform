package org.example.Healthcareplatform.ai.ocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
public class OpenRouterOCRProvider implements OCRProvider {

    private final RestClient restClient;
    private final String model;
    private final ObjectMapper objectMapper;

    private static final int MAX_RESULT_CHARS = 3000;

    private static final String OCR_PROMPT =
            "You are a medical OCR system. Extract ALL readable text from this prescription image. "
            + "Include: patient name, medication names, dosages, frequencies, doctor name, date, "
            + "and any other text visible on the prescription. "
            + "Format the output as clean, structured text. "
            + "If no text is visible, respond with 'No readable text detected in this image.' "
            + "Do NOT add explanations, disclaimers, or commentary — only output the extracted text.";

    public OpenRouterOCRProvider(String baseUrl, String apiKey, String model, ObjectMapper objectMapper) {
        this.model = model;
        this.objectMapper = objectMapper;

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENROUTER_API_KEY is not set — OCR requires an API key");
        }

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactoryWithTimeouts())
                .build();

        log.info("OpenRouterOCRProvider initialized — model={}, baseUrl={}", model, baseUrl);
    }

    private static SimpleClientHttpRequestFactory requestFactoryWithTimeouts() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15_000);
        factory.setReadTimeout(60_000);
        return factory;
    }

    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1000;

    @Override
    public String extractText(byte[] imageBytes, String mimeType) {
        log.info("OpenRouterOCRProvider — extracting text from {} image ({} bytes)", mimeType, imageBytes.length);

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String dataUri = "data:" + mimeType + ";base64," + base64Image;

        List<Map<String, Object>> content = List.of(
                Map.of("type", "text", "text", OCR_PROMPT),
                Map.of("type", "image_url", "image_url", Map.of("url", dataUri))
        );

        var message = Map.of("role", "user", "content", content);
        var body = Map.of("model", model, "messages", List.of(message));

        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String responseJson = restClient.post()
                        .uri("/chat/completions")
                        .body(body)
                        .exchange((req, resp) -> {
                            HttpStatusCode status = resp.getStatusCode();
                            if (status.is5xxServerError() || status.value() == 429) {
                                byte[] errorBody = resp.getBody().readAllBytes();
                                String errorText = new String(errorBody);
                                log.warn("OpenRouter OCR HTTP {} on attempt {}/{} — retryable: {}",
                                        status.value(), attempt, MAX_RETRIES, errorText);
                                throw new RuntimeException("HTTP " + status.value() + ": " + errorText);
                            }
                            if (status.isError()) {
                                byte[] errorBody = resp.getBody().readAllBytes();
                                String errorText = new String(errorBody);
                                log.error("OpenRouter OCR HTTP {} — non-retryable: {}", status.value(), errorText);
                                throw new RuntimeException("OpenRouter OCR error: HTTP " + status.value());
                            }
                            byte[] responseBytes = resp.getBody().readAllBytes();
                            return new String(responseBytes);
                        });

                return extractContentFromResponse(responseJson);

            } catch (Exception e) {
                lastException = e;
                if (attempt < MAX_RETRIES && isRetryableOcr(e)) {
                    long delayMs = BASE_DELAY_MS * (1L << (attempt - 1));
                    log.warn("OpenRouter OCR attempt {}/{} failed ({}), retrying in {}ms",
                            attempt, MAX_RETRIES, e.getMessage(), delayMs);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("OCR retry interrupted", ie);
                    }
                } else {
                    if (attempt >= MAX_RETRIES) {
                        log.error("OpenRouter OCR call exhausted all {} retries", MAX_RETRIES, e);
                    }
                }
            }
        }

        throw new RuntimeException("OCR extraction failed after " + MAX_RETRIES + " attempts",
                lastException);
    }

    private boolean isRetryableOcr(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return e instanceof java.io.IOException
                || msg.contains("timeout")
                || msg.contains("connection")
                || msg.contains("network")
                || msg.contains("reset")
                || msg.contains("refused")
                || msg.contains("5")
                || msg.contains("429");
    }

    @SuppressWarnings("unchecked")
    private String extractContentFromResponse(String responseJson) {
        try {
            var response = objectMapper.readValue(responseJson, Map.class);
            var choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                log.warn("OCR response has no choices");
                return "";
            }
            var message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) {
                log.warn("OCR response choice has no message");
                return "";
            }
            String content = (String) message.get("content");
            if (content == null) {
                return "";
            }
            content = content.trim();
            if (content.length() > MAX_RESULT_CHARS) {
                content = content.substring(0, MAX_RESULT_CHARS);
            }
            return content;
        } catch (Exception e) {
            log.error("Failed to parse OCR response JSON: {}", responseJson, e);
            return "";
        }
    }

    @Override
    public String providerName() {
        return "OpenRouter-" + model;
    }
}
