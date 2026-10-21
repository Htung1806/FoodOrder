package food_ordering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import food_ordering.entity.Category;
import food_ordering.entity.CustomerOrder;
import food_ordering.entity.OrderLine;
import food_ordering.entity.PaymentMethod;
import food_ordering.entity.Product;
import food_ordering.repository.CategoryRepository;
import food_ordering.repository.CustomerOrderRepository;
import food_ordering.repository.ProductRepository;
import food_ordering.service.CartService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@SpringBootTest
class AdminCatalogCrudIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CustomerOrderRepository customerOrderRepository;

	@Autowired
	private CartService cartService;

	private final List<Long> testCategoryIds = new ArrayList<>();
	private final List<Long> testProductIds = new ArrayList<>();
	private final List<Long> testOrderIds = new ArrayList<>();
	private Category category;

	@BeforeEach
	void setUp() {
		category = categoryRepository.saveAndFlush(
				new Category("crud-test-" + UUID.randomUUID(), "Danh mục kiểm thử"));
		testCategoryIds.add(category.getId());
	}

	@AfterEach
	void cleanUp() {
		for (Long orderId : testOrderIds) {
			customerOrderRepository.deleteById(orderId);
		}
		for (Long productId : testProductIds) {
			productRepository.findById(productId).ifPresent(productRepository::delete);
		}
		for (Long categoryId : testCategoryIds) {
			categoryRepository.findById(categoryId).ifPresent(categoryRepository::delete);
		}
	}

	@Test
	void adminCanCreateListEditAndDeleteProductWithoutLosingCartOrOrderSnapshot() throws Exception {
		String originalName = "Product CRUD " + UUID.randomUUID();
		String updatedName = originalName + " đã sửa";

		mockMvc.perform(get("/admin/products/new").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Thêm món ăn")));

		mockMvc.perform(post("/admin/products/save")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.param("name", originalName)
				.param("description", "Mô tả ban đầu")
				.param("price", "12500.00")
				.param("categoryId", category.getId().toString())
				.param("available", "true"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/products"));

		Product product = productRepository.findAllByOrderByNameAsc().stream()
				.filter(candidate -> candidate.getName().equals(originalName))
				.findFirst().orElseThrow();
		testProductIds.add(product.getId());

		mockMvc.perform(get("/admin/products").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(originalName)));
		mockMvc.perform(get("/admin/products/" + product.getId() + "/edit")
				.with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(originalName)));

		mockMvc.perform(post("/admin/products/save")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.param("id", product.getId().toString())
				.param("name", updatedName)
				.param("description", "Mô tả đã cập nhật")
				.param("price", "15000.00")
				.param("imageUrl", "/images/updated-product.svg")
				.param("categoryId", category.getId().toString())
				.param("available", "true"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/products"));

		Product updated = productRepository.findById(product.getId()).orElseThrow();
		assertEquals(updatedName, updated.getName());
		assertEquals("Mô tả đã cập nhật", updated.getDescription());
		assertEquals(new BigDecimal("15000.00"), updated.getPrice());
		assertEquals("/images/updated-product.svg", updated.getImageUrl());

		MockHttpSession session = new MockHttpSession();
		cartService.add(session, product.getId(), 2);
		assertEquals(new BigDecimal("30000.00"), cartService.getCart(session).total());

		CustomerOrder previousOrder = new CustomerOrder(
				"FO-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(),
				UUID.randomUUID().toString(),
				"crud-test-customer",
				"Khách kiểm thử",
				"0912345678",
				"Địa chỉ kiểm thử",
				null,
				PaymentMethod.COD,
				null);
		previousOrder.addItem(new OrderLine(product.getId(), originalName, null, 1,
				new BigDecimal("12500.00")));
		CustomerOrder savedOrder = customerOrderRepository.saveAndFlush(previousOrder);
		testOrderIds.add(savedOrder.getId());

		mockMvc.perform(post("/admin/products/" + product.getId() + "/delete")
				.with(user("admin").roles("ADMIN"))
				.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/products"));

		assertFalse(productRepository.existsById(product.getId()));
		var cartAfterDelete = cartService.getCart(session);
		assertTrue(cartAfterDelete.items().isEmpty());
		assertEquals(1, cartAfterDelete.removedUnavailableItems());

		CustomerOrder persistedOrder = customerOrderRepository.findById(savedOrder.getId()).orElseThrow();
		assertEquals(originalName, persistedOrder.getItems().getFirst().getProductName());
		assertEquals(new BigDecimal("12500.00"), persistedOrder.getItems().getFirst().getUnitPrice());
	}

	@Test
	void adminCanCreateEditAndDeleteCategoryAndCannotDeleteCategoryWithProducts() throws Exception {
		String originalName = "Category CRUD " + UUID.randomUUID();
		String updatedName = originalName + " đã sửa";

		mockMvc.perform(get("/admin/categories/new").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Thêm danh mục")));
		mockMvc.perform(post("/admin/categories/save")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.param("name", originalName)
				.param("description", "Mô tả danh mục"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/categories"));

		Category created = categoryRepository.findAll().stream()
				.filter(candidate -> candidate.getName().equals(originalName))
				.findFirst().orElseThrow();
		testCategoryIds.add(created.getId());

		mockMvc.perform(get("/admin/categories").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(originalName)));
		mockMvc.perform(get("/admin/categories/" + created.getId() + "/edit")
				.with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(originalName)));

		mockMvc.perform(post("/admin/categories/save")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.param("id", created.getId().toString())
				.param("name", updatedName)
				.param("description", "Mô tả đã cập nhật"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/categories"));
		assertEquals(updatedName, categoryRepository.findById(created.getId()).orElseThrow().getName());

		Product linkedProduct = productRepository.saveAndFlush(new Product(
				"Sản phẩm liên kết " + UUID.randomUUID(), null, new BigDecimal("10000.00"),
				null, created, true));
		testProductIds.add(linkedProduct.getId());
		mockMvc.perform(post("/admin/categories/" + created.getId() + "/delete")
				.with(user("admin").roles("ADMIN"))
				.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/categories"))
				.andExpect(flash().attributeExists("errorMessage"));
		assertTrue(categoryRepository.existsById(created.getId()));
		assertTrue(productRepository.existsById(linkedProduct.getId()));

		productRepository.deleteById(linkedProduct.getId());
		testProductIds.remove(linkedProduct.getId());
		mockMvc.perform(post("/admin/categories/" + created.getId() + "/delete")
				.with(user("admin").roles("ADMIN"))
				.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/categories"));
		assertFalse(categoryRepository.existsById(created.getId()));
		testCategoryIds.remove(created.getId());
	}

	@Test
	void catalogFormsRejectInvalidDataAndOnlyAdminCanWriteWithCsrf() throws Exception {
		mockMvc.perform(post("/admin/products/save")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.param("name", "")
				.param("description", "Mô tả")
				.param("price", "-1")
				.param("imageUrl", "javascript:alert(1)")
				.param("categoryId", category.getId().toString()))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Vui lòng nhập tên món.")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Giá phải lớn hơn 0.")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Ảnh phải dùng URL")));

		mockMvc.perform(post("/admin/products/save")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.param("name", "Không có danh mục")
				.param("price", "1000")
				.param("categoryId", Long.toString(Long.MAX_VALUE)))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Danh mục đã chọn không tồn tại.")));

		mockMvc.perform(post("/admin/categories/save")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.param("name", " "))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Vui lòng nhập tên danh mục.")));

		mockMvc.perform(post("/admin/categories/save")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.param("name", category.getName()))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Tên danh mục này đã tồn tại.")));

		mockMvc.perform(post("/admin/categories/save")
				.with(user("user").roles("USER"))
				.with(csrf())
				.param("name", "Không được phép"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/admin/products/save")
				.with(user("user").roles("USER"))
				.with(csrf())
				.param("name", "Không được phép"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/admin/categories/save")
				.with(user("admin").roles("ADMIN"))
				.param("name", "Thiếu CSRF"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/admin/products/" + Long.MAX_VALUE + "/delete")
				.with(user("admin").roles("ADMIN"))
				.with(csrf()))
				.andExpect(status().isNotFound());
	}
}
