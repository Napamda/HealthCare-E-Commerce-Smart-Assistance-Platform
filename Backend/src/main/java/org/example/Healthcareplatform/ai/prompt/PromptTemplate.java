package org.example.Healthcareplatform.ai.prompt;

import lombok.RequiredArgsConstructor;
import org.example.Healthcareplatform.ai.entity.ConversationMessage;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PromptTemplate {

    private final SystemPrompt systemPrompt;

    public static final String SYSTEM_MARKER    = "[SYSTEM_PROMPT]";
    public static final String PROFILE_MARKER   = "[HEALTH_PROFILE]";
    public static final String SUMMARY_MARKER   = "[CONVERSATION_SUMMARY]";
    public static final String HISTORY_MARKER   = "[CONVERSATION_HISTORY]";
    public static final String USER_MARKER      = "[USER_MESSAGE]";

    public String build(String userMessage) {
        return build(userMessage, Collections.emptyList(), "");
    }

    public String build(String userMessage, List<ConversationMessage> history) {
        return build(userMessage, history, "");
    }

    public String build(String userMessage, List<ConversationMessage> history, String summary) {
        var sb = new StringBuilder();

        sb.append(SYSTEM_MARKER).append('\n')
                .append(systemPrompt.getContent()).append('\n');

        if (summary != null && !summary.isBlank()) {
            sb.append(SUMMARY_MARKER).append('\n')
                    .append(summary).append('\n');
        }

        if (history != null && !history.isEmpty()) {
            sb.append(HISTORY_MARKER).append('\n');
            for (ConversationMessage msg : history) {
                sb.append(formatHistoryMessage(msg)).append('\n');
            }
        }

        sb.append(USER_MARKER).append('\n');
        sb.append(userMessage);

        return sb.toString();
    }

    private String formatHistoryMessage(ConversationMessage msg) {
        String prefix = switch (msg.getRole()) {
            case USER -> "USER: ";
            case ASSISTANT -> "ASSISTANT: ";
            case SYSTEM -> "SYSTEM: ";
        };
        return prefix + msg.getContent();
    }
}
