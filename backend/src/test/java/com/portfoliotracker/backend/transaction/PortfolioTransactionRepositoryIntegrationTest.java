package com.portfoliotracker.backend.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import javax.sql.DataSource;

import com.portfoliotracker.backend.asset.Asset;
import com.portfoliotracker.backend.asset.AssetRepository;
import com.portfoliotracker.backend.asset.AssetType;
import com.portfoliotracker.backend.portfolio.Portfolio;
import com.portfoliotracker.backend.portfolio.PortfolioRepository;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
@Transactional
class PortfolioTransactionRepositoryIntegrationTest {

	private static final BigDecimal QUANTITY = new BigDecimal("10.00000000");

	private static final BigDecimal UNIT_PRICE = new BigDecimal("180.50000000");

	private static final BigDecimal FEE = new BigDecimal("2.50000000");

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
			.withDatabaseName("transaction_test")
			.withUsername("transaction_test")
			.withPassword("transaction_test");

	@Autowired
	private PortfolioTransactionRepository transactionRepository;

	@Autowired
	private PortfolioRepository portfolioRepository;

	@Autowired
	private AssetRepository assetRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DataSource dataSource;

	@DynamicPropertySource
	static void postgresProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
		registry.add("spring.flyway.enabled", () -> "true");
	}

	@Test
	void migrationAndMappingAllowBuyTransactionPersistence() throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			String jdbcUrl = connection.getMetaData().getURL();
			assertEquals(postgres.getJdbcUrl(), jdbcUrl);
			assertFalse(jdbcUrl.contains(":5433/"));
		}

		Integer migrationCount = jdbcTemplate.queryForObject("""
				SELECT count(*)
				FROM flyway_schema_history
				WHERE version IN ('1', '2', '3')
				  AND success = true
				""", Integer.class);

		Integer tableCount = jdbcTemplate.queryForObject("""
				SELECT count(*)
				FROM information_schema.tables
				WHERE table_schema = 'public'
				  AND table_name = 'transactions'
				""", Integer.class);

		assertEquals(3, migrationCount);
		assertEquals(1, tableCount);

		Portfolio portfolio = createPortfolio();
		Asset asset = createAsset();
		OffsetDateTime transactionDate = OffsetDateTime.of(2026, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC);

		PortfolioTransaction transaction = transactionRepository.saveAndFlush(new PortfolioTransaction(
				portfolio,
				asset,
				TransactionType.BUY,
				QUANTITY,
				UNIT_PRICE,
				FEE,
				transactionDate));
		Long transactionId = transaction.getId();

		entityManager.clear();
		PortfolioTransaction found = transactionRepository.findById(transactionId).orElseThrow();
		String storedTransactionType = jdbcTemplate.queryForObject(
				"SELECT transaction_type FROM transactions WHERE id = ?",
				String.class,
				transactionId);

		assertNotNull(found.getId());
		assertEquals(portfolio.getId(), found.getPortfolio().getId());
		assertEquals(asset.getId(), found.getAsset().getId());
		assertEquals(TransactionType.BUY, found.getTransactionType());
		assertEquals(QUANTITY, found.getQuantity());
		assertEquals(UNIT_PRICE, found.getUnitPrice());
		assertEquals(FEE, found.getFee());
		assertEquals(transactionDate, found.getTransactionDate());
		assertEquals("BUY", storedTransactionType);
		assertNotNull(found.getCreatedAt());
	}

	@Test
	void invalidQuantityIsRejectedByDatabaseConstraint() {
		Portfolio portfolio = createPortfolio();
		Asset asset = createAsset();

		assertThrows(DataIntegrityViolationException.class, () -> transactionRepository.saveAndFlush(
				new PortfolioTransaction(
						portfolio,
						asset,
						TransactionType.BUY,
						new BigDecimal("0.00000000"),
						UNIT_PRICE,
						FEE,
						OffsetDateTime.now(ZoneOffset.UTC))));
	}

	@Test
	void missingPortfolioForeignKeyIsRejected() {
		Asset asset = createAsset();

		assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
				INSERT INTO transactions (
				    portfolio_id, asset_id, transaction_type, quantity, unit_price, fee, transaction_date
				)
				VALUES (?, ?, 'BUY', 1.00000000, 10.00000000, 0.00000000, CURRENT_TIMESTAMP)
				""", -1L, asset.getId()));
	}

	@Test
	void missingAssetForeignKeyIsRejected() {
		Portfolio portfolio = createPortfolio();

		assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
				INSERT INTO transactions (
				    portfolio_id, asset_id, transaction_type, quantity, unit_price, fee, transaction_date
				)
				VALUES (?, ?, 'BUY', 1.00000000, 10.00000000, 0.00000000, CURRENT_TIMESTAMP)
				""", portfolio.getId(), -1L));
	}

	@Test
	void findAllByPortfolioIdOrderByTransactionDateAscIdAscReturnsChronologicalTransactions() {
		Portfolio portfolio = createPortfolio();
		Asset asset = createAsset();
		OffsetDateTime firstDate = OffsetDateTime.of(2026, 1, 1, 9, 0, 0, 0, ZoneOffset.UTC);
		OffsetDateTime secondDate = OffsetDateTime.of(2026, 1, 2, 9, 0, 0, 0, ZoneOffset.UTC);
		OffsetDateTime thirdDate = OffsetDateTime.of(2026, 1, 3, 9, 0, 0, 0, ZoneOffset.UTC);

		PortfolioTransaction later = saveTransaction(portfolio, asset, thirdDate);
		PortfolioTransaction sameDateFirst = saveTransaction(portfolio, asset, secondDate);
		PortfolioTransaction sameDateSecond = saveTransaction(portfolio, asset, secondDate);
		PortfolioTransaction earlier = saveTransaction(portfolio, asset, firstDate);

		entityManager.clear();
		List<PortfolioTransaction> transactions =
				transactionRepository.findAllByPortfolioIdOrderByTransactionDateAscIdAsc(portfolio.getId());

		assertEquals(List.of(
				earlier.getId(),
				sameDateFirst.getId(),
				sameDateSecond.getId(),
				later.getId()), transactions.stream().map(PortfolioTransaction::getId).toList());
	}

	private Portfolio createPortfolio() {
		return portfolioRepository.saveAndFlush(new Portfolio("Uzun Vadeli", "USD"));
	}

	private Asset createAsset() {
		return assetRepository.saveAndFlush(new Asset("AAPL", "Apple Inc.", AssetType.STOCK, "USD"));
	}

	private PortfolioTransaction saveTransaction(Portfolio portfolio, Asset asset, OffsetDateTime transactionDate) {
		return transactionRepository.saveAndFlush(new PortfolioTransaction(
				portfolio,
				asset,
				TransactionType.BUY,
				QUANTITY,
				UNIT_PRICE,
				FEE,
				transactionDate));
	}
}
