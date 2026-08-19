package com.portfoliotracker.backend.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.portfoliotracker.backend.portfolio.PortfolioSummaryService;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PortfolioTransactionControllerTest {

	private final PortfolioTransactionService transactionService = mock(PortfolioTransactionService.class);

	private final PortfolioSummaryService portfolioSummaryService = mock(PortfolioSummaryService.class);

	private final PortfolioTransactionController controller = new PortfolioTransactionController(
			transactionService,
			portfolioSummaryService);

	@Test
	void deleteTransactionCallsServiceAndReturnsNoContent() {
		ResponseEntity<Void> response = controller.deleteTransaction(1L, 10L);

		verify(transactionService).delete(1L, 10L);
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	}
}
