package org.example.Healthcareplatform.ai;

import org.example.Healthcareplatform.ai.dto.ChatRequest;
import org.example.Healthcareplatform.ai.dto.ChatResponse;
import org.example.Healthcareplatform.ai.entity.Conversation;
import org.example.Healthcareplatform.ai.entity.ConversationMessage;
import org.example.Healthcareplatform.ai.provider.AIProvider;
import org.example.Healthcareplatform.ai.service.AIService;
import org.example.Healthcareplatform.ai.service.ConversationService;
import org.example.Healthcareplatform.ai.service.ConversationSummaryService;
import org.example.Healthcareplatform.ai.service.PromptService;
import org.example.Healthcareplatform.consultation.repository.ConsultationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void rejectsBlankMessage() {
        ChatRequest request = ChatRequest.builder().message("   ").build();

        assertThrows(IllegalArgumentException.class, () -> aiService.chat(request, 1L));
    }

    @Test
    void persistsProviderResponse() {
        ChatRequest request = ChatRequest.builder().message("Hello").build();
        Conversation conversation = new Conversation();
        conversation.setId(7L);

        when(conversationService.findOrCreateConversation(isNull(), eq(1L), eq("Hello")))
                .thenReturn(conversation);
        when(consultationRepository.findFirstByConversationIdAndStatusIn(eq(7L), anyList()))
                .thenReturn(Optional.empty());
        when(conversationService.getMessages(7L)).thenReturn(List.of());
        when(promptService.buildPrompt(anyString(), anyList(), anyString())).thenReturn("prompt");
        when(aiProvider.chat("prompt")).thenReturn("AI answer");
        when(aiProvider.providerName()).thenReturn("Mock");
        when(aiProvider.modelName()).thenReturn("mock/v1");

        ConversationMessage savedMessage = new ConversationMessage();
        savedMessage.setId(11L);
        when(conversationService.saveAssistantMessage(7L, "AI answer", "Mock", "mock/v1"))
                .thenReturn(savedMessage);

        ChatResponse response = aiService.chat(request, 1L);

        assertEquals("AI answer", response.getResponse());
        assertEquals("Mock", response.getProvider());
        verify(conversationService).saveUserMessage(7L, "Hello");
        verify(conversationService).saveAssistantMessage(7L, "AI answer", "Mock", "mock/v1");
    }
}
