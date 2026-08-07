package com.portfoliotracker.backend.portfolio.calculation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class PositionValuationCalculator {

	private static final int SUMMARY_SCALE = 8;

	private static final BigDecimal HUNDRED = new BigDecimal("100");

	private static final BigDecimal ZERO = BigDecimal.ZERO;

	public PositionValuation calculate(PositionSummary summary, BigDecimal currentPrice) {
		Objects.requireNonNull(summary, "summary must not be null");
		Objects.requireNonNull(currentPrice, "currentPrice must not be null");

		BigDecimal normalizedCurrentPrice = normalize(currentPrice);
		BigDecimal marketValue = normalize(summary.quantity().multiply(normalizedCurrentPrice));
		BigDecimal unrealizedProfit = normalize(marketValue.subtract(summary.costBasis()));
		BigDecimal unrealizedProfitPercentage = summary.costBasis().compareTo(ZERO) > 0
				? normalize(unrealizedProfit.multiply(HUNDRED).divide(summary.costBasis(), SUMMARY_SCALE, RoundingMode.HALF_EVEN))
				: normalize(ZERO);

		return new PositionValuation(
				normalizedCurrentPrice,
				marketValue,
				unrealizedProfit,
				unrealizedProfitPercentage);
	}

	private static BigDecimal normalize(BigDecimal value) {
		return value.setScale(SUMMARY_SCALE, RoundingMode.HALF_EVEN);
	}
}
