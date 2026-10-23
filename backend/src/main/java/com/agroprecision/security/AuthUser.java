package com.agroprecision.security;

import java.util.Set;

/** Authenticated principal stored in the Spring Security context. */
public record AuthUser(String id, String email, Set<String> roles) {
    public boolean isAdmin() { return roles.contains("admin"); }
    public boolean isFarmer() { return roles.contains("farmer"); }
}
