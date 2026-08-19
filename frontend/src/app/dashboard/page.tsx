import { BackendErrorState, Dashboard, EmptyPortfolioState } from "@/components/dashboard";
import {
  getAssets,
  getPortfolioPositions,
  getPortfolios,
  getPortfolioSummary,
  getPortfolioTransactions,
} from "@/lib/api";
import type { Asset, Portfolio, PortfolioSummary, PortfolioTransaction, Position } from "@/types/api";

export default async function DashboardPage() {
  const dashboardData = await loadDashboardData();

  if (dashboardData.status === "backend-error") {
    return <BackendErrorState />;
  }

  if (dashboardData.status === "empty") {
    return <EmptyPortfolioState />;
  }

  return (
    <Dashboard
      assets={dashboardData.assets}
      portfolio={dashboardData.portfolio}
      positions={dashboardData.positions}
      summary={dashboardData.summary}
      transactions={dashboardData.transactions}
    />
  );
}

async function loadDashboardData(): Promise<DashboardData> {
  try {
    const portfolios = await getPortfolios();

    if (portfolios.length === 0) {
      return { status: "empty" };
    }

    const portfolio = portfolios[0];
    const [assets, summary, positions, transactions] = await Promise.all([
      getAssets(),
      getPortfolioSummary(portfolio.id),
      getPortfolioPositions(portfolio.id),
      getPortfolioTransactions(portfolio.id),
    ]);

    return { assets, portfolio, positions, status: "ready", summary, transactions };
  } catch {
    return { status: "backend-error" };
  }
}

type DashboardData =
  | { status: "backend-error" }
  | { status: "empty" }
  | {
      assets: Asset[];
      portfolio: Portfolio;
      positions: Position[];
      status: "ready";
      summary: PortfolioSummary;
      transactions: PortfolioTransaction[];
    };
