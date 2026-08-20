package com.portfoliotracker.backend.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PortfolioTransactionControllerTest {

	private final PortfolioTransactionService transactionService = mock(PortfolioTransactionService.class);

	private final PortfolioTransactionController controller = new PortfolioTransactionController(transactionService);

	@Test
	void updateTransactionCallsServiceAndReturnsResponse() {
		UpdatePortfolioTransactionRequest request = request();
		PortfolioTransactionResponse expected = response();
		when(transactionService.update(1L, 10L, request)).thenReturn(expected);

		PortfolioTransactionResponse actual = controller.updateTransaction(1L, 10L, request);

		verify(transactionService).update(1L, 10L, request);
		assertEquals(expected, actual);
	}

	@Test
	void deleteTransactionCallsServiceAndReturnsNoContent() {
		ResponseEntity<Void> response = controller.deleteTransaction(1L, 10L);

		verify(transactionService).delete(1L, 10L);
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	private static UpdatePortfolioTransactionRequest request() {
		return new UpdatePortfolioTransactionRequest(
				20L,
				TransactionType.BUY,
				new BigDecimal("10"),
				new BigDecimal("100"),
				BigDecimal.ZERO,
				date());
	}

	private static PortfolioTransactionResponse response() {
		return new PortfolioTransactionResponse(
				10L,
				1L,
				20L,
				"AAPL",
				TransactionType.BUY,
				new BigDecimal("10"),
				new BigDecimal("100"),
				BigDecimal.ZERO,
				date(),
				date());
	}

	private static OffsetDateTime date() {
		return OffsetDateTime.of(2026, 8, 1, 10, 0, 0, 0, ZoneOffset.UTC);
	}
}
