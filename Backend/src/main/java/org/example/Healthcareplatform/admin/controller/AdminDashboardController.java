package org.example.Healthcareplatform.admin.controller;

import lombok.RequiredArgsConstructor;
import org.example.Healthcareplatform.admin.dto.DashboardStatsResponse;
import org.example.Healthcareplatform.admin.service.AdminDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin dashboard statistics — Task 3.5.
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/stats")
    public DashboardStatsResponse getStats() {
        return dashboardService.getStats();
    }
}
