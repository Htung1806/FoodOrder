package food_ordering.service;

import food_ordering.dto.CartItemView;
import food_ordering.dto.CartView;
import food_ordering.entity.Product;
import food_ordering.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CartService {

	public static final int MAX_QUANTITY_PER_PRODUCT = 99;
	private static final String SESSION_ATTRIBUTE = CartService.class.getName() + ".cart";

	private final ProductRepository productRepository;

	public CartService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public void add(HttpSession session, Long productId, int quantity) {
		validateQuantity(quantity);
		Product product = requireAvailableProduct(productId);
		SessionCart cart = getOrCreateCart(session);
		synchronized (cart) {
			int updatedQuantity = cart.quantities.getOrDefault(product.getId(), 0) + quantity;
			validateQuantity(updatedQuantity);
			cart.quantities.put(product.getId(), updatedQuantity);
		}
	}

	public void updateQuantity(HttpSession session, Long productId, int quantity) {
		validateQuantity(quantity);
		Product product = requireAvailableProduct(productId);
		SessionCart cart = getCartState(session);
		synchronized (cart) {
			if (!cart.quantities.containsKey(product.getId())) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Món ăn không có trong giỏ hàng.");
			}
			cart.quantities.put(product.getId(), quantity);
		}
	}

	public void increase(HttpSession session, Long productId) {
		requireAvailableProduct(productId);
		SessionCart cart = getCartState(session);
		synchronized (cart) {
			int quantity = currentQuantity(cart, productId);
			validateQuantity(quantity + 1);
			cart.quantities.put(productId, quantity + 1);
		}
	}

	public void decrease(HttpSession session, Long productId) {
		Product product = requireAvailableProduct(productId);
		SessionCart cart = getCartState(session);
		synchronized (cart) {
			int quantity = currentQuantity(cart, productId);
			if (quantity <= 1) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng không thể nhỏ hơn 1.");
			}
			cart.quantities.put(product.getId(), quantity - 1);
		}
	}

	public void remove(HttpSession session, Long productId) {
		SessionCart cart = getCartState(session);
		synchronized (cart) {
			cart.quantities.remove(productId);
			storeOrClear(session, cart);
		}
	}

	public Map<Long, Integer> snapshotQuantities(HttpSession session) {
		SessionCart cart = getCartState(session);
		synchronized (cart) {
			return Map.copyOf(cart.quantities);
		}
	}

	public void clearAfterCheckout(HttpSession session, Map<Long, Integer> orderedQuantities) {
		if (orderedQuantities.isEmpty()) {
			return;
		}
		SessionCart cart = getCartState(session);
		synchronized (cart) {
			if (cart.quantities.equals(orderedQuantities)) {
				session.removeAttribute(SESSION_ATTRIBUTE);
			}
		}
	}

	@Transactional(readOnly = true)
	public CartView getCart(HttpSession session) {
		SessionCart cart = getCartState(session);
		synchronized (cart) {
			if (cart.quantities.isEmpty()) {
				return new CartView(List.of(), BigDecimal.ZERO, 0, 0);
			}

			Map<Long, Product> productsById = new LinkedHashMap<>();
			for (Product product : productRepository.findAllById(cart.quantities.keySet())) {
				productsById.put(product.getId(), product);
			}
			List<CartItemView> items = new ArrayList<>();
			int removedUnavailableItems = 0;
			int totalQuantity = 0;
			BigDecimal total = BigDecimal.ZERO;

			for (var entry : new ArrayList<>(cart.quantities.entrySet())) {
				Product product = productsById.get(entry.getKey());
				if (product == null || !product.isAvailable()) {
					cart.quantities.remove(entry.getKey());
					removedUnavailableItems++;
					continue;
				}

				BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(entry.getValue()));
				items.add(new CartItemView(product.getId(), product.getName(), product.getImageUrl(),
						product.getPrice(), entry.getValue(), lineTotal));
				total = total.add(lineTotal);
				totalQuantity += entry.getValue();
			}
			storeOrClear(session, cart);
			return new CartView(List.copyOf(items), total, totalQuantity, removedUnavailableItems);
		}
	}

	private Product requireAvailableProduct(Long productId) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy món ăn."));
		if (!product.isAvailable()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Món ăn hiện không còn bán.");
		}
		return product;
	}

	private int currentQuantity(SessionCart cart, Long productId) {
		Integer quantity = cart.quantities.get(productId);
		if (quantity == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Món ăn không có trong giỏ hàng.");
		}
		return quantity;
	}

	private void validateQuantity(int quantity) {
		if (quantity < 1 || quantity > MAX_QUANTITY_PER_PRODUCT) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Số lượng phải từ 1 đến " + MAX_QUANTITY_PER_PRODUCT + ".");
		}
	}

	private SessionCart getOrCreateCart(HttpSession session) {
		synchronized (session) {
			SessionCart cart = (SessionCart) session.getAttribute(SESSION_ATTRIBUTE);
			if (cart == null) {
				cart = new SessionCart();
				session.setAttribute(SESSION_ATTRIBUTE, cart);
			}
			return cart;
		}
	}

	private SessionCart getCartState(HttpSession session) {
		synchronized (session) {
			SessionCart cart = (SessionCart) session.getAttribute(SESSION_ATTRIBUTE);
			return cart == null ? new SessionCart() : cart;
		}
	}

	private void storeOrClear(HttpSession session, SessionCart cart) {
		if (cart.quantities.isEmpty()) {
			session.removeAttribute(SESSION_ATTRIBUTE);
		} else {
			session.setAttribute(SESSION_ATTRIBUTE, cart);
		}
	}

	private static final class SessionCart implements Serializable {

		@Serial
		private static final long serialVersionUID = 1L;

		private final Map<Long, Integer> quantities = new LinkedHashMap<>();
	}
}
