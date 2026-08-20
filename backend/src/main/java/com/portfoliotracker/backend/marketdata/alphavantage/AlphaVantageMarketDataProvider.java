package com.portfoliotracker.backend.marketdata.alphavantage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.portfoliotracker.backend.asset.Asset;
import com.portfoliotracker.backend.asset.AssetType;
import com.portfoliotracker.backend.marketdata.MarketDataNotAvailableException;
import com.portfoliotracker.backend.marketdata.MarketDataProvider;
import com.portfoliotracker.backend.marketdata.MarketPrice;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

@Component
@Profile("alpha-vantage")
public class AlphaVantageMarketDataProvider implements MarketDataProvider {

	private static final int PRICE_SCALE = 8;

	private static final BigDecimal CASH_PRICE = new BigDecimal("1.00000000");

	private static final BigDecimal ZERO = BigDecimal.ZERO;

	private static final String QUERY_PATH = "/query";

	private static final String GLOBAL_QUOTE = "Global Quote";

	private static final String EXCHANGE_RATE = "Realtime Currency Exchange Rate";

	private static final ParameterizedTypeReference<Map<String, Object>> RESPONSE_TYPE =
			new ParameterizedTypeReference<>() {
			};

	private static final DateTimeFormatter EXCHANGE_RATE_AS_OF_FORMAT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final RestClient restClient;

	private final AlphaVantageProperties properties;

	public AlphaVantageMarketDataProvider(
			@Qualifier("alphaVantageRestClient") RestClient restClient,
			AlphaVantageProperties properties) {
		this.restClient = restClient;
		this.properties = properties;
	}

	@Override
	public MarketPrice getCurrentPrice(Asset asset) {
		return switch (asset.getAssetType()) {
			case CASH -> getCashPrice(asset);
			case STOCK -> {
				ensureApiKeyConfigured();
				yield getStockPrice(asset);
			}
			case CRYPTO -> getExchangeRatePrice(
					asset,
					normalizeSymbol(asset.getSymbol()),
					normalizeCurrency(asset.getCurrency(), "asset currency"));
			case FOREX -> {
				ForexPair forexPair = parseForexSymbol(asset.getSymbol(), asset.getCurrency());
				yield getExchangeRatePrice(asset, forexPair.baseCurrency(), forexPair.quoteCurrency());
			}
		};
	}

	private MarketPrice getCashPrice(Asset asset) {
		return new MarketPrice(
				asset.getId(),
				normalizeSymbol(asset.getSymbol()),
				CASH_PRICE,
				normalizeCurrency(asset.getCurrency(), "asset currency"),
				nowUtc());
	}

	private MarketPrice getStockPrice(Asset asset) {
		Map<String, Object> response = fetch(queryParameters(
				"function", "GLOBAL_QUOTE",
				"symbol", normalizeSymbol(asset.getSymbol()),
				"apikey", properties.getApiKey()));

		Map<?, ?> quote = responseSection(response, GLOBAL_QUOTE, "Alpha Vantage global quote is not available.");
		if (quote.isEmpty()) {
			throw unavailable("Alpha Vantage global quote is empty.");
		}

		BigDecimal price = positivePrice(quote, "05. price");
		return new MarketPrice(
				asset.getId(),
				normalizeSymbol(asset.getSymbol()),
				price,
				normalizeCurrency(asset.getCurrency(), "asset currency"),
				stockAsOf(quote));
	}

	private MarketPrice getExchangeRatePrice(Asset asset, String fromCurrency, String toCurrency) {
		ensureApiKeyConfigured();

		Map<String, Object> response = fetch(queryParameters(
				"function", "CURRENCY_EXCHANGE_RATE",
				"from_currency", fromCurrency,
				"to_currency", toCurrency,
				"apikey", properties.getApiKey()));

		Map<?, ?> exchangeRate = responseSection(
				response,
				EXCHANGE_RATE,
				"Alpha Vantage exchange rate is not available.");
		if (exchangeRate.isEmpty()) {
			throw unavailable("Alpha Vantage exchange rate is empty.");
		}

		validateCurrencyField(exchangeRate, "3. To_Currency Code", toCurrency);
		BigDecimal price = positivePrice(exchangeRate, "5. Exchange Rate");
		return new MarketPrice(
				asset.getId(),
				normalizeSymbol(asset.getSymbol()),
				price,
				toCurrency,
				exchangeRateAsOf(exchangeRate));
	}

	private Map<String, Object> fetch(Map<String, String> queryParameters) {
		try {
			Map<String, Object> body = restClient.get()
					.uri(uriBuilder -> buildUri(uriBuilder, queryParameters))
					.retrieve()
					.onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
						throw unavailable("Alpha Vantage request failed with HTTP status "
								+ clientResponse.getStatusCode().value() + ".");
					})
					.body(RESPONSE_TYPE);

			if (body == null || body.isEmpty()) {
				throw unavailable("Alpha Vantage response is empty.");
			}

			rejectServiceMessages(body);
			return body;
		}
		catch (MarketDataNotAvailableException exception) {
			throw exception;
		}
		catch (RestClientException exception) {
			throw unavailable("Alpha Vantage request failed.");
		}
	}

	private static java.net.URI buildUri(UriBuilder uriBuilder, Map<String, String> queryParameters) {
		UriBuilder builder = uriBuilder.path(QUERY_PATH);
		queryParameters.forEach((name, value) -> builder.queryParam(name, value));
		return builder.build();
	}

	private void ensureApiKeyConfigured() {
		if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
			throw unavailable("Alpha Vantage API key is not configured.");
		}
	}

	private static Map<String, String> queryParameters(String firstName, String firstValue, String... remaining) {
		Map<String, String> parameters = new LinkedHashMap<>();
		parameters.put(firstName, firstValue);
		for (int i = 0; i < remaining.length; i += 2) {
			parameters.put(remaining[i], remaining[i + 1]);
		}
		return parameters;
	}

	private static Map<?, ?> responseSection(Map<String, Object> response, String key, String missingMessage) {
		Object section = response.get(key);
		if (!(section instanceof Map<?, ?> map)) {
			throw unavailable(missingMessage);
		}
		return map;
	}

	private static void rejectServiceMessages(Map<String, Object> response) {
		for (String key : new String[] {"Error Message", "Information", "Note"}) {
			Object message = response.get(key);
			if (message != null && !message.toString().isBlank()) {
				throw unavailable("Alpha Vantage did not return market data: " + key + ".");
			}
		}
	}

	private static BigDecimal positivePrice(Map<?, ?> values, String fieldName) {
		String rawValue = textValue(values.get(fieldName));
		if (rawValue.isBlank()) {
			throw unavailable("Alpha Vantage price field is missing.");
		}

		BigDecimal price;
		try {
			price = new BigDecimal(rawValue);
		}
		catch (NumberFormatException exception) {
			throw unavailable("Alpha Vantage price field is not numeric.");
		}

		if (price.compareTo(ZERO) <= 0) {
			throw unavailable("Alpha Vantage price must be greater than zero.");
		}
		return price.setScale(PRICE_SCALE, RoundingMode.HALF_EVEN);
	}

	private static void validateCurrencyField(Map<?, ?> values, String fieldName, String expectedCurrency) {
		String actualCurrency = textValue(values.get(fieldName));
		if (actualCurrency.isBlank()) {
			throw unavailable("Alpha Vantage quote currency field is missing.");
		}
		if (!actualCurrency.toUpperCase(Locale.ROOT).equals(expectedCurrency)) {
			throw unavailable("Alpha Vantage returned an unexpected quote currency.");
		}
	}

	private static OffsetDateTime stockAsOf(Map<?, ?> quote) {
		String latestTradingDay = textValue(quote.get("07. latest trading day"));
		if (!latestTradingDay.isBlank()) {
			try {
				return LocalDate.parse(latestTradingDay).atStartOfDay().atOffset(ZoneOffset.UTC);
			}
			catch (DateTimeParseException exception) {
				return nowUtc();
			}
		}
		return nowUtc();
	}

	private static OffsetDateTime exchangeRateAsOf(Map<?, ?> exchangeRate) {
		String lastRefreshed = textValue(exchangeRate.get("6. Last Refreshed"));
		if (!lastRefreshed.isBlank()) {
			try {
				return LocalDateTime.parse(lastRefreshed, EXCHANGE_RATE_AS_OF_FORMAT).atOffset(ZoneOffset.UTC);
			}
			catch (DateTimeParseException exception) {
				return nowUtc();
			}
		}
		return nowUtc();
	}

	private static OffsetDateTime nowUtc() {
		return OffsetDateTime.now(ZoneOffset.UTC).withNano(0);
	}

	static ForexPair parseForexSymbol(String symbol, String assetCurrency) {
		String normalizedSymbol = normalizeSymbol(symbol);
		String quoteCurrency = normalizeCurrency(assetCurrency, "asset currency");

		ForexPair forexPair;
		if (normalizedSymbol.contains("/") || normalizedSymbol.contains("-")) {
			String[] parts = normalizedSymbol.split("[/-]", -1);
			if (parts.length != 2) {
				throw unavailable("Forex symbol format is not supported.");
			}
			forexPair = new ForexPair(parts[0], parts[1]);
		}
		else if (normalizedSymbol.length() == 6) {
			forexPair = new ForexPair(normalizedSymbol.substring(0, 3), normalizedSymbol.substring(3, 6));
		}
		else {
			throw unavailable("Forex symbol format is not supported.");
		}

		if (!isThreeLetterCurrency(forexPair.baseCurrency()) || !isThreeLetterCurrency(forexPair.quoteCurrency())) {
			throw unavailable("Forex symbol currencies must use three alphabetic characters.");
		}
		if (!forexPair.quoteCurrency().equals(quoteCurrency)) {
			throw unavailable("Forex quote currency must match the asset currency.");
		}
		return forexPair;
	}

	private static String normalizeSymbol(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw unavailable("Asset symbol is required for market data.");
		}
		return symbol.trim().toUpperCase(Locale.ROOT);
	}

	private static String normalizeCurrency(String currency, String fieldName) {
		if (currency == null || currency.isBlank()) {
			throw unavailable(fieldName + " is required for market data.");
		}
		return currency.trim().toUpperCase(Locale.ROOT);
	}

	private static boolean isThreeLetterCurrency(String value) {
		return value != null && value.matches("[A-Z]{3}");
	}

	private static String textValue(Object value) {
		return value == null ? "" : value.toString().trim();
	}

	private static MarketDataNotAvailableException unavailable(String message) {
		return new MarketDataNotAvailableException(message);
	}

	record ForexPair(String baseCurrency, String quoteCurrency) {
	}
}
