package com.smartagro.backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.HexFormat;
import java.time.LocalDateTime;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.smartagro.backend.auth.dto.AuthResponse;
import com.smartagro.backend.auth.dto.AuthUserResponse;
import com.smartagro.backend.auth.dto.ForgotPasswordRequest;
import com.smartagro.backend.auth.dto.LoginRequest;
import com.smartagro.backend.auth.dto.RegisterRequest;
import com.smartagro.backend.auth.dto.ResetPasswordRequest;
import com.smartagro.backend.user.AppUser;
import com.smartagro.backend.user.UserRepository;
import com.smartagro.backend.user.UserRole;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService jwtTokenService;
	private final JavaMailSender mailSender;
	private final String mailHost;
	private final String mailFrom;
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			JwtTokenService jwtTokenService,
			JavaMailSender mailSender,
			@Value("${spring.mail.host:}") String mailHost,
			@Value("${app.mail.from:}") String mailFrom) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenService = jwtTokenService;
		this.mailSender = mailSender;
		this.mailHost = mailHost == null ? "" : mailHost.trim();
		this.mailFrom = mailFrom == null ? "" : mailFrom.trim();
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

	@Transactional
	public void sendPasswordResetCode(ForgotPasswordRequest request) {
		userRepository.findByEmail(normalizeEmail(request.email())).ifPresent(user -> {
			if (mailHost.isBlank() || mailFrom.isBlank()) {
				throw new PasswordResetEmailUnavailableException();
			}

			byte[] tokenBytes = new byte[32];
			SECURE_RANDOM.nextBytes(tokenBytes);
			String token = HexFormat.of().formatHex(tokenBytes);

			user.setResetTokenHash(hashResetToken(token));
			user.setResetExpires(LocalDateTime.now().plusMinutes(30));
			userRepository.save(user);

			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(mailFrom);
			message.setTo(user.getEmail());
			message.setSubject("AgroPrecision password reset code");
			message.setText("Your password reset code is:\n\n" + token
					+ "\n\nEnter this code in AgroPrecision to set a new password. "
					+ "It expires in 30 minutes. If you did not request a reset, ignore this email.");
			try {
				mailSender.send(message);
			} catch (MailException exception) {
				throw new PasswordResetEmailUnavailableException(exception);
			}
		});
	}

	@Transactional
	public void resetPassword(ResetPasswordRequest request) {
		String tokenHash = hashResetToken(request.token().trim());
		AppUser user = userRepository.findByResetTokenHash(tokenHash)
				.filter(existing -> existing.getResetExpires() != null
						&& existing.getResetExpires().isAfter(LocalDateTime.now()))
				.orElseThrow(() -> new BadCredentialsException("Reset code is invalid or has expired."));
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setResetTokenHash(null);
		user.setResetExpires(null);
		userRepository.save(user);
	}

	private String hashResetToken(String token) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable.", exception);
		}
	}

	private AuthResponse createResponse(AppUser user) {
		String token = jwtTokenService.createToken(user);
		return new AuthResponse(
				token,
				token,
				"Bearer",
				jwtTokenService.getExpirationSeconds(),
				AuthUserResponse.from(user));
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
