package com.portfoliotracker.backend.transaction;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolios/{portfolioId}")
public class PortfolioTransactionController {

	private final PortfolioTransactionService transactionService;

	public PortfolioTransactionController(PortfolioTransactionService transactionService) {
		this.transactionService = transactionService;
	}

	@PostMapping("/transactions")
	public ResponseEntity<PortfolioTransactionResponse> createTransaction(
			@PathVariable Long portfolioId,
			@Valid @RequestBody CreatePortfolioTransactionRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(transactionService.create(portfolioId, request));
	}

	@GetMapping("/transactions")
	public List<PortfolioTransactionResponse> listTransactions(@PathVariable Long portfolioId) {
		return transactionService.findAll(portfolioId);
	}

	@GetMapping("/positions/{assetId}")
	public PositionResponse getPosition(@PathVariable Long portfolioId, @PathVariable Long assetId) {
		return transactionService.getPosition(portfolioId, assetId);
	}
}
