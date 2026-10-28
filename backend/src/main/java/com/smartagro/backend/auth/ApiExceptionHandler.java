package com.smartagro.backend.auth;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.smartagro.backend.farm.DataApiException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(EmailAlreadyRegisteredException.class)
	public ResponseEntity<ApiError> handleDuplicateEmail(
			EmailAlreadyRegisteredException exception,
			HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ApiError> handleBadCredentials(
			BadCredentialsException exception,
			HttpServletRequest request) {
		return error(HttpStatus.UNAUTHORIZED, exception.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler(DataApiException.class)
	public ResponseEntity<ApiError> handleDataApiException(
			DataApiException exception,
			HttpServletRequest request) {
		return error(exception.getStatus(), exception.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(
			MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		exception.getBindingResult().getFieldErrors().forEach(fieldError ->
				errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));
		ApiError response = new ApiError(
				Instant.now(),
				HttpStatus.BAD_REQUEST.value(),
				"Bad Request",
				"Request validation failed.",
				request.getRequestURI(),
				errors);
		return ResponseEntity.badRequest().body(response);
	}

	private ResponseEntity<ApiError> error(HttpStatus status, String message, String path) {
		return ResponseEntity.status(status).body(new ApiError(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				path,
				Map.of()));
	}

	public record ApiError(
			Instant timestamp,
			int status,
			String error,
			String message,
			String path,
			Map<String, String> validationErrors) {
	}
}
