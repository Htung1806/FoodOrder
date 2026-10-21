package food_ordering.service;

import food_ordering.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class AccountUserDetailsService implements UserDetailsService {

	private final UserAccountRepository userAccountRepository;
	private final UserDetails adminAccount;
	private final UserDetails developmentUser;

	public AccountUserDetailsService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
		this.userAccountRepository = userAccountRepository;
		this.adminAccount = User.withUsername("admin")
				.password(passwordEncoder.encode("12345"))
				.roles("ADMIN")
				.build();
		this.developmentUser = User.withUsername("user")
				.password(passwordEncoder.encode("user123"))
				.roles("USER")
				.build();
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		if (username.equalsIgnoreCase("admin")) {
			return adminAccount;
		}
		if (username.equalsIgnoreCase("user")) {
			return developmentUser;
		}
		return userAccountRepository.findByUsernameIgnoreCase(username)
				.map(account -> User.withUsername(account.getUsername())
						.password(account.getPasswordHash())
						.authorities(account.getRole())
						.build())
				.orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản."));
	}
}
