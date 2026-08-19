package com.portfoliotracker.backend.portfolio;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PortfolioNotFoundException extends RuntimeException {

	public PortfolioNotFoundException(Long portfolioId) {
		super("Portfolio not found: %d".formatted(portfolioId));
	}
}
