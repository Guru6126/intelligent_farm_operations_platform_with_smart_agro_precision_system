package com.agroprecision.service;

import com.agroprecision.dto.AuthDtos.ForgotRequest;
import com.agroprecision.dto.AuthDtos.ResetRequest;
import com.agroprecision.entity.User;
import com.agroprecision.exception.ApiException;
import com.agroprecision.repository.UserRepository;
import com.agroprecision.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JwtService jwt = mock(JwtService.class);
    private final InviteService invites = mock(InviteService.class);
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final User user = new User();
    private AuthService auth;

    @BeforeEach
    void setUp() {
        user.setEmail("owner@example.com");
        when(users.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user));
        auth = new AuthService(users, encoder, jwt, "", invites, mailSender,
                "smtp.example.com", "noreply@example.com");
    }

    @Test
    void forgotSendsCodeAndStoresOnlyItsHash() throws Exception {
        auth.forgot(new ForgotRequest("owner@example.com"));

        var messageCaptor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertArrayEquals(new String[]{"owner@example.com"}, message.getTo());
        assertEquals("noreply@example.com", message.getFrom());

        String code = message.getText().replaceFirst("^Your password reset code is ", "")
                .replaceFirst("\\. It expires in 30 minutes\\.$", "");
        assertTrue(code.matches("[0-9a-f]{32}"));
        String expectedHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(code.getBytes(StandardCharsets.UTF_8)));
        assertEquals(expectedHash, user.getResetToken());
        assertNotEquals(code, user.getResetToken());
        assertNotNull(user.getResetExpires());
    }

    @Test
    void forgotReturnsServiceUnavailableWhenMailIsNotConfigured() {
        auth = new AuthService(users, encoder, jwt, "", invites, mailSender, "", "");

        ApiException error = assertThrows(ApiException.class,
                () -> auth.forgot(new ForgotRequest("owner@example.com")));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatus());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void forgotReturnsServiceUnavailableWhenDeliveryFails() {
        doThrow(new MailSendException("smtp unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        ApiException error = assertThrows(ApiException.class,
                () -> auth.forgot(new ForgotRequest("owner@example.com")));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatus());
    }

    @Test
    void forgotDoesNothingForUnknownEmail() {
        when(users.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> auth.forgot(new ForgotRequest("missing@example.com")));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void resetLooksUpHashedCodeAndConsumesIt() throws Exception {
        String code = "0123456789abcdef0123456789abcdef";
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(code.getBytes(StandardCharsets.UTF_8)));
        user.setResetToken(hash);
        user.setResetExpires(LocalDateTime.now().plusMinutes(10));
        when(users.findByResetToken(hash)).thenReturn(Optional.of(user));
        when(encoder.encode("new-password")).thenReturn("encoded-password");

        auth.reset(new ResetRequest(code, "new-password"));

        verify(users).findByResetToken(hash);
        assertEquals("encoded-password", user.getPasswordHash());
        assertNull(user.getResetToken());
        assertNull(user.getResetExpires());
    }
}
