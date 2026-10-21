package food_ordering.dto;

import java.math.BigDecimal;

public record RevenueChartPoint(String label, long orderCount, BigDecimal revenue, int heightPercent) {
}
