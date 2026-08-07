package com.portfoliotracker.backend.transaction;

import java.math.BigDecimal;

import tools.jackson.databind.annotation.JsonSerialize;

public record PositionResponse(
		Long portfolioId,
		Long assetId,
		String assetSymbol,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal quantity,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal averageCost,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal costBasis,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal realizedProfit) {
}
