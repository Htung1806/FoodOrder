package food_ordering;

import food_ordering.entity.UserAccount;
import food_ordering.repository.UserAccountRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class FoodOrderingApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void homePageIsPublicAndUsesFoodOrderBrand() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Food Order")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/css/home.css\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Trang chủ")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Khuyến mãi")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Đơn hàng")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Liên hệ")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"keyword\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"categoryId\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/login\">Đăng nhập</a>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("data-carousel-prev")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("data-carousel-next")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("data-carousel-dot=\"2\"")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("href=\"/admin/categories\""))))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("href=\"/admin/products\""))));
	}

	@Test
	void loginPageIsAvailable() throws Exception {
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Đăng nhập Food Order")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/css/home.css\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Đăng ký")));
	}

	@Test
	void loginFailureShowsVietnameseMessage() throws Exception {
		mockMvc.perform(post("/login")
				.with(SecurityMockMvcRequestPostProcessors.csrf())
				.param("username", "unknown_account")
				.param("password", "incorrect"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?error"));
		mockMvc.perform(get("/login").param("error", ""))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"Tên đăng nhập hoặc mật khẩu không đúng.")));
	}

	@Test
	void defaultAdminCanLogInAndAccessAdminPages() throws Exception {
		var result = mockMvc.perform(post("/login")
				.with(SecurityMockMvcRequestPostProcessors.csrf())
				.param("username", "admin")
				.param("password", "12345"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		mockMvc.perform(get("/admin/dashboard")
				.session((org.springframework.mock.web.MockHttpSession) result.getRequest().getSession(false)))
				.andExpect(status().isOk());
	}

	@Test
	void registrationPageIsAvailableAndUsesSharedHeader() throws Exception {
		mockMvc.perform(get("/register"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Tạo tài khoản")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/css/home.css\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/cart\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/login\">Đăng nhập</a>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/register\">Đăng ký</a>")));
	}

	@Test
	void registrationStoresEncodedPasswordAndOnlyUserRole() throws Exception {
		String username = "test" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
		try {
			mockMvc.perform(post("/register")
					.with(SecurityMockMvcRequestPostProcessors.csrf())
					.param("username", username)
					.param("password", "securePass123")
					.param("confirmPassword", "securePass123"))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrl("/login"))
					.andExpect(flash().attribute("registrationSuccess",
							"Tạo tài khoản thành công. Vui lòng đăng nhập."));

			UserAccount account = userAccountRepository.findByUsernameIgnoreCase(username).orElseThrow();
			assertNotNull(account.getId());
			assertNotEquals("securePass123", account.getPasswordHash());
			assertTrue(passwordEncoder.matches("securePass123", account.getPasswordHash()));
			assertTrue(account.getRole().equals("ROLE_USER"));
			assertFalse(account.getRole().equals("ROLE_ADMIN"));
			assertFalse(userDetailsService.loadUserByUsername(username).getAuthorities().stream()
					.anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
		} finally {
			userAccountRepository.findByUsernameIgnoreCase(username)
					.ifPresent(account -> userAccountRepository.deleteById(account.getId()));
		}
	}

	@Test
	void registrationRejectsReservedOrDuplicateUsername() throws Exception {
		mockMvc.perform(post("/register")
				.with(SecurityMockMvcRequestPostProcessors.csrf())
				.param("username", "admin")
				.param("password", "securePass123")
				.param("confirmPassword", "securePass123"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Tên đăng nhập này đã được sử dụng.")));
	}

	@Test
	void registrationRejectsMismatchedPasswordsAndInvalidFields() throws Exception {
		mockMvc.perform(post("/register")
				.with(SecurityMockMvcRequestPostProcessors.csrf())
				.param("username", "valid_name")
				.param("password", "securePass123")
				.param("confirmPassword", "differentPass123"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Mật khẩu nhập lại không khớp.")));

		mockMvc.perform(post("/register")
				.with(SecurityMockMvcRequestPostProcessors.csrf())
				.param("username", "x")
				.param("password", "short")
				.param("confirmPassword", "short"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Tên đăng nhập phải có từ 3 đến 50 ký tự.")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Mật khẩu phải có từ 8 đến 72 ký tự.")));
	}

	@Test
	void registrationRequiresCsrfToken() throws Exception {
		mockMvc.perform(post("/register")
				.param("username", "csrf_test")
				.param("password", "securePass123")
				.param("confirmPassword", "securePass123"))
				.andExpect(status().isForbidden());
	}

	@Test
	void accountManagementIsAdminOnlyAndSearchNeverRendersPasswordHash() throws Exception {
		String username = "account" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		String passwordHash = passwordEncoder.encode("accountTestPass123");
		try {
			UserAccount account = userAccountRepository.saveAndFlush(new UserAccount(username, passwordHash));

			mockMvc.perform(get("/admin/accounts"))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrl("/login"));
			mockMvc.perform(get("/admin/accounts").with(
					SecurityMockMvcRequestPostProcessors.user("user").roles("USER")))
					.andExpect(status().isForbidden());

			String result = mockMvc.perform(get("/admin/accounts")
					.with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
					.param("keyword", username.substring(0, 10)))
					.andExpect(status().isOk())
					.andExpect(content().string(org.hamcrest.Matchers.containsString(username)))
					.andExpect(content().string(org.hamcrest.Matchers.containsString("Khách hàng")))
					.andExpect(content().string(org.hamcrest.Matchers.not(
							org.hamcrest.Matchers.containsString(passwordHash))))
					.andReturn().getResponse().getContentAsString();

			org.junit.jupiter.api.Assertions.assertFalse(result.contains("name=\"passwordHash\""));
			org.junit.jupiter.api.Assertions.assertFalse(result.contains("name=\"password\""));
			assertTrue(account.getRole().equals("ROLE_USER"));
		} finally {
			userAccountRepository.findByUsernameIgnoreCase(username)
					.ifPresent(saved -> userAccountRepository.deleteById(saved.getId()));
		}
	}

	@Test
	void menuSearchAndCategoryFilterArePublic() throws Exception {
		mockMvc.perform(get("/").param("keyword", "món thử").param("categoryId", "1"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"món thử\"")));
	}

	@Test
	void cartIsAvailableWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/cart"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Giỏ hàng của bạn")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/css/home.css\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/login\">Đăng nhập</a>")));
	}

	@Test
	void adminPagesRequireAuthentication() throws Exception {
		mockMvc.perform(get("/admin/categories"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));

		mockMvc.perform(get("/admin/products"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	@WithUserDetails("admin")
	void adminCanOpenAdminPagesAndSeesAdminMenu() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/admin/categories\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/admin/products\"")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("href=\"/login\">Đăng nhập</a>"))))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("<strong>admin</strong>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/logout\" method=\"post\"")));
		mockMvc.perform(get("/admin/categories"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/css/home.css\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/#trang-chu\">Trang chủ</a>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/#danh-muc\">Thực đơn</a>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/cart\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/admin/categories\">Quản lý danh mục</a>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/admin/products\">Quản lý món ăn</a>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/logout\" method=\"post\"")));
		mockMvc.perform(get("/admin/categories/new"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/css/home.css\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/cart\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/#khuyen-mai\">Khuyến mãi</a>")));
		mockMvc.perform(get("/admin/products"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/css/home.css\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/admin/categories\">Quản lý danh mục</a>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/admin/products\">Quản lý món ăn</a>")));
		mockMvc.perform(get("/admin/products/new"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/css/home.css\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/cart\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/#khuyen-mai\">Khuyến mãi</a>")));
	}

	@Test
	@WithUserDetails("user")
	void regularUserCannotOpenAdminPagesOrSeeAdminMenu() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("href=\"/admin/categories\""))))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("href=\"/admin/products\""))))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("href=\"/login\">Đăng nhập</a>"))))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("<strong>user</strong>")));
		mockMvc.perform(get("/admin/categories")).andExpect(status().isForbidden());
		mockMvc.perform(get("/admin/products")).andExpect(status().isForbidden());
	}

	@Test
	void configuredDevelopmentAccountsUseEncodedPasswordsAndSeparateRoles() {
		UserDetails admin = userDetailsService.loadUserByUsername("admin");
		UserDetails user = userDetailsService.loadUserByUsername("user");

		assertTrue(passwordEncoder.matches("12345", admin.getPassword()));
		assertTrue(admin.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
		assertFalse(user.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
		assertTrue(user.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));
		assertTrue(user.getPassword().startsWith("{bcrypt}"));
	}
}
