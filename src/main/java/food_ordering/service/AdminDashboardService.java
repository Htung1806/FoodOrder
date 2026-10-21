package food_ordering.service;

import food_ordering.dto.AdminDashboardData;
import food_ordering.dto.RevenueChartPoint;
import food_ordering.entity.CustomerOrder;
import food_ordering.entity.OrderStatus;
import food_ordering.repository.CustomerOrderRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {

	private static final DateTimeFormatter DAY_KEY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");
	private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd/MM");
	private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MM/yyyy");

	private final CustomerOrderRepository customerOrderRepository;

	public AdminDashboardService(CustomerOrderRepository customerOrderRepository) {
		this.customerOrderRepository = customerOrderRepository;
	}

	@Transactional(readOnly = true)
	public AdminDashboardData getDashboard(LocalDate startDate, LocalDate endDate,
			RevenueGranularity granularity) {
		ZoneId zone = ZoneId.systemDefault();
		var startAt = startDate.atStartOfDay(zone).toInstant();
		var endExclusive = endDate.plusDays(1).atStartOfDay(zone).toInstant();
		Map<String, CustomerOrderRepository.RevenuePeriodProjection> summaries = new HashMap<>();
		for (var summary : customerOrderRepository.summarizeByPeriod(
				startAt, endExclusive, granularity.getMysqlFormat())) {
			summaries.put(summary.getPeriodKey(), summary);
		}

		List<RevenueChartPoint> points = createPoints(startDate, endDate, granularity, summaries);
		BigDecimal maxRevenue = points.stream()
				.map(RevenueChartPoint::revenue)
				.max(BigDecimal::compareTo)
				.orElse(BigDecimal.ZERO);
		if (maxRevenue.signum() > 0) {
			points = points.stream()
					.map(point -> new RevenueChartPoint(point.label(), point.orderCount(), point.revenue(),
							point.revenue().signum() == 0 ? 0
									: Math.max(4, point.revenue().multiply(BigDecimal.valueOf(100))
											.divide(maxRevenue, 0, RoundingMode.HALF_UP).intValue())))
					.toList();
		}

		return new AdminDashboardData(
				customerOrderRepository.count(),
				customerOrderRepository.countByStatus(OrderStatus.PENDING),
				customerOrderRepository.countByStatus(OrderStatus.COMPLETED),
				customerOrderRepository.sumTotalAmountByStatus(OrderStatus.COMPLETED),
				startDate,
				endDate,
				granularity.name(),
				points,
				customerOrderRepository.findTop10ByOrderByCreatedAtDesc());
	}

	private List<RevenueChartPoint> createPoints(LocalDate startDate, LocalDate endDate,
			RevenueGranularity granularity,
			Map<String, CustomerOrderRepository.RevenuePeriodProjection> summaries) {
		if (granularity == RevenueGranularity.MONTH) {
			YearMonth current = YearMonth.from(startDate);
			YearMonth finalMonth = YearMonth.from(endDate);
			var points = new ArrayList<RevenueChartPoint>();
			while (!current.isAfter(finalMonth)) {
				String key = current.format(MONTH_KEY);
				var summary = summaries.get(key);
				points.add(new RevenueChartPoint(current.format(MONTH_LABEL),
						summary == null ? 0 : summary.getOrderCount(),
						summary == null ? BigDecimal.ZERO : summary.getRevenue(), 0));
				current = current.plusMonths(1);
			}
			return points;
		}

		var points = new ArrayList<RevenueChartPoint>();
		for (LocalDate current = startDate; !current.isAfter(endDate); current = current.plusDays(1)) {
			var summary = summaries.get(current.format(DAY_KEY));
			points.add(new RevenueChartPoint(current.format(DAY_LABEL),
					summary == null ? 0 : summary.getOrderCount(),
					summary == null ? BigDecimal.ZERO : summary.getRevenue(), 0));
		}
		return points;
	}
}
