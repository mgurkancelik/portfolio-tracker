package com.portfoliotracker.backend.portfolio;

import java.math.BigDecimal;
import java.util.List;

import com.portfoliotracker.backend.transaction.PlainBigDecimalSerializer;

import tools.jackson.databind.annotation.JsonSerialize;

public record PortfolioSummaryResponse(
		Long portfolioId,
		String portfolioName,
		String baseCurrency,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal totalPortfolioValue,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal totalCashBalance,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal totalUnrealizedProfit,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal totalUnrealizedProfitPercentage,
		int openPositionCount,
		List<CurrencyPortfolioSummary> totalsByCurrency) {
}
