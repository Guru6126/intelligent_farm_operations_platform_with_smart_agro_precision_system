package com.smartagro.backend.auth.dto;

import com.smartagro.backend.user.AppUser;
import com.smartagro.backend.user.UserRole;
import java.util.List;

public record AuthUserResponse(
		Long id,
		String name,
		String email,
		UserRole role,
		String userId,
		String fullName,
		List<String> roles) {

	public static AuthUserResponse from(AppUser user) {
		String role = user.getRole().name().toLowerCase();
		return new AuthUserResponse(
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getRole(),
				user.getId().toString(),
				user.getName(),
				List.of(role));
	}
}
