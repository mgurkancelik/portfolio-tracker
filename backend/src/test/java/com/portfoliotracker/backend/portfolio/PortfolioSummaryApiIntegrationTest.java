package com.portfoliotracker.backend.portfolio;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.portfoliotracker.backend.marketdata.FakeMarketDataProvider;
import com.portfoliotracker.backend.transaction.PortfolioTransaction;
import com.portfoliotracker.backend.transaction.PortfolioTransactionRepository;
import com.portfoliotracker.backend.transaction.TransactionType;
import com.portfoliotracker.backend.user.User;
import com.portfoliotracker.backend.user.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
@ActiveProfiles("fake-market-data")
class PortfolioSummaryApiIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
			.withDatabaseName("portfolio_summary_api_test")
			.withUsername("portfolio_summary_api_test")
			.withPassword("portfolio_summary_api_test");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PortfolioRepository portfolioRepository;

	@Autowired
	private AssetRepository assetRepository;

	@Autowired
	private PortfolioTransactionRepository transactionRepository;

	@Autowired
	private UserRepository userRepository;

	@MockitoSpyBean
	private FakeMarketDataProvider marketDataProvider;

	private User currentUser;

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
		SecurityContextHolder.clearContext();
		transactionRepository.deleteAll();
		portfolioRepository.deleteAll();
		assetRepository.deleteAll();
		userRepository.deleteAll();
		currentUser = createUser("owner@example.com");
		authenticate(currentUser);
		reset(marketDataProvider);
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void summaryAggregatesOpenPositionsByCurrencyAndKeepsClosedRealizedProfit() throws Exception {
		Portfolio portfolio = createPortfolio();
		Asset cash = createAsset("TRY", "TRY Cash", AssetType.CASH, "TRY");
		Asset aapl = createAsset("AAPL", "Apple Inc.", AssetType.STOCK, "USD");
		Asset btc = createAsset("BTC", "Bitcoin", AssetType.CRYPTO, "USD");
		Asset eurtry = createAsset("EURTRY", "Euro Turkish Lira", AssetType.FOREX, "TRY");

		saveTransactions(List.of(
				transaction(portfolio, cash, TransactionType.BUY, "5000", "1", "0", 1),
				transaction(portfolio, aapl, TransactionType.BUY, "10", "100", "10", 1),
				transaction(portfolio, aapl, TransactionType.BUY, "5", "120", "5", 2),
				transaction(portfolio, aapl, TransactionType.SELL, "3", "140", "2", 3),
				transaction(portfolio, btc, TransactionType.BUY, "0.50000000", "60000", "20", 4),
				transaction(portfolio, eurtry, TransactionType.BUY, "10", "40", "0", 5),
				transaction(portfolio, eurtry, TransactionType.SELL, "10", "42", "0", 6)));

		mockMvc.perform(get(summaryPath(portfolio)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.portfolioId").value(portfolio.getId().intValue()))
				.andExpect(jsonPath("$.portfolioName").value("Ana Portfoy"))
				.andExpect(jsonPath("$.baseCurrency").value("TRY"))
				.andExpect(content().string(containsString("\"totalPortfolioValue\":39300.00000000")))
				.andExpect(content().string(containsString("\"totalCashBalance\":5000.00000000")))
				.andExpect(content().string(containsString("\"totalUnrealizedProfit\":2988.00000000")))
				.andExpect(content().string(containsString("\"totalUnrealizedProfitPercentage\":9.54266735")))
				.andExpect(jsonPath("$.openPositionCount").value(2))
				.andExpect(jsonPath("$.totalsByCurrency", hasSize(2)))
				.andExpect(jsonPath("$.totalsByCurrency[0].currency").value("TRY"))
				.andExpect(content().string(containsString("\"costBasis\":5000.00000000")))
				.andExpect(content().string(containsString("\"marketValue\":5000.00000000")))
				.andExpect(content().string(containsString("\"unrealizedProfit\":0.00000000")))
				.andExpect(content().string(containsString("\"realizedProfit\":20.00000000")))
				.andExpect(content().string(containsString("\"totalProfit\":20.00000000")))
				.andExpect(jsonPath("$.totalsByCurrency[1].currency").value("USD"))
				.andExpect(content().string(containsString("\"costBasis\":31312.00000000")))
				.andExpect(content().string(containsString("\"marketValue\":34300.00000000")))
				.andExpect(content().string(containsString("\"unrealizedProfit\":2988.00000000")))
				.andExpect(content().string(containsString("\"realizedProfit\":95.00000000")))
				.andExpect(content().string(containsString("\"totalProfit\":3083.00000000")));

		verify(marketDataProvider, times(2)).getCurrentPrice(any(Asset.class));
		verify(marketDataProvider).getCurrentPrice(argThat(asset -> "AAPL".equals(asset.getSymbol())));
		verify(marketDataProvider).getCurrentPrice(argThat(asset -> "BTC".equals(asset.getSymbol())));
		verify(marketDataProvider, never()).getCurrentPrice(argThat(asset -> "EURTRY".equals(asset.getSymbol())));
		verify(marketDataProvider, never()).getCurrentPrice(argThat(asset -> "TRY".equals(asset.getSymbol())));
	}

	@Test
	void summaryReturnsNotFoundForUnknownPortfolio() throws Exception {
		mockMvc.perform(get("/api/portfolios/999999/summary"))
				.andExpect(status().isNotFound());
	}

	@Test
	void summaryReturnsNotFoundForAnotherUsersPortfolio() throws Exception {
		User otherUser = createUser("other@example.com");
		Portfolio otherPortfolio = portfolioRepository.saveAndFlush(new Portfolio(
				"Baska Portfoy",
				"TRY",
				otherUser.getId()));

		mockMvc.perform(get(summaryPath(otherPortfolio)))
				.andExpect(status().isNotFound());
	}

	@Test
	void summaryReturnsCashOnlyTotalsWithoutMarketDataLookup() throws Exception {
		Portfolio portfolio = createPortfolio();
		Asset cash = createAsset("TRY", "TRY Cash", AssetType.CASH, "TRY");
		saveTransactions(List.of(transaction(portfolio, cash, TransactionType.BUY, "1200", "1", "0", 1)));

		mockMvc.perform(get(summaryPath(portfolio)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.baseCurrency").value("TRY"))
				.andExpect(content().string(containsString("\"totalPortfolioValue\":1200.00000000")))
				.andExpect(content().string(containsString("\"totalCashBalance\":1200.00000000")))
				.andExpect(content().string(containsString("\"totalUnrealizedProfit\":0.00000000")))
				.andExpect(content().string(containsString("\"totalUnrealizedProfitPercentage\":0.00000000")))
				.andExpect(jsonPath("$.openPositionCount").value(0))
				.andExpect(jsonPath("$.totalsByCurrency", hasSize(1)))
				.andExpect(jsonPath("$.totalsByCurrency[0].currency").value("TRY"))
				.andExpect(content().string(containsString("\"costBasis\":1200.00000000")))
				.andExpect(content().string(containsString("\"marketValue\":1200.00000000")));

		verify(marketDataProvider, never()).getCurrentPrice(any(Asset.class));
	}

	@Test
	void summaryReturnsEmptyTotalsForPortfolioWithoutTransactions() throws Exception {
		Portfolio portfolio = createPortfolio();

		mockMvc.perform(get(summaryPath(portfolio)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.portfolioId").value(portfolio.getId().intValue()))
				.andExpect(jsonPath("$.portfolioName").value("Ana Portfoy"))
				.andExpect(jsonPath("$.baseCurrency").value("TRY"))
				.andExpect(content().string(containsString("\"totalPortfolioValue\":0.00000000")))
				.andExpect(content().string(containsString("\"totalCashBalance\":0.00000000")))
				.andExpect(content().string(containsString("\"totalUnrealizedProfit\":0.00000000")))
				.andExpect(content().string(containsString("\"totalUnrealizedProfitPercentage\":0.00000000")))
				.andExpect(jsonPath("$.openPositionCount").value(0))
				.andExpect(jsonPath("$.totalsByCurrency", hasSize(0)));

		verify(marketDataProvider, never()).getCurrentPrice(any(Asset.class));
	}

	@Test
	void summaryReturnsServiceUnavailableWhenMarketDataIsMissingForOpenPosition() throws Exception {
		Portfolio portfolio = createPortfolio();
		Asset msft = createAsset("MSFT", "Microsoft Corp.", AssetType.STOCK, "USD");
		saveTransactions(List.of(transaction(portfolio, msft, TransactionType.BUY, "1", "100", "0", 1)));

		mockMvc.perform(get(summaryPath(portfolio)))
				.andExpect(status().isServiceUnavailable())
				.andExpect(content().string(containsString("Market data is not available for symbol: MSFT")));
	}

	@Test
	void summaryDoesNotRequestMarketDataForClosedPosition() throws Exception {
		Portfolio portfolio = createPortfolio();
		Asset msft = createAsset("MSFT", "Microsoft Corp.", AssetType.STOCK, "USD");
		saveTransactions(List.of(
				transaction(portfolio, msft, TransactionType.BUY, "1", "100", "0", 1),
				transaction(portfolio, msft, TransactionType.SELL, "1", "120", "0", 2)));

		mockMvc.perform(get(summaryPath(portfolio)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("\"totalPortfolioValue\":0.00000000")))
				.andExpect(content().string(containsString("\"totalCashBalance\":0.00000000")))
				.andExpect(content().string(containsString("\"totalUnrealizedProfit\":0.00000000")))
				.andExpect(content().string(containsString("\"totalUnrealizedProfitPercentage\":0.00000000")))
				.andExpect(jsonPath("$.openPositionCount").value(0))
				.andExpect(jsonPath("$.totalsByCurrency", hasSize(1)))
				.andExpect(jsonPath("$.totalsByCurrency[0].currency").value("USD"))
				.andExpect(content().string(containsString("\"costBasis\":0.00000000")))
				.andExpect(content().string(containsString("\"marketValue\":0.00000000")))
				.andExpect(content().string(containsString("\"unrealizedProfit\":0.00000000")))
				.andExpect(content().string(containsString("\"realizedProfit\":20.00000000")))
				.andExpect(content().string(containsString("\"totalProfit\":20.00000000")));

		verify(marketDataProvider, never()).getCurrentPrice(any(Asset.class));
	}

	@Test
	void summaryReturnsServiceUnavailableWhenMarketDataCurrencyDoesNotMatchAssetCurrency() throws Exception {
		Portfolio portfolio = createPortfolio();
		Asset eurtry = createAsset("EURTRY", "Euro Turkish Lira", AssetType.FOREX, "USD");
		saveTransactions(List.of(transaction(portfolio, eurtry, TransactionType.BUY, "10", "40", "0", 1)));

		mockMvc.perform(get(summaryPath(portfolio)))
				.andExpect(status().isServiceUnavailable())
				.andExpect(content().string(containsString("Market data currency TRY does not match asset currency USD")));
	}

	private Portfolio createPortfolio() {
		return portfolioRepository.saveAndFlush(new Portfolio("Ana Portfoy", "TRY", currentUser.getId()));
	}

	private User createUser(String email) {
		return userRepository.saveAndFlush(new User(email, "password-hash"));
	}

	private Asset createAsset(String symbol, String name, AssetType assetType, String currency) {
		return assetRepository.saveAndFlush(new Asset(symbol, name, assetType, currency));
	}

	private void saveTransactions(List<PortfolioTransaction> transactions) {
		transactionRepository.saveAllAndFlush(transactions);
		reset(marketDataProvider);
	}

	private static PortfolioTransaction transaction(
			Portfolio portfolio,
			Asset asset,
			TransactionType transactionType,
			String quantity,
			String unitPrice,
			String fee,
			int dayOfMonth) {
		return new PortfolioTransaction(
				portfolio,
				asset,
				transactionType,
				new BigDecimal(quantity),
				new BigDecimal(unitPrice),
				new BigDecimal(fee),
				date(dayOfMonth));
	}

	private static String summaryPath(Portfolio portfolio) {
		return "/api/portfolios/%d/summary".formatted(portfolio.getId());
	}

	private static void authenticate(User user) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				user.getEmail(),
				null,
				List.of(new SimpleGrantedAuthority("ROLE_USER"))));
	}

	private static OffsetDateTime date(int dayOfMonth) {
		return OffsetDateTime.of(2026, 8, dayOfMonth, 10, 0, 0, 0, ZoneOffset.UTC);
	}
}
