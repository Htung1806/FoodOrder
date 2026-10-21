package food_ordering.controller.admin;

import food_ordering.entity.OrderStatus;
import food_ordering.service.CustomerOrderService;
import food_ordering.service.InvalidOrderStatusTransitionException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

	private final CustomerOrderService customerOrderService;

	public AdminOrderController(CustomerOrderService customerOrderService) {
		this.customerOrderService = customerOrderService;
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("orders", customerOrderService.findAllOrders());
		return "admin/orders/list";
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Model model) {
		var order = customerOrderService.findOrder(id);
		model.addAttribute("order", order);
		model.addAttribute("nextStatuses", order.getStatus().allowedNextStatuses());
		return "admin/orders/detail";
	}

	@PostMapping("/{id}/status")
	public String updateStatus(@PathVariable Long id, @RequestParam OrderStatus status,
			RedirectAttributes redirectAttributes) {
		try {
			customerOrderService.updateStatus(id, status);
			redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái đơn hàng.");
		} catch (InvalidOrderStatusTransitionException exception) {
			redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
		}
		return "redirect:/admin/orders/" + id;
	}
}
