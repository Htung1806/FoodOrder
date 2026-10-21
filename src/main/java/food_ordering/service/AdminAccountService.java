package food_ordering.service;

import food_ordering.dto.AdminAccountSummary;
import food_ordering.repository.UserAccountRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccountService {

	private final UserAccountRepository userAccountRepository;

	public AdminAccountService(UserAccountRepository userAccountRepository) {
		this.userAccountRepository = userAccountRepository;
	}

	@Transactional(readOnly = true)
	public List<AdminAccountSummary> searchAccounts(String keyword) {
		var accounts = keyword == null || keyword.isBlank()
				? userAccountRepository.findAllByOrderByUsernameAsc()
				: userAccountRepository.findByUsernameContainingIgnoreCaseOrderByUsernameAsc(keyword.trim());
		return accounts.stream()
				.map(account -> new AdminAccountSummary(account.getId(), account.getUsername(), account.getRole()))
				.toList();
	}
}
