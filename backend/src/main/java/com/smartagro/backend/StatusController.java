package com.smartagro.backend;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

	@GetMapping("/")
	public ApiStatus status() {
		return new ApiStatus(
				"Intelligent Farm Operations API",
				"UP",
				List.of(
						"POST /api/auth/register",
						"POST /api/auth/login",
						"GET /api/auth/me (Bearer token required)"));
	}

	public record ApiStatus(String service, String status, List<String> endpoints) {
	}
}
