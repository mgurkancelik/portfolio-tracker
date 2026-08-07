package com.portfoliotracker.backend.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

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
class AssetRepositoryIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
			.withDatabaseName("asset_test")
			.withUsername("asset_test")
			.withPassword("asset_test");

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
	void migrationAndMappingAllowAssetPersistence() throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			String jdbcUrl = connection.getMetaData().getURL();
			assertEquals(postgres.getJdbcUrl(), jdbcUrl);
			assertFalse(jdbcUrl.contains(":5433/"));
		}

		Integer migrationCount = jdbcTemplate.queryForObject("""
				SELECT count(*)
				FROM flyway_schema_history
				WHERE version IN ('1', '2')
				  AND success = true
				""", Integer.class);

		Integer tableCount = jdbcTemplate.queryForObject("""
				SELECT count(*)
				FROM information_schema.tables
				WHERE table_schema = 'public'
				  AND table_name = 'assets'
				""", Integer.class);

		assertEquals(2, migrationCount);
		assertEquals(1, tableCount);

		Asset asset = assetRepository.saveAndFlush(new Asset("AAPL", "Apple Inc.", AssetType.STOCK, "USD"));
		Long assetId = asset.getId();

		entityManager.clear();
		Asset found = assetRepository.findBySymbolAndAssetType("AAPL", AssetType.STOCK).orElseThrow();
		String storedAssetType = jdbcTemplate.queryForObject(
				"SELECT asset_type FROM assets WHERE id = ?",
				String.class,
				assetId);

		assertNotNull(found.getId());
		assertEquals("AAPL", found.getSymbol());
		assertEquals("Apple Inc.", found.getName());
		assertEquals(AssetType.STOCK, found.getAssetType());
		assertEquals("USD", found.getCurrency());
		assertEquals("STOCK", storedAssetType);
		assertNotNull(found.getCreatedAt());
		assertNotNull(found.getUpdatedAt());
	}

	@Test
	void symbolAndAssetTypeMustBeUnique() {
		assetRepository.saveAndFlush(new Asset("AAPL", "Apple Inc.", AssetType.STOCK, "USD"));

		assertThrows(DataIntegrityViolationException.class, () ->
				assetRepository.saveAndFlush(new Asset("AAPL", "Apple Inc.", AssetType.STOCK, "USD")));
	}
}
