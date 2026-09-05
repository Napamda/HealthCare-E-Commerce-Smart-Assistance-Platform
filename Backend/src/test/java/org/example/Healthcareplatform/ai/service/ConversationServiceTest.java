package org.example.Healthcareplatform.ai.service;

import org.example.Healthcareplatform.ai.entity.Conversation;
import org.example.Healthcareplatform.ai.entity.ConversationMessage;
import org.example.Healthcareplatform.ai.repository.ConversationMessageRepository;
import org.example.Healthcareplatform.ai.repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMessageRepository messageRepository;

    @InjectMocks
    private ConversationService conversationService;

    private Conversation conversation;

    @BeforeEach
    void setUp() {
        conversation = Conversation.builder()
                .id(10L)
                .userId(1L)
                .title("How can I improve my sleep?")
                .status(Conversation.ConversationStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void shouldCreateConversationWithDerivedTitle() {
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> {
                    Conversation value = invocation.getArgument(0);
                    value.setId(10L);
                    return value;
                });

        Conversation result = conversationService.createConversation(
                1L,
                "How can I improve my sleep?"
        );

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());

        Conversation saved = captor.getValue();

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getTitle()).isEqualTo("How can I improve my sleep?");
        assertThat(saved.getStatus()).isEqualTo(Conversation.ConversationStatus.ACTIVE);
    }

    @Test
    void shouldUseDefaultTitleForBlankMessage() {
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Conversation result = conversationService.createConversation(1L, "   ");

        assertThat(result.getTitle()).isEqualTo("New conversation");
    }

    @Test
    void shouldTruncateLongConversationTitle() {
        String message = "a".repeat(100);

        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Conversation result = conversationService.createConversation(1L, message);

        assertThat(result.getTitle()).hasSize(83);
        assertThat(result.getTitle()).endsWith("...");
    }

    @Test
    void shouldReturnExistingConversationForOwner() {
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        Conversation result = conversationService.findOrCreateConversation(
                10L,
                1L,
                "New message"
        );

        assertThat(result).isSameAs(conversation);
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void shouldRejectConversationOwnedByAnotherUser() {
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() ->
                conversationService.findOrCreateConversation(10L, 2L, "Hello")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Conversation does not belong to user");

        verify(conversationRepository, never()).save(any());
    }

    @Test
    void shouldRejectDeletedConversation() {
        conversation.setStatus(Conversation.ConversationStatus.DELETED);

        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() ->
                conversationService.findOrCreateConversation(10L, 1L, "Hello")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Conversation has been deleted");
    }

    @Test
    void shouldCreateNewConversationWhenConversationIdIsNull() {
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> {
                    Conversation value = invocation.getArgument(0);
                    value.setId(20L);
                    return value;
                });

        Conversation result = conversationService.findOrCreateConversation(
                null,
                1L,
                "First message"
        );

        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getUserId()).isEqualTo(1L);
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void shouldThrowWhenConversationDoesNotExist() {
        when(conversationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.findConversation(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Conversation not found: 99");
    }

    @Test
    void shouldListOnlyNonDeletedConversations() {
        List<Conversation> conversations = List.of(conversation);

        when(conversationRepository.findByUserIdAndStatusNotOrderByUpdatedAtDesc(
                1L,
                Conversation.ConversationStatus.DELETED
        )).thenReturn(conversations);

        List<Conversation> result = conversationService.listConversations(1L);

        assertThat(result).containsExactly(conversation);
    }

    @Test
    void shouldListPaginatedConversations() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Conversation> expected = new PageImpl<>(List.of(conversation), pageable, 1);

        when(conversationRepository.findByUserIdAndStatusNot(
                eq(1L),
                eq(Conversation.ConversationStatus.DELETED),
                eq(pageable)
        )).thenReturn(expected);

        Page<Conversation> result = conversationService.listConversations(1L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).containsExactly(conversation);
    }

    @Test
    void shouldSoftDeleteConversation() {
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        conversationService.deleteConversation(10L);

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());

        assertThat(captor.getValue().getStatus())
                .isEqualTo(Conversation.ConversationStatus.DELETED);
    }

    @Test
    void shouldSaveUserMessage() {
        when(messageRepository.save(any(ConversationMessage.class)))
                .thenAnswer(invocation -> {
                    ConversationMessage message = invocation.getArgument(0);
                    message.setId(100L);
                    return message;
                });

        ConversationMessage result =
                conversationService.saveUserMessage(10L, "Hello AI");

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getConversationId()).isEqualTo(10L);
        assertThat(result.getRole())
                .isEqualTo(ConversationMessage.MessageRole.USER);
        assertThat(result.getContent()).isEqualTo("Hello AI");
    }

    @Test
    void shouldSaveAssistantMessageWithProviderAndModel() {
        when(messageRepository.save(any(ConversationMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConversationMessage result = conversationService.saveAssistantMessage(
                10L,
                "AI response",
                "OpenRouter",
                "model-x"
        );

        assertThat(result.getConversationId()).isEqualTo(10L);
        assertThat(result.getRole())
                .isEqualTo(ConversationMessage.MessageRole.ASSISTANT);
        assertThat(result.getProvider()).isEqualTo("OpenRouter");
        assertThat(result.getModel()).isEqualTo("model-x");
    }

    @Test
    void shouldReturnMessagesInRepositoryOrder() {
        List<ConversationMessage> messages = List.of(
                ConversationMessage.builder()
                        .id(1L)
                        .conversationId(10L)
                        .role(ConversationMessage.MessageRole.USER)
                        .content("First")
                        .build(),
                ConversationMessage.builder()
                        .id(2L)
                        .conversationId(10L)
                        .role(ConversationMessage.MessageRole.ASSISTANT)
                        .content("Second")
                        .build()
        );

        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(10L))
                .thenReturn(messages);

        List<ConversationMessage> result =
                conversationService.getMessages(10L);

        assertThat(result).containsExactlyElementsOf(messages);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoConversations() {
        when(conversationRepository.findByUserIdAndStatusNotOrderByUpdatedAtDesc(
                1L,
                Conversation.ConversationStatus.DELETED
        )).thenReturn(List.of());

        List<Conversation> result = conversationService.listConversations(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyPaginatedResult() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Conversation> empty = new PageImpl<>(List.of(), pageable, 0);

        when(conversationRepository.findByUserIdAndStatusNot(
                eq(1L),
                eq(Conversation.ConversationStatus.DELETED),
                eq(pageable)
        )).thenReturn(empty);

        Page<Conversation> result = conversationService.listConversations(1L, pageable);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void shouldNotTruncateTitleAtExactMaxLength() {
        String message = "a".repeat(80);

        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Conversation result = conversationService.createConversation(1L, message);

        assertThat(result.getTitle()).hasSize(80);
        assertThat(result.getTitle()).doesNotEndWith("...");
    }

    @Test
    void shouldThrowWhenDeletingNonExistentConversation() {
        when(conversationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.deleteConversation(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    @Test
    void shouldReturnEmptyMessageListForConversationWithNoMessages() {
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of());

        List<ConversationMessage> result = conversationService.getMessages(10L);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldRejectNullMessageContentWhenSavingUserMessage() {
        assertThatThrownBy(() -> conversationService.saveUserMessage(10L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("message");

        verify(messageRepository, never()).save(any());
    }

    @Test
    void shouldRejectBlankMessageContentWhenSavingUserMessage() {
        assertThatThrownBy(() -> conversationService.saveUserMessage(10L, "   "))
                .isInstanceOf(IllegalArgumentException.class);

        verify(messageRepository, never()).save(any());
    }
}
