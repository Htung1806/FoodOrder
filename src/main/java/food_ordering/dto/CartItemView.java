package food_ordering.dto;

import java.math.BigDecimal;

public record CartItemView(
		Long productId,
		String name,
		String imageUrl,
		BigDecimal unitPrice,
		int quantity,
		BigDecimal lineTotal) {
}
