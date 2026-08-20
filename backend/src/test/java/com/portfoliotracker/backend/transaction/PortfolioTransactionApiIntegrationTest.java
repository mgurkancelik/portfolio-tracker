package com.portfoliotracker.backend.transaction;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import org.springframework.test.context.ActiveProfiles;
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
@ActiveProfiles("fake-market-data")
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

		assertEquals(3, transactionRepository.count());

		createTransaction(context, "BUY", "5", "120", "5", date(2))
				.andExpect(status().isCreated());

		mockMvc.perform(get(positionPath(context)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.portfolioId").value(context.portfolio.getId().intValue()))
				.andExpect(jsonPath("$.assetId").value(context.asset.getId().intValue()))
				.andExpect(jsonPath("$.assetSymbol").value("AAPL"))
				.andExpect(jsonPath("$.assetType").value("STOCK"))
				.andExpect(jsonPath("$.currency").value("USD"))
				.andExpect(content().string(containsString("\"quantity\":15.00000000")))
				.andExpect(content().string(containsString("\"costBasis\":1615.00000000")))
				.andExpect(content().string(containsString("\"averageCost\":107.66666667")))
				.andExpect(content().string(containsString("\"realizedProfit\":0.00000000")))
				.andExpect(content().string(containsString("\"currentPrice\":150.00000000")))
				.andExpect(content().string(containsString("\"marketValue\":2250.00000000")))
				.andExpect(content().string(containsString("\"unrealizedProfit\":635.00000000")))
				.andExpect(content().string(containsString("\"unrealizedProfitPercentage\":39.31888545")));

		createTransaction(context, "SELL", "3", "140", "2", date(3))
				.andExpect(status().isCreated());

		mockMvc.perform(get(positionPath(context)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("\"quantity\":12.00000000")))
				.andExpect(content().string(containsString("\"costBasis\":1292.00000000")))
				.andExpect(content().string(containsString("\"averageCost\":107.66666667")))
				.andExpect(content().string(containsString("\"realizedProfit\":95.00000000")))
				.andExpect(content().string(containsString("\"currentPrice\":150.00000000")))
				.andExpect(content().string(containsString("\"marketValue\":1800.00000000")))
				.andExpect(content().string(containsString("\"unrealizedProfit\":508.00000000")))
				.andExpect(content().string(containsString("\"unrealizedProfitPercentage\":39.31888545")));

		mockMvc.perform(get("/api/portfolios/%d/positions/%d".formatted(
				context.portfolio.getId(),
				context.cashAsset.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.assetSymbol").value("TRY"))
				.andExpect(jsonPath("$.assetType").value("CASH"))
				.andExpect(content().string(containsString("\"quantity\":98803.00000000")))
				.andExpect(content().string(containsString("\"currentPrice\":1.00000000")))
				.andExpect(content().string(containsString("\"marketValue\":98803.00000000")));

		mockMvc.perform(get(transactionsPath(context.portfolio)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(7)))
				.andExpect(jsonPath("$[0].assetSymbol").value("TRY"))
				.andExpect(jsonPath("$[0].transactionType").value("BUY"))
				.andExpect(jsonPath("$[1].assetSymbol").value("AAPL"))
				.andExpect(jsonPath("$[1].transactionType").value("BUY"))
				.andExpect(jsonPath("$[2].assetSymbol").value("TRY"))
				.andExpect(jsonPath("$[2].transactionType").value("SELL"))
				.andExpect(jsonPath("$[3].assetSymbol").value("AAPL"))
				.andExpect(jsonPath("$[3].transactionType").value("BUY"))
				.andExpect(jsonPath("$[4].assetSymbol").value("TRY"))
				.andExpect(jsonPath("$[4].transactionType").value("SELL"))
				.andExpect(jsonPath("$[5].assetSymbol").value("AAPL"))
				.andExpect(jsonPath("$[5].transactionType").value("SELL"))
				.andExpect(jsonPath("$[6].assetSymbol").value("TRY"))
				.andExpect(jsonPath("$[6].transactionType").value("BUY"));
	}

	@Test
	void listOpenPositionsReturnsOnlyOpenPositionsWithMetadataAndDeterministicOrder() throws Exception {
		Portfolio portfolio = createPortfolio();
		Asset cashAsset = createCashAsset(portfolio.getBaseCurrency());
		Asset aapl = createAsset("AAPL", "Apple Inc.", AssetType.STOCK, "USD");
		Asset btc = createAsset("BTC", "Bitcoin", AssetType.CRYPTO, "USD");
		Asset eurtry = createAsset("EURTRY", "Euro Turkish Lira", AssetType.FOREX, "TRY");
		fundPortfolio(portfolio, cashAsset, "100000");

		createTransaction(portfolio, aapl, "BUY", "10", "100", "10", date(1))
				.andExpect(status().isCreated());
		createTransaction(portfolio, aapl, "BUY", "5", "120", "5", date(2))
				.andExpect(status().isCreated());
		createTransaction(portfolio, aapl, "SELL", "3", "140", "2", date(3))
				.andExpect(status().isCreated());
		createTransaction(portfolio, btc, "BUY", "0.50000000", "60000", "20", date(4))
				.andExpect(status().isCreated());
		createTransaction(portfolio, eurtry, "BUY", "10", "40", "0", date(5))
				.andExpect(status().isCreated());
		createTransaction(portfolio, eurtry, "SELL", "10", "42", "0", date(6))
				.andExpect(status().isCreated());

		mockMvc.perform(get(positionsPath(portfolio)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[0].portfolioId").value(portfolio.getId().intValue()))
				.andExpect(jsonPath("$[0].assetId").value(aapl.getId().intValue()))
				.andExpect(jsonPath("$[0].assetSymbol").value("AAPL"))
				.andExpect(jsonPath("$[0].assetType").value("STOCK"))
				.andExpect(jsonPath("$[0].currency").value("USD"))
				.andExpect(content().string(containsString("\"quantity\":12.00000000")))
				.andExpect(content().string(containsString("\"averageCost\":107.66666667")))
				.andExpect(content().string(containsString("\"costBasis\":1292.00000000")))
				.andExpect(content().string(containsString("\"realizedProfit\":95.00000000")))
				.andExpect(content().string(containsString("\"currentPrice\":150.00000000")))
				.andExpect(content().string(containsString("\"marketValue\":1800.00000000")))
				.andExpect(content().string(containsString("\"unrealizedProfit\":508.00000000")))
				.andExpect(content().string(containsString("\"unrealizedProfitPercentage\":39.31888545")))
				.andExpect(jsonPath("$[1].assetId").value(btc.getId().intValue()))
				.andExpect(jsonPath("$[1].assetSymbol").value("BTC"))
				.andExpect(jsonPath("$[1].assetType").value("CRYPTO"))
				.andExpect(jsonPath("$[1].currency").value("USD"))
				.andExpect(content().string(containsString("\"quantity\":0.50000000")))
				.andExpect(content().string(containsString("\"costBasis\":30020.00000000")))
				.andExpect(content().string(containsString("\"currentPrice\":65000.00000000")))
				.andExpect(content().string(containsString("\"marketValue\":32500.00000000")))
				.andExpect(content().string(containsString("\"unrealizedProfit\":2480.00000000")))
				.andExpect(jsonPath("$[2].assetId").value(cashAsset.getId().intValue()))
				.andExpect(jsonPath("$[2].assetSymbol").value("TRY"))
				.andExpect(jsonPath("$[2].assetType").value("CASH"))
				.andExpect(jsonPath("$[2].currency").value("TRY"))
				.andExpect(content().string(containsString("\"quantity\":68803.00000000")))
				.andExpect(content().string(containsString("\"currentPrice\":1.00000000")))
				.andExpect(content().string(containsString("\"marketValue\":68803.00000000")));

		mockMvc.perform(get("/api/portfolios/%d/positions/%d".formatted(portfolio.getId(), eurtry.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.assetSymbol").value("EURTRY"))
				.andExpect(jsonPath("$.assetType").value("FOREX"))
				.andExpect(jsonPath("$.currency").value("TRY"))
				.andExpect(content().string(containsString("\"quantity\":0.00000000")))
				.andExpect(content().string(containsString("\"currentPrice\":42.00000000")))
				.andExpect(content().string(containsString("\"marketValue\":0.00000000")))
				.andExpect(content().string(containsString("\"unrealizedProfit\":0.00000000")));
	}

	@Test
	void listOpenPositionsReturnsEmptyListWhenPortfolioHasNoTransactions() throws Exception {
		Portfolio portfolio = createPortfolio();

		mockMvc.perform(get(positionsPath(portfolio)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void listOpenPositionsReturnsNotFoundForUnknownPortfolio() throws Exception {
		mockMvc.perform(get("/api/portfolios/999999/positions"))
				.andExpect(status().isNotFound());
	}

	@Test
	void singlePositionReturnsServiceUnavailableWhenMarketDataIsMissing() throws Exception {
		Portfolio portfolio = createPortfolio();
		Asset cashAsset = createCashAsset(portfolio.getBaseCurrency());
		Asset unknownAsset = createAsset("MSFT", "Microsoft Corp.", AssetType.STOCK, "USD");
		fundPortfolio(portfolio, cashAsset, "100000");
		createTransaction(portfolio, unknownAsset, "BUY", "1", "100", "0", date(1))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/portfolios/%d/positions/%d".formatted(portfolio.getId(), unknownAsset.getId())))
				.andExpect(status().isServiceUnavailable())
				.andExpect(content().string(containsString("Market data is not available for symbol: MSFT")));
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

		assertEquals(3, transactionRepository.count());
		List<PortfolioTransaction> transactions = transactionRepository
				.findAllByPortfolioIdAndAssetIdOrderByTransactionDateAscIdAsc(
						context.portfolio.getId(),
						context.asset.getId());
		assertEquals(List.of(TransactionType.BUY), transactions.stream()
				.map(PortfolioTransaction::getTransactionType)
				.toList());
	}

	@Test
	void updateTransactionChangesTransactionAndRecalculatesPosition() throws Exception {
		TestContext context = createContext();
		createTransaction(context, "BUY", "10", "100", "10", date(1))
				.andExpect(status().isCreated());
		createTransaction(context, "BUY", "5", "120", "5", date(2))
				.andExpect(status().isCreated());
		createTransaction(context, "SELL", "3", "140", "2", date(3))
				.andExpect(status().isCreated());
		PortfolioTransaction secondBuy = transactionRepository
				.findAllByPortfolioIdAndAssetIdOrderByTransactionDateAscIdAsc(
						context.portfolio.getId(),
						context.asset.getId())
				.get(1);

		mockMvc.perform(put(transactionPath(context.portfolio, secondBuy.getId()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequest(context.asset.getId(), "BUY", "8", "125", "4", date(2))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(secondBuy.getId().intValue()))
				.andExpect(jsonPath("$.assetId").value(context.asset.getId().intValue()))
				.andExpect(jsonPath("$.transactionType").value("BUY"))
				.andExpect(content().string(containsString("\"quantity\":8.00000000")))
				.andExpect(content().string(containsString("\"unitPrice\":125.00000000")))
				.andExpect(content().string(containsString("\"fee\":4.00000000")));

		PortfolioTransaction updated = transactionRepository.findById(secondBuy.getId()).orElseThrow();
		assertEquals(0, updated.getQuantity().compareTo(new BigDecimal("8")));
		assertEquals(0, updated.getUnitPrice().compareTo(new BigDecimal("125")));
		assertEquals(0, updated.getFee().compareTo(new BigDecimal("4")));

		mockMvc.perform(get(positionPath(context)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("\"quantity\":15.00000000")))
				.andExpect(content().string(containsString("\"averageCost\":111.88888889")))
				.andExpect(content().string(containsString("\"costBasis\":1678.33333333")))
				.andExpect(content().string(containsString("\"realizedProfit\":82.33333333")));
	}

	@Test
	void updateTransactionReturnsConflictAndKeepsOriginalTransactionWhenHistoryWouldOversell() throws Exception {
		TestContext context = createContext();
		createTransaction(context, "BUY", "10", "100", "0", date(1))
				.andExpect(status().isCreated());
		createTransaction(context, "SELL", "8", "110", "0", date(2))
				.andExpect(status().isCreated());
		PortfolioTransaction buyTransaction = transactionRepository
				.findAllByPortfolioIdAndAssetIdOrderByTransactionDateAscIdAsc(
						context.portfolio.getId(),
						context.asset.getId())
				.get(0);

		mockMvc.perform(put(transactionPath(context.portfolio, buyTransaction.getId()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequest(context.asset.getId(), "BUY", "5", "100", "0", date(1))))
				.andExpect(status().isConflict());

		PortfolioTransaction unchanged = transactionRepository.findById(buyTransaction.getId()).orElseThrow();
		assertEquals(5, transactionRepository.count());
		assertEquals(0, unchanged.getQuantity().compareTo(new BigDecimal("10")));
	}

	@Test
	void updateTransactionReturnsNotFoundForUnknownTransaction() throws Exception {
		TestContext context = createContext();

		mockMvc.perform(put(transactionPath(context.portfolio, 999999L))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequest(context.asset.getId(), "BUY", "10", "100", "0", date(1))))
				.andExpect(status().isNotFound());
	}

	@Test
	void invalidUpdateRequestReturnsBadRequest() throws Exception {
		TestContext context = createContext();
		createTransaction(context, "BUY", "10", "100", "0", date(1))
				.andExpect(status().isCreated());
		PortfolioTransaction transaction = transactionRepository
				.findAllByPortfolioIdAndAssetIdOrderByTransactionDateAscIdAsc(
						context.portfolio.getId(),
						context.asset.getId())
				.get(0);

		mockMvc.perform(put(transactionPath(context.portfolio, transaction.getId()))
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
	}

	@Test
	void deleteTransactionRemovesTransactionAndRecalculatesPosition() throws Exception {
		TestContext context = createContext();
		createTransaction(context, "BUY", "10", "100", "10", date(1))
				.andExpect(status().isCreated());
		createTransaction(context, "BUY", "5", "120", "5", date(2))
				.andExpect(status().isCreated());
		createTransaction(context, "SELL", "3", "140", "2", date(3))
				.andExpect(status().isCreated());

		PortfolioTransaction sellTransaction = transactionRepository
				.findAllByPortfolioIdAndAssetIdOrderByTransactionDateAscIdAsc(
						context.portfolio.getId(),
						context.asset.getId())
				.get(2);

		mockMvc.perform(delete(transactionPath(context.portfolio, sellTransaction.getId())))
				.andExpect(status().isNoContent());

		assertEquals(2, transactionRepository
				.findAllByPortfolioIdAndAssetIdOrderByTransactionDateAscIdAsc(
						context.portfolio.getId(),
						context.asset.getId())
				.size());
		mockMvc.perform(get(positionPath(context)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("\"quantity\":15.00000000")))
				.andExpect(content().string(containsString("\"costBasis\":1615.00000000")))
				.andExpect(content().string(containsString("\"realizedProfit\":0.00000000")));
	}

	@Test
	void deleteTransactionReturnsConflictAndKeepsTransactionWhenRemainingHistoryWouldOversell() throws Exception {
		TestContext context = createContext();
		createTransaction(context, "BUY", "10", "100", "0", date(1))
				.andExpect(status().isCreated());
		createTransaction(context, "SELL", "8", "110", "0", date(2))
				.andExpect(status().isCreated());
		PortfolioTransaction buyTransaction = transactionRepository
				.findAllByPortfolioIdAndAssetIdOrderByTransactionDateAscIdAsc(
						context.portfolio.getId(),
						context.asset.getId())
				.get(0);

		mockMvc.perform(delete(transactionPath(context.portfolio, buyTransaction.getId())))
				.andExpect(status().isConflict());

		assertEquals(5, transactionRepository.count());
	}

	@Test
	void deleteTransactionReturnsNotFoundForUnknownTransaction() throws Exception {
		Portfolio portfolio = createPortfolio();

		mockMvc.perform(delete(transactionPath(portfolio, 999999L)))
				.andExpect(status().isNotFound());
	}

	@Test
	void invalidRequestReturnsBadRequest() throws Exception {
		Portfolio portfolio = createPortfolio();
		Asset asset = createAsset();

		mockMvc.perform(post(transactionsPath(portfolio))
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
						""".formatted(asset.getId(), date(1))))
				.andExpect(status().isBadRequest());

		assertEquals(0, transactionRepository.count());
	}

	@Test
	void buyReturnsConflictWhenCashBalanceIsInsufficientAndRollsBack() throws Exception {
		Portfolio portfolio = createPortfolio();
		Asset cashAsset = createCashAsset(portfolio.getBaseCurrency());
		Asset asset = createAsset();
		fundPortfolio(portfolio, cashAsset, "100");

		createTransaction(portfolio, asset, "BUY", "2", "60", "1", date(1))
				.andExpect(status().isConflict());

		assertEquals(1, transactionRepository.count());
		assertEquals(0, transactionRepository
				.findAllByPortfolioIdAndAssetIdOrderByTransactionDateAscIdAsc(
						portfolio.getId(),
						asset.getId())
				.size());
		assertEquals(1, transactionRepository
				.findAllByPortfolioIdAndAssetIdOrderByTransactionDateAscIdAsc(
						portfolio.getId(),
						cashAsset.getId())
				.size());
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
		Portfolio portfolio = createPortfolio();
		Asset cashAsset = createCashAsset(portfolio.getBaseCurrency());
		Asset asset = createAsset();
		fundPortfolio(portfolio, cashAsset, "100000");
		return new TestContext(portfolio, asset, cashAsset);
	}

	private Portfolio createPortfolio() {
		return portfolioRepository.saveAndFlush(new Portfolio("Ana Portfoy", "TRY"));
	}

	private Asset createAsset() {
		return createAsset("AAPL", "Apple Inc.", AssetType.STOCK, "USD");
	}

	private Asset createAsset(String symbol, String name, AssetType assetType, String currency) {
		return assetRepository.saveAndFlush(new Asset(symbol, name, assetType, currency));
	}

	private Asset createCashAsset(String currency) {
		return createAsset(currency, currency + " Cash", AssetType.CASH, currency);
	}

	private void fundPortfolio(Portfolio portfolio, Asset cashAsset, String quantity) {
		transactionRepository.saveAndFlush(new PortfolioTransaction(
				portfolio,
				cashAsset,
				TransactionType.BUY,
				new BigDecimal(quantity),
				new BigDecimal("1.00000000"),
				BigDecimal.ZERO,
				date(1).minusDays(1)));
	}

	private ResultActions createTransaction(
			TestContext context,
			String transactionType,
			String quantity,
			String unitPrice,
			String fee,
			OffsetDateTime transactionDate) throws Exception {
		return createTransaction(context.portfolio, context.asset, transactionType, quantity, unitPrice, fee, transactionDate);
	}

	private ResultActions createTransaction(
			Portfolio portfolio,
			Asset asset,
			String transactionType,
			String quantity,
			String unitPrice,
			String fee,
			OffsetDateTime transactionDate) throws Exception {
		return mockMvc.perform(post(transactionsPath(portfolio))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequest(
						asset.getId(),
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

	private static String transactionPath(Portfolio portfolio, Long transactionId) {
		return "/api/portfolios/%d/transactions/%d".formatted(portfolio.getId(), transactionId);
	}

	private static String positionsPath(Portfolio portfolio) {
		return "/api/portfolios/%d/positions".formatted(portfolio.getId());
	}

	private static String positionPath(TestContext context) {
		return "/api/portfolios/%d/positions/%d".formatted(context.portfolio.getId(), context.asset.getId());
	}

	private static OffsetDateTime date(int dayOfMonth) {
		return OffsetDateTime.of(2026, 8, dayOfMonth, 10, 0, 0, 0, ZoneOffset.UTC);
	}

	private record TestContext(Portfolio portfolio, Asset asset, Asset cashAsset) {
	}
}
