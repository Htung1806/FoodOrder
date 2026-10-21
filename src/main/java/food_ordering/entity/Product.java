package food_ordering.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "food_products")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(length = 2000)
	private String description;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Column(name = "image_url", length = 1000)
	private String imageUrl;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@Column(nullable = false)
	private boolean available;

	protected Product() {
	}

	public Product(String name, String description, BigDecimal price, String imageUrl,
			Category category, boolean available) {
		update(name, description, price, imageUrl, category, available);
	}

	public void update(String name, String description, BigDecimal price, String imageUrl,
			Category category, boolean available) {
		this.name = name.trim();
		this.description = description == null || description.isBlank() ? null : description.trim();
		this.price = price;
		this.imageUrl = imageUrl == null || imageUrl.isBlank() ? null : imageUrl.trim();
		this.category = category;
		this.available = available;
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

	public BigDecimal getPrice() {
		return price;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public Category getCategory() {
		return category;
	}

	public boolean isAvailable() {
		return available;
	}
}
