package com.portfoliotracker.backend.marketdata;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MarketPrice(
		Long assetId,
		String symbol,
		BigDecimal price,
		String currency,
		OffsetDateTime asOf) {
}
