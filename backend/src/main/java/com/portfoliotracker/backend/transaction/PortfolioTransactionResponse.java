package com.portfoliotracker.backend.transaction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import tools.jackson.databind.annotation.JsonSerialize;

public record PortfolioTransactionResponse(
		Long id,
		Long portfolioId,
		Long assetId,
		String assetSymbol,
		TransactionType transactionType,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal quantity,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal unitPrice,
		@JsonSerialize(using = PlainBigDecimalSerializer.class)
		BigDecimal fee,
		OffsetDateTime transactionDate,
		OffsetDateTime createdAt) {
}
