package com.portfoliotracker.backend.asset;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, Long> {

	Optional<Asset> findBySymbolAndAssetType(String symbol, AssetType assetType);
}
