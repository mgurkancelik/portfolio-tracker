package com.portfoliotracker.backend.portfolio.calculation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.portfoliotracker.backend.asset.Asset;
import com.portfoliotracker.backend.transaction.PortfolioTransaction;
import com.portfoliotracker.backend.transaction.TransactionType;

public class PositionCalculator {

	private static final int SUMMARY_SCALE = 8;

	private static final int INTERNAL_SCALE = 16;

	private static final BigDecimal ZERO = BigDecimal.ZERO;

	public PositionSummary calculate(List<PortfolioTransaction> transactions) {
		if (transactions == null || transactions.isEmpty()) {
			return summary(ZERO, ZERO, ZERO);
		}

		List<PortfolioTransaction> chronologicalTransactions = transactions.stream()
				.map(transaction -> Objects.requireNonNull(transaction, "transaction must not be null"))
				.sorted(Comparator
						.comparing(PortfolioTransaction::getTransactionDate)
						.thenComparing(PortfolioTransaction::getId, Comparator.nullsLast(Comparator.naturalOrder())))
				.toList();

		validateSingleAsset(chronologicalTransactions);

		BigDecimal quantity = ZERO;
		BigDecimal costBasis = ZERO;
		BigDecimal realizedProfit = ZERO;

		for (PortfolioTransaction transaction : chronologicalTransactions) {
			if (transaction.getTransactionType() == TransactionType.BUY) {
				BigDecimal buyCost = transaction.getQuantity()
						.multiply(transaction.getUnitPrice())
						.add(transaction.getFee());
				quantity = quantity.add(transaction.getQuantity());
				costBasis = costBasis.add(buyCost);
			}
			else if (transaction.getTransactionType() == TransactionType.SELL) {
				if (transaction.getQuantity().compareTo(quantity) > 0) {
					throw new InsufficientPositionException("Sell quantity exceeds current position quantity.");
				}

				boolean closesPosition = transaction.getQuantity().compareTo(quantity) == 0;
				BigDecimal costRemoved = closesPosition
						? costBasis
						: costBasis.multiply(transaction.getQuantity()).divide(quantity, INTERNAL_SCALE, RoundingMode.HALF_EVEN);
				BigDecimal saleProceeds = transaction.getQuantity().multiply(transaction.getUnitPrice());
				realizedProfit = realizedProfit.add(saleProceeds).subtract(costRemoved).subtract(transaction.getFee());
				quantity = quantity.subtract(transaction.getQuantity());
				costBasis = closesPosition ? ZERO : costBasis.subtract(costRemoved);
			}
			else {
				throw new IllegalArgumentException("Unsupported transaction type: " + transaction.getTransactionType());
			}

			if (quantity.compareTo(ZERO) == 0) {
				costBasis = ZERO;
			}
		}

		return summary(quantity, costBasis, realizedProfit);
	}

	private static void validateSingleAsset(List<PortfolioTransaction> transactions) {
		Asset firstAsset = transactions.get(0).getAsset();
		Long firstAssetId = firstAsset.getId();

		for (PortfolioTransaction transaction : transactions) {
			Asset asset = transaction.getAsset();
			Long assetId = asset.getId();
			boolean samePersistedAsset = firstAssetId != null && assetId != null && firstAssetId.equals(assetId);
			boolean sameTransientAsset = firstAssetId == null && assetId == null && firstAsset == asset;
			if (!samePersistedAsset && !sameTransientAsset) {
				throw new IllegalArgumentException("Position calculation requires transactions for a single asset.");
			}
		}
	}

	private static PositionSummary summary(BigDecimal quantity, BigDecimal costBasis, BigDecimal realizedProfit) {
		BigDecimal averageCost = quantity.compareTo(ZERO) == 0
				? ZERO
				: costBasis.divide(quantity, SUMMARY_SCALE, RoundingMode.HALF_EVEN);
		return new PositionSummary(
				normalize(quantity),
				normalize(averageCost),
				normalize(costBasis),
				normalize(realizedProfit));
	}

	private static BigDecimal normalize(BigDecimal value) {
		return value.setScale(SUMMARY_SCALE, RoundingMode.HALF_EVEN);
	}
}
