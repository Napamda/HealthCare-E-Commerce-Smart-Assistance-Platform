package org.example.Healthcareplatform.user.service;

import org.example.Healthcareplatform.user.dto.AddressRequest;
import org.example.Healthcareplatform.user.dto.AddressResponse;
import org.example.Healthcareplatform.user.dto.ProfileResponse;
import org.example.Healthcareplatform.user.dto.UpdateProfileRequest;
import org.example.Healthcareplatform.user.entity.Address;
import org.example.Healthcareplatform.user.entity.User;
import org.example.Healthcareplatform.user.entity.UserRole;
import org.example.Healthcareplatform.user.repository.AddressRepository;
import org.example.Healthcareplatform.user.repository.HealthProfileRepository;
import org.example.Healthcareplatform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HealthProfileRepository healthProfileRepository;

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    @TempDir
    Path tempDir;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userProfileService, "avatarStorageRoot", tempDir.toString());

        user = User.builder()
                .id(1L)
                .email("nana@patient.com")
                .password("secret")
                .firstName("Nana")
                .lastName("Kwadwo")
                .role(UserRole.PATIENT)
                .emailVerified(true)
                .build();
    }

    // ---- Task 2.1 — Profile ----

    @Test
    void shouldReturnMappedProfile() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ProfileResponse response = userProfileService.getProfile(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("nana@patient.com");
        assertThat(response.getFirstName()).isEqualTo("Nana");
        assertThat(response.getRole()).isEqualTo("PATIENT");
        assertThat(response.isEmailVerified()).isTrue();
    }

    @Test
    void shouldUpdateProfileFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("  John  ");
        request.setLastName("Doe");
        request.setPhone("+250 788 123 456");
        request.setDateOfBirth(LocalDate.of(2000, 5, 20));
        request.setGender("MALE");

        ProfileResponse response = userProfileService.updateProfile(1L, request);

        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.getPhone()).isEqualTo("+250 788 123 456");
        assertThat(response.getDateOfBirth()).isEqualTo(LocalDate.of(2000, 5, 20));
        assertThat(response.getGender()).isEqualTo("MALE");
    }

    @Test
    void shouldRejectBlankFirstName() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("   ");

        assertThatThrownBy(() -> userProfileService.updateProfile(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("First name");
    }

    @Test
    void shouldUploadAvatarAndSetUrl() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "file", "me.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});

        ProfileResponse response = userProfileService.uploadAvatar(1L, file);

        assertThat(response.getAvatarUrl()).startsWith("/api/avatars/");
        assertThat(response.getAvatarUrl()).endsWith(".png");
    }

    @Test
    void shouldRejectOversizedAvatar() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        byte[] big = new byte[2 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.png", "image/png", big);

        assertThatThrownBy(() -> userProfileService.uploadAvatar(1L, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2MB");
    }

    @Test
    void shouldRejectNonImageAvatar() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf",
                new byte[]{0x25, 0x50, 0x44, 0x46});

        assertThatThrownBy(() -> userProfileService.uploadAvatar(1L, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JPEG, PNG, WebP or GIF");
    }

    @Test
    void shouldRejectFakeImageContent() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Claims to be a PNG but the magic bytes are wrong.
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.png", "image/png",
                new byte[]{0x00, 0x01, 0x02, 0x03, 0x04});

        assertThatThrownBy(() -> userProfileService.uploadAvatar(1L, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid image");
    }

    // ---- Task 2.3 — Address Management ----

    private AddressRequest addressRequest() {
        AddressRequest request = new AddressRequest();
        request.setLabel("Home");
        request.setRecipientName("Nana Kwadwo");
        request.setPhone("+250788123456");
        request.setStreet("12 KG Ave");
        request.setCity("Kigali");
        return request;
    }

    @Test
    void firstAddressShouldBecomeDefaultAutomatically() {
        when(addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(1L))
                .thenReturn(List.of()); // no addresses yet
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        AddressResponse response = userProfileService.addAddress(1L, addressRequest());

        assertThat(response.isDefault()).isTrue();
        verify(addressRepository, never()).clearDefaultForUser(anyLong());
    }

    @Test
    void addingDefaultAddressShouldClearPreviousDefault() {
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        AddressRequest request = addressRequest();
        request.setIsDefault(true);

        userProfileService.addAddress(1L, request);

        verify(addressRepository).clearDefaultForUser(1L);
    }

    @Test
    void shouldNotTouchOtherUsersAddresses() {
        Address other = Address.builder()
                .id(5L)
                .userId(99L)
                .recipientName("Someone else")
                .phone("+250111222333")
                .street("X")
                .city("Y")
                .isDefault(true)
                .build();
        when(addressRepository.findById(5L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> userProfileService.updateAddress(1L, 5L, addressRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Address not found");

        assertThatThrownBy(() -> userProfileService.deleteAddress(1L, 5L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deletingDefaultAddressShouldPromoteAnother() {
        Address defaultAddr = Address.builder()
                .id(5L).userId(1L).recipientName("Nana").phone("+250788123456")
                .street("12 KG Ave").city("Kigali").isDefault(true).build();
        when(addressRepository.findById(5L)).thenReturn(Optional.of(defaultAddr));
        when(addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(1L))
                .thenReturn(List.of()); // after deletion nothing remains

        userProfileService.deleteAddress(1L, 5L);

        verify(addressRepository).delete(defaultAddr);
    }

    @Test
    void setDefaultShouldClearOthersThenSave() {
        Address addr = Address.builder()
                .id(7L).userId(1L).recipientName("Nana").phone("+250788123456")
                .street("12 KG Ave").city("Kigali").isDefault(false).build();
        when(addressRepository.findById(7L)).thenReturn(Optional.of(addr));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        AddressResponse response = userProfileService.setDefaultAddress(1L, 7L);

        assertThat(response.isDefault()).isTrue();
        verify(addressRepository).clearDefaultForUser(1L);
    }
}
