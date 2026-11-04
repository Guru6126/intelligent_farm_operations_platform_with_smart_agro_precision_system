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
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final String adminEmail;
    private final InviteService inviteService;
    private final JavaMailSender mailSender;
    private final String mailHost;
    private final String mailFrom;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt,
                       @Value("${app.admin.email}") String adminEmail,
                       InviteService inviteService,
                       JavaMailSender mailSender,
                       @Value("${spring.mail.host:}") String mailHost,
                       @Value("${app.mail.from:}") String mailFrom) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.adminEmail = adminEmail == null ? "" : adminEmail.trim();
        this.inviteService = inviteService;
        this.mailSender = mailSender;
        this.mailHost = mailHost == null ? "" : mailHost.trim();
        this.mailFrom = mailFrom == null ? "" : mailFrom.trim();
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
        u.setFullName(r.fullName().trim());
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
            SECURE_RANDOM.nextBytes(b);
            String code = HexFormat.of().formatHex(b);
            u.setResetToken(hashResetToken(code));
            u.setResetExpires(LocalDateTime.now().plusMinutes(30));
            users.save(u);
            sendResetCode(u.getEmail(), code);
        });
    }

    @Transactional
    public void reset(ResetRequest r) {
        User u = users.findByResetToken(hashResetToken(r.token().trim()))
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

    private void sendResetCode(String email, String code) {
        if (mailHost.isBlank() || mailFrom.isBlank())
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Password reset email delivery is not configured. Contact the administrator.");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject("FarmVerse password reset code");
        message.setText("Your password reset code is " + code + ". It expires in 30 minutes.");
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Password reset email could not be sent. Please try again later.");
        }
    }

    private static String hashResetToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }
}
