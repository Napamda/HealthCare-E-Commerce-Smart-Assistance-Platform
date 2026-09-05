package org.example.Healthcareplatform.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.Healthcareplatform.admin.dto.ActivityLogResponse;
import org.example.Healthcareplatform.admin.dto.AdminUserResponse;
import org.example.Healthcareplatform.admin.dto.PagedResponse;
import org.example.Healthcareplatform.admin.dto.RoleUpdateRequest;
import org.example.Healthcareplatform.admin.dto.StatusUpdateRequest;
import org.example.Healthcareplatform.admin.service.AdminUserService;
import org.example.Healthcareplatform.auth.util.SecurityContextUtil;
import org.springframework.web.bind.annotation.*;

/**
 * Admin user management — Task 3.1.
 * Secured by the "/api/admin/**" ADMIN-only rule in SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final SecurityContextUtil securityContextUtil;

    @GetMapping("/users")
    public PagedResponse<AdminUserResponse> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminUserService.listUsers(search, role, status, page, size);
    }

    @PatchMapping("/users/{id}/status")
    public AdminUserResponse updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody StatusUpdateRequest request) {
        return adminUserService.updateStatus(
                securityContextUtil.getCurrentUserId(), id, request.getStatus(), request.getReason());
    }

    @PatchMapping("/users/{id}/role")
    public AdminUserResponse changeRole(@PathVariable Long id,
                                        @Valid @RequestBody RoleUpdateRequest request) {
        return adminUserService.changeRole(
                securityContextUtil.getCurrentUserId(), id, request.getRole());
    }

    @GetMapping("/users/{id}/activity")
    public PagedResponse<ActivityLogResponse> userActivity(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminUserService.getActivityForUser(id, page, size);
    }

    @GetMapping("/activity")
    public PagedResponse<ActivityLogResponse> recentActivity(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminUserService.getRecentActivity(page, size);
    }
}
