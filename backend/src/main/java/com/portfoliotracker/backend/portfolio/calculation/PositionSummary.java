package com.portfoliotracker.backend.portfolio.calculation;

import java.math.BigDecimal;

public record PositionSummary(
		BigDecimal quantity,
		BigDecimal averageCost,
		BigDecimal costBasis,
		BigDecimal realizedProfit) {
}
