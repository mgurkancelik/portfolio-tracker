package com.portfoliotracker.backend.portfolio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

	List<Portfolio> findAllByUserIdOrderByIdAsc(Long userId);

	Optional<Portfolio> findByIdAndUserId(Long id, Long userId);

	boolean existsByIdAndUserId(Long id, Long userId);
}
