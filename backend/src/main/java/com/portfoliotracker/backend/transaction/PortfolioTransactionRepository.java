package com.portfoliotracker.backend.transaction;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioTransactionRepository extends JpaRepository<PortfolioTransaction, Long> {

	Optional<PortfolioTransaction> findByIdAndPortfolioId(Long id, Long portfolioId);

	List<PortfolioTransaction> findAllByPortfolioIdOrderByTransactionDateAscIdAsc(Long portfolioId);

	List<PortfolioTransaction> findAllByPortfolioIdAndAssetIdOrderByTransactionDateAscIdAsc(
			Long portfolioId,
			Long assetId);
}
