package food_ordering.service;

import food_ordering.dto.CategoryForm;
import food_ordering.entity.Category;
import food_ordering.repository.CategoryRepository;
import food_ordering.repository.ProductRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class CategoryService {

	private final CategoryRepository categoryRepository;
	private final ProductRepository productRepository;

	public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
		this.categoryRepository = categoryRepository;
		this.productRepository = productRepository;
	}

	public List<Category> findAll() {
		return categoryRepository.findAll();
	}

	public Category findById(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục."));
	}

	public boolean nameExists(String name, Long excludingId) {
		return excludingId == null
				? categoryRepository.existsByNameIgnoreCase(name.trim())
				: categoryRepository.existsByNameIgnoreCaseAndIdNot(name.trim(), excludingId);
	}

	public boolean existsById(Long id) {
		return categoryRepository.existsById(id);
	}

	@Transactional
	public Category save(CategoryForm form) {
		if (form.getId() == null) {
			return categoryRepository.save(new Category(form.getName(), form.getDescription()));
		}

		Category category = findById(form.getId());
		category.update(form.getName(), form.getDescription());
		return categoryRepository.save(category);
	}

	@Transactional
	public boolean delete(Long id) {
		Category category = findById(id);
		if (productRepository.existsByCategory_Id(id)) {
			return false;
		}
		categoryRepository.delete(category);
		return true;
	}
}
