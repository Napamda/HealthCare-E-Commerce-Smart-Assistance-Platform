package org.example.Healthcareplatform.ai.provider;

import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.ai.exception.ProviderUnavailableException;
import org.example.Healthcareplatform.ai.prompt.PromptTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class OpenRouterProvider implements AIProvider {

    private final RestClient restClient;
    private final String model;

    public OpenRouterProvider(String baseUrl, String apiKey, String model) {
        this.model = model;

        if (apiKey == null || apiKey.isBlank()) {
            throw new ProviderUnavailableException("OpenRouter",
                    new IllegalStateException("OPENROUTER_API_KEY is not set"));
        }

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactoryWithTimeouts())
                .build();

        log.info("OpenRouterProvider initialized — model={}, baseUrl={}", model, baseUrl);
    }

    private static SimpleClientHttpRequestFactory requestFactoryWithTimeouts() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        return factory;
    }

    @Override
    public String chat(String prompt) {
        log.info("OpenRouterProvider sending prompt ({} chars)", prompt.length());

        try {
            var messages = buildMessages(prompt);

            var body = Map.of(
                    "model", model,
                    "messages", messages
            );

            var response = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .exchange((req, resp) -> {
                        HttpStatusCode status = resp.getStatusCode();
                        if (status.isError()) {
                            byte[] errorBody = resp.getBody().readAllBytes();
                            String errorText = new String(errorBody);
                            log.error("OpenRouter HTTP {} — body: {}", status.value(), errorText);
                            throw new ProviderUnavailableException("OpenRouter",
                                    new RuntimeException("HTTP " + status.value() + ": " + errorText));
                        }
                        return resp.bodyTo(OpenRouterChatResponse.class);
                    });

            if (response == null
                    || response.choices() == null
                    || response.choices().isEmpty()
                    || response.choices().get(0).message() == null) {
                throw new ProviderUnavailableException("OpenRouter",
                        new IllegalStateException("Empty response from API"));
            }

            String content = response.choices().get(0).message().content();
            log.info("OpenRouterProvider response received ({} chars)", content.length());
            return content;

        } catch (ProviderUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenRouter API call failed", e);
            throw new ProviderUnavailableException("OpenRouter", e);
        }
    }

    @Override
    public String providerName() {
        return "OpenRouter";
    }

    @Override
    public String modelName() {
        return model;
    }

    private List<Map<String, String>> buildMessages(String prompt) {
        var messages = new ArrayList<Map<String, String>>();

        var systemContext = new StringBuilder();

        String systemContent = extractSection(prompt,
                PromptTemplate.SYSTEM_MARKER, nextMarker(prompt, PromptTemplate.SYSTEM_MARKER));
        if (!systemContent.isBlank()) {
            systemContext.append(systemContent).append("\n\n");
        }

        String summaryContent = extractSection(prompt,
                PromptTemplate.SUMMARY_MARKER, nextMarker(prompt, PromptTemplate.SUMMARY_MARKER));
        if (!summaryContent.isBlank()) {
            systemContext.append(summaryContent).append("\n\n");
        }

        if (!systemContext.isEmpty()) {
            messages.add(Map.of("role", "system", "content", systemContext.toString().trim()));
        }

        String historyContent = extractSection(prompt,
                PromptTemplate.HISTORY_MARKER, nextMarker(prompt, PromptTemplate.HISTORY_MARKER));
        if (!historyContent.isBlank()) {
            messages.addAll(parseHistorySection(historyContent));
        }

        String userContent = extractSection(prompt,
                PromptTemplate.USER_MARKER, null);
        if (!userContent.isBlank()) {
            messages.add(Map.of("role", "user", "content", userContent));
        }

        if (messages.isEmpty()) {
            messages.add(Map.of("role", "user", "content", prompt));
        }

        return messages;
    }

    private List<Map<String, String>> parseHistorySection(String history) {
        var messages = new ArrayList<Map<String, String>>();
        String[] lines = history.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.startsWith("USER: ")) {
                messages.add(Map.of("role", "user", "content", trimmed.substring(6)));
            } else if (trimmed.startsWith("ASSISTANT: ")) {
                messages.add(Map.of("role", "assistant", "content", trimmed.substring(11)));
            } else if (trimmed.startsWith("SYSTEM: ")) {
                messages.add(Map.of("role", "system", "content", trimmed.substring(8)));
            }
        }
        return messages;
    }

    private String nextMarker(String prompt, String currentMarker) {
        int currentPos = prompt.indexOf(currentMarker);
        if (currentPos < 0) return null;

        int searchFrom = currentPos + currentMarker.length();

        String[] candidates = {
                PromptTemplate.SYSTEM_MARKER,
                PromptTemplate.PROFILE_MARKER,
                PromptTemplate.SUMMARY_MARKER,
                PromptTemplate.HISTORY_MARKER,
                PromptTemplate.USER_MARKER
        };

        String best = null;
        int bestPos = Integer.MAX_VALUE;

        for (String candidate : candidates) {
            if (candidate.equals(currentMarker)) continue;
            int pos = prompt.indexOf(candidate, searchFrom);
            if (pos >= 0 && pos < bestPos) {
                bestPos = pos;
                best = candidate;
            }
        }
        return best;
    }

    private String extractSection(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        if (start < 0) return "";
        start += startMarker.length();

        int end = (endMarker != null) ? text.indexOf(endMarker, start) : -1;
        if (end < 0) end = text.length();

        return text.substring(start, end).trim();
    }

    private record OpenRouterChatResponse(List<Choice> choices) {}
    private record Choice(Message message) {}
    private record Message(String role, String content) {}
}
