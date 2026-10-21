package food_ordering.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "food_orders", uniqueConstraints = {
		@UniqueConstraint(name = "uk_food_orders_number", columnNames = "order_number"),
		@UniqueConstraint(name = "uk_food_orders_checkout_token", columnNames = "checkout_token")
})
public class CustomerOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "order_number", nullable = false, length = 40)
	private String orderNumber;

	@Column(name = "checkout_token", nullable = false, length = 36)
	private String checkoutToken;

	@Column(name = "customer_username", nullable = false, length = 50)
	private String customerUsername;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_account_id")
	private UserAccount customerAccount;

	@Column(name = "recipient_name", nullable = false, length = 100)
	private String recipientName;

	@Column(name = "phone_number", nullable = false, length = 30)
	private String phoneNumber;

	@Column(name = "delivery_address", nullable = false, length = 500)
	private String deliveryAddress;

	@Column(length = 1000)
	private String note;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", nullable = false, length = 20)
	private PaymentMethod paymentMethod;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private OrderStatus status;

	@Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal totalAmount;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<OrderLine> items = new ArrayList<>();

	protected CustomerOrder() {
	}

	public CustomerOrder(String orderNumber, String checkoutToken, String customerUsername,
			String recipientName, String phoneNumber, String deliveryAddress, String note,
			PaymentMethod paymentMethod, UserAccount customerAccount) {
		this.orderNumber = orderNumber;
		this.checkoutToken = checkoutToken;
		this.customerUsername = customerUsername;
		this.customerAccount = customerAccount;
		this.recipientName = recipientName;
		this.phoneNumber = phoneNumber;
		this.deliveryAddress = deliveryAddress;
		this.note = note;
		this.paymentMethod = paymentMethod;
		this.status = OrderStatus.PENDING;
		this.totalAmount = BigDecimal.ZERO;
		this.createdAt = Instant.now();
	}

	public void addItem(OrderLine item) {
		items.add(item);
		item.setOrder(this);
		totalAmount = totalAmount.add(item.getLineTotal());
	}

	public void updateStatus(OrderStatus status) {
		if (!this.status.canTransitionTo(status)) {
			throw new IllegalArgumentException(
					"Không thể chuyển trạng thái đơn hàng từ " + this.status + " sang " + status + ".");
		}
		this.status = status;
	}

	public boolean canBeCancelledByCustomer() {
		return status == OrderStatus.PENDING;
	}

	public Long getId() {
		return id;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public String getCheckoutToken() {
		return checkoutToken;
	}

	public String getCustomerUsername() {
		return customerUsername;
	}

	public String getRecipientName() {
		return recipientName;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public String getDeliveryAddress() {
		return deliveryAddress;
	}

	public String getNote() {
		return note;
	}

	public PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public List<OrderLine> getItems() {
		return List.copyOf(items);
	}
}
