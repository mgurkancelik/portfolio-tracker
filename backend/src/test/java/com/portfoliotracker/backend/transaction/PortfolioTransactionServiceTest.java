package com.portfoliotracker.backend.transaction;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.portfoliotracker.backend.asset.Asset;
import com.portfoliotracker.backend.asset.AssetRepository;
import com.portfoliotracker.backend.asset.AssetType;
import com.portfoliotracker.backend.marketdata.MarketDataProvider;
import com.portfoliotracker.backend.portfolio.Portfolio;
import com.portfoliotracker.backend.portfolio.PortfolioRepository;
import com.portfoliotracker.backend.portfolio.calculation.InsufficientPositionException;
import com.portfoliotracker.backend.portfolio.calculation.PositionCalculator;
import com.portfoliotracker.backend.portfolio.calculation.PositionValuationCalculator;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

class PortfolioTransactionServiceTest {

	private final PortfolioTransactionRepository transactionRepository = mock(PortfolioTransactionRepository.class);

	private final PortfolioRepository portfolioRepository = mock(PortfolioRepository.class);

	private final AssetRepository assetRepository = mock(AssetRepository.class);

	private final PositionCalculator positionCalculator = mock(PositionCalculator.class);

	private final PositionValuationCalculator positionValuationCalculator = mock(PositionValuationCalculator.class);

	@SuppressWarnings("unchecked")
	private final ObjectProvider<MarketDataProvider> marketDataProvider = mock(ObjectProvider.class);

	private final EntityManager entityManager = mock(EntityManager.class);

	private PortfolioTransactionService service;

	@BeforeEach
	void setUp() {
		service = new PortfolioTransactionService(
				transactionRepository,
				portfolioRepository,
				assetRepository,
				positionCalculator,
				positionValuationCalculator,
				marketDataProvider,
				entityManager);
	}

	@Test
	void deleteRemovesTransactionAfterValidatingRemainingHistory() {
		Portfolio portfolio = portfolio(1L);
		Asset asset = asset(10L);
		PortfolioTransaction transactionToDelete = transaction(100L, portfolio, asset, TransactionType.SELL, "3", 3);
		PortfolioTransaction remainingTransaction = transaction(101L, portfolio, asset, TransactionType.BUY, "10", 1);
		when(portfolioRepository.existsById(portfolio.getId())).thenReturn(true);
		when(transactionRepository.findByIdAndPortfolioId(transactionToDelete.getId(), portfolio.getId()))
				.thenReturn(Optional.of(transactionToDelete));
		when(transactionRepository.findAllByPortfolioIdAndAssetIdOrderByTransactionDateAscIdAsc(
				portfolio.getId(),
				asset.getId()))
				.thenReturn(List.of(remainingTransaction, transactionToDelete));

		service.delete(portfolio.getId(), transactionToDelete.getId());

		verify(positionCalculator).calculate(List.of(remainingTransaction));
		verify(transactionRepository).delete(transactionToDelete);
	}

	@Test
	void deleteDoesNotRemoveTransactionWhenRemainingHistoryWouldOversell() {
		Portfolio portfolio = portfolio(1L);
		Asset asset = asset(10L);
		PortfolioTransaction transactionToDelete = transaction(100L, portfolio, asset, TransactionType.BUY, "10", 1);
		PortfolioTransaction remainingTransaction = transaction(101L, portfolio, asset, TransactionType.SELL, "8", 2);
		when(portfolioRepository.existsById(portfolio.getId())).thenReturn(true);
		when(transactionRepository.findByIdAndPortfolioId(transactionToDelete.getId(), portfolio.getId()))
				.thenReturn(Optional.of(transactionToDelete));
		when(transactionRepository.findAllByPortfolioIdAndAssetIdOrderByTransactionDateAscIdAsc(
				portfolio.getId(),
				asset.getId()))
				.thenReturn(List.of(transactionToDelete, remainingTransaction));
		when(positionCalculator.calculate(anyList()))
				.thenThrow(new InsufficientPositionException("Insufficient position."));

		assertThrows(
				InsufficientPositionException.class,
				() -> service.delete(portfolio.getId(), transactionToDelete.getId()));

		verify(transactionRepository, never()).delete(transactionToDelete);
	}

	private static Portfolio portfolio(Long id) {
		Portfolio portfolio = new Portfolio("Ana Portfoy", "TRY");
		ReflectionTestUtils.setField(portfolio, "id", id);
		return portfolio;
	}

	private static Asset asset(Long id) {
		Asset asset = new Asset("AAPL", "Apple Inc.", AssetType.STOCK, "USD");
		ReflectionTestUtils.setField(asset, "id", id);
		return asset;
	}

	private static PortfolioTransaction transaction(
			Long id,
			Portfolio portfolio,
			Asset asset,
			TransactionType transactionType,
			String quantity,
			int dayOfMonth) {
		PortfolioTransaction transaction = new PortfolioTransaction(
				portfolio,
				asset,
				transactionType,
				new BigDecimal(quantity),
				new BigDecimal("100"),
				BigDecimal.ZERO,
				OffsetDateTime.of(2026, 8, dayOfMonth, 10, 0, 0, 0, ZoneOffset.UTC));
		ReflectionTestUtils.setField(transaction, "id", id);
		return transaction;
	}
}
