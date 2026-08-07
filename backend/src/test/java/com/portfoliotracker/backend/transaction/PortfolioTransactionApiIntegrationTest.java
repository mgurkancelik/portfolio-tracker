package com.portfoliotracker.backend.transaction;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.portfoliotracker.backend.asset.Asset;
import com.portfoliotracker.backend.asset.AssetRepository;
import com.portfoliotracker.backend.asset.AssetType;
import com.portfoliotracker.backend.portfolio.Portfolio;
import com.portfoliotracker.backend.portfolio.PortfolioRepository;

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
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PortfolioTransactionApiIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
			.withDatabaseName("transaction_api_test")
			.withUsername("transaction_api_test")
			.withPassword("transaction_api_test");

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
	void deleteRecords() {
		transactionRepository.deleteAll();
		portfolioRepository.deleteAll();
		assetRepository.deleteAll();
	}

	@Test
	void buySellPositionAndListWorkflow() throws Exception {
		TestContext context = createContext();

		createTransaction(context, "BUY", "10", "100", "10", date(1))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.portfolioId").value(context.portfolio.getId().intValue()))
				.andExpect(jsonPath("$.assetId").value(context.asset.getId().intValue()))
				.andExpect(jsonPath("$.assetSymbol").value("AAPL"))
				.andExpect(jsonPath("$.transactionType").value("BUY"))
				.andExpect(jsonPath("$.createdAt").isNotEmpty());

		assertEquals(1, transactionRepository.count());

		createTransaction(context, "BUY", "5", "120", "5", date(2))
				.andExpect(status().isCreated());

		mockMvc.perform(get(positionPath(context)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.portfolioId").value(context.portfolio.getId().intValue()))
				.andExpect(jsonPath("$.assetId").value(context.asset.getId().intValue()))
				.andExpect(jsonPath("$.assetSymbol").value("AAPL"))
				.andExpect(content().string(containsString("\"quantity\":15.00000000")))
				.andExpect(content().string(containsString("\"costBasis\":1615.00000000")))
				.andExpect(content().string(containsString("\"averageCost\":107.66666667")))
				.andExpect(content().string(containsString("\"realizedProfit\":0.00000000")));

		createTransaction(context, "SELL", "3", "140", "2", date(3))
				.andExpect(status().isCreated());

		mockMvc.perform(get(positionPath(context)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("\"quantity\":12.00000000")))
				.andExpect(content().string(containsString("\"costBasis\":1292.00000000")))
				.andExpect(content().string(containsString("\"averageCost\":107.66666667")))
				.andExpect(content().string(containsString("\"realizedProfit\":95.00000000")));

		mockMvc.perform(get(transactionsPath(context.portfolio)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[0].transactionType").value("BUY"))
				.andExpect(jsonPath("$[1].transactionType").value("BUY"))
				.andExpect(jsonPath("$[2].transactionType").value("SELL"));
	}

	@Test
	void oversellReturnsConflictAndRollsBackTransaction() throws Exception {
		TestContext context = createContext();
		createTransaction(context, "BUY", "10", "100", "10", date(1))
				.andExpect(status().isCreated());
		createTransaction(context, "BUY", "5", "120", "5", date(2))
				.andExpect(status().isCreated());
		createTransaction(context, "SELL", "3", "140", "2", date(3))
				.andExpect(status().isCreated());

		long transactionCountBeforeOversell = transactionRepository.count();

		createTransaction(context, "SELL", "13", "140", "2", date(4))
				.andExpect(status().isConflict());

		assertEquals(transactionCountBeforeOversell, transactionRepository.count());
	}

	@Test
	void backdatedSellThatPrecedesBuyReturnsConflictAndRollsBackTransaction() throws Exception {
		TestContext context = createContext();
		createTransaction(context, "BUY", "10", "100", "0", date(10))
				.andExpect(status().isCreated());

		createTransaction(context, "SELL", "5", "110", "0", date(5))
				.andExpect(status().isConflict());

		assertEquals(1, transactionRepository.count());
		List<PortfolioTransaction> transactions = transactionRepository
				.findAllByPortfolioIdAndAssetIdOrderByTransactionDateAscIdAsc(
						context.portfolio.getId(),
						context.asset.getId());
		assertEquals(List.of(TransactionType.BUY), transactions.stream()
				.map(PortfolioTransaction::getTransactionType)
				.toList());
	}

	@Test
	void invalidRequestReturnsBadRequest() throws Exception {
		TestContext context = createContext();

		mockMvc.perform(post(transactionsPath(context.portfolio))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "assetId": %d,
						  "transactionType": "BUY",
						  "quantity": 0,
						  "unitPrice": 0,
						  "fee": -1,
						  "transactionDate": "%s"
						}
						""".formatted(context.asset.getId(), date(1))))
				.andExpect(status().isBadRequest());

		assertEquals(0, transactionRepository.count());
	}

	@Test
	void unknownPortfolioReturnsNotFound() throws Exception {
		Asset asset = createAsset();

		mockMvc.perform(post("/api/portfolios/999999/transactions")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequest(asset.getId(), "BUY", "10", "100", "0", date(1))))
				.andExpect(status().isNotFound());

		assertEquals(0, transactionRepository.count());
	}

	@Test
	void unknownAssetReturnsNotFound() throws Exception {
		Portfolio portfolio = createPortfolio();

		mockMvc.perform(post(transactionsPath(portfolio))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequest(999999L, "BUY", "10", "100", "0", date(1))))
				.andExpect(status().isNotFound());

		assertEquals(0, transactionRepository.count());
	}

	private TestContext createContext() {
		return new TestContext(createPortfolio(), createAsset());
	}

	private Portfolio createPortfolio() {
		return portfolioRepository.saveAndFlush(new Portfolio("Ana Portfoy", "TRY"));
	}

	private Asset createAsset() {
		return assetRepository.saveAndFlush(new Asset("AAPL", "Apple Inc.", AssetType.STOCK, "USD"));
	}

	private ResultActions createTransaction(
			TestContext context,
			String transactionType,
			String quantity,
			String unitPrice,
			String fee,
			OffsetDateTime transactionDate) throws Exception {
		return mockMvc.perform(post(transactionsPath(context.portfolio))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequest(
						context.asset.getId(),
						transactionType,
						quantity,
						unitPrice,
						fee,
						transactionDate)));
	}

	private static String validRequest(
			Long assetId,
			String transactionType,
			String quantity,
			String unitPrice,
			String fee,
			OffsetDateTime transactionDate) {
		return """
				{
				  "assetId": %d,
				  "transactionType": "%s",
				  "quantity": %s,
				  "unitPrice": %s,
				  "fee": %s,
				  "transactionDate": "%s"
				}
				""".formatted(assetId, transactionType, quantity, unitPrice, fee, transactionDate);
	}

	private static String transactionsPath(Portfolio portfolio) {
		return "/api/portfolios/%d/transactions".formatted(portfolio.getId());
	}

	private static String positionPath(TestContext context) {
		return "/api/portfolios/%d/positions/%d".formatted(context.portfolio.getId(), context.asset.getId());
	}

	private static OffsetDateTime date(int dayOfMonth) {
		return OffsetDateTime.of(2026, 8, dayOfMonth, 10, 0, 0, 0, ZoneOffset.UTC);
	}

	private record TestContext(Portfolio portfolio, Asset asset) {
	}
}
