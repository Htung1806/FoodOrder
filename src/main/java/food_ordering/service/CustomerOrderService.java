package food_ordering.service;

import food_ordering.dto.CheckoutForm;
import food_ordering.entity.CustomerOrder;
import food_ordering.entity.OrderLine;
import food_ordering.entity.OrderStatus;
import food_ordering.entity.PaymentMethod;
import food_ordering.entity.Product;
import food_ordering.repository.CustomerOrderRepository;
import food_ordering.repository.ProductRepository;
import food_ordering.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class CustomerOrderService {

	private final CustomerOrderRepository customerOrderRepository;
	private final ProductRepository productRepository;
	private final UserAccountRepository userAccountRepository;

	public CustomerOrderService(CustomerOrderRepository customerOrderRepository,
			ProductRepository productRepository, UserAccountRepository userAccountRepository) {
		this.customerOrderRepository = customerOrderRepository;
		this.productRepository = productRepository;
		this.userAccountRepository = userAccountRepository;
	}

	@Transactional
	public CustomerOrder placeOrder(String customerUsername, String checkoutToken,
			CheckoutForm form, Map<Long, Integer> cartQuantities) {
		var previousOrder = customerOrderRepository
				.findByCheckoutTokenAndCustomerUsername(checkoutToken, customerUsername);
		if (previousOrder.isPresent()) {
			return previousOrder.get();
		}
		if (cartQuantities.isEmpty()) {
			throw new OrderPlacementException("Giỏ hàng đang trống, chưa thể đặt hàng.");
		}
		if (form.getPaymentMethod() != PaymentMethod.COD) {
			throw new OrderPlacementException("Hiện chỉ hỗ trợ thanh toán khi nhận hàng.");
		}
		if (cartQuantities.entrySet().stream().anyMatch(entry ->
				entry.getKey() == null || entry.getValue() == null
						|| entry.getValue() < 1 || entry.getValue() > CartService.MAX_QUANTITY_PER_PRODUCT)) {
			throw new OrderPlacementException("Số lượng món trong giỏ không hợp lệ. Vui lòng kiểm tra lại giỏ hàng.");
		}

		List<Product> products = productRepository.findAllForUpdateByIdIn(cartQuantities.keySet());
		if (products.size() != cartQuantities.size()) {
			throw new OrderPlacementException("Một hoặc nhiều món không còn tồn tại. Vui lòng kiểm tra lại giỏ hàng.");
		}

		CustomerOrder order = new CustomerOrder(
				"FO-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(),
				checkoutToken,
				customerUsername,
				form.getRecipientName().trim(),
				form.getPhoneNumber().trim(),
				form.getDeliveryAddress().trim(),
				form.getNote() == null || form.getNote().isBlank() ? null : form.getNote().trim(),
				PaymentMethod.COD,
				userAccountRepository.findByUsernameIgnoreCase(customerUsername).orElse(null));

		for (Product product : products) {
			if (!product.isAvailable()) {
				throw new OrderPlacementException(
						"Món \"" + product.getName() + "\" hiện không còn bán. Vui lòng cập nhật giỏ hàng.");
			}
			int quantity = cartQuantities.get(product.getId());
			BigDecimal unitPrice = product.getPrice();
			order.addItem(new OrderLine(product.getId(), product.getName(), product.getImageUrl(),
					quantity, unitPrice));
		}

		return customerOrderRepository.saveAndFlush(order);
	}

	@Transactional(readOnly = true)
	public List<CustomerOrder> findCustomerOrders(String customerUsername) {
		return customerOrderRepository.findAllByCustomerUsernameOrderByCreatedAtDesc(customerUsername);
	}

	@Transactional(readOnly = true)
	public CustomerOrder findCustomerOrder(Long orderId, String customerUsername) {
		return customerOrderRepository.findByIdAndCustomerUsername(orderId, customerUsername)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng."));
	}

	@Transactional(readOnly = true)
	public List<CustomerOrder> findAllOrders() {
		return customerOrderRepository.findAllByOrderByCreatedAtDesc();
	}

	@Transactional(readOnly = true)
	public CustomerOrder findOrder(Long orderId) {
		return customerOrderRepository.findById(orderId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng."));
	}

	@Transactional
	public void updateStatus(Long orderId, OrderStatus status) {
		CustomerOrder order = customerOrderRepository.findById(orderId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng."));
		if (!order.getStatus().canTransitionTo(status)) {
			throw new InvalidOrderStatusTransitionException("Không thể cập nhật đơn "
					+ order.getOrderNumber() + " từ trạng thái " + order.getStatus().getLabel()
					+ " sang " + (status == null ? "không xác định" : status.getLabel()) + ".");
		}
		order.updateStatus(status);
	}

	@Transactional
	public void cancelCustomerOrder(Long orderId, String customerUsername) {
		CustomerOrder order = findCustomerOrder(orderId, customerUsername);
		if (!order.canBeCancelledByCustomer()) {
			throw new InvalidOrderStatusTransitionException(
					"Chỉ có thể hủy đơn hàng khi đơn còn ở trạng thái chờ xác nhận.");
		}
		order.updateStatus(OrderStatus.CANCELLED);
	}

	@Transactional(readOnly = true)
	public Optional<CustomerOrder> findByCheckoutToken(String checkoutToken, String customerUsername) {
		return customerOrderRepository.findByCheckoutTokenAndCustomerUsername(checkoutToken, customerUsername);
	}
}
