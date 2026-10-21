package food_ordering.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/", "/login", "/register", "/cart", "/cart/**", "/css/**", "/js/**", "/images/**").permitAll()
				.requestMatchers("/admin/**").hasRole("ADMIN")
				.requestMatchers("/checkout", "/orders/**").authenticated()
				.anyRequest().authenticated())
				.formLogin(form -> form
						.loginPage("/login")
						.defaultSuccessUrl("/", false)
						.failureUrl("/login?error"))
				.logout(logout -> logout.logoutSuccessUrl("/login?logout"));

		return http.build();
	}
}
