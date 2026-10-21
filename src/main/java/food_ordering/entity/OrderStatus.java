package food_ordering.entity;

import java.util.List;

public enum OrderStatus {
	PENDING("Chờ xác nhận"),
	CONFIRMED("Đã xác nhận"),
	DELIVERING("Đang giao"),
	COMPLETED("Hoàn thành"),
	CANCELLED("Đã hủy"),
	@Deprecated
	PREPARING("Đang chuẩn bị"),
	@Deprecated
	OUT_FOR_DELIVERY("Đang giao");

	private final String label;

	OrderStatus(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public List<OrderStatus> allowedNextStatuses() {
		return switch (this) {
			case PENDING -> List.of(CONFIRMED, CANCELLED);
			case CONFIRMED -> List.of(DELIVERING, CANCELLED);
			case DELIVERING -> List.of(COMPLETED);
			case COMPLETED, CANCELLED -> List.of();
			case PREPARING -> List.of(DELIVERING, CANCELLED);
			case OUT_FOR_DELIVERY -> List.of(COMPLETED);
		};
	}

	public boolean canTransitionTo(OrderStatus nextStatus) {
		return nextStatus != null && allowedNextStatuses().contains(nextStatus);
	}
}
