package food_ordering.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistrationForm {

	@NotBlank(message = "Vui lòng nhập tên đăng nhập.")
	@Size(min = 3, max = 50, message = "Tên đăng nhập phải có từ 3 đến 50 ký tự.")
	@Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Tên đăng nhập chỉ được gồm chữ cái không dấu, số, dấu chấm, gạch dưới hoặc gạch ngang.")
	private String username;

	@NotBlank(message = "Vui lòng nhập mật khẩu.")
	@Size(min = 8, max = 72, message = "Mật khẩu phải có từ 8 đến 72 ký tự.")
	private String password;

	@NotBlank(message = "Vui lòng nhập lại mật khẩu.")
	private String confirmPassword;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}
}
