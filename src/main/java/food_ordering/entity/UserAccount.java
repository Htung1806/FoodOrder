package food_ordering.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "food_user_accounts", uniqueConstraints = {
		@UniqueConstraint(name = "uk_food_user_accounts_username", columnNames = "username")
})
public class UserAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String username;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(nullable = false, length = 20)
	private String role;

	protected UserAccount() {
	}

	public UserAccount(String username, String passwordHash) {
		this.username = username;
		this.passwordHash = passwordHash;
		this.role = "ROLE_USER";
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getRole() {
		return role;
	}
}
