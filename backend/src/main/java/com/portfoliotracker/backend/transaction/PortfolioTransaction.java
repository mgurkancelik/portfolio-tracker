package com.portfoliotracker.backend.transaction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.portfoliotracker.backend.asset.Asset;
import com.portfoliotracker.backend.portfolio.Portfolio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "transactions")
public class PortfolioTransaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "portfolio_id", nullable = false)
	private Portfolio portfolio;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "asset_id", nullable = false)
	private Asset asset;

	@Enumerated(EnumType.STRING)
	@Column(name = "transaction_type", nullable = false, length = 10)
	private TransactionType transactionType;

	@Column(name = "quantity", nullable = false, precision = 24, scale = 8)
	private BigDecimal quantity;

	@Column(name = "unit_price", nullable = false, precision = 24, scale = 8)
	private BigDecimal unitPrice;

	@Column(name = "fee", nullable = false, precision = 24, scale = 8)
	private BigDecimal fee;

	@Column(name = "transaction_date", nullable = false)
	private OffsetDateTime transactionDate;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	protected PortfolioTransaction() {
	}

	public PortfolioTransaction(
			Portfolio portfolio,
			Asset asset,
			TransactionType transactionType,
			BigDecimal quantity,
			BigDecimal unitPrice,
			BigDecimal fee,
			OffsetDateTime transactionDate) {
		this.portfolio = portfolio;
		this.asset = asset;
		this.transactionType = transactionType;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
		this.fee = fee;
		this.transactionDate = transactionDate;
	}

	public Long getId() {
		return id;
	}

	public Portfolio getPortfolio() {
		return portfolio;
	}

	public void setPortfolio(Portfolio portfolio) {
		this.portfolio = portfolio;
	}

	public Asset getAsset() {
		return asset;
	}

	public void setAsset(Asset asset) {
		this.asset = asset;
	}

	public TransactionType getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(TransactionType transactionType) {
		this.transactionType = transactionType;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(BigDecimal unitPrice) {
		this.unitPrice = unitPrice;
	}

	public BigDecimal getFee() {
		return fee;
	}

	public void setFee(BigDecimal fee) {
		this.fee = fee;
	}

	public OffsetDateTime getTransactionDate() {
		return transactionDate;
	}

	public void setTransactionDate(OffsetDateTime transactionDate) {
		this.transactionDate = transactionDate;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
