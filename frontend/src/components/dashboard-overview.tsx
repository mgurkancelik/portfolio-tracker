import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { PortfolioAllocationChart } from "@/components/portfolio-allocation-chart";
import { formatCurrency, formatPercentage, formatSignedCurrency, toSafeNumber } from "@/lib/format";
import type { PortfolioSummary } from "@/types/api";

type DashboardOverviewProps = {
  summary: PortfolioSummary;
};

export function DashboardOverview({ summary }: DashboardOverviewProps) {
  const profitTone = getProfitTone(summary.totalUnrealizedProfit);
  const openPositionCount = toSafeNumber(summary.openPositionCount);

  return (
    <section id="overview" className="scroll-mt-24" aria-labelledby="overview-heading">
      <h2 id="overview-heading" className="sr-only">
        Genel Durum
      </h2>

      <div className="space-y-4">
        <div className="grid auto-rows-fr gap-4 md:grid-cols-3 xl:grid-cols-[1.35fr_1fr_1fr]">
          <Card className="min-h-44">
            <CardHeader className="pb-3">
              <CardTitle>Toplam Varlık</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-4xl font-semibold text-slate-950 dark:text-zinc-50">
              {formatCurrency(summary.totalPortfolioValue, summary.baseCurrency)}
            </p>
            <p className="mt-4 text-sm text-slate-500 dark:text-zinc-400">
              {openPositionCount} açık yatırım pozisyonu
            </p>
            </CardContent>
          </Card>

          <Card className="min-h-44">
            <CardHeader className="flex-row items-center justify-between gap-4 pb-3">
              <CardTitle>Nakit Bakiyesi</CardTitle>
              <span
                aria-hidden="true"
                className="grid h-9 w-9 place-items-center rounded-full border border-slate-200 bg-slate-50 text-sm font-semibold text-slate-500 dark:border-zinc-800 dark:bg-zinc-950 dark:text-zinc-400"
              >
                {summary.baseCurrency}
              </span>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-semibold text-slate-950 dark:text-zinc-50">
                {formatCurrency(summary.totalCashBalance, summary.baseCurrency)}
              </p>
              <p className="mt-4 text-sm text-slate-500 dark:text-zinc-400">
                Harcanabilir nakit
              </p>
            </CardContent>
          </Card>

          <Card className="min-h-44">
            <CardHeader className="pb-3">
              <CardTitle>Kar/Zarar</CardTitle>
            </CardHeader>
            <CardContent>
              <p className={`text-2xl font-semibold ${profitTone}`}>
                {formatSignedCurrency(summary.totalUnrealizedProfit, summary.baseCurrency)}
              </p>
              <p className={`mt-4 text-sm font-medium ${profitTone}`}>
                {formatPercentage(summary.totalUnrealizedProfitPercentage)}
              </p>
            </CardContent>
          </Card>
        </div>

        <PortfolioAllocationChart summary={summary} />
      </div>
    </section>
  );
}

function getProfitTone(value: number | string | null | undefined) {
  const safeValue = toSafeNumber(value);

  if (safeValue > 0) {
    return "text-green-500";
  }
  if (safeValue < 0) {
    return "text-red-500";
  }
  return "text-slate-950 dark:text-zinc-50";
}
