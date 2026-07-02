package org.example.Healthcareplatform.ai.prompt;

import lombok.RequiredArgsConstructor;
import org.example.Healthcareplatform.ai.entity.ConversationMessage;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Assembles the final prompt string from system prompt, health profile,
 * conversation summary, recent history, and the current user message.
 *
 * <h3>Output format (markers)</h3>
 * <pre>{@code
 * [SYSTEM_PROMPT]
 *   ... healthcare instructions ...
 *
 * [HEALTH_PROFILE]          ← Phase 6
 *   ... patient profile ...
 *
 * [CONVERSATION_SUMMARY]    ← Phase 5
 *   Symptoms: headache, nausea
 *   Recommendations: consult doctor, hydration
 *   ...
 *
 * [CONVERSATION_HISTORY]    ← last N messages
 *   USER: I have a headache.
 *   ASSISTANT: I'm sorry...
 *
 * [USER_MESSAGE]
 *   What should I take for it?
 * }</pre>
 * <p>
 * Providers parse these markers into structured API messages.
 * The prompt assembly order is designed so the AI sees:
 * <b>system rules → patient context → convo summary → recent history → current message</b>.
 */
@Component
@RequiredArgsConstructor
public class PromptTemplate {

    private final SystemPrompt systemPrompt;

    /** Markers used to delimit prompt sections. */
    public static final String SYSTEM_MARKER    = "[SYSTEM_PROMPT]";
    public static final String PROFILE_MARKER   = "[HEALTH_PROFILE]";
    public static final String SUMMARY_MARKER   = "[CONVERSATION_SUMMARY]";
    public static final String HISTORY_MARKER   = "[CONVERSATION_HISTORY]";
    public static final String USER_MARKER      = "[USER_MESSAGE]";

    /**
     * Build a prompt without history or summary (backward-compatible).
     */
    public String build(String userMessage) {
        return build(userMessage, Collections.emptyList(), "");
    }

    /**
     * Build a prompt with system + history + current message.
     */
    public String build(String userMessage, List<ConversationMessage> history) {
        return build(userMessage, history, "");
    }

    /**
     * Build the full prompt with system instructions, conversation summary,
     * recent history, and the current user message.
     *
     * @param userMessage the current user input
     * @param history     recent messages (already trimmed to max-history)
     * @param summary     conversation summary for older messages (empty if none)
     * @return the complete prompt string
     */
    public String build(String userMessage, List<ConversationMessage> history, String summary) {
        var sb = new StringBuilder();

        // 1. System prompt
        sb.append(SYSTEM_MARKER).append('\n')
                .append(systemPrompt.getContent()).append('\n');

        // 2. Conversation summary (older messages condensed)
        if (summary != null && !summary.isBlank()) {
            sb.append(SUMMARY_MARKER).append('\n')
                    .append(summary).append('\n');
        }

        // 3. Recent conversation history (last N messages)
        if (history != null && !history.isEmpty()) {
            sb.append(HISTORY_MARKER).append('\n');
            for (ConversationMessage msg : history) {
                sb.append(formatHistoryMessage(msg)).append('\n');
            }
        }

        // 4. Current user message
        sb.append(USER_MARKER).append('\n');
        sb.append(userMessage);

        return sb.toString();
    }

    /**
     * Format a single historical message for the prompt.
     */
    private String formatHistoryMessage(ConversationMessage msg) {
        String prefix = switch (msg.getRole()) {
            case USER -> "USER: ";
            case ASSISTANT -> "ASSISTANT: ";
            case SYSTEM -> "SYSTEM: ";
        };
        return prefix + msg.getContent();
    }
}
