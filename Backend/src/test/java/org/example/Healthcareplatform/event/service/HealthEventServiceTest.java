package org.example.Healthcareplatform.event.service;

import org.example.Healthcareplatform.auth.util.SecurityContextUtil;
import org.example.Healthcareplatform.event.dto.HealthEventRequest;
import org.example.Healthcareplatform.event.dto.HealthEventResponse;
import org.example.Healthcareplatform.event.entity.EventStatus;
import org.example.Healthcareplatform.event.entity.HealthEvent;
import org.example.Healthcareplatform.event.repository.HealthEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthEventServiceTest {

    @Mock
    private HealthEventRepository eventRepository;

    @Mock
    private SecurityContextUtil securityContextUtil;

    @InjectMocks
    private HealthEventService healthEventService;

    private HealthEventRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = HealthEventRequest.builder()
                .title(" Health Screening ")
                .description("Free screening")
                .category(" Wellness ")
                .tags(List.of("Blood Pressure", "Wellness", "Blood Pressure", " "))
                .startDateTime(LocalDateTime.of(2026, 9, 1, 10, 0))
                .endDateTime(LocalDateTime.of(2026, 9, 1, 12, 0))
                .venue("Community Hall")
                .city("Accra")
                .capacity(100)
                .build();
    }

    @Test
    void shouldCreateEventWithAuthenticatedUserAndDefaultPublishedStatus() {
        when(securityContextUtil.getCurrentUserId()).thenReturn(5L);

        when(eventRepository.save(any(HealthEvent.class)))
                .thenAnswer(invocation -> {
                    HealthEvent event = invocation.getArgument(0);
                    event.setId(10L);
                    return event;
                });

        HealthEventResponse result =
                healthEventService.createEvent(validRequest);

        ArgumentCaptor<HealthEvent> captor =
                ArgumentCaptor.forClass(HealthEvent.class);

        verify(eventRepository).save(captor.capture());

        HealthEvent saved = captor.getValue();

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(saved.getTitle()).isEqualTo("Health Screening");
        assertThat(saved.getCategory()).isEqualTo("Wellness");
        assertThat(saved.getCreatedBy()).isEqualTo(5L);
        assertThat(saved.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(saved.getTags())
                .containsExactly("Blood Pressure", "Wellness");
    }

    @Test
    void shouldUseRequestedStatusWhenProvided() {
        validRequest.setStatus(EventStatus.DRAFT);

        when(securityContextUtil.getCurrentUserId()).thenReturn(5L);
        when(eventRepository.save(any(HealthEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        HealthEventResponse result =
                healthEventService.createEvent(validRequest);

        assertThat(result.getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void shouldRejectMissingTitle() {
        validRequest.setTitle(" ");

        assertThatThrownBy(() ->
                healthEventService.createEvent(validRequest)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Event title is required");

        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldRejectMissingCategory() {
        validRequest.setCategory(null);

        assertThatThrownBy(() ->
                healthEventService.createEvent(validRequest)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Event category is required");
    }

    @Test
    void shouldRejectMissingStartDate() {
        validRequest.setStartDateTime(null);

        assertThatThrownBy(() ->
                healthEventService.createEvent(validRequest)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Event start date is required");
    }

    @Test
    void shouldRejectEndDateBeforeStartDate() {
        validRequest.setEndDateTime(
                validRequest.getStartDateTime().minusHours(1)
        );

        assertThatThrownBy(() ->
                healthEventService.createEvent(validRequest)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("End date must be after start date");
    }

    @Test
    void shouldListEventsByStatusAndCategory() {
        HealthEvent event = event(
                1L,
                "Screening",
                "Wellness",
                EventStatus.PUBLISHED
        );

        when(eventRepository
                .findByStatusAndCategoryIgnoreCaseOrderByStartDateTimeAsc(
                        EventStatus.PUBLISHED,
                        "Wellness"
                ))
                .thenReturn(List.of(event));

        List<HealthEventResponse> result =
                healthEventService.listEvents(
                        "Wellness",
                        "published"
                );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void shouldListEventsByStatusOnly() {
        HealthEvent event = event(
                1L,
                "Screening",
                "Wellness",
                EventStatus.PUBLISHED
        );

        when(eventRepository
                .findByStatusOrderByStartDateTimeAsc(EventStatus.PUBLISHED))
                .thenReturn(List.of(event));

        List<HealthEventResponse> result =
                healthEventService.listEvents(null, "PUBLISHED");

        assertThat(result).hasSize(1);
        verify(eventRepository)
                .findByStatusOrderByStartDateTimeAsc(EventStatus.PUBLISHED);
    }

    @Test
    void shouldListEventsByCategoryOnly() {
        HealthEvent event = event(
                1L,
                "Screening",
                "Wellness",
                EventStatus.PUBLISHED
        );

        when(eventRepository
                .findByCategoryIgnoreCaseOrderByStartDateTimeAsc("Wellness"))
                .thenReturn(List.of(event));

        List<HealthEventResponse> result =
                healthEventService.listEvents("Wellness", null);

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldRejectInvalidEventStatus() {
        assertThatThrownBy(() ->
                healthEventService.listEvents(null, "NOT_A_STATUS")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid event status: NOT_A_STATUS");
    }

    @Test
    void shouldUpdateEvent() {
        HealthEvent existing = event(
                1L,
                "Old title",
                "Old category",
                EventStatus.PUBLISHED
        );

        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(eventRepository.save(existing))
                .thenReturn(existing);

        HealthEventResponse result =
                healthEventService.updateEvent(1L, validRequest);

        assertThat(result.getTitle()).isEqualTo("Health Screening");
        assertThat(result.getCategory()).isEqualTo("Wellness");
        assertThat(existing.getTags())
                .containsExactly("Blood Pressure", "Wellness");
    }

    @Test
    void shouldNotChangeStatusOnUpdateWhenRequestStatusIsNull() {
        HealthEvent existing = event(
                1L,
                "Old title",
                "Old category",
                EventStatus.DRAFT
        );

        validRequest.setStatus(null);

        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(eventRepository.save(existing))
                .thenReturn(existing);

        HealthEventResponse result =
                healthEventService.updateEvent(1L, validRequest);

        assertThat(result.getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void shouldDeleteExistingEvent() {
        HealthEvent event = event(
                1L,
                "Screening",
                "Wellness",
                EventStatus.PUBLISHED
        );

        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));

        healthEventService.deleteEvent(1L);

        verify(eventRepository).delete(event);
    }

    @Test
    void shouldThrowWhenDeletingUnknownEvent() {
        when(eventRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                healthEventService.deleteEvent(999L)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Event not found with id: 999");
    }

    @Test
    void shouldReturnDistinctCategories() {
        when(eventRepository.findDistinctCategories())
                .thenReturn(List.of("Screening", "Wellness"));

        List<String> result =
                healthEventService.listCategories();

        assertThat(result)
                .containsExactly("Screening", "Wellness");
    }

    private HealthEvent event(
            Long id,
            String title,
            String category,
            EventStatus status
    ) {
        return HealthEvent.builder()
                .id(id)
                .title(title)
                .category(category)
                .tags(new java.util.LinkedHashSet<>())
                .startDateTime(LocalDateTime.of(2026, 9, 1, 10, 0))
                .status(status)
                .createdBy(5L)
                .build();
    }
}
