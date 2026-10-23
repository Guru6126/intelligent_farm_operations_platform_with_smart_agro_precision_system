package com.smartagro.backend.auth;

import java.util.Locale;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartagro.backend.auth.dto.AuthResponse;
import com.smartagro.backend.auth.dto.AuthUserResponse;
import com.smartagro.backend.auth.dto.LoginRequest;
import com.smartagro.backend.auth.dto.RegisterRequest;
import com.smartagro.backend.user.AppUser;
import com.smartagro.backend.user.UserRepository;
import com.smartagro.backend.user.UserRole;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService jwtTokenService;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			JwtTokenService jwtTokenService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenService = jwtTokenService;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new EmailAlreadyRegisteredException();
		}

		AppUser user = userRepository.save(new AppUser(
				request.name().trim(),
				email,
				passwordEncoder.encode(request.password()),
				UserRole.FARMER));
		return createResponse(user);
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());
		AppUser user = userRepository.findByEmail(email)
				.filter(existing -> passwordEncoder.matches(request.password(), existing.getPasswordHash()))
				.orElseThrow(() -> new BadCredentialsException("Invalid email or password."));
		return createResponse(user);
	}

	@Transactional(readOnly = true)
	public AuthUserResponse getCurrentUser(String email) {
		AppUser user = userRepository.findByEmail(normalizeEmail(email))
				.orElseThrow(() -> new BadCredentialsException("Invalid authentication token."));
		return AuthUserResponse.from(user);
	}

	private AuthResponse createResponse(AppUser user) {
		return new AuthResponse(
				jwtTokenService.createToken(user),
				"Bearer",
				jwtTokenService.getExpirationSeconds(),
				AuthUserResponse.from(user));
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
