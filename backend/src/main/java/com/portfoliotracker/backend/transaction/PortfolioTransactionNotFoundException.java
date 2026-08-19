package com.portfoliotracker.backend.transaction;

public class PortfolioTransactionNotFoundException extends RuntimeException {

	public PortfolioTransactionNotFoundException(Long transactionId) {
		super("Portfolio transaction not found: " + transactionId);
	}
}
