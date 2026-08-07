package com.portfoliotracker.backend.asset;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "assets")
public class Asset {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "symbol", nullable = false, length = 20)
	private String symbol;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "asset_type", nullable = false, length = 20)
	private AssetType assetType;

	@Column(name = "currency", nullable = false, length = 3)
	private String currency;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime updatedAt;

	protected Asset() {
	}

	public Asset(String symbol, String name, AssetType assetType, String currency) {
		this.symbol = symbol;
		this.name = name;
		this.assetType = assetType;
		this.currency = currency;
	}

	public Long getId() {
		return id;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public AssetType getAssetType() {
		return assetType;
	}

	public void setAssetType(AssetType assetType) {
		this.assetType = assetType;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
