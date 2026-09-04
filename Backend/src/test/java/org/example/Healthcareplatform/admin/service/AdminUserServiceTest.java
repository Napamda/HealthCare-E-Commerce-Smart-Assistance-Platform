package org.example.Healthcareplatform.admin.service;

import org.example.Healthcareplatform.admin.dto.ActivityLogResponse;
import org.example.Healthcareplatform.admin.dto.AdminUserResponse;
import org.example.Healthcareplatform.admin.dto.PagedResponse;
import org.example.Healthcareplatform.admin.entity.UserActivityLog;
import org.example.Healthcareplatform.admin.repository.UserActivityLogRepository;
import org.example.Healthcareplatform.user.entity.User;
import org.example.Healthcareplatform.user.entity.UserRole;
import org.example.Healthcareplatform.user.entity.UserStatus;
import org.example.Healthcareplatform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserActivityLogRepository activityLogRepository;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private AdminUserService adminUserService;

    private static final Long ADMIN_ID = 99L;

    private User patient;

    @BeforeEach
    void setUp() {
        patient = User.builder()
                .id(1L)
                .email("patient@example.com")
                .password("secret")
                .firstName("Pat")
                .lastName("Ient")
                .role(UserRole.PATIENT)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
    }

    // ---- View all users + search/filter ----

    @Test
    void listUsersMapsPageAndPassesFilters() {
        when(userRepository.searchUsers(eq("pat"), eq(UserRole.PATIENT), eq(UserStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(patient), PageRequest.of(0, 20), 1));

        PagedResponse<AdminUserResponse> result =
                adminUserService.listUsers("pat", "patient", "active", 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("patient@example.com");
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void listUsersRejectsInvalidRoleFilter() {
        assertThatThrownBy(() -> adminUserService.listUsers(null, "WIZARD", null, 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid role filter");
    }

    // ---- Suspend / activate ----

    @Test
    void suspendUserSetsStatusRevokesTokenAndLogs() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserResponse response =
                adminUserService.updateStatus(ADMIN_ID, 1L, "SUSPENDED", "Repeated abuse");

        assertThat(response.getStatus()).isEqualTo("SUSPENDED");
        assertThat(response.getSuspendedReason()).isEqualTo("Repeated abuse");
        assertThat(patient.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(patient.getRefreshToken()).isNull();
        assertThat(patient.getSuspendedAt()).isNotNull();

        ArgumentCaptor<UserActivityLog.ActivityAction> actionCaptor =
                ArgumentCaptor.forClass(UserActivityLog.ActivityAction.class);
        verify(activityLogService).record(eq(1L), eq(ADMIN_ID), actionCaptor.capture(), eq("Repeated abuse"));
        assertThat(actionCaptor.getValue()).isEqualTo(UserActivityLog.ActivityAction.SUSPENDED);
    }

    @Test
    void activateUserClearsSuspensionAndLogs() {
        patient.setStatus(UserStatus.SUSPENDED);
        patient.setSuspendedReason("earlier reason");
        patient.setSuspendedAt(java.time.LocalDateTime.now().minusDays(1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserResponse response =
                adminUserService.updateStatus(ADMIN_ID, 1L, "ACTIVE", null);

        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(patient.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(patient.getSuspendedReason()).isNull();
        assertThat(patient.getSuspendedAt()).isNull();
        verify(activityLogService).record(eq(1L), eq(ADMIN_ID),
                eq(UserActivityLog.ActivityAction.ACTIVATED), anyString());
    }

    @Test
    void activatingAlreadyActiveUserIsRejected() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(patient));

        assertThatThrownBy(() -> adminUserService.updateStatus(ADMIN_ID, 1L, "ACTIVE", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already active");
        verify(activityLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    void adminCannotSuspendThemselves() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(patient));

        assertThatThrownBy(() -> adminUserService.updateStatus(ADMIN_ID, ADMIN_ID, "SUSPENDED", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("your own account");
    }

    @Test
    void adminCannotSuspendAnotherAdmin() {
        User otherAdmin = User.builder()
                .id(2L).email("boss@example.com").role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherAdmin));

        assertThatThrownBy(() -> adminUserService.updateStatus(ADMIN_ID, 2L, "SUSPENDED", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Administrator accounts");
    }

    @Test
    void updateStatusRejectsInvalidStatus() {
        assertThatThrownBy(() -> adminUserService.updateStatus(ADMIN_ID, 1L, "BANNED", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- Change role ----

    @Test
    void changeRoleUpdatesAndLogs() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserResponse response = adminUserService.changeRole(ADMIN_ID, 1L, "doctor");

        assertThat(response.getRole()).isEqualTo("DOCTOR");
        assertThat(patient.getRole()).isEqualTo(UserRole.DOCTOR);
        verify(activityLogService).record(eq(1L), eq(ADMIN_ID),
                eq(UserActivityLog.ActivityAction.ROLE_CHANGED), eq("Role changed: PATIENT → DOCTOR"));
    }

    @Test
    void changeRoleRejectsSelf() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(patient));

        assertThatThrownBy(() -> adminUserService.changeRole(ADMIN_ID, ADMIN_ID, "DOCTOR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("your own role");
    }

    @Test
    void changeRoleRejectsAdminTarget() {
        User otherAdmin = User.builder()
                .id(2L).email("boss@example.com").role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherAdmin));

        assertThatThrownBy(() -> adminUserService.changeRole(ADMIN_ID, 2L, "DOCTOR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Administrator roles");
    }

    @Test
    void changeRoleRejectsAssigningAdmin() {
        assertThatThrownBy(() -> adminUserService.changeRole(ADMIN_ID, 1L, "ADMIN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ADMIN role cannot be assigned");
    }

    @Test
    void changeRoleRejectsSameRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(patient));

        assertThatThrownBy(() -> adminUserService.changeRole(ADMIN_ID, 1L, "PATIENT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already has role");
    }

    // ---- Activity log ----

    @Test
    void getActivityForUserReturnsMappedEntries() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(patient));
        UserActivityLog logEntry = UserActivityLog.builder()
                .id(7L).userId(1L).actorAdminId(ADMIN_ID)
                .action(UserActivityLog.ActivityAction.SUSPENDED)
                .details("Repeated abuse")
                .build();
        when(activityLogRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(logEntry), PageRequest.of(0, 20), 1));

        PagedResponse<ActivityLogResponse> result = adminUserService.getActivityForUser(1L, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAction()).isEqualTo("SUSPENDED");
        assertThat(result.getContent().get(0).getActorAdminId()).isEqualTo(ADMIN_ID);
    }

    @Test
    void getActivityForUnknownUserThrows() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.getActivityForUser(404L, 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }
}
