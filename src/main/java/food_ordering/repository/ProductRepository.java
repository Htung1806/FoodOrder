package food_ordering.repository;

import food_ordering.entity.Product;
import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long> {

	List<Product> findAllByOrderByNameAsc();

	@Query("""
			select product
			from Product product
			join fetch product.category
			where product.available = true
				and (:keyword is null or lower(product.name) like lower(concat('%', :keyword, '%')))
				and (:categoryId is null or product.category.id = :categoryId)
			order by product.name asc
			""")
	List<Product> findAvailableForMenu(@Param("keyword") String keyword,
			@Param("categoryId") Long categoryId);

	boolean existsByCategory_Id(Long categoryId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select product from Product product where product.id in :ids")
	List<Product> findAllForUpdateByIdIn(@Param("ids") Collection<Long> ids);
}
