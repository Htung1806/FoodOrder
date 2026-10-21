package food_ordering.controller.admin;

import food_ordering.service.AdminAccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/accounts")
public class AdminAccountController {

	private final AdminAccountService adminAccountService;

	public AdminAccountController(AdminAccountService adminAccountService) {
		this.adminAccountService = adminAccountService;
	}

	@GetMapping
	public String list(@RequestParam(required = false) String keyword, Model model) {
		model.addAttribute("accounts", adminAccountService.searchAccounts(keyword));
		model.addAttribute("keyword", keyword == null ? "" : keyword);
		return "admin/accounts/list";
	}
}
