import { BackendErrorState, Dashboard, EmptyPortfolioState } from "@/components/dashboard";
import { loadDashboardData } from "@/lib/dashboard-data";

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

function getSingleValue(value?: string | string[]) {
  return Array.isArray(value) ? value[0] : value;
}
