package com.portfoliotracker.backend.portfolio.calculation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import com.portfoliotracker.backend.asset.Asset;
import com.portfoliotracker.backend.asset.AssetType;
import com.portfoliotracker.backend.portfolio.Portfolio;
import com.portfoliotracker.backend.transaction.PortfolioTransaction;
import com.portfoliotracker.backend.transaction.TransactionType;

import org.junit.jupiter.api.Test;

class PositionCalculatorTest {

	private final PositionCalculator calculator = new PositionCalculator();

	private final Portfolio portfolio = new Portfolio("Uzun Vadeli", "USD");

	private final Asset aapl = new Asset("AAPL", "Apple Inc.", AssetType.STOCK, "USD");

	@Test
	void singleBuyCalculatesPosition() {
		PositionSummary summary = calculator.calculate(List.of(
				transaction(TransactionType.BUY, "10", "100", "0", date(1))));

		assertSummary(summary, "10.00000000", "100.00000000", "1000.00000000", "0.00000000");
	}

	@Test
	void multipleBuysIncludeFeesInAverageCost() {
		PositionSummary summary = calculator.calculate(List.of(
				transaction(TransactionType.BUY, "10", "100", "10", date(1)),
				transaction(TransactionType.BUY, "5", "120", "5", date(2))));

		assertSummary(summary, "15.00000000", "107.66666667", "1615.00000000", "0.00000000");
	}

	@Test
	void partialSellRemovesProportionalCostBasisAndRealizesProfit() {
		PositionSummary summary = calculator.calculate(twoBuysAndPartialSell());

		assertSummary(summary, "12.00000000", "107.66666667", "1292.00000000", "95.00000000");
	}

	@Test
	void fullSellClosesPositionAndKeepsRealizedProfit() {
		List<PortfolioTransaction> transactions = new ArrayList<>(twoBuysAndPartialSell());
		transactions.add(transaction(TransactionType.SELL, "12", "110", "0", date(4)));

		PositionSummary summary = calculator.calculate(transactions);

		assertSummary(summary, "0.00000000", "0.00000000", "0.00000000", "123.00000000");
	}

	@Test
	void oversellThrowsInsufficientPositionException() {
		List<PortfolioTransaction> transactions = List.of(
				transaction(TransactionType.BUY, "5", "100", "0", date(1)),
				transaction(TransactionType.SELL, "6", "100", "0", date(2)));

		assertThrows(InsufficientPositionException.class, () -> calculator.calculate(transactions));
	}

	@Test
	void sellAsFirstTransactionThrowsInsufficientPositionException() {
		List<PortfolioTransaction> transactions = List.of(
				transaction(TransactionType.SELL, "1", "100", "0", date(1)));

		assertThrows(InsufficientPositionException.class, () -> calculator.calculate(transactions));
	}

	@Test
	void transactionsAreCalculatedChronologicallyEvenWhenInputIsOutOfOrder() {
		PortfolioTransaction buy = transaction(TransactionType.BUY, "5", "100", "0", date(1));
		PortfolioTransaction sell = transaction(TransactionType.SELL, "2", "120", "0", date(2));

		PositionSummary summary = calculator.calculate(List.of(sell, buy));

		assertSummary(summary, "3.00000000", "100.00000000", "300.00000000", "40.00000000");
	}

	@Test
	void inputListIsNotMutated() {
		PortfolioTransaction buy = transaction(TransactionType.BUY, "5", "100", "0", date(1));
		PortfolioTransaction sell = transaction(TransactionType.SELL, "2", "120", "0", date(2));
		List<PortfolioTransaction> transactions = new ArrayList<>(List.of(sell, buy));

		calculator.calculate(transactions);

		assertSame(sell, transactions.get(0));
		assertSame(buy, transactions.get(1));
	}

	@Test
	void mixedAssetsAreRejected() {
		Asset msft = new Asset("MSFT", "Microsoft Corp.", AssetType.STOCK, "USD");
		List<PortfolioTransaction> transactions = List.of(
				transaction(aapl, TransactionType.BUY, "5", "100", "0", date(1)),
				transaction(msft, TransactionType.BUY, "5", "100", "0", date(2)));

		assertThrows(IllegalArgumentException.class, () -> calculator.calculate(transactions));
	}

	private List<PortfolioTransaction> twoBuysAndPartialSell() {
		return List.of(
				transaction(TransactionType.BUY, "10", "100", "10", date(1)),
				transaction(TransactionType.BUY, "5", "120", "5", date(2)),
				transaction(TransactionType.SELL, "3", "140", "2", date(3)));
	}

	private PortfolioTransaction transaction(
			TransactionType transactionType,
			String quantity,
			String unitPrice,
			String fee,
			OffsetDateTime transactionDate) {
		return transaction(aapl, transactionType, quantity, unitPrice, fee, transactionDate);
	}

	private PortfolioTransaction transaction(
			Asset asset,
			TransactionType transactionType,
			String quantity,
			String unitPrice,
			String fee,
			OffsetDateTime transactionDate) {
		return new PortfolioTransaction(
				portfolio,
				asset,
				transactionType,
				new BigDecimal(quantity),
				new BigDecimal(unitPrice),
				new BigDecimal(fee),
				transactionDate);
	}

	private static OffsetDateTime date(int dayOfMonth) {
		return OffsetDateTime.of(2026, 1, dayOfMonth, 10, 0, 0, 0, ZoneOffset.UTC);
	}

	private static void assertSummary(
			PositionSummary summary,
			String quantity,
			String averageCost,
			String costBasis,
			String realizedProfit) {
		assertEquals(new BigDecimal(quantity), summary.quantity());
		assertEquals(new BigDecimal(averageCost), summary.averageCost());
		assertEquals(new BigDecimal(costBasis), summary.costBasis());
		assertEquals(new BigDecimal(realizedProfit), summary.realizedProfit());
	}
}
