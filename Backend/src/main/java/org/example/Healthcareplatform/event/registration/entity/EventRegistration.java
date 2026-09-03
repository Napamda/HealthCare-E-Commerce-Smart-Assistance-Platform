package org.example.Healthcareplatform.event.registration.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.Healthcareplatform.event.entity.HealthEvent;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "event_registrations",
        uniqueConstraints = @UniqueConstraint(name = "uk_event_registration", columnNames = {"event_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private HealthEvent event;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RegistrationStatus status = RegistrationStatus.CONFIRMED;

    /**
     * When a doctor or pharmacist registers for an event, they are automatically
     * marked as a volunteer. This field stores their professional role (DOCTOR,
     * PHARMACIST) so the badge can be rendered in the UI. Null means a regular
     * attendee with no volunteer role.
     */
    @Column(name = "volunteer_role", length = 20)
    private String volunteerRole;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
