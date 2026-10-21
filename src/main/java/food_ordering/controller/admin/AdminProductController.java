package food_ordering.controller.admin;

import food_ordering.dto.ProductForm;
import food_ordering.service.CategoryService;
import food_ordering.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

	private final ProductService productService;
	private final CategoryService categoryService;

	public AdminProductController(ProductService productService, CategoryService categoryService) {
		this.productService = productService;
		this.categoryService = categoryService;
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("products", productService.findAll());
		return "admin/products/list";
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		model.addAttribute("productForm", new ProductForm());
		model.addAttribute("pageTitle", "Thêm món ăn");
		addCategories(model);
		return "admin/products/form";
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		var product = productService.findById(id);
		ProductForm form = new ProductForm();
		form.setId(product.getId());
		form.setName(product.getName());
		form.setDescription(product.getDescription());
		form.setPrice(product.getPrice());
		form.setImageUrl(product.getImageUrl());
		form.setCategoryId(product.getCategory().getId());
		form.setAvailable(product.isAvailable());
		model.addAttribute("productForm", form);
		model.addAttribute("pageTitle", "Sửa món ăn");
		addCategories(model);
		return "admin/products/form";
	}

	@PostMapping("/save")
	public String save(@Valid @ModelAttribute("productForm") ProductForm form,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (form.getCategoryId() != null && !categoryService.existsById(form.getCategoryId())) {
			bindingResult.rejectValue("categoryId", "invalid", "Danh mục đã chọn không tồn tại.");
		}
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", form.getId() == null ? "Thêm món ăn" : "Sửa món ăn");
			addCategories(model);
			return "admin/products/form";
		}

		productService.save(form);
		redirectAttributes.addFlashAttribute("successMessage", "Đã lưu món ăn.");
		return "redirect:/admin/products";
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		productService.delete(id);
		redirectAttributes.addFlashAttribute("successMessage", "Đã xóa món ăn.");
		return "redirect:/admin/products";
	}

	private void addCategories(Model model) {
		model.addAttribute("categories", categoryService.findAll());
	}
}
