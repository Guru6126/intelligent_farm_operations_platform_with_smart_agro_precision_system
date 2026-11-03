package com.agroprecision.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

    public record SignupRequest(@Email @NotBlank String email, @NotBlank @Size(min = 8, max = 72) String password,
                                @NotBlank String fullName, String role, String inviteToken) {}

    public record ForgotRequest(@Email @NotBlank String email) {}

    public record ResetRequest(@NotBlank String token, @NotBlank @Size(min = 8, max = 72) String password) {}

    public record TokenResponse(String token) {}

    public record SessionResponse(String userId, String email, String fullName, List<String> roles) {}

    public record ApiMessage(String message) {}

    public record InviteRequest(String farmId) {}

    public record InvitePreview(String farmId, String farmName, String expiresAt) {}

    public record InviteCreated(String token, String inviteUrl, String expiresAt) {}

    public record UserSummary(String id, String email, String fullName, List<String> roles, String createdAt) {}

    public record RoleUpdateRequest(List<String> roles) {}
}
