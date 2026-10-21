package food_ordering.controller;

import food_ordering.service.CustomerOrderService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import food_ordering.service.InvalidOrderStatusTransitionException;

@Controller
@RequestMapping("/orders")
public class CustomerOrdersController {

	private final CustomerOrderService customerOrderService;

	public CustomerOrdersController(CustomerOrderService customerOrderService) {
		this.customerOrderService = customerOrderService;
	}

	@GetMapping
	public String history(Authentication authentication, Model model) {
		model.addAttribute("orders", customerOrderService.findCustomerOrders(authentication.getName()));
		return "orders/list";
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Authentication authentication, Model model) {
		var order = customerOrderService.findCustomerOrder(id, authentication.getName());
		model.addAttribute("order", order);
		model.addAttribute("canCancel", order.canBeCancelledByCustomer());
		return "orders/detail";
	}

	@PostMapping("/{id}/cancel")
	public String cancel(@PathVariable Long id, Authentication authentication,
			RedirectAttributes redirectAttributes) {
		try {
			customerOrderService.cancelCustomerOrder(id, authentication.getName());
			redirectAttributes.addFlashAttribute("orderSuccess", "Đơn hàng đã được hủy.");
		} catch (InvalidOrderStatusTransitionException exception) {
			redirectAttributes.addFlashAttribute("orderError", exception.getMessage());
		}
		return "redirect:/orders/" + id;
	}
}
