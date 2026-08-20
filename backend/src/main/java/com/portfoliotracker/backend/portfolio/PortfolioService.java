package com.portfoliotracker.backend.portfolio;

import java.util.List;
import java.util.Locale;

import jakarta.persistence.EntityManager;

import com.portfoliotracker.backend.asset.Asset;
import com.portfoliotracker.backend.asset.AssetRepository;
import com.portfoliotracker.backend.asset.AssetType;
import com.portfoliotracker.backend.security.CurrentUserService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioService {

	private final PortfolioRepository portfolioRepository;

	private final AssetRepository assetRepository;

	private final EntityManager entityManager;

	private final CurrentUserService currentUserService;

	public PortfolioService(
			PortfolioRepository portfolioRepository,
			AssetRepository assetRepository,
			EntityManager entityManager,
			CurrentUserService currentUserService) {
		this.portfolioRepository = portfolioRepository;
		this.assetRepository = assetRepository;
		this.entityManager = entityManager;
		this.currentUserService = currentUserService;
	}

	@Transactional
	public PortfolioResponse createPortfolio(CreatePortfolioRequest request) {
		Long userId = currentUserService.currentUserId();
		String normalizedBaseCurrency = request.baseCurrency().toUpperCase(Locale.ROOT);
		Portfolio portfolio = new Portfolio(request.name(), normalizedBaseCurrency, userId);
		Portfolio saved = portfolioRepository.saveAndFlush(portfolio);
		ensureCashAssetExists(normalizedBaseCurrency);
		entityManager.refresh(saved);
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public List<PortfolioResponse> listPortfolios() {
		Long userId = currentUserService.currentUserId();
		return portfolioRepository.findAllByUserIdOrderByIdAsc(userId).stream()
				.map(PortfolioService::toResponse)
				.toList();
	}

	@Transactional
	public PortfolioResponse updatePortfolio(Long portfolioId, UpdatePortfolioRequest request) {
		Portfolio portfolio = getCurrentUserPortfolio(portfolioId);
		String normalizedBaseCurrency = request.baseCurrency().toUpperCase(Locale.ROOT);

		portfolio.setName(request.name().trim());
		portfolio.setBaseCurrency(normalizedBaseCurrency);

		Portfolio saved = portfolioRepository.saveAndFlush(portfolio);
		entityManager.refresh(saved);
		return toResponse(saved);
	}

	@Transactional
	public void deletePortfolio(Long portfolioId) {
		Portfolio portfolio = getCurrentUserPortfolio(portfolioId);

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

	private Portfolio getCurrentUserPortfolio(Long portfolioId) {
		Long userId = currentUserService.currentUserId();
		return portfolioRepository.findByIdAndUserId(portfolioId, userId)
				.orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
	}

	private void ensureCashAssetExists(String currency) {
		assetRepository.findBySymbolAndAssetType(currency, AssetType.CASH)
				.orElseGet(() -> assetRepository.saveAndFlush(new Asset(
						currency,
						currency + " Cash",
						AssetType.CASH,
						currency)));
	}
}
