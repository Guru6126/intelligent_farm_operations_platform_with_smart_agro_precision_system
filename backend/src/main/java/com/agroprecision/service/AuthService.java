package com.agroprecision.service;

import com.agroprecision.dto.AuthDtos.*;
import com.agroprecision.entity.User;
import com.agroprecision.entity.UserRole;
import com.agroprecision.exception.ApiException;
import com.agroprecision.repository.UserRepository;
import com.agroprecision.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final String adminEmail;
    private final InviteService inviteService;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt,
                       @Value("${app.admin.email}") String adminEmail,
                       InviteService inviteService) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.adminEmail = adminEmail == null ? "" : adminEmail.trim();
        this.inviteService = inviteService;
    }

    @Transactional
    public TokenResponse signup(SignupRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw ApiException.bad("An account with that email already exists.");

        boolean hasInvite = r.inviteToken() != null && !r.inviteToken().isBlank();
        String role = hasInvite ? "field_worker" : ("field_worker".equals(r.role()) ? "field_worker" : "farmer");

        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail(email);

        String fn = r.firstName() != null && !r.firstName().isBlank() ? r.firstName().trim() : null;
        String ln = r.lastName() != null && !r.lastName().isBlank() ? r.lastName().trim() : null;
        String computedFullName = r.fullName() != null && !r.fullName().isBlank() ? r.fullName().trim() :
                ((fn != null ? fn : "") + (ln != null ? " " + ln : "")).trim();
        if (computedFullName.isBlank()) {
            computedFullName = email.split("@")[0];
        }
        u.setFirstName(fn);
        u.setLastName(ln);
        u.setFullName(computedFullName);

        String p = r.phoneNumber() != null && !r.phoneNumber().isBlank() ? r.phoneNumber().trim() :
                (r.phone() != null && !r.phone().isBlank() ? r.phone().trim() : null);
        u.setPhone(p);
        u.setPhoneNumber(p);
        u.setGender(r.gender() != null && !r.gender().isBlank() ? r.gender().trim() : null);

        if (r.dateOfBirth() != null && !r.dateOfBirth().isBlank()) {
            try {
                u.setDateOfBirth(java.time.LocalDate.parse(r.dateOfBirth().trim()));
            } catch (Exception ex) {
                log.warn("Could not parse dateOfBirth: {}", r.dateOfBirth());
            }
        }

        u.setAddress(r.address() != null && !r.address().isBlank() ? r.address().trim() : null);
        u.setCity(r.city() != null && !r.city().isBlank() ? r.city().trim() : null);
        u.setState(r.state() != null && !r.state().isBlank() ? r.state().trim() : null);
        u.setPincode(r.pincode() != null && !r.pincode().isBlank() ? r.pincode().trim() : null);

        u.setPasswordHash(encoder.encode(r.password()));
        u.getRoles().add(new UserRole(UUID.randomUUID().toString(), u, role));
        if (!adminEmail.isEmpty() && adminEmail.equalsIgnoreCase(email))
            u.getRoles().add(new UserRole(UUID.randomUUID().toString(), u, "admin"));
        users.save(u);

        if (hasInvite) {
            try {
                inviteService.consumeInvite(r.inviteToken().trim(), u.getId());
            } catch (Exception ex) {
                log.warn("Failed to consume invite token on signup: {}", ex.getMessage());
            }
        }

        return new TokenResponse(jwt.issue(u.getId()));
    }

    public TokenResponse login(LoginRequest r) {
        User u = users.findByEmailIgnoreCase(r.email().trim())
                .filter(x -> encoder.matches(r.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));
        return new TokenResponse(jwt.issue(u.getId()));
    }

    @Transactional
    public void forgot(ForgotRequest r) {
        users.findByEmailIgnoreCase(r.email().trim()).ifPresent(u -> {
            byte[] b = new byte[16];
            new SecureRandom().nextBytes(b);
            u.setResetToken(HexFormat.of().formatHex(b));
            u.setResetExpires(LocalDateTime.now().plusMinutes(30));
            users.save(u);
            // No mail server is configured: the code is written to the backend log for local use.
            log.info("Password reset code for {}: {} (valid 30 minutes)", u.getEmail(), u.getResetToken());
        });
    }

    @Transactional
    public void reset(ResetRequest r) {
        User u = users.findByResetToken(r.token().trim())
                .filter(x -> x.getResetExpires() != null && x.getResetExpires().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> ApiException.bad("Reset code is invalid or has expired."));
        u.setPasswordHash(encoder.encode(r.password()));
        u.setResetToken(null);
        u.setResetExpires(null);
        users.save(u);
    }

    public SessionResponse me(String userId) {
        User u = users.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session expired."));
        return new SessionResponse(u.getId(), u.getEmail(), u.getFullName(),
                u.getRoles().stream().map(UserRole::getRole).sorted().toList());
    }
}
