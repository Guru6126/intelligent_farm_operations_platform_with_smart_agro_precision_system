package com.smartagro.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.smartagro.backend.user.AppUser;
import com.smartagro.backend.user.UserRepository;
import com.smartagro.backend.user.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
		"app.mail.from=noreply@example.com",
		"spring.mail.host=localhost"
})
class AuthApiTests {

	private static final String EMAIL = "farmer@example.com";
	private static final String PASSWORD = "FarmPass123!";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private JavaMailSender mailSender;

	@BeforeEach
	void clearUsers() {
		userRepository.deleteAll();
		reset(mailSender);
	}

	@Test
	void rootShowsPublicApiStatus() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.endpoints").isArray());
	}

	@Test
	void registerStoresHashedPasswordAndIssuesTokenForAuthenticatedRequests() throws Exception {
		MvcResult registration = mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Test Farmer","email":"FARMER@example.com","password":"FarmPass123!","role":"ADMIN"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.user.email").value(EMAIL))
				.andExpect(jsonPath("$.user.userId").isNotEmpty())
				.andExpect(jsonPath("$.user.fullName").value("Test Farmer"))
				.andExpect(jsonPath("$.user.roles[0]").value("farmer"))
				.andExpect(jsonPath("$.user.role").value("FARMER"))
				.andExpect(jsonPath("$.user.passwordHash").doesNotExist())
				.andReturn();

		JsonNode body = objectMapper.readTree(registration.getResponse().getContentAsString());
		String accessToken = body.get("accessToken").asText();
		AppUser savedUser = userRepository.findByEmail(EMAIL).orElseThrow();
		org.junit.jupiter.api.Assertions.assertNotEquals(PASSWORD, savedUser.getPasswordHash());
		org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(PASSWORD, savedUser.getPasswordHash()));

		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.role").value("FARMER"));

		mockMvc.perform(get("/api/admin/probe").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isForbidden());
	}

	@Test
	void frontendSignupContractReturnsTokenAcceptedByProtectedGisEndpoints() throws Exception {
		MvcResult registration = mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":"GIS Farmer","email":"gis@example.com","password":"FarmPass123!","role":"farmer"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.user.userId").isNotEmpty())
				.andExpect(jsonPath("$.user.fullName").value("GIS Farmer"))
				.andExpect(jsonPath("$.user.roles[0]").value("farmer"))
				.andReturn();

		String token = objectMapper.readTree(registration.getResponse().getContentAsString())
				.get("token").asText();
		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("gis@example.com"));
		mockMvc.perform(get("/api/data/farms").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"gis@example.com","password":"FarmPass123!"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty());
	}

	@Test
	void adminRoleCanAccessAdminPaths() throws Exception {
		userRepository.save(new AppUser(
				"Test Admin",
				"admin@example.com",
				passwordEncoder.encode("AdminPass123!"),
				UserRole.ADMIN));

		MvcResult login = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"admin@example.com","password":"AdminPass123!"}
								"""))
				.andExpect(status().isOk())
				.andReturn();
		String accessToken = objectMapper.readTree(login.getResponse().getContentAsString())
				.get("accessToken").asText();

		mockMvc.perform(get("/api/admin/probe").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isNotFound());
	}

	@Test
	void loginRejectsInvalidCredentialsAndAllowsValidCredentials() throws Exception {
		registerFarmer();

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"farmer@example.com","password":"WrongPass123!"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid email or password."));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"FARMER@example.com","password":"FarmPass123!"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.user.role").value("FARMER"));
	}

	@Test
	void registrationValidatesInputAndRejectsDuplicateEmail() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Test Farmer","email":"farmer@example.com","password":"short"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.validationErrors.password").exists());

		registerFarmer();

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Another Farmer","email":"FARMER@example.com","password":"FarmPass123!"}
								"""))
				.andExpect(status().isConflict());
	}

	@Test
	void protectedProfileRequiresBearerToken() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void forgotPasswordEmailsCodeToRegisteredAddressAndCodeResetsPasswordOnce() throws Exception {
		registerFarmer();

		mockMvc.perform(post("/api/auth/forgot-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"FARMER@example.com"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value(
						"If an account exists for that email, a reset code has been sent."));

		ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(messageCaptor.capture());
		SimpleMailMessage message = messageCaptor.getValue();
		org.junit.jupiter.api.Assertions.assertArrayEquals(new String[] { EMAIL }, message.getTo());

		Matcher tokenMatcher = Pattern.compile("[0-9a-f]{64}").matcher(message.getText());
		org.junit.jupiter.api.Assertions.assertTrue(tokenMatcher.find());
		String token = tokenMatcher.group();
		AppUser user = userRepository.findByEmail(EMAIL).orElseThrow();
		org.junit.jupiter.api.Assertions.assertNotEquals(token, user.getResetTokenHash());

		mockMvc.perform(post("/api/auth/reset-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(java.util.Map.of(
								"token", token,
								"password", "NewFarmPass123!"))))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"farmer@example.com","password":"NewFarmPass123!"}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/auth/reset-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(java.util.Map.of(
								"token", token,
								"password", "AnotherPass123!"))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void forgotPasswordDoesNotSendEmailForUnknownAddress() throws Exception {
		mockMvc.perform(post("/api/auth/forgot-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"unknown@example.com"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value(
						"If an account exists for that email, a reset code has been sent."));

		verifyNoInteractions(mailSender);
	}

	@Test
	void failedResetEmailReturnsServiceUnavailableAndDoesNotSaveCode() throws Exception {
		registerFarmer();
		doThrow(new MailSendException("SMTP unavailable"))
				.when(mailSender).send(any(SimpleMailMessage.class));

		mockMvc.perform(post("/api/auth/forgot-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"farmer@example.com"}
								"""))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.message").value(
						"Password reset email could not be sent. Please try again later."));

		AppUser user = userRepository.findByEmail(EMAIL).orElseThrow();
		org.junit.jupiter.api.Assertions.assertNull(user.getResetTokenHash());
		org.junit.jupiter.api.Assertions.assertNull(user.getResetExpires());
	}

	private void registerFarmer() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Test Farmer","email":"farmer@example.com","password":"FarmPass123!"}
								"""))
				.andExpect(status().isCreated());
	}
}
