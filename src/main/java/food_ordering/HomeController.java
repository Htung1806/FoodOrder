package food_ordering;

import food_ordering.service.CategoryService;
import food_ordering.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

	private final ProductService productService;
	private final CategoryService categoryService;

	public HomeController(ProductService productService, CategoryService categoryService) {
		this.productService = productService;
		this.categoryService = categoryService;
	}

	@GetMapping("/")
	public String home(@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Long categoryId, Model model) {
		model.addAttribute("products", productService.findAvailableForMenu(keyword, categoryId));
		model.addAttribute("categories", categoryService.findAll());
		model.addAttribute("keyword", keyword == null ? "" : keyword);
		model.addAttribute("selectedCategoryId", categoryId);
		return "index";
	}
}
