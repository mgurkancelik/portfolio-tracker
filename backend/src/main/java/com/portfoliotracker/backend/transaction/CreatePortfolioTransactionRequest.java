package com.portfoliotracker.backend.transaction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CreatePortfolioTransactionRequest(
		@NotNull
		@Positive
		Long assetId,

		@NotNull
		TransactionType transactionType,

		@NotNull
		@Positive
		BigDecimal quantity,

		@NotNull
		@Positive
		BigDecimal unitPrice,

		@NotNull
		@PositiveOrZero
		BigDecimal fee,

		@NotNull
		OffsetDateTime transactionDate) {
}
