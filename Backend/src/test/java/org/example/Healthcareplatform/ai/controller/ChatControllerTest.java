package org.example.Healthcareplatform.ai.controller;

import org.example.Healthcareplatform.ai.dto.ChatRequest;
import org.example.Healthcareplatform.ai.dto.ChatResponse;
import org.example.Healthcareplatform.ai.entity.Conversation;
import org.example.Healthcareplatform.ai.entity.ConversationMessage;
import org.example.Healthcareplatform.ai.service.AIService;
import org.example.Healthcareplatform.ai.service.ConversationService;
import org.example.Healthcareplatform.auth.util.SecurityContextUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private AIService aiService;

    @Mock
    private ConversationService conversationService;

    @Mock
    private SecurityContextUtil securityContextUtil;

    @InjectMocks
    private ChatController chatController;

    private Conversation conversation;

    @BeforeEach
    void setUp() {
        conversation = Conversation.builder()
                .id(10L)
                .userId(1L)
                .title("Test conversation")
                .status(Conversation.ConversationStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void shouldReturnBadRequestForBlankChatMessage() {
        ChatRequest request = ChatRequest.builder()
                .message("   ")
                .build();

        when(securityContextUtil.getCurrentUserId()).thenReturn(1L);

        ResponseEntity<ChatResponse> response = chatController.chat(request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verify(aiService, never()).chat(any(), anyLong());
    }

    @Test
    void shouldSendChatRequestUsingAuthenticatedUserId() {
        ChatRequest request = ChatRequest.builder()
                .conversationId(10L)
                .message("Hello")
                .build();

        ChatResponse expected = ChatResponse.builder()
                .conversationId(10L)
                .messageId(2L)
                .response("AI response")
                .build();

        when(securityContextUtil.getCurrentUserId()).thenReturn(1L);
        when(aiService.chat(request, 1L)).thenReturn(expected);

        ResponseEntity<ChatResponse> response = chatController.chat(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(expected);
        verify(aiService).chat(request, 1L);
    }

    @Test
    void shouldReturnOnlyCurrentUsersConversations() {
        when(securityContextUtil.getCurrentUserId()).thenReturn(1L);
        when(conversationService.listConversations(1L))
                .thenReturn(List.of(conversation));
        when(conversationService.getMessages(10L))
                .thenReturn(List.of(
                        ConversationMessage.builder()
                                .id(1L)
                                .conversationId(10L)
                                .role(ConversationMessage.MessageRole.USER)
                                .content("Hello")
                                .build()
                ));

        ResponseEntity<?> response =
                chatController.listConversations(null, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isInstanceOf(List.class);

        List<?> body = (List<?>) response.getBody();
        assertThat(body).hasSize(1);

        verify(conversationService).listConversations(1L);
        verify(conversationService, never()).listConversations(eq(2L));
    }

    @Test
    void shouldReturnPaginatedConversationResponse() {
        Page<Conversation> page = new PageImpl<>(List.of(conversation));

        when(securityContextUtil.getCurrentUserId()).thenReturn(1L);
        when(conversationService.listConversations(eq(1L), any()))
                .thenReturn(page);
        when(conversationService.getMessages(10L))
                .thenReturn(List.of());

        ResponseEntity<?> response =
                chatController.listConversations(0, 10);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isInstanceOf(Map.class);

        Map<?, ?> body = (Map<?, ?>) response.getBody();

        assertThat(body.get("page")).isEqualTo(0);
        assertThat(body.get("size")).isEqualTo(1);
        assertThat(body.get("totalElements")).isEqualTo(1L);
    }

    @Test
    void shouldReturnForbiddenForConversationOwnedByAnotherUser() {
        Conversation otherUsersConversation = Conversation.builder()
                .id(20L)
                .userId(2L)
                .title("Private")
                .status(Conversation.ConversationStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(securityContextUtil.getCurrentUserId()).thenReturn(1L);
        when(conversationService.findConversation(20L))
                .thenReturn(otherUsersConversation);

        ResponseEntity<?> response =
                chatController.getConversation(20L);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        verify(conversationService, never()).getMessages(20L);
    }

    @Test
    void shouldReturnConversationForOwner() {
        when(securityContextUtil.getCurrentUserId()).thenReturn(1L);
        when(conversationService.findConversation(10L))
                .thenReturn(conversation);
        when(conversationService.getMessages(10L))
                .thenReturn(List.of());

        ResponseEntity<?> response =
                chatController.getConversation(10L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(conversationService).getMessages(10L);
    }

    @Test
    void shouldReturnNotFoundWhenConversationDoesNotExist() {
        when(securityContextUtil.getCurrentUserId()).thenReturn(1L);
        when(conversationService.findConversation(999L))
                .thenThrow(new IllegalArgumentException("Conversation not found"));

        ResponseEntity<?> response =
                chatController.getConversation(999L);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void shouldNotDeleteConversationOwnedByAnotherUser() {
        Conversation otherUsersConversation = Conversation.builder()
                .id(20L)
                .userId(2L)
                .status(Conversation.ConversationStatus.ACTIVE)
                .build();

        when(securityContextUtil.getCurrentUserId()).thenReturn(1L);
        when(conversationService.findConversation(20L))
                .thenReturn(otherUsersConversation);

        ResponseEntity<Void> response =
                chatController.deleteConversation(20L);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        verify(conversationService, never()).deleteConversation(anyLong());
    }

    @Test
    void shouldDeleteConversationForOwner() {
        when(securityContextUtil.getCurrentUserId()).thenReturn(1L);
        when(conversationService.findConversation(10L))
                .thenReturn(conversation);

        ResponseEntity<Void> response =
                chatController.deleteConversation(10L);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(conversationService).deleteConversation(10L);
    }

    @Test
    void shouldReturnBadRequestForNullMessage() {
        ChatRequest request = ChatRequest.builder()
                .message(null)
                .build();

        when(securityContextUtil.getCurrentUserId()).thenReturn(1L);

        ResponseEntity<ChatResponse> response = chatController.chat(request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verify(aiService, never()).chat(any(), anyLong());
    }

    @Test
    void shouldProcessChatForNewConversationWithoutId() {
        ChatRequest request = ChatRequest.builder()
                .message("Start new thread")
                .build();

        ChatResponse expected = ChatResponse.builder()
                .conversationId(30L)
                .messageId(1L)
                .response("Welcome")
                .build();

        when(securityContextUtil.getCurrentUserId()).thenReturn(1L);
        when(aiService.chat(request, 1L)).thenReturn(expected);

        ResponseEntity<ChatResponse> response = chatController.chat(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getConversationId()).isEqualTo(30L);
    }

    @Test
    void shouldProcessChatForExistingConversationId() {
        ChatRequest request = ChatRequest.builder()
                .conversationId(10L)
                .message("Continue thread")
                .build();

        when(securityContextUtil.getCurrentUserId()).thenReturn(1L);
        when(aiService.chat(request, 1L)).thenReturn(
                ChatResponse.builder()
                        .conversationId(10L)
                        .messageId(5L)
                        .response("Continued")
                        .build()
        );

        ResponseEntity<ChatResponse> response = chatController.chat(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(aiService).chat(request, 1L);
    }

    @Test
    void shouldReturnEmptyConversationListWhenUserHasNoHistory() {
        when(securityContextUtil.getCurrentUserId()).thenReturn(1L);
        when(conversationService.listConversations(1L)).thenReturn(List.of());

        ResponseEntity<?> response =
                chatController.listConversations(null, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat((List<?>) response.getBody()).isEmpty();
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentConversation() {
        when(securityContextUtil.getCurrentUserId()).thenReturn(1L);
        when(conversationService.findConversation(999L))
                .thenThrow(new IllegalArgumentException("Conversation not found: 999"));

        ResponseEntity<Void> response =
                chatController.deleteConversation(999L);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        verify(conversationService, never()).deleteConversation(anyLong());
    }
}
