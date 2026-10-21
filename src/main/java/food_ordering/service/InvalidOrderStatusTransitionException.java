package food_ordering.service;

public class InvalidOrderStatusTransitionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidOrderStatusTransitionException(String message) {
		super(message);
	}
}
