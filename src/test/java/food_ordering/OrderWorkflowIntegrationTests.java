package food_ordering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import food_ordering.entity.Category;
import food_ordering.entity.CustomerOrder;
import food_ordering.entity.OrderStatus;
import food_ordering.entity.OrderLine;
import food_ordering.entity.PaymentMethod;
import food_ordering.entity.Product;
import food_ordering.service.AdminDashboardService;
import food_ordering.dto.CheckoutForm;
import food_ordering.repository.CategoryRepository;
import food_ordering.repository.CustomerOrderRepository;
import food_ordering.repository.ProductRepository;
import food_ordering.repository.UserAccountRepository;
import food_ordering.service.CartService;
import food_ordering.service.CustomerOrderService;
import food_ordering.service.OrderPlacementException;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
@SpringBootTest
class OrderWorkflowIntegrationTests {

	private static final Pattern CHECKOUT_TOKEN_PATTERN =
			Pattern.compile("name=\"checkoutToken\" value=\"([^\"]+)\"");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CustomerOrderRepository customerOrderRepository;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private CartService cartService;

	@Autowired
	private CustomerOrderService customerOrderService;

	@Autowired
	private AdminDashboardService adminDashboardService;

	private Category category;
	private Product product;
	private String checkoutToken;
	private String registeredUsername;
	private final java.util.List<Long> dashboardOrderIds = new ArrayList<>();
	private final java.util.List<Long> testProductIds = new ArrayList<>();

	@BeforeEach
	void setUp() {
		category = categoryRepository.saveAndFlush(new Category("order-test-" + UUID.randomUUID(), null));
		product = productRepository.saveAndFlush(new Product("Món kiểm thử đặt hàng", null,
				new BigDecimal("12500.00"), null, category, true));
		testProductIds.add(product.getId());
	}

	@AfterEach
	void cleanUp() {
		for (Long orderId : dashboardOrderIds) {
			customerOrderRepository.deleteById(orderId);
		}
		dashboardOrderIds.clear();
		if (checkoutToken != null) {
			String orderUsername = registeredUsername == null ? "user" : registeredUsername;
			customerOrderRepository.findByCheckoutTokenAndCustomerUsername(checkoutToken, orderUsername)
					.ifPresent(customerOrderRepository::delete);
		}
		if (registeredUsername != null) {
			userAccountRepository.findByUsernameIgnoreCase(registeredUsername)
					.ifPresent(account -> userAccountRepository.deleteById(account.getId()));
		}
		for (Long productId : testProductIds) {
			productRepository.findById(productId).ifPresent(productRepository::delete);
		}
		testProductIds.clear();
		if (category != null && category.getId() != null) {
			categoryRepository.findById(category.getId()).ifPresent(categoryRepository::delete);
		}
	}

	@Test
	void checkoutRequiresAuthenticationAndShowsDatabasePricedItems() throws Exception {
		mockMvc.perform(get("/checkout"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));

		MockHttpSession session = addProductToCart();
		MvcResult checkout = mockMvc.perform(get("/checkout").session(session).with(user("user").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Xác nhận đơn hàng")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Món kiểm thử đặt hàng")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("12,500")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("COD")))
				.andReturn();

		checkoutToken = extractCheckoutToken(checkout.getResponse().getContentAsString());
		assertNotNull(checkoutToken);
	}

	@Test
	void registeredCustomerCanLogInPlaceCodOrderAndViewTheirOrder() throws Exception {
		registeredUsername = "journey" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
		String password = "journeyPass123";

		mockMvc.perform(post("/register")
				.with(csrf())
				.param("username", registeredUsername)
				.param("password", password)
				.param("confirmPassword", password))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));

		var account = userAccountRepository.findByUsernameIgnoreCase(registeredUsername).orElseThrow();
		assertTrue(passwordEncoder.matches(password, account.getPasswordHash()));
		assertEquals("ROLE_USER", account.getRole());

		MvcResult login = mockMvc.perform(post("/login")
				.with(csrf())
				.param("username", registeredUsername)
				.param("password", password))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/"))
				.andReturn();
		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
		assertNotNull(session);

		mockMvc.perform(post("/cart/items/" + product.getId())
				.session(session)
				.with(csrf())
				.param("quantity", "2"))
				.andExpect(status().is3xxRedirection());
		MvcResult checkout = mockMvc.perform(get("/checkout").session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("12,500")))
				.andReturn();
		String token = extractCheckoutToken(checkout.getResponse().getContentAsString());
		checkoutToken = token;

		MvcResult placement = mockMvc.perform(post("/checkout")
				.session(session)
				.with(csrf())
				.param("checkoutToken", token)
				.param("recipientName", "Khách kiểm thử")
				.param("phoneNumber", "0912345678")
				.param("deliveryAddress", "Địa chỉ kiểm thử hợp lệ")
				.param("paymentMethod", "COD")
				.param("unitPrice", "1")
				.param("totalAmount", "1"))
				.andExpect(status().is3xxRedirection())
				.andReturn();

		CustomerOrder order = customerOrderRepository
				.findByCheckoutTokenAndCustomerUsername(token, registeredUsername).orElseThrow();
		assertEquals(placement.getResponse().getRedirectedUrl(), "/orders/" + order.getId());
		assertEquals(new BigDecimal("25000.00"), order.getTotalAmount());
		assertEquals(registeredUsername, order.getCustomerUsername());
		assertEquals(OrderStatus.PENDING, order.getStatus());
		assertTrue(cartService.snapshotQuantities(session).isEmpty());

		mockMvc.perform(get("/orders").session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(order.getOrderNumber())));
		mockMvc.perform(get("/orders/" + order.getId()).session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(order.getOrderNumber())));
	}

	@Test
	void cartEndpointsValidateQuantityProductAvailabilityAndCsrf() throws Exception {
		MockHttpSession session = new MockHttpSession();
		mockMvc.perform(get("/checkout").session(session).with(user("user").roles("USER")))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/cart"));

		mockMvc.perform(post("/cart/items/" + product.getId())
				.with(user("user").roles("USER"))
				.param("quantity", "1"))
				.andExpect(status().isForbidden());
		assertTrue(cartService.snapshotQuantities(session).isEmpty());

		mockMvc.perform(post("/cart/items/" + product.getId())
				.session(session)
				.with(user("user").roles("USER"))
				.with(csrf())
				.param("quantity", "0"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/cart/items/" + Long.MAX_VALUE)
				.session(session)
				.with(user("user").roles("USER"))
				.with(csrf()))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/cart/items/" + product.getId())
				.session(session)
				.with(user("user").roles("USER"))
				.with(csrf())
				.param("quantity", "2"))
				.andExpect(status().is3xxRedirection());
		MockHttpSession populatedSession = (MockHttpSession) mockMvc.perform(get("/cart").session(session))
				.andExpect(status().isOk())
				.andReturn().getRequest().getSession(false);
		assertEquals(2, cartService.snapshotQuantities(populatedSession).get(product.getId()));

		mockMvc.perform(post("/cart/items/" + product.getId() + "/increase")
				.session(populatedSession)
				.with(csrf()))
				.andExpect(status().is3xxRedirection());
		mockMvc.perform(post("/cart/items/" + product.getId() + "/decrease")
				.session(populatedSession)
				.with(csrf()))
				.andExpect(status().is3xxRedirection());
		mockMvc.perform(post("/cart/items/" + product.getId() + "/quantity")
				.session(populatedSession)
				.with(csrf())
				.param("quantity", "4"))
				.andExpect(status().is3xxRedirection());
		assertEquals(4, cartService.snapshotQuantities(populatedSession).get(product.getId()));

		mockMvc.perform(post("/cart/items/" + product.getId() + "/quantity")
				.session(populatedSession)
				.with(csrf())
				.param("quantity", "100"))
				.andExpect(status().isBadRequest());
		assertEquals(4, cartService.snapshotQuantities(populatedSession).get(product.getId()));

		product.update(product.getName(), product.getDescription(), product.getPrice(),
				product.getImageUrl(), category, false);
		productRepository.saveAndFlush(product);
		mockMvc.perform(post("/cart/items/" + product.getId())
				.session(populatedSession)
				.with(csrf()))
				.andExpect(status().isConflict());
		product.update(product.getName(), product.getDescription(), product.getPrice(),
				product.getImageUrl(), category, true);
		productRepository.saveAndFlush(product);

		mockMvc.perform(post("/cart/items/" + product.getId() + "/remove")
				.session(populatedSession))
				.andExpect(status().isForbidden());
		assertEquals(4, cartService.snapshotQuantities(populatedSession).get(product.getId()));
		mockMvc.perform(post("/cart/items/" + product.getId() + "/remove")
				.session(populatedSession)
				.with(csrf()))
				.andExpect(status().is3xxRedirection());
		assertTrue(cartService.snapshotQuantities(populatedSession).isEmpty());
	}

	@Test
	void orderIsSavedWithDatabasePriceAndCartClearsAfterSuccess() throws Exception {
		MockHttpSession session = addProductToCart(2);
		String token = openCheckout(session);
		product.update(product.getName(), product.getDescription(), new BigDecimal("15000.00"),
				product.getImageUrl(), category, true);
		productRepository.saveAndFlush(product);

		MvcResult result = submitOrder(session, token, "Nguyễn An", "0912345678", "Số 1 đường Hoa Sen");
		assertTrue(result.getResponse().getRedirectedUrl().startsWith("/orders/"));
		checkoutToken = token;

		CustomerOrder order = customerOrderRepository
				.findByCheckoutTokenAndCustomerUsername(token, "user").orElseThrow();
		assertEquals("user", order.getCustomerUsername());
		assertEquals("Nguyễn An", order.getRecipientName());
		assertEquals(PaymentMethod.COD, order.getPaymentMethod());
		assertEquals(OrderStatus.PENDING, order.getStatus());
		assertEquals(new BigDecimal("30000.00"), order.getTotalAmount());
		assertEquals(1, order.getItems().size());
		assertEquals(2, order.getItems().getFirst().getQuantity());
		assertEquals(new BigDecimal("15000.00"), order.getItems().getFirst().getUnitPrice());
		assertEquals(new BigDecimal("30000.00"), order.getItems().getFirst().getLineTotal());
		assertTrue(cartService.snapshotQuantities(session).isEmpty());

		mockMvc.perform(get("/orders/" + order.getId()).session(session).with(user("user").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(order.getOrderNumber())))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Số 1 đường Hoa Sen")));
		mockMvc.perform(get("/orders/" + order.getId()).session(session).with(user("other").roles("USER")))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/orders").session(session).with(user("user").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(order.getOrderNumber())));
		mockMvc.perform(get("/admin/orders").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(order.getOrderNumber())));
		mockMvc.perform(get("/admin/orders/" + order.getId()).with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Cập nhật trạng thái")));

		mockMvc.perform(post("/admin/orders/" + order.getId() + "/status")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.param("status", "DELIVERING"))
				.andExpect(status().is3xxRedirection());
		assertEquals(OrderStatus.PENDING, customerOrderRepository.findById(order.getId()).orElseThrow().getStatus());

		mockMvc.perform(post("/admin/orders/" + order.getId() + "/status")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.param("status", "CONFIRMED"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/orders/" + order.getId()));
		assertEquals(OrderStatus.CONFIRMED, customerOrderRepository.findById(order.getId()).orElseThrow().getStatus());

		mockMvc.perform(post("/orders/" + order.getId() + "/cancel")
				.with(user("user").roles("USER"))
				.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/orders/" + order.getId()));
		assertEquals(OrderStatus.CONFIRMED, customerOrderRepository.findById(order.getId()).orElseThrow().getStatus());

		mockMvc.perform(post("/admin/orders/" + order.getId() + "/status")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.param("status", "DELIVERING"))
				.andExpect(status().is3xxRedirection());
		assertEquals(OrderStatus.DELIVERING, customerOrderRepository.findById(order.getId()).orElseThrow().getStatus());

		mockMvc.perform(post("/admin/orders/" + order.getId() + "/status")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.param("status", "COMPLETED"))
				.andExpect(status().is3xxRedirection());
		assertEquals(OrderStatus.COMPLETED, customerOrderRepository.findById(order.getId()).orElseThrow().getStatus());

		mockMvc.perform(post("/orders/" + order.getId() + "/cancel")
				.with(user("user").roles("USER"))
				.with(csrf()))
				.andExpect(status().is3xxRedirection());
		assertEquals(OrderStatus.COMPLETED, customerOrderRepository.findById(order.getId()).orElseThrow().getStatus());

		mockMvc.perform(post("/admin/orders/" + order.getId() + "/status")
				.with(user("admin").roles("ADMIN"))
				.param("status", "CANCELLED"))
				.andExpect(status().isForbidden());
		assertEquals(OrderStatus.COMPLETED, customerOrderRepository.findById(order.getId()).orElseThrow().getStatus());

		mockMvc.perform(post("/admin/orders/" + order.getId() + "/status")
				.with(user("user").roles("USER"))
				.with(csrf())
				.param("status", "CANCELLED"))
				.andExpect(status().isForbidden());
		assertEquals(OrderStatus.COMPLETED, customerOrderRepository.findById(order.getId()).orElseThrow().getStatus());

		mockMvc.perform(post("/admin/orders/" + order.getId() + "/status")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.param("status", "PENDING"))
				.andExpect(status().is3xxRedirection());
		assertEquals(OrderStatus.COMPLETED, customerOrderRepository.findById(order.getId()).orElseThrow().getStatus());
	}

	@Test
	void customerCanCancelOnlyTheirPendingOrderAndCsrfIsRequired() throws Exception {
		MockHttpSession session = addProductToCart();
		String token = openCheckout(session);
		assertEquals(302, submitOrder(session, token, "Nguyễn An", "0912345678",
				"Số 1 đường Hoa Sen").getResponse().getStatus());
		CustomerOrder order = customerOrderRepository
				.findByCheckoutTokenAndCustomerUsername(token, "user").orElseThrow();

		mockMvc.perform(get("/orders/" + order.getId()).with(user("user").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Hủy đơn hàng")));

		mockMvc.perform(post("/orders/" + order.getId() + "/cancel")
				.with(user("other").roles("USER"))
				.with(csrf()))
				.andExpect(status().isNotFound());
		assertEquals(OrderStatus.PENDING, customerOrderRepository.findById(order.getId()).orElseThrow().getStatus());

		mockMvc.perform(post("/orders/" + order.getId() + "/cancel")
				.with(user("user").roles("USER")))
				.andExpect(status().isForbidden());
		assertEquals(OrderStatus.PENDING, customerOrderRepository.findById(order.getId()).orElseThrow().getStatus());

		mockMvc.perform(post("/orders/" + order.getId() + "/cancel")
				.with(user("user").roles("USER"))
				.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/orders/" + order.getId()));
		assertEquals(OrderStatus.CANCELLED, customerOrderRepository.findById(order.getId()).orElseThrow().getStatus());

		mockMvc.perform(post("/orders/" + order.getId() + "/cancel")
				.with(user("user").roles("USER"))
				.with(csrf()))
				.andExpect(status().is3xxRedirection());
		assertEquals(OrderStatus.CANCELLED, customerOrderRepository.findById(order.getId()).orElseThrow().getStatus());
	}

	@Test
	void dashboardIsAdminOnly() throws Exception {
		mockMvc.perform(get("/admin/dashboard"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));

		mockMvc.perform(get("/admin/dashboard").with(user("user").roles("USER")))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/admin/dashboard").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Thống kê doanh thu")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"name=\"startDate\" value=\"" + LocalDate.now().withDayOfMonth(1) + "\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"name=\"endDate\" value=\"" + LocalDate.now() + "\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Đơn hàng gần đây")));
	}

	@Test
	void dashboardCountsAllOrdersButIncludesOnlyCompletedOrdersInRevenue() {
		LocalDate today = LocalDate.now();
		LocalDate startDate = today.minusDays(1);
		LocalDate endDate = today.plusDays(1);
		var before = adminDashboardService.getDashboard(startDate, endDate,
				food_ordering.service.RevenueGranularity.DAY);
		long beforePeriodOrders = before.revenuePoints().stream()
				.mapToLong(food_ordering.dto.RevenueChartPoint::orderCount).sum();
		BigDecimal beforePeriodRevenue = before.revenuePoints().stream()
				.map(food_ordering.dto.RevenueChartPoint::revenue)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		var beforeMonthly = adminDashboardService.getDashboard(startDate, endDate,
				food_ordering.service.RevenueGranularity.MONTH);
		long beforeMonthlyOrders = beforeMonthly.revenuePoints().stream()
				.mapToLong(food_ordering.dto.RevenueChartPoint::orderCount).sum();
		BigDecimal beforeMonthlyRevenue = beforeMonthly.revenuePoints().stream()
				.map(food_ordering.dto.RevenueChartPoint::revenue)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		CustomerOrder pending = saveDashboardOrder("Đơn chờ kiểm thử", 2, false);
		CustomerOrder completed = saveDashboardOrder("Đơn hoàn thành kiểm thử", 3, true);

		var after = adminDashboardService.getDashboard(startDate, endDate,
				food_ordering.service.RevenueGranularity.DAY);
		long afterPeriodOrders = after.revenuePoints().stream()
				.mapToLong(food_ordering.dto.RevenueChartPoint::orderCount).sum();
		BigDecimal afterPeriodRevenue = after.revenuePoints().stream()
				.map(food_ordering.dto.RevenueChartPoint::revenue)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		var afterMonthly = adminDashboardService.getDashboard(startDate, endDate,
				food_ordering.service.RevenueGranularity.MONTH);
		long afterMonthlyOrders = afterMonthly.revenuePoints().stream()
				.mapToLong(food_ordering.dto.RevenueChartPoint::orderCount).sum();
		BigDecimal afterMonthlyRevenue = afterMonthly.revenuePoints().stream()
				.map(food_ordering.dto.RevenueChartPoint::revenue)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		assertEquals(before.totalOrders() + 2, after.totalOrders());
		assertEquals(before.pendingOrders() + 1, after.pendingOrders());
		assertEquals(before.completedOrders() + 1, after.completedOrders());
		assertEquals(before.totalRevenue().add(completed.getTotalAmount()), after.totalRevenue());
		assertEquals(beforePeriodOrders + 2, afterPeriodOrders);
		assertEquals(beforePeriodRevenue.add(completed.getTotalAmount()), afterPeriodRevenue);
		assertEquals(beforeMonthlyOrders + 2, afterMonthlyOrders);
		assertEquals(beforeMonthlyRevenue.add(completed.getTotalAmount()), afterMonthlyRevenue);
		assertEquals(OrderStatus.PENDING, pending.getStatus());
	}

	@Test
	void rejectsUnavailableItemAndPreservesCart() throws Exception {
		MockHttpSession session = addProductToCart();
		String token = openCheckout(session);
		checkoutToken = token;
		product.update(product.getName(), product.getDescription(), product.getPrice(),
				product.getImageUrl(), category, false);
		productRepository.saveAndFlush(product);

		MvcResult rejectedOrder = submitOrder(session, token,
				"Nguyễn An", "0912345678", "Số 1 đường Hoa Sen");
		assertEquals(302, rejectedOrder.getResponse().getStatus());
		assertEquals("/checkout", rejectedOrder.getResponse().getRedirectedUrl());

		assertEquals(1, cartService.snapshotQuantities(session).get(product.getId()));
		assertFalse(customerOrderRepository.findByCheckoutTokenAndCustomerUsername(token, "user").isPresent());
	}

	@Test
	void unavailableProductInMultiItemCheckoutCreatesNoPartialOrder() throws Exception {
		Product secondProduct = productRepository.saveAndFlush(new Product("Món kiểm thử thứ hai", null,
				new BigDecimal("8000.00"), null, category, true));
		testProductIds.add(secondProduct.getId());
		MockHttpSession session = addProductToCart();
		mockMvc.perform(post("/cart/items/" + secondProduct.getId())
				.session(session)
				.with(user("user").roles("USER"))
				.with(csrf())
				.param("quantity", "2"))
				.andExpect(status().is3xxRedirection());

		String token = openCheckout(session);
		checkoutToken = token;
		secondProduct.update(secondProduct.getName(), secondProduct.getDescription(), secondProduct.getPrice(),
				secondProduct.getImageUrl(), category, false);
		productRepository.saveAndFlush(secondProduct);

		MvcResult result = submitOrder(session, token, "Nguyễn An", "0912345678", "Số 1 đường Hoa Sen");
		assertEquals("/checkout", result.getResponse().getRedirectedUrl());
		assertEquals(1, cartService.snapshotQuantities(session).get(product.getId()));
		assertEquals(2, cartService.snapshotQuantities(session).get(secondProduct.getId()));
		assertFalse(customerOrderRepository.findByCheckoutTokenAndCustomerUsername(token, "user").isPresent());
	}

	@Test
	void rejectsInvalidCheckoutDataWithoutCreatingOrderOrClearingCart() throws Exception {
		MockHttpSession session = addProductToCart();
		String token = openCheckout(session);
		checkoutToken = token;

		mockMvc.perform(post("/checkout")
				.session(session)
				.with(user("user").roles("USER"))
				.with(csrf())
				.param("checkoutToken", token)
				.param("recipientName", "")
				.param("phoneNumber", "bad")
				.param("deliveryAddress", "x")
				.param("paymentMethod", "COD"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Vui lòng nhập họ tên người nhận.")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Vui lòng nhập số điện thoại hợp lệ.")));

		assertEquals(1, cartService.snapshotQuantities(session).get(product.getId()));
		assertFalse(customerOrderRepository.findByCheckoutTokenAndCustomerUsername(token, "user").isPresent());
	}

	@Test
	void rejectsCheckoutWithoutCsrfAndKeepsCart() throws Exception {
		MockHttpSession session = addProductToCart();
		String token = openCheckout(session);
		checkoutToken = token;

		mockMvc.perform(post("/checkout")
				.session(session)
				.with(user("user").roles("USER"))
				.param("checkoutToken", token)
				.param("recipientName", "Nguyễn An")
				.param("phoneNumber", "0912345678")
				.param("deliveryAddress", "Số 1 đường Hoa Sen")
				.param("paymentMethod", "COD"))
				.andExpect(status().isForbidden());

		assertEquals(1, cartService.snapshotQuantities(session).get(product.getId()));
		assertFalse(customerOrderRepository.findByCheckoutTokenAndCustomerUsername(token, "user").isPresent());
	}

	@Test
	void rejectsDeletedProductsAndKeepsCart() throws Exception {
		MockHttpSession session = addProductToCart();
		String token = openCheckout(session);
		checkoutToken = token;
		productRepository.deleteById(product.getId());

		MvcResult result = submitOrder(session, token, "Nguyễn An", "0912345678", "Số 1 đường Hoa Sen");
		assertEquals(302, result.getResponse().getStatus());
		assertEquals("/checkout", result.getResponse().getRedirectedUrl());
		assertEquals(1, cartService.snapshotQuantities(session).get(product.getId()));
		assertFalse(customerOrderRepository.findByCheckoutTokenAndCustomerUsername(token, "user").isPresent());
	}

	@Test
	void orderServiceRejectsAnEmptyCart() {
		CheckoutForm form = new CheckoutForm();
		form.setRecipientName("Nguyễn An");
		form.setPhoneNumber("0912345678");
		form.setDeliveryAddress("Số 1 đường Hoa Sen");

		assertThrows(OrderPlacementException.class, () -> customerOrderService.placeOrder(
				"user", UUID.randomUUID().toString(), form, java.util.Map.of()));
	}

	@Test
	void repeatedCheckoutTokenDoesNotCreateAnotherOrder() throws Exception {
		MockHttpSession session = addProductToCart();
		String token = openCheckout(session);
		checkoutToken = token;

		assertEquals(302, submitOrder(session, token, "Nguyễn An", "0912345678", "Số 1 đường Hoa Sen")
				.getResponse().getStatus());
		assertEquals(302, submitOrder(session, token, "Nguyễn An", "0912345678", "Số 1 đường Hoa Sen")
				.getResponse().getStatus());

		assertEquals(1, customerOrderRepository.findAllByCustomerUsernameOrderByCreatedAtDesc("user").stream()
				.filter(order -> order.getCheckoutToken().equals(token)).count());
	}

	@Test
	void adminCanManageOrdersButRegularUserCannot() throws Exception {
		mockMvc.perform(get("/admin/orders"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));

		mockMvc.perform(get("/admin/orders").with(user("user").roles("USER")))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/admin/orders").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Quản lý đơn hàng")));
	}

	private MockHttpSession addProductToCart() throws Exception {
		return addProductToCart(1);
	}

	private MockHttpSession addProductToCart(int quantity) throws Exception {
		MvcResult result = mockMvc.perform(post("/cart/items/" + product.getId())
				.with(user("user").roles("USER"))
				.with(csrf())
				.param("quantity", Integer.toString(quantity)))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private String openCheckout(MockHttpSession session) throws Exception {
		MvcResult result = mockMvc.perform(get("/checkout").session(session).with(user("user").roles("USER")))
				.andExpect(status().isOk())
				.andReturn();
		String token = extractCheckoutToken(result.getResponse().getContentAsString());
		checkoutToken = token;
		return token;
	}

	private MvcResult submitOrder(MockHttpSession session, String token,
			String recipient, String phone, String address) throws Exception {
		return mockMvc.perform(post("/checkout")
				.session(session)
				.with(user("user").roles("USER"))
				.with(csrf())
				.param("checkoutToken", token)
				.param("recipientName", recipient)
				.param("phoneNumber", phone)
				.param("deliveryAddress", address)
				.param("note", "Gọi khi đến")
				.param("paymentMethod", "COD")
				.param("unitPrice", "1")
				.param("totalAmount", "1"))
				.andReturn();
	}

	private String extractCheckoutToken(String html) {
		Matcher matcher = CHECKOUT_TOKEN_PATTERN.matcher(html);
		assertTrue(matcher.find(), "Checkout page should render the one-time checkout token");
		return matcher.group(1);
	}

	private CustomerOrder saveDashboardOrder(String recipient, int quantity, boolean completed) {
		CustomerOrder order = new CustomerOrder(
				"FO-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(),
				UUID.randomUUID().toString(),
				"dashboard-test",
				recipient,
				"0912345678",
				"Địa chỉ kiểm thử",
				null,
				PaymentMethod.COD,
				null);
		order.addItem(new OrderLine(product.getId(), product.getName(), product.getImageUrl(),
				quantity, product.getPrice()));
		if (completed) {
			order.updateStatus(OrderStatus.CONFIRMED);
			order.updateStatus(OrderStatus.DELIVERING);
			order.updateStatus(OrderStatus.COMPLETED);
		}
		CustomerOrder saved = customerOrderRepository.saveAndFlush(order);
		dashboardOrderIds.add(saved.getId());
		return saved;
	}
}
