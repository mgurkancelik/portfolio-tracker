package com.portfoliotracker.backend.marketdata;

public class MarketDataNotAvailableException extends RuntimeException {

	public MarketDataNotAvailableException(String message) {
		super(message);
	}
}
