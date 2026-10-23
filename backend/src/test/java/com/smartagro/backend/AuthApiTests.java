package com.smartagro.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.smartagro.backend.user.AppUser;
import com.smartagro.backend.user.UserRepository;
import com.smartagro.backend.user.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
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

	@BeforeEach
	void clearUsers() {
		userRepository.deleteAll();
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
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.user.email").value(EMAIL))
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

	private void registerFarmer() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Test Farmer","email":"farmer@example.com","password":"FarmPass123!"}
								"""))
				.andExpect(status().isCreated());
	}
}
