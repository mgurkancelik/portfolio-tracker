package com.portfoliotracker.backend.portfolio.calculation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class PositionValuationCalculatorTest {

	private final PositionValuationCalculator calculator = new PositionValuationCalculator();

	@Test
	void calculatesPositiveUnrealizedProfit() {
		PositionValuation valuation = calculator.calculate(
				summary("12.00000000", "107.66666667", "1292.00000000", "95.00000000"),
				new BigDecimal("150.00000000"));

		assertValuation(valuation, "150.00000000", "1800.00000000", "508.00000000", "39.31888545");
	}

	@Test
	void calculatesNegativeUnrealizedProfit() {
		PositionValuation valuation = calculator.calculate(
				summary("10.00000000", "100.00000000", "1000.00000000", "0.00000000"),
				new BigDecimal("80.00000000"));

		assertValuation(valuation, "80.00000000", "800.00000000", "-200.00000000", "-20.00000000");
	}

	@Test
	void zeroCostBasisReturnsZeroPercentage() {
		PositionValuation valuation = calculator.calculate(
				summary("0.00000000", "0.00000000", "0.00000000", "0.00000000"),
				new BigDecimal("150.00000000"));

		assertValuation(valuation, "150.00000000", "0.00000000", "0.00000000", "0.00000000");
	}

	@Test
	void normalizesPrecisionToEightDecimalPlaces() {
		PositionValuation valuation = calculator.calculate(
				summary("1.00000000", "0.00000000", "0.00000000", "0.00000000"),
				new BigDecimal("0.123456789"));

		assertValuation(valuation, "0.12345679", "0.12345679", "0.12345679", "0.00000000");
	}

	private static PositionSummary summary(
			String quantity,
			String averageCost,
			String costBasis,
			String realizedProfit) {
		return new PositionSummary(
				new BigDecimal(quantity),
				new BigDecimal(averageCost),
				new BigDecimal(costBasis),
				new BigDecimal(realizedProfit));
	}

	private static void assertValuation(
			PositionValuation valuation,
			String currentPrice,
			String marketValue,
			String unrealizedProfit,
			String unrealizedProfitPercentage) {
		assertEquals(new BigDecimal(currentPrice), valuation.currentPrice());
		assertEquals(new BigDecimal(marketValue), valuation.marketValue());
		assertEquals(new BigDecimal(unrealizedProfit), valuation.unrealizedProfit());
		assertEquals(new BigDecimal(unrealizedProfitPercentage), valuation.unrealizedProfitPercentage());
	}
}
