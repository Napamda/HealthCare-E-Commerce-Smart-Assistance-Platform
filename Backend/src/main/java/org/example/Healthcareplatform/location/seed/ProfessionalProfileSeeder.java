package org.example.Healthcareplatform.location.seed;

import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.location.entity.ProfessionalProfile;
import org.example.Healthcareplatform.location.repository.ProfessionalProfileRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class ProfessionalProfileSeeder implements CommandLineRunner {

    private final ProfessionalProfileRepository profileRepository;

    public ProfessionalProfileSeeder(ProfessionalProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public void run(String... args) {
        if (profileRepository.count() > 0) {
            return;
        }

        List<ProfessionalProfile> seeds = List.of(
                seed(101L, "Kwame", "Mensah", "Dr.", "CARDIOLOGY", "Cardiology",
                        "Consultant cardiologist with 15 years of experience in heart disease prevention and treatment.",
                        "+233 24 000 1001", "Ridge Hospital, 4th Floor", "Accra", 5.6037, -0.1870),
                seed(102L, "Ama", "Owusu", "Dr.", "DERMATOLOGY", "Dermatology",
                        "Specialist in clinical dermatology, skin allergies, and cosmetic procedures.",
                        "+233 24 000 1002", "Korle Bu Teaching Hospital", "Accra", 5.5333, -0.2233),
                seed(103L, "Kofi", "Boateng", "Dr.", "GENERAL_PRACTICE", "General Practice",
                        "Family physician offering comprehensive primary care for all ages.",
                        "+233 24 000 1003", "37 Military Hospital", "Accra", 5.6037, -0.1686),
                seed(104L, "Efua", "Asante", "Dr.", "PEDIATRICS", "Pediatrics",
                        "Pediatrician focused on child health, immunizations, and growth monitoring.",
                        "+233 24 000 1004", "Komfo Anokye Teaching Hospital", "Kumasi", 6.6932, -1.6308),
                seed(105L, "Yaw", "Appiah", "Dr.", "ORTHOPEDICS", "Orthopedic Surgery",
                        "Orthopedic surgeon specializing in sports injuries and joint replacement.",
                        "+233 24 000 1005", "Komfo Anokye Teaching Hospital", "Kumasi", 6.6932, -1.6308),
                seed(106L, "Abena", "Darko", "Dr.", "GYNECOLOGY", "Gynecology",
                        "Obstetrician and gynecologist providing maternal care and women's health services.",
                        "+233 24 000 1006", "Tema General Hospital", "Tema", 5.6698, -0.0166),
                seed(107L, "Kwabena", "Osei", "Dr.", "CARDIOLOGY", "Cardiology",
                        "Interventional cardiologist with expertise in hypertension and heart failure.",
                        "+233 24 000 1007", "Effia Nkwanta Regional Hospital", "Takoradi", 4.8845, -1.7554),
                seed(108L, "Akosua", "Frimpong", "Dr.", "NEUROLOGY", "Neurology",
                        "Neurologist treating stroke, epilepsy, and chronic headache disorders.",
                        "+233 24 000 1008", "Tamale Teaching Hospital", "Tamale", 9.4008, -0.8393)
        );

        profileRepository.saveAll(seeds);
        log.info("Seeded {} professional profiles", seeds.size());
    }

    private ProfessionalProfile seed(Long userId, String firstName, String lastName, String title,
                                     String specialty, String specialtyLabel, String bio,
                                     String phone, String address, String city,
                                     double latitude, double longitude) {
        return ProfessionalProfile.builder()
                .userId(userId)
                .firstName(firstName)
                .lastName(lastName)
                .title(title)
                .specialty(specialty)
                .bio(bio)
                .phone(phone)
                .address(address)
                .city(city)
                .latitude(latitude)
                .longitude(longitude)
                .active(true)
                .build();
    }
}
