package com.portfoliotracker.backend.portfolio;

import java.math.BigDecimal;

import com.portfoliotracker.backend.transaction.PlainBigDecimalSerializer;

import tools.jackson.databind.annotation.JsonSerialize;

public record CurrencyPortfolioSummary(
		String currency,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal costBasis,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal marketValue,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal unrealizedProfit,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal realizedProfit,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal totalProfit) {
}
