package org.example.Healthcareplatform.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.ai.entity.ConversationMessage;
import org.example.Healthcareplatform.ai.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptService {

    private final PromptTemplate template;

    public String buildPrompt(String userMessage) {
        return buildPrompt(userMessage, Collections.emptyList(), "");
    }

    public String buildPrompt(String userMessage, List<ConversationMessage> history) {
        return buildPrompt(userMessage, history, "");
    }

    public String buildPrompt(String userMessage, List<ConversationMessage> history, String summary) {
        int historySize = (history != null) ? history.size() : 0;
        int summaryLen = (summary != null) ? summary.length() : 0;
        log.debug("Building prompt — user ({} chars), history ({} msgs), summary ({} chars)",
                userMessage.length(), historySize, summaryLen);
        return template.build(userMessage, history, summary);
    }
}
