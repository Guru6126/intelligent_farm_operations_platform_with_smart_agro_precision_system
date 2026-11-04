package com.agroprecision.controller;

import com.agroprecision.dto.AuthDtos.*;
import com.agroprecision.security.AuthUser;
import com.agroprecision.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "ok"); }

    @PostMapping("/auth/signup")
    public TokenResponse signup(@Valid @RequestBody SignupRequest r) { return auth.signup(r); }

    @PostMapping("/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest r) { return auth.login(r); }

    @PostMapping("/auth/forgot-password")
    public ApiMessage forgot(@Valid @RequestBody ForgotRequest r) {
        auth.forgot(r);
        return new ApiMessage("If that email exists, a reset code has been issued.");
    }

    @PostMapping("/auth/reset-password")
    public ApiMessage reset(@Valid @RequestBody ResetRequest r) {
        auth.reset(r);
        return new ApiMessage("Password updated.");
    }

    @GetMapping("/auth/me")
    public SessionResponse me(@AuthenticationPrincipal AuthUser user) { return auth.me(user.id()); }
}
