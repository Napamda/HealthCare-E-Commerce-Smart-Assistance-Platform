package org.example.Healthcareplatform.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Healthcareplatform.user.dto.*;
import org.example.Healthcareplatform.user.entity.Address;
import org.example.Healthcareplatform.user.entity.EmergencyContact;
import org.example.Healthcareplatform.user.entity.HealthProfile;
import org.example.Healthcareplatform.user.entity.User;
import org.example.Healthcareplatform.user.repository.AddressRepository;
import org.example.Healthcareplatform.user.repository.HealthProfileRepository;
import org.example.Healthcareplatform.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * User management module (Member 2, Tasks 2.1-2.3):
 * profile view/edit, avatar upload, health profile and address book.
 * Every operation is scoped to the authenticated user — the client never
 * supplies a userId.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserRepository userRepository;
    private final HealthProfileRepository healthProfileRepository;
    private final AddressRepository addressRepository;

    @Value("${user.avatar.storage-root:${user.home}/Desktop/HealthCare/healthcare-uploads/avatars}")
    private String avatarStorageRoot;

    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024; // 2MB
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] GIF_MAGIC = new byte[]{0x47, 0x49, 0x46};
    private static final byte[] WEBP_MAGIC = new byte[]{0x52, 0x49, 0x46, 0x46};

    // =====================================================================
    // Task 2.1 — Profile
    // =====================================================================

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        User user = findUser(userId);
        return toProfileResponse(user);
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);

        if (request.getFirstName() != null) {
            String name = request.getFirstName().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("First name must not be blank");
            }
            user.setFirstName(name);
        }
        if (request.getLastName() != null) {
            String name = request.getLastName().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Last name must not be blank");
            }
            user.setLastName(name);
        }
        user.setPhone(request.getPhone());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());

        user = userRepository.save(user);
        log.info("Profile updated — userId={}", userId);
        return toProfileResponse(user);
    }

    /**
     * Validates and stores an avatar image, then points the user's
     * avatarUrl at the public serving endpoint.
     */
    @Transactional
    public ProfileResponse uploadAvatar(Long userId, MultipartFile file) {
        User user = findUser(userId);
        validateAvatar(file);

        String originalName = file.getOriginalFilename() == null ? "avatar" : file.getOriginalFilename();
        String extension = extensionOf(originalName);
        String storedName = UUID.randomUUID() + "." + extension;

        Path dir = Paths.get(avatarStorageRoot);
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(storedName);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("Failed to store avatar for userId={}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to store avatar image", e);
        }

        // Clean up the previous avatar file if it exists.
        deleteAvatarFile(user.getAvatarUrl());

        user.setAvatarUrl("/api/avatars/" + storedName);
        user = userRepository.save(user);
        log.info("Avatar uploaded — userId={}, stored={}", userId, storedName);
        return toProfileResponse(user);
    }

    // =====================================================================
    // Task 2.2 — Health Profile
    // =====================================================================

    @Transactional(readOnly = true)
    public HealthProfileResponse getHealthProfile(Long userId) {
        return healthProfileRepository.findByUserId(userId)
                .map(this::toHealthResponse)
                .orElseGet(this::defaultHealthResponse);
    }

    @Transactional
    public HealthProfileResponse updateHealthProfile(Long userId, UpdateHealthProfileRequest request) {
        HealthProfile profile = healthProfileRepository.findByUserId(userId)
                .orElseGet(() -> HealthProfile.builder().userId(userId).build());

        if (request.getAllergies() != null) {
            profile.setAllergies(new ArrayList<>(trimList(request.getAllergies(), "Allergy")));
        }
        if (request.getChronicConditions() != null) {
            profile.setChronicConditions(new ArrayList<>(trimList(request.getChronicConditions(), "Condition")));
        }
        if (request.getEmergencyContacts() != null) {
            List<EmergencyContact> contacts = new ArrayList<>();
            for (EmergencyContactDTO dto : request.getEmergencyContacts()) {
                if (dto == null) continue;
                String name = dto.getName() == null ? "" : dto.getName().trim();
                if (name.isEmpty()) continue; // skip blank rows
                contacts.add(EmergencyContact.builder()
                        .name(name)
                        .phone(dto.getPhone())
                        .relationship(dto.getRelationship())
                        .build());
            }
            profile.setEmergencyContacts(contacts);
        }
        if (request.getDataProcessingConsent() != null) profile.setDataProcessingConsent(request.getDataProcessingConsent());
        if (request.getEmailNotifications() != null) profile.setEmailNotifications(request.getEmailNotifications());
        if (request.getProfileVisible() != null) profile.setProfileVisible(request.getProfileVisible());
        if (request.getShareHealthDataWithDoctors() != null) {
            profile.setShareHealthDataWithDoctors(request.getShareHealthDataWithDoctors());
        }

        profile = healthProfileRepository.save(profile);
        log.info("Health profile saved — userId={}, allergies={}, conditions={}",
                userId, profile.getAllergies().size(), profile.getChronicConditions().size());
        return toHealthResponse(profile);
    }

    // =====================================================================
    // Task 2.3 — Address Management
    // =====================================================================

    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream().map(this::toAddressResponse).toList();
    }

    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest request) {
        Address address = Address.builder()
                .userId(userId)
                .label(trimOrNull(request.getLabel()))
                .recipientName(request.getRecipientName().trim())
                .phone(request.getPhone().trim())
                .street(request.getStreet().trim())
                .city(request.getCity().trim())
                .state(trimOrNull(request.getState()))
                .postalCode(trimOrNull(request.getPostalCode()))
                .country(trimOrNull(request.getCountry()))
                .build();
        return toAddressResponse(saveWithDefaultHandling(userId, address,
                Boolean.TRUE.equals(request.getIsDefault())));
    }

    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        Address address = findOwnedAddress(userId, addressId);
        address.setLabel(trimOrNull(request.getLabel()));
        address.setRecipientName(request.getRecipientName().trim());
        address.setPhone(request.getPhone().trim());
        address.setStreet(request.getStreet().trim());
        address.setCity(request.getCity().trim());
        address.setState(trimOrNull(request.getState()));
        address.setPostalCode(trimOrNull(request.getPostalCode()));
        address.setCountry(trimOrNull(request.getCountry()));
        return toAddressResponse(saveWithDefaultHandling(userId, address,
                Boolean.TRUE.equals(request.getIsDefault())));
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = findOwnedAddress(userId, addressId);
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        addressRepository.delete(address);
        // Promote the most recent remaining address to default.
        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
            if (!remaining.isEmpty()) {
                Address promote = remaining.get(0);
                promote.setIsDefault(true);
                addressRepository.save(promote);
            }
        }
        log.info("Address deleted — addressId={}, userId={}", addressId, userId);
    }

    @Transactional
    public AddressResponse setDefaultAddress(Long userId, Long addressId) {
        Address address = findOwnedAddress(userId, addressId);
        return toAddressResponse(saveWithDefaultHandling(userId, address, true));
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private Address saveWithDefaultHandling(Long userId, Address address, boolean makeDefault) {
        if (makeDefault || Boolean.TRUE.equals(address.getIsDefault())) {
            address.setIsDefault(true);
            addressRepository.clearDefaultForUser(userId);
        } else if (address.getId() == null) {
            // New address, not flagged default: the very first address for a
            // user becomes the default automatically.
            boolean hasAny = !addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).isEmpty();
            address.setIsDefault(!hasAny);
        }
        return addressRepository.save(address);
    }

    private Address findOwnedAddress(Long userId, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + addressId));
        if (!address.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Address not found: " + addressId);
        }
        return address;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private ProfileResponse toProfileResponse(User user) {
        return ProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .role(user.getRole() == null ? null : user.getRole().name())
                .avatarUrl(user.getAvatarUrl())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private HealthProfileResponse defaultHealthResponse() {
        return HealthProfileResponse.builder()
                .allergies(List.of())
                .chronicConditions(List.of())
                .emergencyContacts(List.of())
                .dataProcessingConsent(false)
                .emailNotifications(true)
                .profileVisible(false)
                .shareHealthDataWithDoctors(true)
                .build();
    }

    private HealthProfileResponse toHealthResponse(HealthProfile profile) {
        return HealthProfileResponse.builder()
                .allergies(profile.getAllergies() == null ? List.of() : profile.getAllergies())
                .chronicConditions(profile.getChronicConditions() == null
                        ? List.of() : profile.getChronicConditions())
                .emergencyContacts(profile.getEmergencyContacts() == null ? List.of()
                        : profile.getEmergencyContacts().stream()
                            .map(c -> EmergencyContactDTO.builder()
                                    .name(c.getName())
                                    .phone(c.getPhone())
                                    .relationship(c.getRelationship())
                                    .build())
                            .toList())
                .dataProcessingConsent(Boolean.TRUE.equals(profile.getDataProcessingConsent()))
                .emailNotifications(Boolean.TRUE.equals(profile.getEmailNotifications()))
                .profileVisible(Boolean.TRUE.equals(profile.getProfileVisible()))
                .shareHealthDataWithDoctors(Boolean.TRUE.equals(profile.getShareHealthDataWithDoctors()))
                .build();
    }

    private AddressResponse toAddressResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .label(address.getLabel())
                .recipientName(address.getRecipientName())
                .phone(address.getPhone())
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .isDefault(Boolean.TRUE.equals(address.getIsDefault()))
                .createdAt(address.getCreatedAt())
                .build();
    }

    private List<String> trimList(List<String> values, String what) {
        List<String> cleaned = new ArrayList<>();
        for (String v : values) {
            if (v == null) continue;
            String t = v.trim();
            if (t.isEmpty()) continue;
            cleaned.add(t);
        }
        return cleaned;
    }

    private String trimOrNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private void validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose an image to upload");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("Avatar image must be 2MB or smaller");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_AVATAR_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Avatar must be a JPEG, PNG, WebP or GIF image");
        }
        try (InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(12);
            if (!magicMatches(header)) {
                throw new IllegalArgumentException("File content does not look like a valid image");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the uploaded file");
        }
    }

    private boolean magicMatches(byte[] h) {
        if (h == null || h.length < 4) return false;
        if (startsWith(h, JPEG_MAGIC)) return true;
        if (startsWith(h, PNG_MAGIC)) return true;
        if (startsWith(h, GIF_MAGIC)) return true;
        return startsWith(h, WEBP_MAGIC) && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return "jpg";
        String ext = fileName.substring(dot + 1).toLowerCase();
        return Set.of("jpg", "jpeg", "png", "webp", "gif").contains(ext) ? ext : "jpg";
    }

    private void deleteAvatarFile(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith("/api/avatars/")) return;
        String storedName = avatarUrl.substring("/api/avatars/".length());
        // Only ever delete UUID-named files inside our storage root.
        if (!storedName.matches("[0-9a-fA-F\\-]{36}\\.(jpg|jpeg|png|webp|gif)")) return;
        try {
            Files.deleteIfExists(Paths.get(avatarStorageRoot).resolve(storedName));
        } catch (IOException e) {
            log.warn("Could not delete old avatar file {}: {}", storedName, e.getMessage());
        }
    }
}
