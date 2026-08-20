package com.portfoliotracker.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portfoliotracker.backend.security.JwtService;
import com.portfoliotracker.backend.user.User;
import com.portfoliotracker.backend.user.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthApiIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
			.withDatabaseName("auth_api_test")
			.withUsername("auth_api_test")
			.withPassword("auth_api_test");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@DynamicPropertySource
	static void postgresProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("jwt.secret", () -> "auth-test-secret");
	}

	@BeforeEach
	void deleteUsers() {
		userRepository.deleteAll();
	}

	@Test
	void registerCreatesUserWithHashedPasswordAndReturnsToken() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "USER@example.com",
						  "password": "strong-password"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token").isString());

		User user = userRepository.findByEmail("user@example.com").orElseThrow();
		assertThat(user.getPasswordHash()).isNotEqualTo("strong-password");
		assertThat(passwordEncoder.matches("strong-password", user.getPasswordHash())).isTrue();
	}

	@Test
	void registerReturnsConflictWhenEmailAlreadyExists() throws Exception {
		userRepository.saveAndFlush(new User("user@example.com", passwordEncoder.encode("strong-password")));

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "user@example.com",
						  "password": "strong-password"
						}
						"""))
				.andExpect(status().isConflict());
	}

	@Test
	void loginReturnsTokenForValidCredentials() throws Exception {
		userRepository.saveAndFlush(new User("user@example.com", passwordEncoder.encode("strong-password")));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "user@example.com",
						  "password": "strong-password"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isString());
	}

	@Test
	void loginReturnsUnauthorizedForInvalidCredentials() throws Exception {
		userRepository.saveAndFlush(new User("user@example.com", passwordEncoder.encode("strong-password")));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "user@example.com",
						  "password": "wrong-password"
						}
						"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpointsRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/portfolios"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void bearerTokenAuthenticatesProtectedEndpoint() throws Exception {
		userRepository.saveAndFlush(new User("user@example.com", passwordEncoder.encode("strong-password")));
		String token = jwtService.generateToken("user@example.com");

		mockMvc.perform(get("/api/portfolios")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}
}
