package org.example.Healthcareplatform.location.service;

import org.example.Healthcareplatform.location.dto.NearbyProfessionalResponse;
import org.example.Healthcareplatform.location.dto.NearbyProfessionalRow;
import org.example.Healthcareplatform.location.dto.ProfessionalProfileRequest;
import org.example.Healthcareplatform.location.dto.ProfessionalProfileResponse;
import org.example.Healthcareplatform.location.entity.ProfessionalProfile;
import org.example.Healthcareplatform.location.mapper.ProfessionalProfileGeoMapper;
import org.example.Healthcareplatform.location.repository.ProfessionalProfileRepository;
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
class ProfessionalProfileServiceTest {

    @Mock
    private ProfessionalProfileRepository profileRepository;

    @Mock
    private ProfessionalProfileGeoMapper professionalProfileGeoMapper;

    @InjectMocks
    private ProfessionalProfileService professionalProfileService;

    private ProfessionalProfile profile;

    @BeforeEach
    void setUp() {
        profile = ProfessionalProfile.builder()
                .id(1L)
                .userId(100L)
                .firstName("Ama")
                .lastName("Mensah")
                .title("Dr")
                .specialty("Cardiology")
                .city("Accra")
                .latitude(5.6037)
                .longitude(-0.1870)
                .active(true)
                .build();
    }

    @Test
    void shouldListActiveProfilesWithoutSpecialtyFilter() {
        when(profileRepository.findByActiveTrueOrderByLastNameAsc())
                .thenReturn(List.of(profile));

        List<ProfessionalProfileResponse> result =
                professionalProfileService.listProfiles(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFullName())
                .isEqualTo("Ama Mensah");
    }

    @Test
    void shouldListProfilesBySpecialty() {
        when(profileRepository
                .findBySpecialtyContainingIgnoreCaseAndActiveTrueOrderByLastNameAsc(
                        "cardio"
                ))
                .thenReturn(List.of(profile));

        List<ProfessionalProfileResponse> result =
                professionalProfileService.listProfiles("cardio");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSpecialty())
                .isEqualTo("Cardiology");
    }

    @Test
    void shouldRejectDuplicateProfileForSameUser() {
        ProfessionalProfileRequest request = request(100L);

        when(profileRepository.findByUserId(100L))
                .thenReturn(Optional.of(profile));

        assertThatThrownBy(() ->
                professionalProfileService.createProfile(request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(profileRepository, never()).save(any());
    }

    @Test
    void shouldCreateProfessionalProfile() {
        ProfessionalProfileRequest request = request(200L);

        when(profileRepository.findByUserId(200L))
                .thenReturn(Optional.empty());

        when(profileRepository.save(any(ProfessionalProfile.class)))
                .thenAnswer(invocation -> {
                    ProfessionalProfile value = invocation.getArgument(0);
                    value.setId(2L);
                    return value;
                });

        ProfessionalProfileResponse result =
                professionalProfileService.createProfile(request);

        ArgumentCaptor<ProfessionalProfile> captor =
                ArgumentCaptor.forClass(ProfessionalProfile.class);

        verify(profileRepository).save(captor.capture());

        assertThat(result.getId()).isEqualTo(2L);
        assertThat(captor.getValue().getUserId()).isEqualTo(200L);
        assertThat(captor.getValue().getSpecialty())
                .isEqualTo("Cardiology");
    }

    @Test
    void shouldReturnNearbyProfessionalsSortedByDistance() {
        NearbyProfessionalRow near = row(
                1L, 1L, "Near", "Doctor", "General Practice", 5.6037, -0.1870, 0.5);
        NearbyProfessionalRow farther = row(
                2L, 2L, "Farther", "Doctor", "General Practice", 5.6140, -0.2000, 5.2);

        when(professionalProfileGeoMapper.findNearby(5.6037, -0.1870, 10.0, null))
                .thenReturn(List.of(near, farther));

        List<NearbyProfessionalResponse> result =
                professionalProfileService.searchNearby(5.6037, -0.1870, 10.0, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProfile().getId()).isEqualTo(1L);
        assertThat(result.get(0).getDistanceKm()).isEqualTo(0.5);
        assertThat(result.get(1).getProfile().getId()).isEqualTo(2L);
        assertThat(result.get(1).getProfile().getFullName()).isEqualTo("Farther Doctor");
        verify(professionalProfileGeoMapper).findNearby(5.6037, -0.1870, 10.0, null);
    }

    @Test
    void shouldSearchNearbyUsingSpecialtyFilter() {
        NearbyProfessionalRow card = row(
                1L, 100L, "Ama", "Mensah", "Cardiology", 5.6037, -0.1870, 0.2);

        when(professionalProfileGeoMapper.findNearby(5.6037, -0.1870, 10.0, "Cardio"))
                .thenReturn(List.of(card));

        List<NearbyProfessionalResponse> result =
                professionalProfileService.searchNearby(5.6037, -0.1870, 10.0, "Cardio");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProfile().getSpecialty()).isEqualTo("Cardiology");
        assertThat(result.get(0).getDistanceKm()).isEqualTo(0.2);
        verify(professionalProfileGeoMapper).findNearby(5.6037, -0.1870, 10.0, "Cardio");
    }

    @Test
    void shouldPassNullSpecialtyWhenBlank() {
        when(professionalProfileGeoMapper.findNearby(5.6037, -0.1870, 10.0, null))
                .thenReturn(List.of());

        List<NearbyProfessionalResponse> result =
                professionalProfileService.searchNearby(5.6037, -0.1870, 10.0, "   ");

        assertThat(result).isEmpty();
        verify(professionalProfileGeoMapper).findNearby(5.6037, -0.1870, 10.0, null);
    }

    @Test
    void shouldUpdateProfile() {
        ProfessionalProfileRequest request = request(100L);
        request.setFirstName("Akosua");

        when(profileRepository.findById(1L))
                .thenReturn(Optional.of(profile));

        when(profileRepository.save(any(ProfessionalProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProfessionalProfileResponse result =
                professionalProfileService.updateProfile(1L, request);

        assertThat(result.getFirstName()).isEqualTo("Akosua");
        verify(profileRepository).save(profile);
    }

    @Test
    void shouldDeleteExistingProfile() {
        when(profileRepository.findById(1L))
                .thenReturn(Optional.of(profile));

        professionalProfileService.deleteProfile(1L);

        verify(profileRepository).delete(profile);
    }

    @Test
    void shouldGetProfileById() {
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));

        ProfessionalProfileResponse result =
                professionalProfileService.getProfile(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFullName()).isEqualTo("Ama Mensah");
    }

    @Test
    void shouldThrowWhenProfileNotFound() {
        when(profileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professionalProfileService.getProfile(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    @Test
    void shouldReturnEmptyListWhenNoProfilesInRadius() {
        when(professionalProfileGeoMapper.findNearby(5.6037, -0.1870, 1.0, null))
                .thenReturn(List.of());

        List<NearbyProfessionalResponse> result =
                professionalProfileService.searchNearby(
                        5.6037, -0.1870, 1.0, null
                );

        assertThat(result).isEmpty();
        verify(professionalProfileGeoMapper).findNearby(5.6037, -0.1870, 1.0, null);
    }

    @Test
    void shouldRejectInvalidLatitude() {
        assertThatThrownBy(() ->
                professionalProfileService.searchNearby(
                        91.0, -0.1870, 10.0, null
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("latitude");
    }

    @Test
    void shouldRejectInvalidLongitude() {
        assertThatThrownBy(() ->
                professionalProfileService.searchNearby(
                        5.6037, 181.0, 10.0, null
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("longitude");
    }

    @Test
    void shouldReturnEmptyListWhenNoActiveProfiles() {
        when(profileRepository.findByActiveTrueOrderByLastNameAsc())
                .thenReturn(List.of());

        List<ProfessionalProfileResponse> result =
                professionalProfileService.listProfiles(null);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowWhenUpdatingMissingProfile() {
        when(profileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                professionalProfileService.updateProfile(99L, request(100L))
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    @Test
    void shouldThrowWhenDeletingMissingProfile() {
        when(profileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                professionalProfileService.deleteProfile(99L)
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    @Test
    void shouldRejectNegativeSearchRadius() {
        assertThatThrownBy(() ->
                professionalProfileService.searchNearby(
                        5.6037, -0.1870, -5.0, null
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("radius");
    }

    private ProfessionalProfileRequest request(Long userId) {
        return ProfessionalProfileRequest.builder()
                .userId(userId)
                .firstName("Ama")
                .lastName("Mensah")
                .title("Dr")
                .specialty("Cardiology")
                .city("Accra")
                .latitude(5.6037)
                .longitude(-0.1870)
                .active(true)
                .build();
    }

    private NearbyProfessionalRow row(Long id, Long userId, String firstName, String lastName,
                                      String specialty, double latitude, double longitude,
                                      double distanceKm) {
        return NearbyProfessionalRow.builder()
                .id(id)
                .userId(userId)
                .firstName(firstName)
                .lastName(lastName)
                .title("Dr")
                .specialty(specialty)
                .city("Accra")
                .latitude(latitude)
                .longitude(longitude)
                .active(true)
                .createdAt(LocalDateTime.of(2026, 1, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 1, 9, 0))
                .distanceKm(distanceKm)
                .build();
    }
}
