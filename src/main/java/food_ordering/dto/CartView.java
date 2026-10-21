package food_ordering.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartView(
		List<CartItemView> items,
		BigDecimal total,
		int totalQuantity,
		int removedUnavailableItems) {
}
