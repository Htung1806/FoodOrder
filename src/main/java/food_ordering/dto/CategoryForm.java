package food_ordering.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryForm {

	private Long id;

	@NotBlank(message = "Vui lòng nhập tên danh mục.")
	@Size(max = 100, message = "Tên danh mục không được vượt quá 100 ký tự.")
	private String name;

	@Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự.")
	private String description;

	public CategoryForm() {
	}

	public CategoryForm(Long id, String name, String description) {
		this.id = id;
		this.name = name;
		this.description = description;
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
}
