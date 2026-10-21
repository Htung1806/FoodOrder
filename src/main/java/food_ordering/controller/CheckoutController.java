package food_ordering.controller;

import food_ordering.dto.CheckoutForm;
import food_ordering.service.CartService;
import food_ordering.service.CustomerOrderService;
import food_ordering.service.OrderPlacementException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CheckoutController {

	private static final String CHECKOUT_TOKEN_ATTRIBUTE = CheckoutController.class.getName() + ".token";

	private final CartService cartService;
	private final CustomerOrderService customerOrderService;

	public CheckoutController(CartService cartService, CustomerOrderService customerOrderService) {
		this.cartService = cartService;
		this.customerOrderService = customerOrderService;
	}

	@GetMapping("/checkout")
	public String checkout(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
		var cart = cartService.getCart(session);
		if (cart.removedUnavailableItems() > 0) {
			redirectAttributes.addFlashAttribute("cartWarning",
					"Đã bỏ " + cart.removedUnavailableItems()
							+ " món không còn được bán khỏi giỏ hàng. Vui lòng kiểm tra lại.");
			return "redirect:/cart";
		}
		if (cart.items().isEmpty()) {
			redirectAttributes.addFlashAttribute("cartMessage", "Giỏ hàng đang trống, hãy chọn món trước khi đặt hàng.");
			return "redirect:/cart";
		}

		model.addAttribute("cart", cart);
		model.addAttribute("checkoutForm", new CheckoutForm());
		model.addAttribute("checkoutToken", getOrCreateToken(session));
		return "checkout";
	}

	@PostMapping("/checkout")
	public String placeOrder(@Valid @ModelAttribute("checkoutForm") CheckoutForm form,
			BindingResult bindingResult, @RequestParam String checkoutToken, HttpSession session,
			Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		if (!isCurrentToken(session, checkoutToken)) {
			redirectAttributes.addFlashAttribute("orderError",
					"Phiên đặt hàng đã hết hạn. Vui lòng kiểm tra lại giỏ hàng.");
			return "redirect:/checkout";
		}

		Map<Long, Integer> quantities = cartService.snapshotQuantities(session);
		if (bindingResult.hasErrors()) {
			return renderCheckout(session, form, checkoutToken, model, redirectAttributes);
		}

		try {
			var order = customerOrderService.placeOrder(authentication.getName(), checkoutToken, form, quantities);
			cartService.clearAfterCheckout(session, quantities);
			clearTokenIfCurrent(session, checkoutToken);
			redirectAttributes.addFlashAttribute("orderSuccess",
					"Đặt hàng thành công. Mã đơn hàng của bạn là " + order.getOrderNumber() + ".");
			return "redirect:/orders/" + order.getId();
		} catch (DataIntegrityViolationException exception) {
			var existingOrder = customerOrderService.findByCheckoutToken(checkoutToken, authentication.getName())
					.orElseThrow(() -> exception);
			cartService.clearAfterCheckout(session, quantities);
			clearTokenIfCurrent(session, checkoutToken);
			redirectAttributes.addFlashAttribute("orderSuccess",
					"Đơn hàng đã được tiếp nhận. Mã đơn hàng của bạn là " + existingOrder.getOrderNumber() + ".");
			return "redirect:/orders/" + existingOrder.getId();
		} catch (OrderPlacementException exception) {
			redirectAttributes.addFlashAttribute("orderError", exception.getMessage());
			return "redirect:/checkout";
		}
	}

	private String renderCheckout(HttpSession session, CheckoutForm form, String token, Model model,
			RedirectAttributes redirectAttributes) {
		var cart = cartService.getCart(session);
		if (cart.items().isEmpty()) {
			redirectAttributes.addFlashAttribute("cartMessage", "Giỏ hàng đang trống, hãy chọn món trước khi đặt hàng.");
			return "redirect:/cart";
		}
		model.addAttribute("cart", cart);
		model.addAttribute("checkoutForm", form);
		model.addAttribute("checkoutToken", token);
		return "checkout";
	}

	private String getOrCreateToken(HttpSession session) {
		synchronized (session) {
			Object token = session.getAttribute(CHECKOUT_TOKEN_ATTRIBUTE);
			if (token instanceof String value) {
				return value;
			}
			String newToken = UUID.randomUUID().toString();
			session.setAttribute(CHECKOUT_TOKEN_ATTRIBUTE, newToken);
			return newToken;
		}
	}

	private boolean isCurrentToken(HttpSession session, String token) {
		if (token == null) {
			return false;
		}
		try {
			if (!UUID.fromString(token).toString().equalsIgnoreCase(token)) {
				return false;
			}
		} catch (IllegalArgumentException exception) {
			return false;
		}
		synchronized (session) {
			return token.equals(session.getAttribute(CHECKOUT_TOKEN_ATTRIBUTE));
		}
	}

	private void clearTokenIfCurrent(HttpSession session, String token) {
		synchronized (session) {
			if (token.equals(session.getAttribute(CHECKOUT_TOKEN_ATTRIBUTE))) {
				session.removeAttribute(CHECKOUT_TOKEN_ATTRIBUTE);
			}
		}
	}
}
