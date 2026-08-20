package com.portfoliotracker.backend.asset;

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

import com.portfoliotracker.backend.portfolio.Portfolio;
import com.portfoliotracker.backend.portfolio.PortfolioRepository;
import com.portfoliotracker.backend.transaction.PortfolioTransaction;
import com.portfoliotracker.backend.transaction.PortfolioTransactionRepository;
import com.portfoliotracker.backend.transaction.TransactionType;
import com.portfoliotracker.backend.user.User;
import com.portfoliotracker.backend.user.UserRepository;

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
class AssetApiIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
			.withDatabaseName("asset_api_test")
			.withUsername("asset_api_test")
			.withPassword("asset_api_test");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AssetRepository assetRepository;

	@Autowired
	private PortfolioRepository portfolioRepository;

	@Autowired
	private PortfolioTransactionRepository transactionRepository;

	@Autowired
	private UserRepository userRepository;

	@DynamicPropertySource
	static void postgresProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
		registry.add("spring.flyway.enabled", () -> "true");
	}

	@BeforeEach
	void deleteAssets() {
		transactionRepository.deleteAll();
		assetRepository.deleteAll();
		portfolioRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void createAssetThenListAssets() throws Exception {
		mockMvc.perform(post("/api/assets")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "symbol": "aapl",
						  "name": "Apple Inc.",
						  "assetType": "STOCK",
						  "currency": "usd"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.symbol").value("AAPL"))
				.andExpect(jsonPath("$.name").value("Apple Inc."))
				.andExpect(jsonPath("$.assetType").value("STOCK"))
				.andExpect(jsonPath("$.currency").value("USD"))
				.andExpect(jsonPath("$.createdAt").isNotEmpty())
				.andExpect(jsonPath("$.updatedAt").isNotEmpty());

		assertEquals(1, assetRepository.count());
		Asset saved = assetRepository.findBySymbolAndAssetType("AAPL", AssetType.STOCK).orElseThrow();
		assertEquals("USD", saved.getCurrency());

		mockMvc.perform(get("/api/assets"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(saved.getId().intValue()))
				.andExpect(jsonPath("$[0].symbol").value("AAPL"))
				.andExpect(jsonPath("$[0].name").value("Apple Inc."))
				.andExpect(jsonPath("$[0].assetType").value("STOCK"))
				.andExpect(jsonPath("$[0].currency").value("USD"))
				.andExpect(jsonPath("$[0].createdAt").isNotEmpty())
				.andExpect(jsonPath("$[0].updatedAt").isNotEmpty());
	}

	@Test
	void createAssetRejectsInvalidRequest() throws Exception {
		mockMvc.perform(post("/api/assets")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "symbol": "",
						  "name": "",
						  "assetType": null,
						  "currency": "US"
						}
						"""))
				.andExpect(status().isBadRequest());

		assertEquals(0, assetRepository.count());
	}

	@Test
	void createAssetRejectsDuplicateSymbolAndAssetType() throws Exception {
		String request = """
				{
				  "symbol": "aapl",
				  "name": "Apple Inc.",
				  "assetType": "STOCK",
				  "currency": "usd"
				}
				""";

		mockMvc.perform(post("/api/assets")
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/assets")
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
				.andExpect(status().isConflict());

		assertEquals(1, assetRepository.count());
	}

	@Test
	void updateAssetNormalizesFieldsAndKeepsSameSymbolTypeCombination() throws Exception {
		Asset asset = assetRepository.saveAndFlush(new Asset("AAPL", "Apple Inc.", AssetType.STOCK, "USD"));

		mockMvc.perform(put("/api/assets/%d".formatted(asset.getId()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "symbol": "aapl",
						  "name": " Apple Incorporated ",
						  "assetType": "STOCK",
						  "currency": "usd"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(asset.getId().intValue()))
				.andExpect(jsonPath("$.symbol").value("AAPL"))
				.andExpect(jsonPath("$.name").value("Apple Incorporated"))
				.andExpect(jsonPath("$.assetType").value("STOCK"))
				.andExpect(jsonPath("$.currency").value("USD"));

		Asset updated = assetRepository.findById(asset.getId()).orElseThrow();
		assertEquals("AAPL", updated.getSymbol());
		assertEquals("Apple Incorporated", updated.getName());
		assertEquals(AssetType.STOCK, updated.getAssetType());
		assertEquals("USD", updated.getCurrency());
	}

	@Test
	void updateAssetRejectsDuplicateSymbolAndAssetTypeAndKeepsOriginalAsset() throws Exception {
		Asset aapl = assetRepository.saveAndFlush(new Asset("AAPL", "Apple Inc.", AssetType.STOCK, "USD"));
		Asset msft = assetRepository.saveAndFlush(new Asset("MSFT", "Microsoft Corp.", AssetType.STOCK, "USD"));

		mockMvc.perform(put("/api/assets/%d".formatted(msft.getId()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "symbol": "aapl",
						  "name": "Microsoft Corp.",
						  "assetType": "STOCK",
						  "currency": "usd"
						}
						"""))
				.andExpect(status().isConflict());

		Asset unchanged = assetRepository.findById(msft.getId()).orElseThrow();
		assertEquals(2, assetRepository.count());
		assertEquals(aapl.getId(), assetRepository.findBySymbolAndAssetType("AAPL", AssetType.STOCK).orElseThrow().getId());
		assertEquals("MSFT", unchanged.getSymbol());
		assertEquals("Microsoft Corp.", unchanged.getName());
	}

	@Test
	void updateAssetReturnsNotFoundForUnknownAsset() throws Exception {
		mockMvc.perform(put("/api/assets/999999")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "symbol": "aapl",
						  "name": "Apple Inc.",
						  "assetType": "STOCK",
						  "currency": "usd"
						}
						"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void updateAssetRejectsInvalidRequest() throws Exception {
		Asset asset = assetRepository.saveAndFlush(new Asset("AAPL", "Apple Inc.", AssetType.STOCK, "USD"));

		mockMvc.perform(put("/api/assets/%d".formatted(asset.getId()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "symbol": "",
						  "name": "",
						  "assetType": null,
						  "currency": "US"
						}
						"""))
				.andExpect(status().isBadRequest());

		Asset unchanged = assetRepository.findById(asset.getId()).orElseThrow();
		assertEquals("AAPL", unchanged.getSymbol());
		assertEquals("Apple Inc.", unchanged.getName());
	}

	@Test
	void deleteAssetRemovesUnusedAsset() throws Exception {
		Asset asset = assetRepository.saveAndFlush(new Asset("AAPL", "Apple Inc.", AssetType.STOCK, "USD"));

		mockMvc.perform(delete("/api/assets/%d".formatted(asset.getId())))
				.andExpect(status().isNoContent());

		assertEquals(0, assetRepository.count());
	}

	@Test
	void deleteAssetReturnsNotFoundForUnknownAsset() throws Exception {
		mockMvc.perform(delete("/api/assets/999999"))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteAssetRejectsAssetUsedByTransactions() throws Exception {
		User user = userRepository.saveAndFlush(new User("owner@example.com", "password-hash"));
		Portfolio portfolio = portfolioRepository.saveAndFlush(new Portfolio("Uzun Vadeli", "USD", user.getId()));
		Asset asset = assetRepository.saveAndFlush(new Asset("AAPL", "Apple Inc.", AssetType.STOCK, "USD"));
		transactionRepository.saveAndFlush(new PortfolioTransaction(
				portfolio,
				asset,
				TransactionType.BUY,
				new BigDecimal("10.00000000"),
				new BigDecimal("180.00000000"),
				new BigDecimal("1.00000000"),
				OffsetDateTime.now()));

		mockMvc.perform(delete("/api/assets/%d".formatted(asset.getId())))
				.andExpect(status().isConflict());

		assertEquals(1, assetRepository.count());
		assertEquals(1, transactionRepository.count());
	}
}
