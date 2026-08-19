package com.portfoliotracker.backend.transaction;

import java.util.List;

import jakarta.validation.Valid;

import com.portfoliotracker.backend.portfolio.PortfolioSummaryResponse;
import com.portfoliotracker.backend.portfolio.PortfolioSummaryService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolios/{portfolioId}")
public class PortfolioTransactionController {

	private final PortfolioTransactionService transactionService;

	private final PortfolioSummaryService portfolioSummaryService;

	public PortfolioTransactionController(
			PortfolioTransactionService transactionService,
			PortfolioSummaryService portfolioSummaryService) {
		this.transactionService = transactionService;
		this.portfolioSummaryService = portfolioSummaryService;
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

	@PutMapping("/transactions/{transactionId}")
	public PortfolioTransactionResponse updateTransaction(
			@PathVariable Long portfolioId,
			@PathVariable Long transactionId,
			@Valid @RequestBody UpdatePortfolioTransactionRequest request) {
		return transactionService.update(portfolioId, transactionId, request);
	}

	@DeleteMapping("/transactions/{transactionId}")
	public ResponseEntity<Void> deleteTransaction(
			@PathVariable Long portfolioId,
			@PathVariable Long transactionId) {
		transactionService.delete(portfolioId, transactionId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/positions")
	public List<PositionResponse> listOpenPositions(@PathVariable Long portfolioId) {
		return transactionService.getOpenPositions(portfolioId);
	}

	@GetMapping("/positions/{assetId}")
	public PositionResponse getPosition(@PathVariable Long portfolioId, @PathVariable Long assetId) {
		return transactionService.getPosition(portfolioId, assetId);
	}

	@GetMapping("/summary")
	public PortfolioSummaryResponse getSummary(@PathVariable Long portfolioId) {
		return portfolioSummaryService.getSummary(portfolioId);
	}
}
