package org.example.Healthcareplatform.vendor.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VendorDashboardStats {
    private long totalProducts;
    private long totalOrders;
    private long pendingOrders;
    private long processingOrders;
    private long shippedOrders;
    private long deliveredOrders;
    private long ordersToday;
    private BigDecimal revenueToday;
    private BigDecimal totalRevenue;
    private long lowStockCount;
    private long outOfStockCount;
    private long totalCustomers;
}