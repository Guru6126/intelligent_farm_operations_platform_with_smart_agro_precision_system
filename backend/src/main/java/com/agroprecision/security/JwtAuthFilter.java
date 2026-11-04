package com.agroprecision.security;

import com.agroprecision.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;

    public JwtAuthFilter(JwtService jwt, UserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String userId = jwt.verify(header.substring(7));
            if (userId != null) {
                users.findById(userId).ifPresent(u -> {
                    Set<String> roles = u.getRoles().stream().map(r -> r.getRole()).collect(Collectors.toSet());
                    var principal = new AuthUser(u.getId(), u.getEmail(), roles);
                    var auth = new UsernamePasswordAuthenticationToken(principal, null,
                            roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase())).toList());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            }
        }
        chain.doFilter(req, res);
    }
}
