package food_ordering.controller.admin;

import food_ordering.service.AdminDashboardService;
import food_ordering.service.RevenueGranularity;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

	private final AdminDashboardService adminDashboardService;

	public AdminDashboardController(AdminDashboardService adminDashboardService) {
		this.adminDashboardService = adminDashboardService;
	}

	@GetMapping
	public String dashboard(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			@RequestParam(defaultValue = "DAY") RevenueGranularity granularity,
			Model model) {
		LocalDate today = LocalDate.now();
		LocalDate effectiveEnd = endDate == null ? today : endDate;
		LocalDate effectiveStart = startDate == null
				? today.withDayOfMonth(1)
				: startDate;
		String filterError = validateRange(effectiveStart, effectiveEnd, granularity);
		if (filterError != null) {
			model.addAttribute("filterError", filterError);
			effectiveEnd = today;
			effectiveStart = today.withDayOfMonth(1);
		}

		var dashboard = adminDashboardService.getDashboard(effectiveStart, effectiveEnd, granularity);
		model.addAttribute("dashboard", dashboard);
		return "admin/dashboard";
	}

	private String validateRange(LocalDate startDate, LocalDate endDate, RevenueGranularity granularity) {
		if (startDate.isAfter(endDate)) {
			return "Ngày bắt đầu không được sau ngày kết thúc.";
		}
		long range = granularity == RevenueGranularity.DAY
				? ChronoUnit.DAYS.between(startDate, endDate) + 1
				: ChronoUnit.MONTHS.between(startDate.withDayOfMonth(1), endDate.withDayOfMonth(1)) + 1;
		long maximum = granularity == RevenueGranularity.DAY ? 366 : 60;
		return range > maximum
				? "Khoảng lọc tối đa là " + maximum + (granularity == RevenueGranularity.DAY ? " ngày." : " tháng.")
				: null;
	}
}
