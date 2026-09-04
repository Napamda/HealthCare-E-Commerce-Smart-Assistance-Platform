package org.example.Healthcareplatform.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {
    // System statistics
    private long totalUsers;
    private long totalProducts;
    private long totalOrders;
    private long totalEvents;

    // User breakdown by role
    private Map<String, Long> usersByRole;

    // User growth (registrations per day, last 30 days)
    private List<TimeSeriesPoint> userGrowth;

    // Order statistics
    private BigDecimal totalRevenue;
    private Map<String, Long> ordersByStatus;

    // Moderation queues
    private long pendingVendors;
    private long pendingProducts;
    private long pendingEvents;

    // Recent activity
    private List<ActivityLogResponse> recentActivity;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimeSeriesPoint {
        private String date;
        private long count;
    }
}
