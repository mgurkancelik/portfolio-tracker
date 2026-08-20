import { BackendErrorState, Dashboard, EmptyPortfolioState } from "@/components/dashboard";
import { productMenuItems } from "@/data/navigation";
import { loadDashboardData } from "@/lib/dashboard-data";
import type { AssetType } from "@/types/api";

type DashboardPageProps = {
  searchParams?: Promise<{
    assetType?: string | string[];
    currency?: string | string[];
    portfolioId?: string | string[];
    product?: string | string[];
  }>;
};

export default async function DashboardPage({ searchParams }: DashboardPageProps) {
  const resolvedSearchParams = searchParams ? await searchParams : {};
  const selectedProduct = getProduct(getSingleValue(resolvedSearchParams.product));
  const assetTypeFilter = getAssetTypeFilter(
    getSingleValue(resolvedSearchParams.assetType) ?? selectedProduct?.assetType,
  );
  const currencyFilter = getCurrencyFilter(
    getSingleValue(resolvedSearchParams.currency) ?? selectedProduct?.currency,
  );
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
      assetTypeFilter={assetTypeFilter}
      currencyFilter={currencyFilter}
      portfolio={dashboardData.portfolio}
      portfolios={dashboardData.portfolios}
      positions={dashboardData.positions}
      selectedProduct={selectedProduct}
      summary={dashboardData.summary}
      transactions={dashboardData.transactions}
    />
  );
}

function getSingleValue(value?: string | string[]) {
  return Array.isArray(value) ? value[0] : value;
}

function getProduct(productKey?: string) {
  return productMenuItems.find((item) => item.key === productKey) ?? null;
}

function getAssetTypeFilter(value?: string): AssetType | undefined {
  if (value === "STOCK" || value === "CRYPTO" || value === "FOREX") {
    return value;
  }
  return undefined;
}

function getCurrencyFilter(value?: string) {
  if (!value || !/^[A-Za-z]{3}$/.test(value)) {
    return undefined;
  }
  return value.toUpperCase();
}
