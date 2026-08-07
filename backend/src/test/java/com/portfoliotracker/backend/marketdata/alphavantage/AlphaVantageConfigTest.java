package com.portfoliotracker.backend.marketdata.alphavantage;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfoliotracker.backend.marketdata.MarketDataProvider;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class AlphaVantageConfigTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withInitializer(context -> context.getEnvironment().setActiveProfiles("alpha-vantage"))
			.withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
			.withUserConfiguration(AlphaVantageConfig.class, AlphaVantageMarketDataProvider.class)
			.withPropertyValues("market-data.alpha-vantage.api-key=test-key");

	@Test
	void alphaVantageProfileCreatesProviderWithAutoConfiguredRestClientBuilder() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(RestClient.Builder.class);
			assertThat(context).hasSingleBean(MarketDataProvider.class);
			assertThat(context).hasSingleBean(AlphaVantageMarketDataProvider.class);
		});
	}
}
