package com.portfoliotracker.backend.transaction;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.portfoliotracker.backend.asset.Asset;
import com.portfoliotracker.backend.asset.AssetRepository;
import com.portfoliotracker.backend.portfolio.Portfolio;
import com.portfoliotracker.backend.portfolio.PortfolioRepository;
import com.portfoliotracker.backend.portfolio.calculation.PositionCalculator;
import com.portfoliotracker.backend.portfolio.calculation.PositionSummary;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioTransactionService {

	private final PortfolioTransactionRepository transactionRepository;

	private final PortfolioRepository portfolioRepository;

	private final AssetRepository assetRepository;

	private final PositionCalculator positionCalculator;

	private final EntityManager entityManager;

	public PortfolioTransactionService(
			PortfolioTransactionRepository transactionRepository,
			PortfolioRepository portfolioRepository,
			AssetRepository assetRepository,
			PositionCalculator positionCalculator,
			EntityManager entityManager) {
		this.transactionRepository = transactionRepository;
		this.portfolioRepository = portfolioRepository;
		this.assetRepository = assetRepository;
		this.positionCalculator = positionCalculator;
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
					return toPositionResponse(portfolioId, asset, summary);
				})
				.filter(position -> position.quantity().compareTo(BigDecimal.ZERO) > 0)
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

	private static PositionResponse toPositionResponse(Long portfolioId, Asset asset, PositionSummary summary) {
		return new PositionResponse(
				portfolioId,
				asset.getId(),
				asset.getSymbol(),
				asset.getAssetType(),
				asset.getCurrency(),
				summary.quantity(),
				summary.averageCost(),
				summary.costBasis(),
				summary.realizedProfit());
	}
}
