package org.example.Healthcareplatform.ai.service;

import org.example.Healthcareplatform.ai.dto.ChatRequest;
import org.example.Healthcareplatform.ai.dto.ChatResponse;
import org.example.Healthcareplatform.ai.entity.Conversation;
import org.example.Healthcareplatform.ai.entity.ConversationMessage;
import org.example.Healthcareplatform.ai.provider.AIProvider;
import org.example.Healthcareplatform.consultation.entity.Consultation;
import org.example.Healthcareplatform.consultation.repository.ConsultationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIServiceTest {

    @Mock
    private AIProvider aiProvider;

    @Mock
    private PromptService promptService;

    @Mock
    private ConversationService conversationService;

    @Mock
    private ConversationSummaryService summaryService;

    @Mock
    private ConsultationRepository consultationRepository;

    @InjectMocks
    private AIService aiService;

    private Conversation conversation;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiService, "maxHistory", 3);
        ReflectionTestUtils.setField(aiService, "summaryThreshold", 5);

        conversation = Conversation.builder()
                .id(10L)
                .userId(1L)
                .title("Test")
                .status(Conversation.ConversationStatus.ACTIVE)
                .build();

        when(aiProvider.providerName()).thenReturn("MockProvider");
        when(aiProvider.modelName()).thenReturn("mock-model");
        // No active doctor consultation by default.
        lenient().when(consultationRepository.findFirstByConversationIdAndStatusIn(anyLong(), anyList()))
                .thenReturn(Optional.empty());
    }

    @Test
    void shouldCreateChatResponseAndPersistAssistantMessage() {
        ChatRequest request = ChatRequest.builder()
                .conversationId(null)
                .message("Hello")
                .build();

        ConversationMessage userMessage = ConversationMessage.builder()
                .id(1L)
                .conversationId(10L)
                .role(ConversationMessage.MessageRole.USER)
                .content("Hello")
                .build();

        ConversationMessage assistantMessage = ConversationMessage.builder()
                .id(2L)
                .conversationId(10L)
                .role(ConversationMessage.MessageRole.ASSISTANT)
                .content("Hello from AI")
                .build();

        when(conversationService.findOrCreateConversation(null, 1L, "Hello"))
                .thenReturn(conversation);
        when(conversationService.saveUserMessage(10L, "Hello"))
                .thenReturn(userMessage);
        when(conversationService.getMessages(10L))
                .thenReturn(List.of(userMessage));
        when(promptService.buildPrompt(eq("Hello"), anyList(), eq("")))
                .thenReturn("prompt");
        when(aiProvider.chat("prompt"))
                .thenReturn("Hello from AI");
        when(conversationService.saveAssistantMessage(
                10L,
                "Hello from AI",
                "MockProvider",
                "mock-model"
        )).thenReturn(assistantMessage);

        ChatResponse result = aiService.chat(request, 1L);

        assertThat(result.getConversationId()).isEqualTo(10L);
        assertThat(result.getMessageId()).isEqualTo(2L);
        assertThat(result.getResponse()).isEqualTo("Hello from AI");
        assertThat(result.getProvider()).isEqualTo("MockProvider");
        assertThat(result.getModel()).isEqualTo("mock-model");
        assertThat(result.getTimestamp()).isNotNull();

        verify(conversationService).saveUserMessage(10L, "Hello");
        verify(aiProvider).chat("prompt");
        verify(conversationService).saveAssistantMessage(
                10L,
                "Hello from AI",
                "MockProvider",
                "mock-model"
        );
    }

    @Test
    void shouldUseAllMessagesWhenHistoryIsWithinMaxHistory() {
        ChatRequest request = ChatRequest.builder()
                .message("Current message")
                .build();

        List<ConversationMessage> messages = List.of(
                message(1L, "One"),
                message(2L, "Two"),
                message(3L, "Three")
        );

        configureSuccessfulChat(request, messages);

        aiService.chat(request, 1L);

        verify(promptService).buildPrompt(
                eq("Current message"),
                eq(messages),
                eq("")
        );
        verify(summaryService, never()).summarize(anyList());
    }

    @Test
    void shouldKeepOnlyRecentHistoryWhenAboveMaxHistoryButBelowSummaryThreshold() {
        ChatRequest request = ChatRequest.builder()
                .message("Current message")
                .build();

        List<ConversationMessage> messages = List.of(
                message(1L, "One"),
                message(2L, "Two"),
                message(3L, "Three"),
                message(4L, "Four")
        );

        configureSuccessfulChat(request, messages);

        aiService.chat(request, 1L);

        verify(promptService).buildPrompt(
                eq("Current message"),
                eq(messages.subList(1, 4)),
                eq("")
        );
        verify(summaryService, never()).summarize(anyList());
    }

    @Test
    void shouldSummarizeOlderHistoryWhenThresholdExceeded() {
        ChatRequest request = ChatRequest.builder()
                .message("Current message")
                .build();

        List<ConversationMessage> messages = new ArrayList<>();
        for (long i = 1; i <= 6; i++) {
            messages.add(message(i, "Message " + i));
        }

        when(conversationService.findOrCreateConversation(
                request.getConversationId(),
                1L,
                request.getMessage()
        )).thenReturn(conversation);

        when(conversationService.saveUserMessage(10L, "Current message"))
                .thenReturn(message(99L, "Current message"));

        when(conversationService.getMessages(10L))
                .thenReturn(messages);

        when(summaryService.summarize(messages.subList(0, 3)))
                .thenReturn("Older conversation summary");

        when(promptService.buildPrompt(
                eq("Current message"),
                eq(messages.subList(3, 6)),
                eq("Older conversation summary")
        )).thenReturn("prompt");

        when(aiProvider.chat("prompt")).thenReturn("AI answer");

        when(conversationService.saveAssistantMessage(
                10L,
                "AI answer",
                "MockProvider",
                "mock-model"
        )).thenReturn(message(100L, "AI answer"));

        ChatResponse result = aiService.chat(request, 1L);

        assertThat(result.getResponse()).isEqualTo("AI answer");

        verify(summaryService).summarize(messages.subList(0, 3));
        verify(promptService).buildPrompt(
                eq("Current message"),
                eq(messages.subList(3, 6)),
                eq("Older conversation summary")
        );
    }

    private void configureSuccessfulChat(
            ChatRequest request,
            List<ConversationMessage> messages
    ) {
        when(conversationService.findOrCreateConversation(
                request.getConversationId(),
                1L,
                request.getMessage()
        )).thenReturn(conversation);

        when(conversationService.saveUserMessage(10L, request.getMessage()))
                .thenReturn(message(99L, request.getMessage()));

        when(conversationService.getMessages(10L))
                .thenReturn(messages);

        when(promptService.buildPrompt(anyString(), anyList(), anyString()))
                .thenReturn("prompt");

        when(aiProvider.chat("prompt")).thenReturn("AI answer");

        when(conversationService.saveAssistantMessage(
                eq(10L),
                eq("AI answer"),
                eq("MockProvider"),
                eq("mock-model")
        )).thenReturn(message(100L, "AI answer"));
    }

    @Test
    void shouldPropagateProviderFailureWithoutPersistingAssistantMessage() {
        ChatRequest request = ChatRequest.builder()
                .message("Hello")
                .build();

        when(conversationService.findOrCreateConversation(null, 1L, "Hello"))
                .thenReturn(conversation);
        when(conversationService.saveUserMessage(10L, "Hello"))
                .thenReturn(message(1L, "Hello"));
        when(conversationService.getMessages(10L))
                .thenReturn(List.of(message(1L, "Hello")));
        when(promptService.buildPrompt(anyString(), anyList(), anyString()))
                .thenReturn("prompt");
        when(aiProvider.chat("prompt"))
                .thenThrow(new RuntimeException("Provider unavailable"));

        assertThatThrownBy(() -> aiService.chat(request, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Provider unavailable");

        verify(conversationService, never()).saveAssistantMessage(
                anyLong(), anyString(), anyString(), anyString()
        );
    }

    @Test
    void shouldRejectBlankProviderResponse() {
        ChatRequest request = ChatRequest.builder()
                .message("Hello")
                .build();

        when(conversationService.findOrCreateConversation(null, 1L, "Hello"))
                .thenReturn(conversation);
        when(conversationService.saveUserMessage(10L, "Hello"))
                .thenReturn(message(1L, "Hello"));
        when(conversationService.getMessages(10L))
                .thenReturn(List.of(message(1L, "Hello")));
        when(promptService.buildPrompt(anyString(), anyList(), anyString()))
                .thenReturn("prompt");
        when(aiProvider.chat("prompt")).thenReturn("   ");

        assertThatThrownBy(() -> aiService.chat(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("response");

        verify(conversationService, never()).saveAssistantMessage(
                anyLong(), anyString(), anyString(), anyString()
        );
    }

    @Test
    void shouldUseExistingConversationIdWhenProvided() {
        ChatRequest request = ChatRequest.builder()
                .conversationId(10L)
                .message("Follow up")
                .build();

        configureSuccessfulChat(request, List.of(message(1L, "Earlier")));

        ChatResponse result = aiService.chat(request, 1L);

        assertThat(result.getConversationId()).isEqualTo(10L);
        verify(conversationService).findOrCreateConversation(10L, 1L, "Follow up");
    }

    @Test
    void shouldHandleEmptyConversationHistoryOnFirstMessage() {
        ChatRequest request = ChatRequest.builder()
                .message("First message")
                .build();

        when(conversationService.findOrCreateConversation(null, 1L, "First message"))
                .thenReturn(conversation);
        when(conversationService.saveUserMessage(10L, "First message"))
                .thenReturn(message(1L, "First message"));
        when(conversationService.getMessages(10L)).thenReturn(List.of());
        when(promptService.buildPrompt(eq("First message"), eq(List.of()), eq("")))
                .thenReturn("prompt");
        when(aiProvider.chat("prompt")).thenReturn("Welcome");
        when(conversationService.saveAssistantMessage(
                10L, "Welcome", "MockProvider", "mock-model"
        )).thenReturn(message(2L, "Welcome"));

        ChatResponse result = aiService.chat(request, 1L);

        assertThat(result.getResponse()).isEqualTo("Welcome");
        verify(promptService).buildPrompt("First message", List.of(), "");
    }

    private ConversationMessage message(Long id, String content) {
        return ConversationMessage.builder()
                .id(id)
                .conversationId(10L)
                .role(ConversationMessage.MessageRole.USER)
                .content(content)
                .build();
    }
}
