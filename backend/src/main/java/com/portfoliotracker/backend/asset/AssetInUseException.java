package com.portfoliotracker.backend.asset;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AssetInUseException extends RuntimeException {

	public AssetInUseException(Long assetId) {
		super("Asset is used by transactions: %d".formatted(assetId));
	}
}
