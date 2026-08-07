package com.portfoliotracker.backend.asset;

import java.time.OffsetDateTime;

public record AssetResponse(
		Long id,
		String symbol,
		String name,
		AssetType assetType,
		String currency,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {
}
