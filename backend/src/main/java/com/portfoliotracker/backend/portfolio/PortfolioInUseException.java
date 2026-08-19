package com.portfoliotracker.backend.portfolio;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class PortfolioInUseException extends RuntimeException {

	public PortfolioInUseException(Long portfolioId) {
		super("Portfolio is used by transactions: %d".formatted(portfolioId));
	}
}
