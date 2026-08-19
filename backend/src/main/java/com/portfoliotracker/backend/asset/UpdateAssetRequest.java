package com.portfoliotracker.backend.asset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAssetRequest(
		@NotBlank
		@Size(max = 20)
		@Pattern(regexp = "[A-Za-z0-9./-]+")
		String symbol,

		@NotBlank
		@Size(max = 150)
		String name,

		@NotNull
		AssetType assetType,

		@NotBlank
		@Pattern(regexp = "[A-Za-z]{3}")
		String currency) {
}
