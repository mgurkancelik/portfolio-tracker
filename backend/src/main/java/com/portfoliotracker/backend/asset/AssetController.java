package com.portfoliotracker.backend.asset;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

	private final AssetService assetService;

	public AssetController(AssetService assetService) {
		this.assetService = assetService;
	}

	@PostMapping
	public ResponseEntity<AssetResponse> createAsset(@Valid @RequestBody CreateAssetRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(assetService.create(request));
	}

	@GetMapping
	public List<AssetResponse> listAssets() {
		return assetService.findAll();
	}

	@PutMapping("/{assetId}")
	public AssetResponse updateAsset(
			@PathVariable Long assetId,
			@Valid @RequestBody UpdateAssetRequest request) {
		return assetService.update(assetId, request);
	}

	@DeleteMapping("/{assetId}")
	public ResponseEntity<Void> deleteAsset(@PathVariable Long assetId) {
		assetService.delete(assetId);
		return ResponseEntity.noContent().build();
	}
}
