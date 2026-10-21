package food_ordering.dto;

import food_ordering.entity.CustomerOrder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdminDashboardData(
		long totalOrders,
		long pendingOrders,
		long completedOrders,
		BigDecimal totalRevenue,
		LocalDate startDate,
		LocalDate endDate,
		String granularity,
		List<RevenueChartPoint> revenuePoints,
		List<CustomerOrder> recentOrders) {
}
