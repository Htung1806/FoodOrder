package food_ordering.repository;

import food_ordering.entity.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

	boolean existsByUsernameIgnoreCase(String username);

	Optional<UserAccount> findByUsernameIgnoreCase(String username);

	List<UserAccount> findAllByOrderByUsernameAsc();

	List<UserAccount> findByUsernameContainingIgnoreCaseOrderByUsernameAsc(String username);
}
