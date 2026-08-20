package com.portfoliotracker.backend.portfolio;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.portfoliotracker.backend.asset.Asset;
import com.portfoliotracker.backend.asset.AssetRepository;
import com.portfoliotracker.backend.asset.AssetType;
import com.portfoliotracker.backend.transaction.PortfolioTransaction;
import com.portfoliotracker.backend.transaction.PortfolioTransactionRepository;
import com.portfoliotracker.backend.transaction.TransactionType;

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
@AutoConfigureMockMvc(addFilters = false)
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

	@Autowired
	private AssetRepository assetRepository;

	@Autowired
	private PortfolioTransactionRepository transactionRepository;

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
		transactionRepository.deleteAll();
		assetRepository.deleteAll();
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
		assertEquals(1, assetRepository.count());
		Asset cashAsset = assetRepository.findBySymbolAndAssetType("USD", AssetType.CASH).orElseThrow();
		assertEquals("USD Cash", cashAsset.getName());
		assertEquals("USD", cashAsset.getCurrency());

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
	void createPortfolioReusesCashAssetForSameCurrency() throws Exception {
		mockMvc.perform(post("/api/portfolios")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Birinci",
						  "baseCurrency": "try"
						}
						"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/portfolios")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "İkinci",
						  "baseCurrency": "TRY"
						}
						"""))
				.andExpect(status().isCreated());

		assertEquals(2, portfolioRepository.count());
		assertEquals(1, assetRepository.count());
		Asset cashAsset = assetRepository.findBySymbolAndAssetType("TRY", AssetType.CASH).orElseThrow();
		assertEquals("TRY Cash", cashAsset.getName());
		assertEquals("TRY", cashAsset.getCurrency());
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

	@Test
	void deletePortfolioRemovesUnusedPortfolio() throws Exception {
		Portfolio portfolio = portfolioRepository.saveAndFlush(new Portfolio("Uzun Vadeli", "TRY"));

		mockMvc.perform(delete("/api/portfolios/%d".formatted(portfolio.getId())))
				.andExpect(status().isNoContent());

		assertEquals(0, portfolioRepository.count());
	}

	@Test
	void deletePortfolioReturnsNotFoundForUnknownPortfolio() throws Exception {
		mockMvc.perform(delete("/api/portfolios/999999"))
				.andExpect(status().isNotFound());
	}

	@Test
	void deletePortfolioRejectsPortfolioUsedByTransactions() throws Exception {
		Portfolio portfolio = portfolioRepository.saveAndFlush(new Portfolio("Uzun Vadeli", "USD"));
		Asset asset = assetRepository.saveAndFlush(new Asset("AAPL", "Apple Inc.", AssetType.STOCK, "USD"));
		transactionRepository.saveAndFlush(new PortfolioTransaction(
				portfolio,
				asset,
				TransactionType.BUY,
				new BigDecimal("10.00000000"),
				new BigDecimal("180.00000000"),
				new BigDecimal("1.00000000"),
				OffsetDateTime.now()));

		mockMvc.perform(delete("/api/portfolios/%d".formatted(portfolio.getId())))
				.andExpect(status().isConflict());

		assertEquals(1, portfolioRepository.count());
		assertEquals(1, transactionRepository.count());
	}
}
