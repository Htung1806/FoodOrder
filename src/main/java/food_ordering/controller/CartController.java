package food_ordering.controller;

import food_ordering.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
public class CartController {

	private final CartService cartService;

	public CartController(CartService cartService) {
		this.cartService = cartService;
	}

	@GetMapping
	public String view(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
		var cart = cartService.getCart(session);
		model.addAttribute("cart", cart);
		if (cart.removedUnavailableItems() > 0) {
			redirectAttributes.addFlashAttribute("cartWarning",
					"Đã bỏ " + cart.removedUnavailableItems()
							+ " món không còn được bán khỏi giỏ hàng.");
			return "redirect:/cart";
		}
		return "cart";
	}

	@PostMapping("/items/{productId}")
	public String add(@PathVariable Long productId,
			@RequestParam(defaultValue = "1") int quantity,
			HttpSession session, RedirectAttributes redirectAttributes) {
		cartService.add(session, productId, quantity);
		redirectAttributes.addFlashAttribute("cartMessage", "Đã thêm món vào giỏ hàng.");
		return "redirect:/cart";
	}

	@PostMapping("/items/{productId}/increase")
	public String increase(@PathVariable Long productId, HttpSession session) {
		cartService.increase(session, productId);
		return "redirect:/cart";
	}

	@PostMapping("/items/{productId}/decrease")
	public String decrease(@PathVariable Long productId, HttpSession session) {
		cartService.decrease(session, productId);
		return "redirect:/cart";
	}

	@PostMapping("/items/{productId}/quantity")
	public String updateQuantity(@PathVariable Long productId,
			@RequestParam int quantity, HttpSession session) {
		cartService.updateQuantity(session, productId, quantity);
		return "redirect:/cart";
	}

	@PostMapping("/items/{productId}/remove")
	public String remove(@PathVariable Long productId, HttpSession session,
			RedirectAttributes redirectAttributes) {
		cartService.remove(session, productId);
		redirectAttributes.addFlashAttribute("cartMessage", "Đã xóa món khỏi giỏ hàng.");
		return "redirect:/cart";
	}
}
