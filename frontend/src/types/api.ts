export type Portfolio = {
  id: number;
  name: string;
  baseCurrency: string;
  createdAt: string;
  updatedAt: string;
};

export type AssetType = "STOCK" | "CRYPTO" | "FOREX" | "CASH";

export type Asset = {
  id: number;
  symbol: string;
  name: string;
  assetType: AssetType;
  currency: string;
  createdAt: string;
  updatedAt: string;
};

export type TransactionType = "BUY" | "SELL";

export type CreatePortfolioInput = {
  name: string;
  baseCurrency: string;
};

export type UpdatePortfolioInput = CreatePortfolioInput;

export type CreateAssetInput = {
  symbol: string;
  name: string;
  assetType: AssetType;
  currency: string;
};

export type UpdateAssetInput = CreateAssetInput;

export type CreatePortfolioTransactionInput = {
  assetId: number;
  transactionType: TransactionType;
  quantity: number;
  unitPrice: number;
  fee: number;
  transactionDate: string;
};

export type UpdatePortfolioTransactionInput = CreatePortfolioTransactionInput;

export type AuthInput = {
  email: string;
  password: string;
};

export type TokenResponse = {
  token: string;
};

export type PortfolioTransaction = {
  id: number;
  portfolioId: number;
  assetId: number;
  assetSymbol: string;
  transactionType: TransactionType;
  quantity: number;
  unitPrice: number;
  fee: number;
  transactionDate: string;
  createdAt: string;
};

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
  totalPortfolioValue: number;
  totalCashBalance: number;
  totalUnrealizedProfit: number;
  totalUnrealizedProfitPercentage: number;
  openPositionCount: number;
  totalsByCurrency: CurrencyPortfolioSummary[];
};
