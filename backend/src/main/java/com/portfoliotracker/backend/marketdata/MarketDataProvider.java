package com.portfoliotracker.backend.marketdata;

import com.portfoliotracker.backend.asset.Asset;

public interface MarketDataProvider {

	MarketPrice getCurrentPrice(Asset asset);
}
