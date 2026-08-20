package com.portfoliotracker.backend.marketdata;

public class MarketDataUnavailableException extends RuntimeException {

	public MarketDataUnavailableException(String message) {
		super(message);
	}
}
