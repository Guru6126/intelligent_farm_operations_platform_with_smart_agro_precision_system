package com.smartagro.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
		@NotBlank @Size(min = 64, max = 64) String token,
		@NotBlank @Size(min = 8, max = 72) String password) {
}
