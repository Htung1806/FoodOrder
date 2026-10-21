package food_ordering.service;

public class OrderPlacementException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public OrderPlacementException(String message) {
		super(message);
	}
}
