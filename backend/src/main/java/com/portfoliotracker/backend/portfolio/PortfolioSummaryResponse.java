package com.portfoliotracker.backend.portfolio;

import java.util.List;

public record PortfolioSummaryResponse(
		Long portfolioId,
		String portfolioName,
		String baseCurrency,
		int openPositionCount,
		List<CurrencyPortfolioSummary> totalsByCurrency) {
}
