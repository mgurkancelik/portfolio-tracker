package com.portfoliotracker.backend.transaction;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioTransactionRepository extends JpaRepository<PortfolioTransaction, Long> {

	List<PortfolioTransaction> findAllByPortfolioIdOrderByTransactionDateAscIdAsc(Long portfolioId);
}
