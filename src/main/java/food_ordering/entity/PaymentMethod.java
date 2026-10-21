package food_ordering.entity;

public enum PaymentMethod {
	COD("Thanh toán khi nhận hàng");

	private final String label;

	PaymentMethod(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
