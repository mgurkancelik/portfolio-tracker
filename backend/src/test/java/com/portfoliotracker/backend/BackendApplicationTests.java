package com.portfoliotracker.backend;

import com.portfoliotracker.backend.asset.AssetService;
import com.portfoliotracker.backend.portfolio.PortfolioService;
import com.portfoliotracker.backend.portfolio.PortfolioSummaryService;
import com.portfoliotracker.backend.transaction.PortfolioTransactionService;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "spring.autoconfigure.exclude="
		+ "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
		+ "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
		+ "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration")
class BackendApplicationTests {

	@MockitoBean
	private PortfolioService portfolioService;

	@MockitoBean
	private AssetService assetService;

	@MockitoBean
	private PortfolioTransactionService portfolioTransactionService;

	@MockitoBean
	private PortfolioSummaryService portfolioSummaryService;

	@Test
	void contextLoadsWithoutDatabaseAutoConfiguration() {
	}

}
