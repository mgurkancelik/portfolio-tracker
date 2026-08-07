package com.portfoliotracker.backend.portfolio.calculation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class PortfolioCalculationConfig {

	@Bean
	PositionCalculator positionCalculator() {
		return new PositionCalculator();
	}

	@Bean
	PositionValuationCalculator positionValuationCalculator() {
		return new PositionValuationCalculator();
	}
}
