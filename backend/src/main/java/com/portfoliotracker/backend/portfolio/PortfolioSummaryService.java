package com.portfoliotracker.backend.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.portfoliotracker.backend.asset.Asset;
import com.portfoliotracker.backend.asset.AssetType;
import com.portfoliotracker.backend.marketdata.MarketDataProvider;
import com.portfoliotracker.backend.marketdata.MarketPrice;
import com.portfoliotracker.backend.marketdata.MarketDataUnavailableException;
import com.portfoliotracker.backend.portfolio.calculation.PositionCalculator;
import com.portfoliotracker.backend.portfolio.calculation.PositionSummary;
import com.portfoliotracker.backend.portfolio.calculation.PositionValuation;
import com.portfoliotracker.backend.portfolio.calculation.PositionValuationCalculator;
import com.portfoliotracker.backend.security.CurrentUserService;
import com.portfoliotracker.backend.transaction.PortfolioTransaction;
import com.portfoliotracker.backend.transaction.PortfolioTransactionRepository;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioSummaryService {

	private static final int SUMMARY_SCALE = 8;

	private static final BigDecimal ZERO = BigDecimal.ZERO;

	private final PortfolioRepository portfolioRepository;

	private final PortfolioTransactionRepository transactionRepository;

	private final PositionCalculator positionCalculator;

	private final PositionValuationCalculator positionValuationCalculator;

	private final ObjectProvider<MarketDataProvider> marketDataProvider;

	private final CurrentUserService currentUserService;

	public PortfolioSummaryService(
			PortfolioRepository portfolioRepository,
			PortfolioTransactionRepository transactionRepository,
			PositionCalculator positionCalculator,
			PositionValuationCalculator positionValuationCalculator,
			ObjectProvider<MarketDataProvider> marketDataProvider,
			CurrentUserService currentUserService) {
		this.portfolioRepository = portfolioRepository;
		this.transactionRepository = transactionRepository;
		this.positionCalculator = positionCalculator;
		this.positionValuationCalculator = positionValuationCalculator;
		this.marketDataProvider = marketDataProvider;
		this.currentUserService = currentUserService;
	}

	@Transactional(readOnly = true)
	public PortfolioSummaryResponse getSummary(Long portfolioId) {
		Long userId = currentUserService.currentUserId();
		Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
				.orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
		List<PortfolioTransaction> transactions = transactionRepository
				.findAllByPortfolioIdOrderByTransactionDateAscIdAsc(portfolioId);

		Map<Long, List<PortfolioTransaction>> transactionsByAssetId = transactions.stream()
				.collect(Collectors.groupingBy(
						transaction -> transaction.getAsset().getId(),
						LinkedHashMap::new,
						Collectors.toList()));

		Map<String, CurrencyTotals> totalsByCurrency = new TreeMap<>();
		SummaryTotals summaryTotals = new SummaryTotals();
		int openPositionCount = 0;

		for (List<PortfolioTransaction> assetTransactions : transactionsByAssetId.values()) {
			Asset asset = assetTransactions.get(0).getAsset();
			PositionSummary summary = positionCalculator.calculate(assetTransactions);
			CurrencyTotals totals = totalsByCurrency.computeIfAbsent(asset.getCurrency(), ignored -> new CurrencyTotals());
			totals.addRealizedProfit(summary.realizedProfit());

			if (asset.getAssetType() == AssetType.CASH) {
				BigDecimal cashBalance = normalize(summary.quantity());
				summaryTotals.addCashBalance(cashBalance);
				totals.addCostBasis(cashBalance);
				totals.addMarketValue(cashBalance);
				continue;
			}

			if (summary.quantity().compareTo(ZERO) <= 0) {
				continue;
			}

			PositionValuation valuation = calculateValuation(asset, summary);
			summaryTotals.addInvestment(summary.costBasis(), valuation.marketValue(), valuation.unrealizedProfit());
			totals.addCostBasis(summary.costBasis());
			totals.addMarketValue(valuation.marketValue());
			totals.addUnrealizedProfit(valuation.unrealizedProfit());
			openPositionCount++;
		}

		List<CurrencyPortfolioSummary> totals = totalsByCurrency.entrySet().stream()
				.map(entry -> entry.getValue().toSummary(entry.getKey()))
				.toList();

		return new PortfolioSummaryResponse(
				portfolio.getId(),
				portfolio.getName(),
				portfolio.getBaseCurrency(),
				summaryTotals.totalPortfolioValue(),
				summaryTotals.totalCashBalance(),
				summaryTotals.totalUnrealizedProfit(),
				summaryTotals.totalUnrealizedProfitPercentage(),
				openPositionCount,
				totals);
	}

	private PositionValuation calculateValuation(Asset asset, PositionSummary summary) {
		MarketDataProvider provider = marketDataProvider.getIfAvailable(() -> {
			throw new MarketDataUnavailableException("Market data provider is not configured.");
		});
		MarketPrice marketPrice = provider.getCurrentPrice(asset);
		if (!asset.getCurrency().equals(marketPrice.currency())) {
			throw new MarketDataUnavailableException(
					"Market data currency %s does not match asset currency %s for symbol %s."
							.formatted(marketPrice.currency(), asset.getCurrency(), asset.getSymbol()));
		}
		return positionValuationCalculator.calculate(summary, marketPrice.price());
	}

	private static BigDecimal normalize(BigDecimal value) {
		return value.setScale(SUMMARY_SCALE, RoundingMode.HALF_EVEN);
	}

	private static class CurrencyTotals {

		private BigDecimal costBasis = ZERO;

		private BigDecimal marketValue = ZERO;

		private BigDecimal unrealizedProfit = ZERO;

		private BigDecimal realizedProfit = ZERO;

		void addCostBasis(BigDecimal amount) {
			costBasis = costBasis.add(amount);
		}

		void addMarketValue(BigDecimal amount) {
			marketValue = marketValue.add(amount);
		}

		void addUnrealizedProfit(BigDecimal amount) {
			unrealizedProfit = unrealizedProfit.add(amount);
		}

		void addRealizedProfit(BigDecimal amount) {
			realizedProfit = realizedProfit.add(amount);
		}

		CurrencyPortfolioSummary toSummary(String currency) {
			BigDecimal normalizedUnrealizedProfit = normalize(unrealizedProfit);
			BigDecimal normalizedRealizedProfit = normalize(realizedProfit);
			return new CurrencyPortfolioSummary(
					currency,
					normalize(costBasis),
					normalize(marketValue),
					normalizedUnrealizedProfit,
					normalizedRealizedProfit,
					normalize(normalizedRealizedProfit.add(normalizedUnrealizedProfit)));
		}
	}

	private static class SummaryTotals {

		private BigDecimal totalInvestmentCostBasis = ZERO;

		private BigDecimal totalInvestmentMarketValue = ZERO;

		private BigDecimal totalCashBalance = ZERO;

		private BigDecimal totalUnrealizedProfit = ZERO;

		void addCashBalance(BigDecimal amount) {
			totalCashBalance = totalCashBalance.add(amount);
		}

		void addInvestment(BigDecimal costBasis, BigDecimal marketValue, BigDecimal unrealizedProfit) {
			totalInvestmentCostBasis = totalInvestmentCostBasis.add(costBasis);
			totalInvestmentMarketValue = totalInvestmentMarketValue.add(marketValue);
			totalUnrealizedProfit = totalUnrealizedProfit.add(unrealizedProfit);
		}

		BigDecimal totalPortfolioValue() {
			return normalize(totalInvestmentMarketValue.add(totalCashBalance));
		}

		BigDecimal totalCashBalance() {
			return normalize(totalCashBalance);
		}

		BigDecimal totalUnrealizedProfit() {
			return normalize(totalUnrealizedProfit);
		}

		BigDecimal totalUnrealizedProfitPercentage() {
			if (totalInvestmentCostBasis.compareTo(ZERO) == 0) {
				return normalize(ZERO);
			}
			return normalize(totalUnrealizedProfit
					.multiply(new BigDecimal("100"))
					.divide(totalInvestmentCostBasis, SUMMARY_SCALE, RoundingMode.HALF_EVEN));
		}
	}
}
