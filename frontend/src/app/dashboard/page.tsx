import { BackendErrorState, Dashboard, EmptyPortfolioState } from "@/components/dashboard";
import { getPortfolioPositions, getPortfolios, getPortfolioSummary } from "@/lib/api";
import type { Portfolio, PortfolioSummary, Position } from "@/types/api";

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
      portfolio={dashboardData.portfolio}
      positions={dashboardData.positions}
      summary={dashboardData.summary}
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
    const [summary, positions] = await Promise.all([
      getPortfolioSummary(portfolio.id),
      getPortfolioPositions(portfolio.id),
    ]);

    return { portfolio, positions, status: "ready", summary };
  } catch {
    return { status: "backend-error" };
  }
}

type DashboardData =
  | { status: "backend-error" }
  | { status: "empty" }
  | {
      portfolio: Portfolio;
      positions: Position[];
      status: "ready";
      summary: PortfolioSummary;
    };
