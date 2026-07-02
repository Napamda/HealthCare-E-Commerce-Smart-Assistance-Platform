package org.example.Healthcareplatform.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.ai.entity.ConversationMessage;
import org.example.Healthcareplatform.ai.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Builds the final prompt sent to the AI provider.
 *
 * <h3>Prompt assembly order</h3>
 * <pre>{@code
 * System Prompt
 *    ↓
 * Health Profile      (Phase 6)
 *    ↓
 * Conversation Summary (Phase 5 — rule-based)
 *    ↓
 * Last N Messages     (Phase 5 — configurable via ai.memory.max-history)
 *    ↓
 * Current User Message
 * }</pre>
 * <p>
 * The provider interface never changes — it always receives a plain {@code String}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptService {

    private final PromptTemplate template;

    /** Minimal prompt: system + current message only. */
    public String buildPrompt(String userMessage) {
        return buildPrompt(userMessage, Collections.emptyList(), "");
    }

    /** Prompt with system + recent history + current message. */
    public String buildPrompt(String userMessage, List<ConversationMessage> history) {
        return buildPrompt(userMessage, history, "");
    }

    /**
     * Full prompt with system, conversation summary, recent history,
     * and the current user message.
     *
     * @param userMessage  the user's latest input
     * @param history      recent messages within the max-history window
     * @param summary      compact summary of older messages (empty if not needed)
     * @return the complete prompt string
     */
    public String buildPrompt(String userMessage, List<ConversationMessage> history, String summary) {
        int historySize = (history != null) ? history.size() : 0;
        int summaryLen = (summary != null) ? summary.length() : 0;
        log.debug("Building prompt — user ({} chars), history ({} msgs), summary ({} chars)",
                userMessage.length(), historySize, summaryLen);
        return template.build(userMessage, history, summary);
    }
}
