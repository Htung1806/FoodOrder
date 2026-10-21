package food_ordering.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import food_ordering.dto.CartView;
import food_ordering.entity.Product;
import food_ordering.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

class CartServiceTests {

	private ProductRepository productRepository;
	private CartService cartService;
	private Map<Long, Product> products;

	@BeforeEach
	void setUp() {
		productRepository = mock(ProductRepository.class);
		cartService = new CartService(productRepository);
		products = new ConcurrentHashMap<>();
		when(productRepository.findById(any())).thenAnswer(invocation ->
				Optional.ofNullable(products.get(invocation.getArgument(0))));
		when(productRepository.findAllById(any())).thenAnswer(invocation -> {
			Iterable<Long> ids = invocation.getArgument(0);
			return java.util.stream.StreamSupport.stream(ids.spliterator(), false)
					.map(products::get)
					.filter(java.util.Objects::nonNull)
					.toList();
		});
	}

	@Test
	void addsItemsAndCalculatesTotalsUsingCurrentDatabasePrices() {
		Product dish = product(1L, "Phở", "50000.00", true);
		products.put(1L, dish);
		MockHttpSession session = new MockHttpSession();

		cartService.add(session, 1L, 2);
		CartView cart = cartService.getCart(session);

		assertEquals(1, cart.items().size());
		assertEquals(2, cart.items().getFirst().quantity());
		assertEquals(new BigDecimal("50000.00"), cart.items().getFirst().unitPrice());
		assertEquals(new BigDecimal("100000.00"), cart.items().getFirst().lineTotal());
		assertEquals(new BigDecimal("100000.00"), cart.total());
		assertEquals(2, cart.totalQuantity());
	}

	@Test
	void updatedQuantityAndDatabasePriceAreReflectedInTotal() {
		Product dish = product(2L, "Bún", "40000.00", true);
		products.put(2L, dish);
		MockHttpSession session = new MockHttpSession();
		cartService.add(session, 2L, 2);

		cartService.updateQuantity(session, 2L, 4);
		when(dish.getPrice()).thenReturn(new BigDecimal("45000.00"));
		CartView cart = cartService.getCart(session);

		assertEquals(4, cart.items().getFirst().quantity());
		assertEquals(new BigDecimal("180000.00"), cart.total());
	}

	@Test
	void increaseAndDecreaseAdjustQuantityWithoutDroppingBelowOne() {
		products.put(3L, product(3L, "Cơm", "35000.00", true));
		MockHttpSession session = new MockHttpSession();
		cartService.add(session, 3L, 1);

		cartService.increase(session, 3L);
		cartService.decrease(session, 3L);
		assertEquals(1, cartService.getCart(session).items().getFirst().quantity());

		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> cartService.decrease(session, 3L));
		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
	}

	@Test
	void removesItemFromSessionCart() {
		products.put(4L, product(4L, "Gỏi", "30000.00", true));
		MockHttpSession session = new MockHttpSession();
		cartService.add(session, 4L, 1);

		cartService.remove(session, 4L);

		assertEquals(List.of(), cartService.getCart(session).items());
		assertEquals(BigDecimal.ZERO, cartService.getCart(session).total());
	}

	@Test
	void rejectsMissingAndUnavailableProductsWhenAddingOrUpdating() {
		MockHttpSession session = new MockHttpSession();
		ResponseStatusException missing = assertThrows(ResponseStatusException.class,
				() -> cartService.add(session, 404L, 1));
		assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());

		products.put(5L, product(5L, "Hết hàng", "20000.00", false));
		ResponseStatusException unavailable = assertThrows(ResponseStatusException.class,
				() -> cartService.add(session, 5L, 1));
		assertEquals(HttpStatus.CONFLICT, unavailable.getStatusCode());
		assertEquals(List.of(), cartService.getCart(session).items());
	}

	@Test
	void rechecksAvailabilityWhenUpdatingAnExistingCartItem() {
		Product dish = product(10L, "Món đang bán", "28000.00", true);
		products.put(10L, dish);
		MockHttpSession session = new MockHttpSession();
		cartService.add(session, 10L, 1);
		when(dish.isAvailable()).thenReturn(false);

		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> cartService.updateQuantity(session, 10L, 2));
		assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
	}

	@Test
	void rejectsInvalidAndExcessiveQuantities() {
		products.put(6L, product(6L, "Bánh", "10000.00", true));
		MockHttpSession session = new MockHttpSession();

		assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
				() -> cartService.add(session, 6L, 0)).getStatusCode());
		assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
				() -> cartService.add(session, 6L, 100)).getStatusCode());
		cartService.add(session, 6L, 99);
		assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
				() -> cartService.increase(session, 6L)).getStatusCode());
	}

	@Test
	void keepsCartsIsolatedBetweenSessions() {
		products.put(7L, product(7L, "Món A", "12000.00", true));
		products.put(8L, product(8L, "Món B", "18000.00", true));
		MockHttpSession firstSession = new MockHttpSession();
		MockHttpSession secondSession = new MockHttpSession();

		cartService.add(firstSession, 7L, 2);
		cartService.add(secondSession, 8L, 1);

		CartView firstCart = cartService.getCart(firstSession);
		CartView secondCart = cartService.getCart(secondSession);
		assertEquals(7L, firstCart.items().getFirst().productId());
		assertEquals(new BigDecimal("24000.00"), firstCart.total());
		assertEquals(8L, secondCart.items().getFirst().productId());
		assertEquals(new BigDecimal("18000.00"), secondCart.total());
	}

	@Test
	void removesItemsThatBecomeUnavailableBeforeCartIsViewed() {
		Product dish = product(9L, "Món cũ", "25000.00", true);
		products.put(9L, dish);
		MockHttpSession session = new MockHttpSession();
		cartService.add(session, 9L, 2);

		when(dish.isAvailable()).thenReturn(false);
		CartView cart = cartService.getCart(session);

		assertEquals(1, cart.removedUnavailableItems());
		assertEquals(List.of(), cart.items());
		assertEquals(BigDecimal.ZERO, cart.total());
	}

	private Product product(Long id, String name, String price, boolean available) {
		Product product = mock(Product.class);
		when(product.getId()).thenReturn(id);
		when(product.getName()).thenReturn(name);
		when(product.getImageUrl()).thenReturn(null);
		when(product.getPrice()).thenReturn(new BigDecimal(price));
		when(product.isAvailable()).thenReturn(available);
		when(product.getCategory()).thenReturn(null);
		return product;
	}
}
