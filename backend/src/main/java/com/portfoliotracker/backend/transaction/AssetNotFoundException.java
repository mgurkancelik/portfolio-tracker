package com.portfoliotracker.backend.transaction;

public class AssetNotFoundException extends RuntimeException {

	public AssetNotFoundException(Long assetId) {
		super("Asset not found: " + assetId);
	}
}
