package food_ordering.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class ProductForm {

	private Long id;

	@NotBlank(message = "Vui lòng nhập tên món.")
	@Size(max = 150, message = "Tên món không được vượt quá 150 ký tự.")
	private String name;

	@Size(max = 2000, message = "Mô tả không được vượt quá 2.000 ký tự.")
	private String description;

	@NotNull(message = "Vui lòng nhập giá món ăn.")
	@DecimalMin(value = "0.01", message = "Giá phải lớn hơn 0.")
	@Digits(integer = 10, fraction = 2, message = "Giá tối đa 10 chữ số nguyên và 2 chữ số thập phân.")
	private BigDecimal price;

	@Size(max = 1000, message = "Đường dẫn ảnh không được vượt quá 1.000 ký tự.")
	@Pattern(regexp = "^(https?://\\S+|/[^\\s]*)?$", message = "Ảnh phải dùng URL http(s) hoặc đường dẫn bắt đầu bằng /.")
	private String imageUrl;

	@NotNull(message = "Vui lòng chọn danh mục.")
	private Long categoryId;

	private boolean available;

	public ProductForm() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
	}

	public boolean isAvailable() {
		return available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}
}
