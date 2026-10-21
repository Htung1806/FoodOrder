package food_ordering.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "food_categories")
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String name;

	@Column(length = 500)
	private String description;

	@OneToMany(mappedBy = "category")
	private List<Product> products = new ArrayList<>();

	protected Category() {
	}

	public Category(String name, String description) {
		update(name, description);
	}

	private static String normalizeDescription(String description) {
		return description == null || description.isBlank() ? null : description.trim();
	}

	public void update(String name, String description) {
		this.name = name.trim();
		this.description = normalizeDescription(description);
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public List<Product> getProducts() {
		return products;
	}
}
