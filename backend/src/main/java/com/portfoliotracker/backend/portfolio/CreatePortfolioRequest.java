package com.portfoliotracker.backend.portfolio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePortfolioRequest(
		@NotBlank
		@Size(max = 100)
		String name,

		@NotBlank
		@Pattern(regexp = "[A-Za-z]{3}")
		String baseCurrency) {
}
