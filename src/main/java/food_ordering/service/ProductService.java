package food_ordering.service;

import food_ordering.dto.ProductForm;
import food_ordering.entity.Category;
import food_ordering.entity.Product;
import food_ordering.repository.CategoryRepository;
import food_ordering.repository.ProductRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ProductService {

	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;

	public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
	}

	public List<Product> findAll() {
		return productRepository.findAllByOrderByNameAsc();
	}

	public List<Product> findAvailableForMenu(String keyword, Long categoryId) {
		String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
		return productRepository.findAvailableForMenu(normalizedKeyword, categoryId);
	}

	public Product findById(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy món ăn."));
	}

	@Transactional
	public Product save(ProductForm form) {
		Category category = categoryRepository.findById(form.getCategoryId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục."));

		if (form.getId() == null) {
			return productRepository.save(new Product(form.getName(), form.getDescription(), form.getPrice(),
					form.getImageUrl(), category, form.isAvailable()));
		}

		Product product = findById(form.getId());
		product.update(form.getName(), form.getDescription(), form.getPrice(), form.getImageUrl(),
				category, form.isAvailable());
		return productRepository.save(product);
	}

	@Transactional
	public void delete(Long id) {
		productRepository.delete(findById(id));
	}
}
