package com.portfoliotracker.backend.asset;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateAssetException extends RuntimeException {

	public DuplicateAssetException(String symbol, AssetType assetType) {
		super("Asset already exists for symbol %s and type %s.".formatted(symbol, assetType));
	}
}
