package com.portfoliotracker.backend.portfolio;

import java.util.List;
import java.util.Locale;

import jakarta.persistence.EntityManager;

import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioService {

	private final PortfolioRepository portfolioRepository;

	private final EntityManager entityManager;

	public PortfolioService(PortfolioRepository portfolioRepository, EntityManager entityManager) {
		this.portfolioRepository = portfolioRepository;
		this.entityManager = entityManager;
	}

	@Transactional
	public PortfolioResponse createPortfolio(CreatePortfolioRequest request) {
		String normalizedBaseCurrency = request.baseCurrency().toUpperCase(Locale.ROOT);
		Portfolio portfolio = new Portfolio(request.name(), normalizedBaseCurrency);
		Portfolio saved = portfolioRepository.saveAndFlush(portfolio);
		entityManager.refresh(saved);
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public List<PortfolioResponse> listPortfolios() {
		return portfolioRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
				.map(PortfolioService::toResponse)
				.toList();
	}

	@Transactional
	public PortfolioResponse updatePortfolio(Long portfolioId, UpdatePortfolioRequest request) {
		Portfolio portfolio = portfolioRepository.findById(portfolioId)
				.orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
		String normalizedBaseCurrency = request.baseCurrency().toUpperCase(Locale.ROOT);

		portfolio.setName(request.name().trim());
		portfolio.setBaseCurrency(normalizedBaseCurrency);

		Portfolio saved = portfolioRepository.saveAndFlush(portfolio);
		entityManager.refresh(saved);
		return toResponse(saved);
	}

	@Transactional
	public void deletePortfolio(Long portfolioId) {
		Portfolio portfolio = portfolioRepository.findById(portfolioId)
				.orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

		try {
			portfolioRepository.delete(portfolio);
			portfolioRepository.flush();
		}
		catch (DataIntegrityViolationException ex) {
			throw new PortfolioInUseException(portfolioId);
		}
	}

	private static PortfolioResponse toResponse(Portfolio portfolio) {
		return new PortfolioResponse(
				portfolio.getId(),
				portfolio.getName(),
				portfolio.getBaseCurrency(),
				portfolio.getCreatedAt(),
				portfolio.getUpdatedAt());
	}
}
