package org.example.Healthcareplatform.event.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.event.entity.EventStatus;
import org.example.Healthcareplatform.event.entity.HealthEvent;
import org.example.Healthcareplatform.event.repository.HealthEventRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class HealthEventSeeder implements CommandLineRunner {

    private final HealthEventRepository eventRepository;

    @Override
    public void run(String... args) {
        if (eventRepository.count() > 0) {
            return;
        }
        log.info("Seeding health events...");
        eventRepository.saveAll(List.of(
                event("Community Hypertension Screening",
                        "Free blood pressure checks and hypertension counseling for all ages. Walk-ins welcome.",
                        "Screening", new String[]{"screening", "blood-pressure", "free"},
                        LocalDateTime.of(2026, 9, 5, 9, 0), LocalDateTime.of(2026, 9, 5, 15, 0),
                        "Korle Bu Teaching Hospital", "Accra", 150, "Ghana Health Service"),
                event("Diabetes Awareness Talk",
                        "Interactive session on managing diabetes through diet, exercise and medication adherence.",
                        "Health Talk", new String[]{"diabetes", "awareness", "nutrition"},
                        LocalDateTime.of(2026, 9, 12, 10, 0), LocalDateTime.of(2026, 9, 12, 12, 0),
                        "Ridge Hospital Conference Hall", "Accra", 80, "Endocrinology Society of Ghana"),
                event("Free Vaccination Drive",
                        "Community immunization against measles, polio and yellow fever. Bring your child's health card.",
                        "Vaccination", new String[]{"vaccination", "immunization", "children"},
                        LocalDateTime.of(2026, 9, 19, 8, 0), LocalDateTime.of(2026, 9, 19, 14, 0),
                        "Komfo Anokye Teaching Hospital", "Kumasi", 200, "Ministry of Health"),
                event("Mental Wellness Workshop",
                        "Guided mindfulness and stress-management workshop with licensed therapists.",
                        "Wellness", new String[]{"mental-health", "workshop", "stress"},
                        LocalDateTime.of(2026, 9, 26, 9, 30), LocalDateTime.of(2026, 9, 26, 13, 0),
                        "Accra International Conference Centre", "Accra", 100, "MindCare Ghana"),
                event("Maternal Health Clinic",
                        "Prenatal checkups, nutrition guidance and childbirth preparation classes.",
                        "Clinic", new String[]{"maternal", "prenatal", "women"},
                        LocalDateTime.of(2026, 10, 3, 8, 30), LocalDateTime.of(2026, 10, 3, 15, 0),
                        "Tema General Hospital", "Tema", 120, "Tema General Hospital"),
                event("Nutrition and Fitness Bootcamp",
                        "Half-day bootcamp covering meal planning, healthy cooking demos and beginner workouts.",
                        "Wellness", new String[]{"nutrition", "fitness", "bootcamp"},
                        LocalDateTime.of(2026, 10, 10, 7, 0), LocalDateTime.of(2026, 10, 10, 12, 0),
                        "Takoradi Sports Stadium", "Takoradi", 90, "FitGhana Initiative")
        ));
        log.info("Seeded {} health events", eventRepository.count());
    }

    private HealthEvent event(String title, String description, String category,
                              String[] tags, LocalDateTime start, LocalDateTime end,
                              String venue, String city, int capacity, String organizer) {
        return HealthEvent.builder()
                .title(title)
                .description(description)
                .category(category)
                .tags(new LinkedHashSet<>(List.of(tags)))
                .startDateTime(start)
                .endDateTime(end)
                .venue(venue)
                .city(city)
                .capacity(capacity)
                .organizer(organizer)
                .status(EventStatus.PUBLISHED)
                .createdBy(1L)
                .build();
    }
}
