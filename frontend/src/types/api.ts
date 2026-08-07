export type Portfolio = {
  id: number;
  name: string;
  baseCurrency: string;
  createdAt: string;
  updatedAt: string;
};

export type AssetType = "STOCK" | "CRYPTO" | "FOREX";

export type Position = {
  portfolioId: number;
  assetId: number;
  assetSymbol: string;
  assetType: AssetType;
  currency: string;
  quantity: number;
  averageCost: number;
  costBasis: number;
  realizedProfit: number;
  currentPrice: number;
  marketValue: number;
  unrealizedProfit: number;
  unrealizedProfitPercentage: number;
};

export type CurrencyPortfolioSummary = {
  currency: string;
  costBasis: number;
  marketValue: number;
  unrealizedProfit: number;
  realizedProfit: number;
  totalProfit: number;
};

export type PortfolioSummary = {
  portfolioId: number;
  portfolioName: string;
  baseCurrency: string;
  openPositionCount: number;
  totalsByCurrency: CurrencyPortfolioSummary[];
};
