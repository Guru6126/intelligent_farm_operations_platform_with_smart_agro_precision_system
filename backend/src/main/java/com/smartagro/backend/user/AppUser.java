package com.smartagro.backend.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_users")
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false, unique = true, length = 254)
	private String email;

	@Column(nullable = false)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserRole role;

	@Column(name = "reset_token_hash", length = 64)
	private String resetTokenHash;

	@Column(name = "reset_expires")
	private LocalDateTime resetExpires;

	protected AppUser() {
	}

	public AppUser(String name, String email, String passwordHash, UserRole role) {
		this.name = name;
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = role;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public UserRole getRole() {
		return role;
	}

	public String getResetTokenHash() {
		return resetTokenHash;
	}

	public void setResetTokenHash(String resetTokenHash) {
		this.resetTokenHash = resetTokenHash;
	}

	public LocalDateTime getResetExpires() {
		return resetExpires;
	}

	public void setResetExpires(LocalDateTime resetExpires) {
		this.resetExpires = resetExpires;
	}
}
