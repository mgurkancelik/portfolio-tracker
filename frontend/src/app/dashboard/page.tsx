import { BackendErrorState, Dashboard, EmptyPortfolioState } from "@/components/dashboard";
import {
  getAssets,
  getPortfolioPositions,
  getPortfolios,
  getPortfolioSummary,
  getPortfolioTransactions,
} from "@/lib/api";
import type { Asset, Portfolio, PortfolioSummary, PortfolioTransaction, Position } from "@/types/api";

type DashboardPageProps = {
  searchParams?: Promise<{
    portfolioId?: string | string[];
  }>;
};

export default async function DashboardPage({ searchParams }: DashboardPageProps) {
  const resolvedSearchParams = searchParams ? await searchParams : {};
  const dashboardData = await loadDashboardData(getSingleValue(resolvedSearchParams.portfolioId));

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
      portfolios={dashboardData.portfolios}
      positions={dashboardData.positions}
      summary={dashboardData.summary}
      transactions={dashboardData.transactions}
    />
  );
}

async function loadDashboardData(portfolioIdParam?: string): Promise<DashboardData> {
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

function getSingleValue(value?: string | string[]) {
  return Array.isArray(value) ? value[0] : value;
}

function selectPortfolio(portfolios: Portfolio[], portfolioIdParam?: string) {
  const portfolioId = Number(portfolioIdParam);
  if (Number.isInteger(portfolioId)) {
    return portfolios.find((portfolio) => portfolio.id === portfolioId) ?? portfolios[0];
  }
  return portfolios[0];
}

type DashboardData =
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
