package com.portfoliotracker.backend.transaction;

public class InsufficientFundsException extends RuntimeException {

	public InsufficientFundsException(String message) {
		super(message);
	}
}
