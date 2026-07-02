package org.example.Healthcareplatform.ai.provider;

import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.ai.prompt.PromptTemplate;

/**
 * Mock AI provider for local development and testing.
 * Returns a deterministic response without calling any external API.
 * <p>
 * This bean is created by {@code AIConfiguration} — not auto-detected —
 * so provider selection remains in one place.
 */
@Slf4j
public class MockProvider implements AIProvider {

    private static final String MODEL = "mock/v1";

    private static final String MOCK_RESPONSE_TEMPLATE =
            """
            [Mock AI] Thank you for your message.

            You said: "%s"

            ⚕️ Medical Disclaimer: This is a mock response for testing purposes.
            Please consult a licensed healthcare professional for medical advice.
            """;

    @Override
    public String chat(String prompt) {
        log.info("MockProvider received prompt ({} chars)", prompt.length());

        // Extract just the user message portion for the mock echo
        String userMessage = extractSection(prompt,
                PromptTemplate.USER_MARKER, null);

        if (userMessage.isBlank()) {
            userMessage = prompt; // fallback: echo the whole prompt
        }

        String response = String.format(MOCK_RESPONSE_TEMPLATE, userMessage);
        log.info("MockProvider returning response ({} chars)", response.length());
        return response;
    }

    @Override
    public String providerName() {
        return "Mock";
    }

    @Override
    public String modelName() {
        return MODEL;
    }

    private String extractSection(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        if (start < 0) return "";
        start += startMarker.length();

        int end = (endMarker != null) ? text.indexOf(endMarker, start) : -1;
        if (end < 0) end = text.length();

        return text.substring(start, end).trim();
    }
}
