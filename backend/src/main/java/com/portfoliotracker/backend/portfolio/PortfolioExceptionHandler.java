package com.portfoliotracker.backend.portfolio;

import com.portfoliotracker.backend.marketdata.MarketDataNotAvailableException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = PortfolioController.class)
public class PortfolioExceptionHandler {

	@ExceptionHandler(MarketDataNotAvailableException.class)
	ResponseEntity<String> handleMarketDataNotAvailable(MarketDataNotAvailableException exception) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(exception.getMessage());
	}
}
