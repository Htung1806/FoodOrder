package food_ordering.service;

import food_ordering.dto.RegistrationForm;
import food_ordering.entity.UserAccount;
import food_ordering.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

	private final UserAccountRepository userAccountRepository;
	private final PasswordEncoder passwordEncoder;

	public RegistrationService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
		this.userAccountRepository = userAccountRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public boolean usernameExists(String username) {
		String normalizedUsername = username.trim();
		return normalizedUsername.equalsIgnoreCase("admin")
				|| normalizedUsername.equalsIgnoreCase("user")
				|| userAccountRepository.existsByUsernameIgnoreCase(normalizedUsername);
	}

	@Transactional
	public void register(RegistrationForm form) {
		String username = form.getUsername().trim();
		if (usernameExists(username)) {
			throw new DuplicateUsernameException();
		}

		try {
			userAccountRepository.saveAndFlush(
					new UserAccount(username, passwordEncoder.encode(form.getPassword())));
		} catch (DataIntegrityViolationException exception) {
			throw new DuplicateUsernameException(exception);
		}
	}

	public static class DuplicateUsernameException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public DuplicateUsernameException() {
			super("Tên đăng nhập này đã được sử dụng.");
		}

		public DuplicateUsernameException(Throwable cause) {
			super("Tên đăng nhập này đã được sử dụng.", cause);
		}
	}
}
