package com.portfoliotracker.backend.portfolio;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

	private final PortfolioService portfolioService;

	public PortfolioController(PortfolioService portfolioService) {
		this.portfolioService = portfolioService;
	}

	@PostMapping
	public ResponseEntity<PortfolioResponse> createPortfolio(@Valid @RequestBody CreatePortfolioRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(portfolioService.createPortfolio(request));
	}

	@GetMapping
	public List<PortfolioResponse> listPortfolios() {
		return portfolioService.listPortfolios();
	}
}
