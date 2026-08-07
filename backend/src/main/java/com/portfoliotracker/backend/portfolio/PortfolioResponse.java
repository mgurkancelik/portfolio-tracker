package com.portfoliotracker.backend.portfolio;

import java.time.OffsetDateTime;

public record PortfolioResponse(
		Long id,
		String name,
		String baseCurrency,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {
}
