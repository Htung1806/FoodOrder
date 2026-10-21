package food_ordering.controller;

import food_ordering.dto.RegistrationForm;
import food_ordering.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistrationController {

	private final RegistrationService registrationService;

	public RegistrationController(RegistrationService registrationService) {
		this.registrationService = registrationService;
	}

	@GetMapping("/register")
	public String registrationForm(Model model) {
		model.addAttribute("registrationForm", new RegistrationForm());
		return "register";
	}

	@PostMapping("/register")
	public String register(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (!bindingResult.hasFieldErrors("username")
				&& registrationService.usernameExists(form.getUsername())) {
			bindingResult.rejectValue("username", "duplicate", "Tên đăng nhập này đã được sử dụng.");
		}
		if (!bindingResult.hasFieldErrors("password") && !bindingResult.hasFieldErrors("confirmPassword")
				&& !form.getPassword().equals(form.getConfirmPassword())) {
			bindingResult.rejectValue("confirmPassword", "mismatch", "Mật khẩu nhập lại không khớp.");
		}
		if (bindingResult.hasErrors()) {
			return "register";
		}

		try {
			registrationService.register(form);
		} catch (RegistrationService.DuplicateUsernameException exception) {
			bindingResult.rejectValue("username", "duplicate", exception.getMessage());
			return "register";
		}
		redirectAttributes.addFlashAttribute("registrationSuccess",
				"Tạo tài khoản thành công. Vui lòng đăng nhập.");
		return "redirect:/login";
	}
}
