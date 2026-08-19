package com.portfoliotracker.backend.asset;

import java.util.List;
import java.util.Locale;

import jakarta.persistence.EntityManager;

import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssetService {

	private final AssetRepository assetRepository;

	private final EntityManager entityManager;

	public AssetService(AssetRepository assetRepository, EntityManager entityManager) {
		this.assetRepository = assetRepository;
		this.entityManager = entityManager;
	}

	@Transactional
	public AssetResponse create(CreateAssetRequest request) {
		String normalizedSymbol = request.symbol().toUpperCase(Locale.ROOT);
		String normalizedCurrency = request.currency().toUpperCase(Locale.ROOT);
		String trimmedName = request.name().trim();

		assetRepository.findBySymbolAndAssetType(normalizedSymbol, request.assetType())
				.ifPresent(asset -> {
					throw new DuplicateAssetException(normalizedSymbol, request.assetType());
				});

		try {
			Asset asset = new Asset(normalizedSymbol, trimmedName, request.assetType(), normalizedCurrency);
			Asset saved = assetRepository.saveAndFlush(asset);
			entityManager.refresh(saved);
			return toResponse(saved);
		}
		catch (DataIntegrityViolationException ex) {
			throw new DuplicateAssetException(normalizedSymbol, request.assetType());
		}
	}

	@Transactional(readOnly = true)
	public List<AssetResponse> findAll() {
		return assetRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
				.map(AssetService::toResponse)
				.toList();
	}

	@Transactional
	public AssetResponse update(Long assetId, UpdateAssetRequest request) {
		Asset asset = assetRepository.findById(assetId)
				.orElseThrow(() -> new AssetNotFoundException(assetId));
		String normalizedSymbol = request.symbol().toUpperCase(Locale.ROOT);
		String normalizedCurrency = request.currency().toUpperCase(Locale.ROOT);
		String trimmedName = request.name().trim();

		assetRepository.findBySymbolAndAssetType(normalizedSymbol, request.assetType())
				.filter(existingAsset -> !existingAsset.getId().equals(assetId))
				.ifPresent(existingAsset -> {
					throw new DuplicateAssetException(normalizedSymbol, request.assetType());
				});

		asset.setSymbol(normalizedSymbol);
		asset.setName(trimmedName);
		asset.setAssetType(request.assetType());
		asset.setCurrency(normalizedCurrency);

		try {
			Asset saved = assetRepository.saveAndFlush(asset);
			entityManager.refresh(saved);
			return toResponse(saved);
		}
		catch (DataIntegrityViolationException ex) {
			throw new DuplicateAssetException(normalizedSymbol, request.assetType());
		}
	}

	@Transactional
	public void delete(Long assetId) {
		Asset asset = assetRepository.findById(assetId)
				.orElseThrow(() -> new AssetNotFoundException(assetId));

		try {
			assetRepository.delete(asset);
			assetRepository.flush();
		}
		catch (DataIntegrityViolationException ex) {
			throw new AssetInUseException(assetId);
		}
	}

	private static AssetResponse toResponse(Asset asset) {
		return new AssetResponse(
				asset.getId(),
				asset.getSymbol(),
				asset.getName(),
				asset.getAssetType(),
				asset.getCurrency(),
				asset.getCreatedAt(),
				asset.getUpdatedAt());
	}
}
