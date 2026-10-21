package food_ordering.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "food_order_lines")
public class OrderLine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private CustomerOrder order;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "product_name", nullable = false, length = 150)
	private String productName;

	@Column(name = "product_image_url", length = 1000)
	private String productImageUrl;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Column(name = "line_total", nullable = false, precision = 14, scale = 2)
	private BigDecimal lineTotal;

	protected OrderLine() {
	}

	public OrderLine(Long productId, String productName, String productImageUrl,
			int quantity, BigDecimal unitPrice) {
		this.productId = productId;
		this.productName = productName;
		this.productImageUrl = productImageUrl;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
		this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
	}

	void setOrder(CustomerOrder order) {
		this.order = order;
	}

	public Long getId() {
		return id;
	}

	public Long getProductId() {
		return productId;
	}

	public String getProductName() {
		return productName;
	}

	public String getProductImageUrl() {
		return productImageUrl;
	}

	public int getQuantity() {
		return quantity;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public BigDecimal getLineTotal() {
		return lineTotal;
	}
}
