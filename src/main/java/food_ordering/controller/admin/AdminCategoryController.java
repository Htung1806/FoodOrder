package food_ordering.controller.admin;

import food_ordering.dto.CategoryForm;
import food_ordering.service.CategoryService;
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
@RequestMapping("/admin/categories")
public class AdminCategoryController {

	private final CategoryService categoryService;

	public AdminCategoryController(CategoryService categoryService) {
		this.categoryService = categoryService;
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("categories", categoryService.findAll());
		return "admin/categories/list";
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		model.addAttribute("categoryForm", new CategoryForm());
		model.addAttribute("pageTitle", "Thêm danh mục");
		return "admin/categories/form";
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		var category = categoryService.findById(id);
		model.addAttribute("categoryForm",
				new CategoryForm(category.getId(), category.getName(), category.getDescription()));
		model.addAttribute("pageTitle", "Sửa danh mục");
		return "admin/categories/form";
	}

	@PostMapping("/save")
	public String save(@Valid @ModelAttribute("categoryForm") CategoryForm form,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (!bindingResult.hasFieldErrors("name")
				&& categoryService.nameExists(form.getName(), form.getId())) {
			bindingResult.rejectValue("name", "duplicate", "Tên danh mục này đã tồn tại.");
		}
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", form.getId() == null ? "Thêm danh mục" : "Sửa danh mục");
			return "admin/categories/form";
		}

		categoryService.save(form);
		redirectAttributes.addFlashAttribute("successMessage", "Đã lưu danh mục.");
		return "redirect:/admin/categories";
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		if (!categoryService.delete(id)) {
			redirectAttributes.addFlashAttribute("errorMessage",
					"Không thể xóa danh mục đang có món ăn. Hãy chuyển hoặc xóa các món trước.");
		} else {
			redirectAttributes.addFlashAttribute("successMessage", "Đã xóa danh mục.");
		}
		return "redirect:/admin/categories";
	}
}
