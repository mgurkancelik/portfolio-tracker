package com.portfoliotracker.backend.portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
class PortfolioRepositoryIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
			.withDatabaseName("portfolio_test")
			.withUsername("portfolio_test")
			.withPassword("portfolio_test");

	@Autowired
	private PortfolioRepository portfolioRepository;

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
	void migrationAndMappingAllowPortfolioPersistence() throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			assertEquals(postgres.getJdbcUrl(), connection.getMetaData().getURL());
		}

		Integer migrationCount = jdbcTemplate.queryForObject("""
				SELECT count(*)
				FROM flyway_schema_history
				WHERE version = '1'
				  AND success = true
				""", Integer.class);

		Integer tableCount = jdbcTemplate.queryForObject("""
				SELECT count(*)
				FROM information_schema.tables
				WHERE table_schema = 'public'
				  AND table_name = 'portfolios'
				""", Integer.class);

		assertEquals(1, migrationCount);
		assertEquals(1, tableCount);

		Portfolio portfolio = portfolioRepository.saveAndFlush(new Portfolio("Long Term", "USD"));
		Long portfolioId = portfolio.getId();

		entityManager.clear();
		Portfolio found = portfolioRepository.findById(portfolioId).orElseThrow();

		assertNotNull(found.getId());
		assertEquals("Long Term", found.getName());
		assertEquals("USD", found.getBaseCurrency());
		assertNotNull(found.getCreatedAt());
		assertNotNull(found.getUpdatedAt());
	}
}
