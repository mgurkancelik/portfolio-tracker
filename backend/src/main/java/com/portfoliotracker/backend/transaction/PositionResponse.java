package com.portfoliotracker.backend.transaction;

import java.math.BigDecimal;

import com.portfoliotracker.backend.asset.AssetType;

import tools.jackson.databind.annotation.JsonSerialize;

public record PositionResponse(
		Long portfolioId,
		Long assetId,
		String assetSymbol,
		AssetType assetType,
		String currency,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal quantity,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal averageCost,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal costBasis,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal realizedProfit,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal currentPrice,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal marketValue,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal unrealizedProfit,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal unrealizedProfitPercentage) {
}
