package com.smartagro.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smartagro.backend.auth.dto.AuthResponse;
import com.smartagro.backend.auth.dto.AuthUserResponse;
import com.smartagro.backend.auth.dto.ForgotPasswordRequest;
import com.smartagro.backend.auth.dto.LoginRequest;
import com.smartagro.backend.auth.dto.PasswordResetResponse;
import com.smartagro.backend.auth.dto.RegisterRequest;
import com.smartagro.backend.auth.dto.ResetPasswordRequest;
import com.smartagro.backend.auth.dto.SignupRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
		return authService.register(new RegisterRequest(request.fullName(), request.email(), request.password()));
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/forgot-password")
	public PasswordResetResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		authService.sendPasswordResetCode(request);
		return new PasswordResetResponse("If an account exists for that email, a reset code has been sent.");
	}

	@PostMapping("/reset-password")
	public PasswordResetResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		authService.resetPassword(request);
		return new PasswordResetResponse("Password updated. Please sign in with your new password.");
	}

	@GetMapping("/me")
	public AuthUserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
		return authService.getCurrentUser(jwt.getSubject());
	}
}
