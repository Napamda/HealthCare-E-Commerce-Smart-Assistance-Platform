package org.example.Healthcareplatform.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.Healthcareplatform.admin.dto.ModerationDecisionRequest;
import org.example.Healthcareplatform.admin.dto.ModerationEventResponse;
import org.example.Healthcareplatform.admin.dto.ModerationProductResponse;
import org.example.Healthcareplatform.admin.dto.VendorResponse;
import org.example.Healthcareplatform.admin.service.AdminModerationService;
import org.example.Healthcareplatform.auth.util.SecurityContextUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin moderation endpoints for Tasks 3.2-3.4.
 * Secured by the "/api/admin/**" ADMIN-only rule in SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin/moderation")
@RequiredArgsConstructor
public class AdminModerationController {

    private final AdminModerationService moderationService;
    private final SecurityContextUtil securityContextUtil;

    // ---- Task 3.2 — Vendor Management ----

    @GetMapping("/vendors")
    public List<VendorResponse> getVendors(@RequestParam(required = false) String status) {
        return moderationService.getVendors(status);
    }

    @PatchMapping("/vendors/{profileId}")
    public VendorResponse decideOnVendor(@PathVariable Long profileId,
                                         @Valid @RequestBody ModerationDecisionRequest request) {
        return moderationService.decideOnVendor(
                securityContextUtil.getCurrentUserId(), profileId, request);
    }

    // ---- Task 3.3 — Product Moderation ----

    @GetMapping("/products")
    public List<ModerationProductResponse> getProducts(@RequestParam(required = false) String status) {
        return moderationService.getProductsByStatus(status);
    }

    @PatchMapping("/products/{productId}")
    public ModerationProductResponse decideOnProduct(@PathVariable Long productId,
                                                     @Valid @RequestBody ModerationDecisionRequest request) {
        return moderationService.decideOnProduct(
                securityContextUtil.getCurrentUserId(), productId, request);
    }

    // ---- Task 3.4 — Event Moderation ----

    @GetMapping("/events")
    public List<ModerationEventResponse> getEvents(@RequestParam(required = false) String status) {
        return moderationService.getEventsByStatus(status);
    }

    @PatchMapping("/events/{eventId}")
    public ModerationEventResponse decideOnEvent(@PathVariable Long eventId,
                                                 @Valid @RequestBody ModerationDecisionRequest request) {
        return moderationService.decideOnEvent(
                securityContextUtil.getCurrentUserId(), eventId, request);
    }
}
