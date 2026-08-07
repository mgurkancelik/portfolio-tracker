package com.portfoliotracker.backend.portfolio.calculation;

import java.math.BigDecimal;

public record PositionValuation(
		BigDecimal currentPrice,
		BigDecimal marketValue,
		BigDecimal unrealizedProfit,
		BigDecimal unrealizedProfitPercentage) {
}
