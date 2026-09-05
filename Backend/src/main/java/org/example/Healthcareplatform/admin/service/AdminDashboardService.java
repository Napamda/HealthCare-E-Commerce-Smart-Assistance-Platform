package org.example.Healthcareplatform.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.Healthcareplatform.admin.dto.ActivityLogResponse;
import org.example.Healthcareplatform.admin.dto.DashboardStatsResponse;
import org.example.Healthcareplatform.admin.dto.PagedResponse;
import org.example.Healthcareplatform.admin.entity.UserActivityLog;
import org.example.Healthcareplatform.admin.entity.VendorProfile;
import org.example.Healthcareplatform.admin.repository.UserActivityLogRepository;
import org.example.Healthcareplatform.admin.repository.VendorProfileRepository;
import org.example.Healthcareplatform.event.entity.EventStatus;
import org.example.Healthcareplatform.event.repository.HealthEventRepository;
import org.example.Healthcareplatform.order.repository.OrderRepository;
import org.example.Healthcareplatform.product.entity.ProductStatus;
import org.example.Healthcareplatform.product.repository.ProductRepository;
import org.example.Healthcareplatform.user.entity.User;
import org.example.Healthcareplatform.user.entity.UserRole;
import org.example.Healthcareplatform.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final HealthEventRepository healthEventRepository;
    private final VendorProfileRepository vendorProfileRepository;
    private final UserActivityLogRepository activityLogRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        // System statistics
        long totalUsers = userRepository.count();
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();
        long totalEvents = healthEventRepository.count();

        // Users by role — manual count since we don't have a countByRole method
        Map<String, Long> usersByRole = new LinkedHashMap<>();
        for (UserRole role : UserRole.values()) {
            usersByRole.put(role.name(),
                    userRepository.searchUsers(null, role, null, PageRequest.of(0, 1)).getTotalElements());
        }

        // User growth — last 30 days
        List<DashboardStatsResponse.TimeSeriesPoint> userGrowth = buildUserGrowth();

        // Order statistics
        BigDecimal revenue = orderRepository.totalRevenue();
        if (revenue == null) revenue = BigDecimal.ZERO;
        Map<String, Long> ordersByStatus = buildOrdersByStatus();

        // Moderation queues
        long pendingVendors = vendorProfileRepository
                .countByApprovalStatus(VendorProfile.ApprovalStatus.PENDING);
        long pendingProducts = productRepository.countByStatus(ProductStatus.PENDING);
        long pendingEvents = healthEventRepository.countByStatus(EventStatus.PENDING);

        // Recent activity (latest 10)
        Pageable topTen = PageRequest.of(0, 10);
        List<ActivityLogResponse> recentActivity = activityLogRepository
                .findAllByOrderByCreatedAtDesc(topTen)
                .map(this::toLogResponse)
                .getContent();

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .totalEvents(totalEvents)
                .usersByRole(usersByRole)
                .userGrowth(userGrowth)
                .totalRevenue(revenue)
                .ordersByStatus(ordersByStatus)
                .pendingVendors(pendingVendors)
                .pendingProducts(pendingProducts)
                .pendingEvents(pendingEvents)
                .recentActivity(recentActivity)
                .build();
    }

    private List<DashboardStatsResponse.TimeSeriesPoint> buildUserGrowth() {
        // Last 30 days, cumulative registrations per day.
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<User> recent = userRepository.searchUsers(null, null, null,
                PageRequest.of(0, 10000)).getContent().stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(since))
                .toList();

        Map<String, Long> byDate = new TreeMap<>();
        for (User u : recent) {
            String date = u.getCreatedAt().toLocalDate().toString();
            byDate.merge(date, 1L, Long::sum);
        }

        // Fill in days with zero registrations
        List<DashboardStatsResponse.TimeSeriesPoint> points = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 29; i >= 0; i--) {
            String date = today.minusDays(i).toString();
            points.add(DashboardStatsResponse.TimeSeriesPoint.builder()
                    .date(date)
                    .count(byDate.getOrDefault(date, 0L))
                    .build());
        }
        return points;
    }

    private Map<String, Long> buildOrdersByStatus() {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : orderRepository.countOrdersByStatus()) {
            String status = row[0].toString();
            long count = ((Number) row[1]).longValue();
            map.put(status, count);
        }
        return map;
    }

    private ActivityLogResponse toLogResponse(UserActivityLog l) {
        return ActivityLogResponse.builder()
                .id(l.getId())
                .userId(l.getUserId())
                .actorAdminId(l.getActorAdminId())
                .action(l.getAction().name())
                .details(l.getDetails())
                .createdAt(l.getCreatedAt())
                .build();
    }
}
