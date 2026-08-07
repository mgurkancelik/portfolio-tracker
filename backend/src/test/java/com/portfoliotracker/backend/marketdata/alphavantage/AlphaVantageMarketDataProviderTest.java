package com.portfoliotracker.backend.marketdata.alphavantage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URI;
import java.time.OffsetDateTime;

import com.portfoliotracker.backend.asset.Asset;
import com.portfoliotracker.backend.asset.AssetType;
import com.portfoliotracker.backend.marketdata.MarketDataNotAvailableException;
import com.portfoliotracker.backend.marketdata.MarketPrice;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AlphaVantageMarketDataProviderTest {

	private static final String BASE_URL = "https://www.alphavantage.co";

	private static final String API_KEY = "demo";

	private MockRestServiceServer server;

	private RestClient restClient;

	private AlphaVantageMarketDataProvider provider;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		restClient = builder.build();
		provider = new AlphaVantageMarketDataProvider(restClient, properties(API_KEY));
	}

	@AfterEach
	void verifyHttpCalls() {
		server.verify();
	}

	@Test
	void stockResponseReturnsCurrentMarketPrice() {
		expectStock("AAPL", API_KEY, globalQuote("AAPL", "150.25"));

		MarketPrice price = provider.getCurrentPrice(asset(10L, "AAPL", AssetType.STOCK, "USD"));

		assertMarketPrice(price, 10L, "AAPL", "150.25000000", "USD");
		assertEquals(OffsetDateTime.parse("2026-08-07T00:00:00Z"), price.asOf());
	}

	@Test
	void cryptoResponseReturnsCurrentExchangeRate() {
		expectExchangeRate("BTC", "USD", API_KEY, exchangeRate("BTC", "USD", "65000.50"));

		MarketPrice price = provider.getCurrentPrice(asset(11L, "BTC", AssetType.CRYPTO, "USD"));

		assertMarketPrice(price, 11L, "BTC", "65000.50000000", "USD");
		assertEquals(OffsetDateTime.parse("2026-08-07T12:34:56Z"), price.asOf());
	}

	@Test
	void forexSymbolParserSupportsCompactSlashAndDashFormats() {
		assertEquals(
				new AlphaVantageMarketDataProvider.ForexPair("EUR", "TRY"),
				AlphaVantageMarketDataProvider.parseForexSymbol("EURTRY", "TRY"));
		assertEquals(
				new AlphaVantageMarketDataProvider.ForexPair("EUR", "TRY"),
				AlphaVantageMarketDataProvider.parseForexSymbol("EUR/TRY", "TRY"));
		assertEquals(
				new AlphaVantageMarketDataProvider.ForexPair("EUR", "TRY"),
				AlphaVantageMarketDataProvider.parseForexSymbol("EUR-TRY", "TRY"));
	}

	@Test
	void forexResponseUsesExchangeRateEndpoint() {
		expectExchangeRate("EUR", "TRY", API_KEY, exchangeRate("EUR", "TRY", "42.12"));

		MarketPrice price = provider.getCurrentPrice(asset(12L, "EURTRY", AssetType.FOREX, "TRY"));

		assertMarketPrice(price, 12L, "EURTRY", "42.12000000", "TRY");
	}

	@Test
	void invalidForexSymbolThrowsMarketDataUnavailable() {
		assertThrows(
				MarketDataNotAvailableException.class,
				() -> AlphaVantageMarketDataProvider.parseForexSymbol("EURTRYX", "TRY"));
		assertThrows(
				MarketDataNotAvailableException.class,
				() -> AlphaVantageMarketDataProvider.parseForexSymbol("EUR/USD", "TRY"));
	}

	@Test
	void alphaVantageErrorMessageThrowsMarketDataUnavailable() {
		expectStock("AAPL", API_KEY, """
				{
				  "Error Message": "Invalid API call."
				}
				""");

		assertThrows(
				MarketDataNotAvailableException.class,
				() -> provider.getCurrentPrice(asset(13L, "AAPL", AssetType.STOCK, "USD")));
	}

	@Test
	void alphaVantageInformationRateLimitThrowsMarketDataUnavailable() {
		expectStock("AAPL", API_KEY, """
				{
				  "Information": "The standard API rate limit has been reached."
				}
				""");

		assertThrows(
				MarketDataNotAvailableException.class,
				() -> provider.getCurrentPrice(asset(14L, "AAPL", AssetType.STOCK, "USD")));
	}

	@Test
	void emptyGlobalQuoteThrowsMarketDataUnavailable() {
		expectStock("AAPL", API_KEY, """
				{
				  "Global Quote": {}
				}
				""");

		assertThrows(
				MarketDataNotAvailableException.class,
				() -> provider.getCurrentPrice(asset(15L, "AAPL", AssetType.STOCK, "USD")));
	}

	@Test
	void invalidNumericPriceThrowsMarketDataUnavailable() {
		expectStock("AAPL", API_KEY, globalQuote("AAPL", "not-a-number"));

		assertThrows(
				MarketDataNotAvailableException.class,
				() -> provider.getCurrentPrice(asset(16L, "AAPL", AssetType.STOCK, "USD")));
	}

	@Test
	void zeroOrNegativePriceThrowsMarketDataUnavailable() {
		expectStock("AAPL", API_KEY, globalQuote("AAPL", "0"));
		expectStock("MSFT", API_KEY, globalQuote("MSFT", "-1"));

		assertThrows(
				MarketDataNotAvailableException.class,
				() -> provider.getCurrentPrice(asset(17L, "AAPL", AssetType.STOCK, "USD")));
		assertThrows(
				MarketDataNotAvailableException.class,
				() -> provider.getCurrentPrice(asset(18L, "MSFT", AssetType.STOCK, "USD")));
	}

	@Test
	void nonSuccessfulHttpResponseThrowsMarketDataUnavailable() {
		server.expect(requestTo(stockUrl("AAPL", API_KEY)))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

		assertThrows(
				MarketDataNotAvailableException.class,
				() -> provider.getCurrentPrice(asset(19L, "AAPL", AssetType.STOCK, "USD")));
	}

	@Test
	void apiKeyDoesNotLeakToExceptionMessage() {
		String apiKey = "do-not-leak";
		AlphaVantageMarketDataProvider providerWithConfiguredKey =
				new AlphaVantageMarketDataProvider(restClient, properties(apiKey));
		expectStock("AAPL", apiKey, """
				{
				  "Note": "The do-not-leak value should not be exposed."
				}
				""");

		MarketDataNotAvailableException exception = assertThrows(
				MarketDataNotAvailableException.class,
				() -> providerWithConfiguredKey.getCurrentPrice(asset(20L, "AAPL", AssetType.STOCK, "USD")));

		assertFalse(exception.getMessage().contains(apiKey));
		assertFalse(exception.getMessage().contains("should not be exposed"));
	}

	@Test
	void missingApiKeyThrowsBeforeHttpRequest() {
		AlphaVantageMarketDataProvider providerWithoutKey =
				new AlphaVantageMarketDataProvider(restClient, properties(""));

		MarketDataNotAvailableException exception = assertThrows(
				MarketDataNotAvailableException.class,
				() -> providerWithoutKey.getCurrentPrice(asset(21L, "AAPL", AssetType.STOCK, "USD")));

		assertFalse(exception.getMessage().contains(API_KEY));
	}

	private void expectStock(String symbol, String apiKey, String responseBody) {
		server.expect(requestTo(stockUrl(symbol, apiKey)))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
	}

	private void expectExchangeRate(String fromCurrency, String toCurrency, String apiKey, String responseBody) {
		server.expect(requestTo(BASE_URL + "/query?function=CURRENCY_EXCHANGE_RATE"
				+ "&from_currency=" + fromCurrency
				+ "&to_currency=" + toCurrency
				+ "&apikey=" + apiKey))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
	}

	private static String stockUrl(String symbol, String apiKey) {
		return BASE_URL + "/query?function=GLOBAL_QUOTE&symbol=" + symbol + "&apikey=" + apiKey;
	}

	private static String globalQuote(String symbol, String price) {
		return """
				{
				  "Global Quote": {
				    "01. symbol": "%s",
				    "05. price": "%s",
				    "07. latest trading day": "2026-08-07"
				  }
				}
				""".formatted(symbol, price);
	}

	private static String exchangeRate(String fromCurrency, String toCurrency, String price) {
		return """
				{
				  "Realtime Currency Exchange Rate": {
				    "1. From_Currency Code": "%s",
				    "3. To_Currency Code": "%s",
				    "5. Exchange Rate": "%s",
				    "6. Last Refreshed": "2026-08-07 12:34:56"
				  }
				}
				""".formatted(fromCurrency, toCurrency, price);
	}

	private static AlphaVantageProperties properties(String apiKey) {
		AlphaVantageProperties properties = new AlphaVantageProperties();
		properties.setBaseUrl(URI.create(BASE_URL));
		properties.setApiKey(apiKey);
		return properties;
	}

	private static Asset asset(Long id, String symbol, AssetType assetType, String currency) {
		Asset asset = new Asset(symbol, symbol, assetType, currency);
		setId(asset, id);
		return asset;
	}

	private static void setId(Asset asset, Long id) {
		try {
			Field idField = Asset.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(asset, id);
		}
		catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private static void assertMarketPrice(
			MarketPrice actual,
			Long assetId,
			String symbol,
			String price,
			String currency) {
		assertEquals(assetId, actual.assetId());
		assertEquals(symbol, actual.symbol());
		assertEquals(new BigDecimal(price), actual.price());
		assertEquals(currency, actual.currency());
	}
}
