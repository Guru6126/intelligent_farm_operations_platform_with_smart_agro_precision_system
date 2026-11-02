package com.smartagro.backend.auth.dto;

public record AuthResponse(String accessToken, String token, String tokenType, long expiresIn, AuthUserResponse user) {
}
