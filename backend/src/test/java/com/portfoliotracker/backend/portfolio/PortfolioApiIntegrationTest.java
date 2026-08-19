package com.portfoliotracker.backend.portfolio;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PortfolioApiIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
			.withDatabaseName("portfolio_api_test")
			.withUsername("portfolio_api_test")
			.withPassword("portfolio_api_test");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PortfolioRepository portfolioRepository;

	@DynamicPropertySource
	static void postgresProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
		registry.add("spring.flyway.enabled", () -> "true");
	}

	@BeforeEach
	void deletePortfolios() {
		portfolioRepository.deleteAll();
	}

	@Test
	void createPortfolioThenListPortfolios() throws Exception {
		mockMvc.perform(post("/api/portfolios")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Uzun Vadeli",
						  "baseCurrency": "usd"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.name").value("Uzun Vadeli"))
				.andExpect(jsonPath("$.baseCurrency").value("USD"))
				.andExpect(jsonPath("$.createdAt").isNotEmpty())
				.andExpect(jsonPath("$.updatedAt").isNotEmpty());

		assertEquals(1, portfolioRepository.count());
		Portfolio saved = portfolioRepository.findAll().get(0);
		assertEquals("USD", saved.getBaseCurrency());

		mockMvc.perform(get("/api/portfolios"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(saved.getId().intValue()))
				.andExpect(jsonPath("$[0].name").value("Uzun Vadeli"))
				.andExpect(jsonPath("$[0].baseCurrency").value("USD"))
				.andExpect(jsonPath("$[0].createdAt").isNotEmpty())
				.andExpect(jsonPath("$[0].updatedAt").isNotEmpty());
	}

	@Test
	void createPortfolioRejectsInvalidRequest() throws Exception {
		mockMvc.perform(post("/api/portfolios")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "",
						  "baseCurrency": "US"
						}
						"""))
				.andExpect(status().isBadRequest());

		assertEquals(0, portfolioRepository.count());
	}

	@Test
	void updatePortfolioNormalizesBaseCurrency() throws Exception {
		Portfolio portfolio = portfolioRepository.saveAndFlush(new Portfolio("Uzun Vadeli", "TRY"));

		mockMvc.perform(put("/api/portfolios/%d".formatted(portfolio.getId()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": " Büyüme ",
						  "baseCurrency": "usd"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(portfolio.getId().intValue()))
				.andExpect(jsonPath("$.name").value("Büyüme"))
				.andExpect(jsonPath("$.baseCurrency").value("USD"))
				.andExpect(jsonPath("$.createdAt").isNotEmpty())
				.andExpect(jsonPath("$.updatedAt").isNotEmpty());

		Portfolio updated = portfolioRepository.findById(portfolio.getId()).orElseThrow();
		assertEquals("Büyüme", updated.getName());
		assertEquals("USD", updated.getBaseCurrency());
	}

	@Test
	void updatePortfolioReturnsNotFoundForUnknownPortfolio() throws Exception {
		mockMvc.perform(put("/api/portfolios/999999")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Uzun Vadeli",
						  "baseCurrency": "try"
						}
						"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void updatePortfolioRejectsInvalidRequest() throws Exception {
		Portfolio portfolio = portfolioRepository.saveAndFlush(new Portfolio("Uzun Vadeli", "TRY"));

		mockMvc.perform(put("/api/portfolios/%d".formatted(portfolio.getId()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "",
						  "baseCurrency": "US"
						}
						"""))
				.andExpect(status().isBadRequest());

		Portfolio unchanged = portfolioRepository.findById(portfolio.getId()).orElseThrow();
		assertEquals("Uzun Vadeli", unchanged.getName());
		assertEquals("TRY", unchanged.getBaseCurrency());
	}
}
