package food_ordering.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import food_ordering.entity.UserAccount;
import food_ordering.repository.UserAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

class AccountUserDetailsServiceTests {

	@Test
	void defaultAdminTakesPrecedenceOverConflictingDatabaseAccount() {
		UserAccountRepository repository = mock(UserAccountRepository.class);
		var passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
		when(repository.findByUsernameIgnoreCase("admin"))
				.thenReturn(Optional.of(new UserAccount("admin", passwordEncoder.encode("database-password"))));

		AccountUserDetailsService service = new AccountUserDetailsService(repository, passwordEncoder);

		UserDetails admin = service.loadUserByUsername("AdMiN");

		assertTrue(admin.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
		verifyNoInteractions(repository);
	}
}
