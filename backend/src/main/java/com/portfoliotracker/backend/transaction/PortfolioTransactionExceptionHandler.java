package com.portfoliotracker.backend.transaction;

import com.portfoliotracker.backend.marketdata.MarketDataNotAvailableException;
import com.portfoliotracker.backend.portfolio.calculation.InsufficientPositionException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = PortfolioTransactionController.class)
public class PortfolioTransactionExceptionHandler {

	@ExceptionHandler({
			PortfolioNotFoundException.class,
			AssetNotFoundException.class,
			PortfolioTransactionNotFoundException.class})
	ResponseEntity<Void> handleNotFound() {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
	}

	@ExceptionHandler(InsufficientPositionException.class)
	ResponseEntity<Void> handleInsufficientPosition() {
		return ResponseEntity.status(HttpStatus.CONFLICT).build();
	}

	@ExceptionHandler(InsufficientFundsException.class)
	ResponseEntity<Void> handleInsufficientFunds() {
		return ResponseEntity.status(HttpStatus.CONFLICT).build();
	}

	@ExceptionHandler(MarketDataNotAvailableException.class)
	ResponseEntity<String> handleMarketDataNotAvailable(MarketDataNotAvailableException exception) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(exception.getMessage());
	}
}
