package com.portfoliotracker.backend.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.portfoliotracker.backend.asset.Asset;
import com.portfoliotracker.backend.asset.AssetRepository;
import com.portfoliotracker.backend.asset.AssetType;
import com.portfoliotracker.backend.marketdata.MarketDataProvider;
import com.portfoliotracker.backend.marketdata.MarketPrice;
import com.portfoliotracker.backend.marketdata.MarketDataUnavailableException;
import com.portfoliotracker.backend.portfolio.Portfolio;
import com.portfoliotracker.backend.portfolio.PortfolioRepository;
import com.portfoliotracker.backend.portfolio.calculation.InsufficientPositionException;
import com.portfoliotracker.backend.portfolio.calculation.PositionCalculator;
import com.portfoliotracker.backend.portfolio.calculation.PositionSummary;
import com.portfoliotracker.backend.portfolio.calculation.PositionValuation;
import com.portfoliotracker.backend.portfolio.calculation.PositionValuationCalculator;
import com.portfoliotracker.backend.security.CurrentUserService;

import jakarta.persistence.EntityManager;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioTransactionService {

	private static final int MONEY_SCALE = 8;

	private static final BigDecimal CASH_UNIT_PRICE = new BigDecimal("1.00000000");

	private static final BigDecimal ZERO = BigDecimal.ZERO;

	private final PortfolioTransactionRepository transactionRepository;

	private final PortfolioRepository portfolioRepository;

	private final AssetRepository assetRepository;

	private final PositionCalculator positionCalculator;

	private final PositionValuationCalculator positionValuationCalculator;

	private final ObjectProvider<MarketDataProvider> marketDataProvider;

	private final EntityManager entityManager;

	private final CurrentUserService currentUserService;

	public PortfolioTransactionService(
			PortfolioTransactionRepository transactionRepository,
			PortfolioRepository portfolioRepository,
			AssetRepository assetRepository,
			PositionCalculator positionCalculator,
			PositionValuationCalculator positionValuationCalculator,
			ObjectProvider<MarketDataProvider> marketDataProvider,
			EntityManager entityManager,
			CurrentUserService currentUserService) {
		this.transactionRepository = transactionRepository;
		this.portfolioRepository = portfolioRepository;
		this.assetRepository = assetRepository;
		this.positionCalculator = positionCalculator;
		this.positionValuationCalculator = positionValuationCalculator;
		this.marketDataProvider = marketDataProvider;
		this.entityManager = entityManager;
		this.currentUserService = currentUserService;
	}

	@Transactional
	public PortfolioTransactionResponse create(Long portfolioId, CreatePortfolioTransactionRequest request) {
		Portfolio portfolio = getCurrentUserPortfolio(portfolioId);
		Asset asset = assetRepository.findById(request.assetId())
				.orElseThrow(() -> new AssetNotFoundException(request.assetId()));

		if (asset.getAssetType() == AssetType.CASH) {
			PortfolioTransaction saved = saveAndValidateTransaction(portfolioId, newTransaction(portfolio, asset, request));
			entityManager.refresh(saved);
			return toResponse(saved);
		}

		Asset cashAsset = ensureCashAssetExists(portfolio.getBaseCurrency());
		if (request.transactionType() == TransactionType.BUY) {
			BigDecimal totalCost = totalCost(request);
			ensureSufficientCash(portfolioId, cashAsset, totalCost);

			PortfolioTransaction saved = saveAndValidateTransaction(portfolioId, newTransaction(portfolio, asset, request));
			saveAndValidateCashTransaction(
					portfolioId,
					new PortfolioTransaction(
							portfolio,
							cashAsset,
							TransactionType.SELL,
							totalCost,
							CASH_UNIT_PRICE,
							ZERO,
							request.transactionDate()));
			entityManager.refresh(saved);
			return toResponse(saved);
		}

		BigDecimal netProceeds = netSaleProceeds(request);
		if (netProceeds.compareTo(ZERO) <= 0) {
			throw new InsufficientFundsException("Sell proceeds must be greater than fee.");
		}

		PortfolioTransaction saved = saveAndValidateTransaction(portfolioId, newTransaction(portfolio, asset, request));
		saveAndValidateCashTransaction(
				portfolioId,
				new PortfolioTransaction(
						portfolio,
						cashAsset,
						TransactionType.BUY,
						netProceeds,
						CASH_UNIT_PRICE,
						ZERO,
						request.transactionDate()));
		entityManager.refresh(saved);
		return toResponse(saved);
	}

	private PortfolioTransaction newTransaction(
			Portfolio portfolio,
			Asset asset,
			CreatePortfolioTransactionRequest request) {
		PortfolioTransaction transaction = new PortfolioTransaction(
				portfolio,
				asset,
				request.transactionType(),
				request.quantity(),
				request.unitPrice(),
				request.fee(),
				request.transactionDate());
		return transaction;
	}

	private PortfolioTransaction saveAndValidateTransaction(Long portfolioId, PortfolioTransaction transaction) {
		PortfolioTransaction saved = transactionRepository.saveAndFlush(transaction);
		validatePositionHistory(portfolioId, transaction.getAsset());
		return saved;
	}

	private void saveAndValidateCashTransaction(Long portfolioId, PortfolioTransaction cashTransaction) {
		transactionRepository.saveAndFlush(cashTransaction);
		validatePositionHistory(portfolioId, cashTransaction.getAsset());
	}

	private void validatePositionHistory(Long portfolioId, Asset asset) {
		List<PortfolioTransaction> history = transactionRepository
				.findAllByPortfolioIdAndAssetIdOrderByTransactionDateAscIdAsc(portfolioId, asset.getId());
		try {
			positionCalculator.calculate(history);
		}
		catch (InsufficientPositionException exception) {
			if (asset.getAssetType() == AssetType.CASH) {
				throw new InsufficientFundsException("Cash balance is not sufficient.");
			}
			throw exception;
		}
	}

	private Asset ensureCashAssetExists(String currency) {
		return assetRepository.findBySymbolAndAssetType(currency, AssetType.CASH)
				.orElseGet(() -> assetRepository.saveAndFlush(new Asset(
						currency,
						currency + " Cash",
						AssetType.CASH,
						currency)));
	}

	private void ensureSufficientCash(Long portfolioId, Asset cashAsset, BigDecimal requiredCash) {
		BigDecimal availableCash = currentCashQuantity(portfolioId, cashAsset);
		if (requiredCash.compareTo(availableCash) > 0) {
			throw new InsufficientFundsException("Cash balance is not sufficient.");
		}
	}

	private BigDecimal currentCashQuantity(Long portfolioId, Asset cashAsset) {
		List<PortfolioTransaction> history = transactionRepository
				.findAllByPortfolioIdAndAssetIdOrderByTransactionDateAscIdAsc(portfolioId, cashAsset.getId());
		try {
			return positionCalculator.calculate(history).quantity();
		}
		catch (InsufficientPositionException exception) {
			throw new InsufficientFundsException("Cash balance is not sufficient.");
		}
	}

	private static BigDecimal totalCost(CreatePortfolioTransactionRequest request) {
		return normalizeMoney(request.quantity().multiply(request.unitPrice()).add(request.fee()));
	}

	private static BigDecimal netSaleProceeds(CreatePortfolioTransactionRequest request) {
		return normalizeMoney(request.quantity().multiply(request.unitPrice()).subtract(request.fee()));
	}

	private static BigDecimal normalizeMoney(BigDecimal value) {
		return value.setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
	}

	@Transactional(readOnly = true)
	public List<PortfolioTransactionResponse> findAll(Long portfolioId) {
		ensurePortfolioExists(portfolioId);
		return transactionRepository.findAllByPortfolioIdOrderByTransactionDateAscIdAsc(portfolioId).stream()
				.map(PortfolioTransactionService::toResponse)
				.toList();
	}

	@Transactional
	public PortfolioTransactionResponse update(
			Long portfolioId,
			Long transactionId,
			UpdatePortfolioTransactionRequest request) {
		ensurePortfolioExists(portfolioId);
		PortfolioTransaction transaction = transactionRepository.findByIdAndPortfolioId(transactionId, portfolioId)
				.orElseThrow(() -> new PortfolioTransactionNotFoundException(transactionId));
		Asset asset = assetRepository.findById(request.assetId())
				.orElseThrow(() -> new AssetNotFoundException(request.assetId()));
		Long previousAssetId = transaction.getAsset().getId();
		Long updatedAssetId = asset.getId();
		List<PortfolioTransaction> previousAssetHistory = transactionRepository
				.findAllByPortfolioIdAndAssetIdOrderByTransactionDateAscIdAsc(portfolioId, previousAssetId);
		List<PortfolioTransaction> updatedAssetHistory = Objects.equals(previousAssetId, updatedAssetId)
				? previousAssetHistory
				: transactionRepository.findAllByPortfolioIdAndAssetIdOrderByTransactionDateAscIdAsc(
						portfolioId,
						updatedAssetId);

		transaction.setAsset(asset);
		transaction.setTransactionType(request.transactionType());
		transaction.setQuantity(request.quantity());
		transaction.setUnitPrice(request.unitPrice());
		transaction.setFee(request.fee());
		transaction.setTransactionDate(request.transactionDate());

		validateUpdatedHistories(transaction, previousAssetId, updatedAssetId, previousAssetHistory, updatedAssetHistory);
		PortfolioTransaction saved = transactionRepository.saveAndFlush(transaction);
		entityManager.refresh(saved);
		return toResponse(saved);
	}

	@Transactional
	public void delete(Long portfolioId, Long transactionId) {
		ensurePortfolioExists(portfolioId);
		PortfolioTransaction transaction = transactionRepository.findByIdAndPortfolioId(transactionId, portfolioId)
				.orElseThrow(() -> new PortfolioTransactionNotFoundException(transactionId));
		Long assetId = transaction.getAsset().getId();
		List<PortfolioTransaction> remainingHistory = transactionRepository
				.findAllByPortfolioIdAndAssetIdOrderByTransactionDateAscIdAsc(portfolioId, assetId)
				.stream()
				.filter(historyTransaction -> !historyTransaction.getId().equals(transactionId))
				.toList();

		positionCalculator.calculate(remainingHistory);
		transactionRepository.delete(transaction);
	}

	private void validateUpdatedHistories(
			PortfolioTransaction updatedTransaction,
			Long previousAssetId,
			Long updatedAssetId,
			List<PortfolioTransaction> previousAssetHistory,
			List<PortfolioTransaction> updatedAssetHistory) {
		if (Objects.equals(previousAssetId, updatedAssetId)) {
			positionCalculator.calculate(previousAssetHistory);
			return;
		}

		positionCalculator.calculate(previousAssetHistory.stream()
				.filter(historyTransaction -> !historyTransaction.getId().equals(updatedTransaction.getId()))
				.toList());

		List<PortfolioTransaction> nextAssetHistory = new ArrayList<>(updatedAssetHistory);
		nextAssetHistory.add(updatedTransaction);
		positionCalculator.calculate(nextAssetHistory);
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
		Long userId = currentUserService.currentUserId();
		if (!portfolioRepository.existsByIdAndUserId(portfolioId, userId)) {
			throw new PortfolioNotFoundException(portfolioId);
		}
	}

	private Portfolio getCurrentUserPortfolio(Long portfolioId) {
		Long userId = currentUserService.currentUserId();
		return portfolioRepository.findByIdAndUserId(portfolioId, userId)
				.orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
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

	private record PositionData(Asset asset, PositionSummary summary) {
	}
}
