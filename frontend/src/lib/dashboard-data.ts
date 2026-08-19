import {
  getAssets,
  getPortfolioPositions,
  getPortfolios,
  getPortfolioSummary,
  getPortfolioTransactions,
} from "@/lib/api";
import type { Asset, Portfolio, PortfolioSummary, PortfolioTransaction, Position } from "@/types/api";

export async function loadDashboardData(portfolioIdParam?: string): Promise<DashboardData> {
  try {
    const portfolios = await getPortfolios();

    if (portfolios.length === 0) {
      return { status: "empty" };
    }

    const portfolio = selectPortfolio(portfolios, portfolioIdParam);
    const [assets, summary, positions, transactions] = await Promise.all([
      getAssets(),
      getPortfolioSummary(portfolio.id),
      getPortfolioPositions(portfolio.id),
      getPortfolioTransactions(portfolio.id),
    ]);

    return { assets, portfolio, portfolios, positions, status: "ready", summary, transactions };
  } catch {
    return { status: "backend-error" };
  }
}

function selectPortfolio(portfolios: Portfolio[], portfolioIdParam?: string) {
  const portfolioId = Number(portfolioIdParam);
  if (Number.isInteger(portfolioId)) {
    return portfolios.find((portfolio) => portfolio.id === portfolioId) ?? portfolios[0];
  }
  return portfolios[0];
}

export type DashboardData =
  | { status: "backend-error" }
  | { status: "empty" }
  | {
      assets: Asset[];
      portfolio: Portfolio;
      portfolios: Portfolio[];
      positions: Position[];
      status: "ready";
      summary: PortfolioSummary;
      transactions: PortfolioTransaction[];
    };
