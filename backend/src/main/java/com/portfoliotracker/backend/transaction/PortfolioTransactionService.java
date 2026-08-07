package com.portfoliotracker.backend.transaction;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.portfoliotracker.backend.asset.Asset;
import com.portfoliotracker.backend.asset.AssetRepository;
import com.portfoliotracker.backend.marketdata.MarketDataNotAvailableException;
import com.portfoliotracker.backend.marketdata.MarketDataProvider;
import com.portfoliotracker.backend.marketdata.MarketPrice;
import com.portfoliotracker.backend.portfolio.Portfolio;
import com.portfoliotracker.backend.portfolio.PortfolioRepository;
import com.portfoliotracker.backend.portfolio.calculation.PositionCalculator;
import com.portfoliotracker.backend.portfolio.calculation.PositionSummary;
import com.portfoliotracker.backend.portfolio.calculation.PositionValuation;
import com.portfoliotracker.backend.portfolio.calculation.PositionValuationCalculator;

import jakarta.persistence.EntityManager;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioTransactionService {

	private final PortfolioTransactionRepository transactionRepository;

	private final PortfolioRepository portfolioRepository;

	private final AssetRepository assetRepository;

	private final PositionCalculator positionCalculator;

	private final PositionValuationCalculator positionValuationCalculator;

	private final ObjectProvider<MarketDataProvider> marketDataProvider;

	private final EntityManager entityManager;

	public PortfolioTransactionService(
			PortfolioTransactionRepository transactionRepository,
			PortfolioRepository portfolioRepository,
			AssetRepository assetRepository,
			PositionCalculator positionCalculator,
			PositionValuationCalculator positionValuationCalculator,
			ObjectProvider<MarketDataProvider> marketDataProvider,
			EntityManager entityManager) {
		this.transactionRepository = transactionRepository;
		this.portfolioRepository = portfolioRepository;
		this.assetRepository = assetRepository;
		this.positionCalculator = positionCalculator;
		this.positionValuationCalculator = positionValuationCalculator;
		this.marketDataProvider = marketDataProvider;
		this.entityManager = entityManager;
	}

	@Transactional
	public PortfolioTransactionResponse create(Long portfolioId, CreatePortfolioTransactionRequest request) {
		Portfolio portfolio = portfolioRepository.findById(portfolioId)
				.orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
		Asset asset = assetRepository.findById(request.assetId())
				.orElseThrow(() -> new AssetNotFoundException(request.assetId()));

		PortfolioTransaction transaction = new PortfolioTransaction(
				portfolio,
				asset,
				request.transactionType(),
				request.quantity(),
				request.unitPrice(),
				request.fee(),
				request.transactionDate());

		PortfolioTransaction saved = transactionRepository.saveAndFlush(transaction);
		List<PortfolioTransaction> history = transactionRepository
				.findAllByPortfolioIdAndAssetIdOrderByTransactionDateAscIdAsc(portfolioId, request.assetId());
		positionCalculator.calculate(history);
		entityManager.refresh(saved);
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public List<PortfolioTransactionResponse> findAll(Long portfolioId) {
		ensurePortfolioExists(portfolioId);
		return transactionRepository.findAllByPortfolioIdOrderByTransactionDateAscIdAsc(portfolioId).stream()
				.map(PortfolioTransactionService::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public PositionResponse getPosition(Long portfolioId, Long assetId) {
		ensurePortfolioExists(portfolioId);
		Asset asset = assetRepository.findById(assetId)
				.orElseThrow(() -> new AssetNotFoundException(assetId));
		List<PortfolioTransaction> transactions = transactionRepository
				.findAllByPortfolioIdAndAssetIdOrderByTransactionDateAscIdAsc(portfolioId, assetId);
		PositionSummary summary = positionCalculator.calculate(transactions);
		return toPositionResponse(portfolioId, asset, summary);
	}

	@Transactional(readOnly = true)
	public List<PositionResponse> getOpenPositions(Long portfolioId) {
		ensurePortfolioExists(portfolioId);
		List<PortfolioTransaction> transactions = transactionRepository
				.findAllByPortfolioIdOrderByTransactionDateAscIdAsc(portfolioId);

		Map<Long, List<PortfolioTransaction>> transactionsByAssetId = transactions.stream()
				.collect(Collectors.groupingBy(
						transaction -> transaction.getAsset().getId(),
						LinkedHashMap::new,
						Collectors.toList()));

		return transactionsByAssetId.values().stream()
				.map(assetTransactions -> {
					Asset asset = assetTransactions.get(0).getAsset();
					PositionSummary summary = positionCalculator.calculate(assetTransactions);
					return new PositionData(asset, summary);
				})
				.filter(position -> position.summary().quantity().compareTo(BigDecimal.ZERO) > 0)
				.map(position -> toPositionResponse(portfolioId, position.asset(), position.summary()))
				.sorted(Comparator
						.comparing(PositionResponse::assetSymbol)
						.thenComparing(PositionResponse::assetId))
				.toList();
	}

	private void ensurePortfolioExists(Long portfolioId) {
		if (!portfolioRepository.existsById(portfolioId)) {
			throw new PortfolioNotFoundException(portfolioId);
		}
	}

	private static PortfolioTransactionResponse toResponse(PortfolioTransaction transaction) {
		return new PortfolioTransactionResponse(
				transaction.getId(),
				transaction.getPortfolio().getId(),
				transaction.getAsset().getId(),
				transaction.getAsset().getSymbol(),
				transaction.getTransactionType(),
				transaction.getQuantity(),
				transaction.getUnitPrice(),
				transaction.getFee(),
				transaction.getTransactionDate(),
				transaction.getCreatedAt());
	}

	private PositionResponse toPositionResponse(Long portfolioId, Asset asset, PositionSummary summary) {
		PositionValuation valuation = calculateValuation(asset, summary);
		return new PositionResponse(
				portfolioId,
				asset.getId(),
				asset.getSymbol(),
				asset.getAssetType(),
				asset.getCurrency(),
				summary.quantity(),
				summary.averageCost(),
				summary.costBasis(),
				summary.realizedProfit(),
				valuation.currentPrice(),
				valuation.marketValue(),
				valuation.unrealizedProfit(),
				valuation.unrealizedProfitPercentage());
	}

	private PositionValuation calculateValuation(Asset asset, PositionSummary summary) {
		MarketDataProvider provider = marketDataProvider.getIfAvailable(() -> {
			throw new MarketDataNotAvailableException("Market data provider is not configured.");
		});
		MarketPrice marketPrice = provider.getCurrentPrice(asset);
		if (!asset.getCurrency().equals(marketPrice.currency())) {
			throw new MarketDataNotAvailableException(
					"Market data currency %s does not match asset currency %s for symbol %s."
							.formatted(marketPrice.currency(), asset.getCurrency(), asset.getSymbol()));
		}
		return positionValuationCalculator.calculate(summary, marketPrice.price());
	}

	private record PositionData(Asset asset, PositionSummary summary) {
	}
}
