package com.portfoliotracker.backend.marketdata;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import com.portfoliotracker.backend.asset.Asset;
import com.portfoliotracker.backend.asset.AssetType;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("fake-market-data")
public class FakeMarketDataProvider implements MarketDataProvider {

	private static final OffsetDateTime AS_OF = OffsetDateTime.parse("2026-08-07T00:00:00Z");

	private static final BigDecimal CASH_PRICE = new BigDecimal("1.00000000");

	private static final Map<String, FakePrice> PRICES = Map.of(
			"AAPL", new FakePrice(new BigDecimal("150.00000000"), "USD"),
			"BTC", new FakePrice(new BigDecimal("65000.00000000"), "USD"),
			"EURTRY", new FakePrice(new BigDecimal("42.00000000"), "TRY"));

	@Override
	public MarketPrice getCurrentPrice(Asset asset) {
		if (asset.getAssetType() == AssetType.CASH) {
			return new MarketPrice(asset.getId(), asset.getSymbol(), CASH_PRICE, asset.getCurrency(), AS_OF);
		}

		FakePrice fakePrice = PRICES.get(asset.getSymbol());
		if (fakePrice == null) {
			throw new MarketDataNotAvailableException("Market data is not available for symbol: " + asset.getSymbol());
		}
		return new MarketPrice(asset.getId(), asset.getSymbol(), fakePrice.price(), fakePrice.currency(), AS_OF);
	}

	private record FakePrice(BigDecimal price, String currency) {
	}
}
