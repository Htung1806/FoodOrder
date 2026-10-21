package food_ordering.repository;

import food_ordering.entity.CustomerOrder;
import food_ordering.entity.OrderStatus;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

	interface RevenuePeriodProjection {
		String getPeriodKey();
		Long getOrderCount();
		BigDecimal getRevenue();
	}

	@EntityGraph(attributePaths = "items")
	Optional<CustomerOrder> findByCheckoutTokenAndCustomerUsername(String checkoutToken, String customerUsername);

	@EntityGraph(attributePaths = "items")
	Optional<CustomerOrder> findByIdAndCustomerUsername(Long id, String customerUsername);

	@EntityGraph(attributePaths = "items")
	List<CustomerOrder> findAllByCustomerUsernameOrderByCreatedAtDesc(String customerUsername);

	@EntityGraph(attributePaths = "items")
	List<CustomerOrder> findAllByOrderByCreatedAtDesc();

	List<CustomerOrder> findTop10ByOrderByCreatedAtDesc();

	long countByStatus(OrderStatus status);

	@Query("select coalesce(sum(o.totalAmount), 0) from CustomerOrder o where o.status = :status")
	BigDecimal sumTotalAmountByStatus(@Param("status") OrderStatus status);

	@Query(value = """
			SELECT DATE_FORMAT(created_at, :format) AS periodKey,
			       COUNT(*) AS orderCount,
			       COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN total_amount ELSE 0 END), 0) AS revenue
			FROM food_orders
			WHERE created_at >= :startAt AND created_at < :endExclusive
			GROUP BY DATE_FORMAT(created_at, :format)
			ORDER BY periodKey
			""", nativeQuery = true)
	List<RevenuePeriodProjection> summarizeByPeriod(
			@Param("startAt") java.time.Instant startAt,
			@Param("endExclusive") java.time.Instant endExclusive,
			@Param("format") String format);

	@EntityGraph(attributePaths = "items")
	@Override
	Optional<CustomerOrder> findById(Long id);
}
