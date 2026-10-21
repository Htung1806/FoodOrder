package food_ordering.dto;

import food_ordering.entity.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CheckoutForm {

	@NotBlank(message = "Vui lòng nhập họ tên người nhận.")
	@Size(min = 2, max = 100, message = "Họ tên phải có từ 2 đến 100 ký tự.")
	private String recipientName;

	@NotBlank(message = "Vui lòng nhập số điện thoại.")
	@Size(max = 30, message = "Số điện thoại không hợp lệ.")
	@Pattern(regexp = "^[0-9+().\\s-]{8,30}$", message = "Vui lòng nhập số điện thoại hợp lệ.")
	private String phoneNumber;

	@NotBlank(message = "Vui lòng nhập địa chỉ giao hàng.")
	@Size(min = 5, max = 500, message = "Địa chỉ phải có từ 5 đến 500 ký tự.")
	private String deliveryAddress;

	@Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự.")
	private String note;

	@NotNull(message = "Vui lòng chọn phương thức thanh toán.")
	private PaymentMethod paymentMethod = PaymentMethod.COD;

	public String getRecipientName() {
		return recipientName;
	}

	public void setRecipientName(String recipientName) {
		this.recipientName = recipientName;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getDeliveryAddress() {
		return deliveryAddress;
	}

	public void setDeliveryAddress(String deliveryAddress) {
		this.deliveryAddress = deliveryAddress;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(PaymentMethod paymentMethod) {
		this.paymentMethod = paymentMethod;
	}
}
