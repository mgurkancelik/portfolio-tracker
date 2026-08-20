package com.portfoliotracker.backend.portfolio;

import com.portfoliotracker.backend.marketdata.MarketDataUnavailableException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = PortfolioController.class)
public class PortfolioExceptionHandler {

	@ExceptionHandler(MarketDataUnavailableException.class)
	ResponseEntity<String> handleMarketDataUnavailable(MarketDataUnavailableException exception) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(exception.getMessage());
	}
}
