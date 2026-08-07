package com.portfoliotracker.backend.portfolio.calculation;

public class InsufficientPositionException extends RuntimeException {

	public InsufficientPositionException(String message) {
		super(message);
	}
}
