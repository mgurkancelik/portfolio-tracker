package com.portfoliotracker.backend.transaction;

public class PortfolioNotFoundException extends RuntimeException {

	public PortfolioNotFoundException(Long portfolioId) {
		super("Portfolio not found: " + portfolioId);
	}
}
