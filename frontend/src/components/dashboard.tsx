import { formatCurrency, formatSignedCurrency } from "@/lib/format";
import { AssetList } from "@/components/asset-list";
import { AssetForm } from "@/components/asset-form";
import { DashboardOverview } from "@/components/dashboard-overview";
import { PortfolioForm } from "@/components/portfolio-form";
import { PortfolioHeader } from "@/components/portfolio-header";
import { PositionTable } from "@/components/position-table";
import { TransactionHistory } from "@/components/transaction-history";
import { TransactionForm } from "@/components/transaction-form";
import type { Asset, Portfolio, PortfolioSummary, PortfolioTransaction, Position } from "@/types/api";

type DashboardProps = {
  assets: Asset[];
  portfolio: Portfolio;
  portfolios: Portfolio[];
  positions: Position[];
  summary: PortfolioSummary;
  transactions: PortfolioTransaction[];
};

export function Dashboard({
  assets,
  portfolio,
  portfolios,
  positions,
  summary,
  transactions,
}: DashboardProps) {
  return (
    <div className="min-h-screen bg-[#f5f7fa] text-[#1f2933]">
      <PortfolioHeader portfolio={portfolio} portfolios={portfolios} />

      <main className="mx-auto flex max-w-7xl flex-col gap-8 px-4 py-8 sm:px-6 lg:px-8">
        <DashboardOverview
          assetCount={assets.length}
          openPositionCount={summary.openPositionCount}
          portfolioCount={portfolios.length}
          transactionCount={transactions.length}
        />

        <section aria-labelledby="summary-heading">
          <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <h2 id="summary-heading" className="text-xl font-semibold text-[#102033]">
                Özet
              </h2>
              <p className="text-sm text-[#64748b]">
                Açık pozisyon: {summary.openPositionCount}
              </p>
            </div>
          </div>

          {summary.totalsByCurrency.length === 0 ? (
            <EmptyPanel text="Bu portföy için henüz özetlenecek işlem yok." />
          ) : (
            <div className="grid gap-4 lg:grid-cols-2">
              {summary.totalsByCurrency.map((currencySummary) => (
                <article
                  key={currencySummary.currency}
                  className="rounded-lg border border-[#d8dee8] bg-white p-5 shadow-sm"
                >
                  <div className="mb-5 flex items-center justify-between">
                    <h3 className="text-lg font-semibold text-[#102033]">
                      {currencySummary.currency}
                    </h3>
                    <span className="rounded-md bg-[#eef7f1] px-2.5 py-1 text-xs font-semibold text-[#257447]">
                      Currency
                    </span>
                  </div>
                  <div className="grid gap-3 sm:grid-cols-2">
                    <SummaryMetric
                      label="Cost Basis"
                      value={formatCurrency(currencySummary.costBasis, currencySummary.currency)}
                    />
                    <SummaryMetric
                      label="Market Value"
                      value={formatCurrency(currencySummary.marketValue, currencySummary.currency)}
                    />
                    <SummaryMetric
                      label="Unrealized P/L"
                      tone={currencySummary.unrealizedProfit}
                      value={formatSignedCurrency(
                        currencySummary.unrealizedProfit,
                        currencySummary.currency,
                      )}
                    />
                    <SummaryMetric
                      label="Realized P/L"
                      tone={currencySummary.realizedProfit}
                      value={formatSignedCurrency(
                        currencySummary.realizedProfit,
                        currencySummary.currency,
                      )}
                    />
                    <SummaryMetric
                      className="sm:col-span-2"
                      label="Total P/L"
                      tone={currencySummary.totalProfit}
                      value={formatSignedCurrency(
                        currencySummary.totalProfit,
                        currencySummary.currency,
                      )}
                    />
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>

        <PortfolioForm />

        <AssetForm />

        <AssetList assets={assets} />

        <TransactionForm assets={assets} portfolioId={portfolio.id} />

        <TransactionHistory
          assets={assets}
          baseCurrency={portfolio.baseCurrency}
          portfolioId={portfolio.id}
          transactions={transactions}
        />

        <PositionTable positions={positions} />
      </main>
    </div>
  );
}

export function EmptyPortfolioState() {
  return (
    <main className="min-h-screen bg-[#f5f7fa] px-4 py-10 text-[#1f2933] sm:px-6 lg:px-8">
      <div className="mx-auto flex max-w-3xl flex-col gap-6">
        <div>
          <p className="text-sm font-medium uppercase tracking-[0.16em] text-[#5c6b7a]">
            Portfolio Tracker
          </p>
          <h1 className="mt-4 text-2xl font-semibold text-[#102033]">
            Henüz portföy oluşturulmamış.
          </h1>
          <p className="mt-3 text-sm leading-6 text-[#64748b]">
            İlk portföyü oluşturduktan sonra dashboard burada açılır.
          </p>
        </div>
        <PortfolioForm defaultOpen />
      </div>
    </main>
  );
}

export function BackendErrorState() {
  return (
    <CenteredState
      text="Dashboard verileri şu anda alınamıyor."
      title="Backend'e ulaşılamadı."
    />
  );
}

function CenteredState({ text, title }: { text: string; title: string }) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-[#f5f7fa] px-4">
      <section className="w-full max-w-xl rounded-lg border border-[#d8dee8] bg-white p-8 text-center shadow-sm">
        <p className="text-sm font-medium uppercase tracking-[0.16em] text-[#5c6b7a]">
          Portfolio Tracker
        </p>
        <h1 className="mt-4 text-2xl font-semibold text-[#102033]">{title}</h1>
        <p className="mt-3 text-sm leading-6 text-[#64748b]">{text}</p>
      </section>
    </main>
  );
}

function EmptyPanel({ text }: { text: string }) {
  return (
    <div className="rounded-lg border border-dashed border-[#cbd5e1] bg-white px-5 py-8 text-center text-sm text-[#64748b]">
      {text}
    </div>
  );
}

function SummaryMetric({
  className = "",
  label,
  tone,
  value,
}: {
  className?: string;
  label: string;
  tone?: number;
  value: string;
}) {
  return (
    <div className={`rounded-md border border-[#e2e8f0] bg-[#fbfcfe] p-4 ${className}`}>
      <p className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
        {label}
      </p>
      <p className={`mt-2 text-lg font-semibold ${toneClass(tone)}`}>{value}</p>
    </div>
  );
}

function toneClass(value?: number) {
  if (value === undefined || value === 0) {
    return "text-[#334155]";
  }
  return value > 0 ? "text-[#15803d]" : "text-[#b42318]";
}
