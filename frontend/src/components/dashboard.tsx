import type { ReactNode } from "react";

import {
  formatCurrency,
  formatPercentage,
  formatQuantity,
  formatSignedCurrency,
} from "@/lib/format";
import type { Portfolio, PortfolioSummary, Position } from "@/types/api";

type DashboardProps = {
  portfolio: Portfolio;
  positions: Position[];
  summary: PortfolioSummary;
};

export function Dashboard({ portfolio, positions, summary }: DashboardProps) {
  return (
    <div className="min-h-screen bg-[#f5f7fa] text-[#1f2933]">
      <header className="border-b border-[#d8dee8] bg-white">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-5 sm:px-6 lg:px-8">
          <div>
            <p className="text-sm font-medium uppercase tracking-[0.16em] text-[#5c6b7a]">
              Portfolio Tracker
            </p>
            <h1 className="mt-2 text-2xl font-semibold text-[#102033] sm:text-3xl">
              {portfolio.name}
            </h1>
          </div>
          <div className="rounded-md border border-[#cfd8e3] bg-[#f9fafb] px-4 py-3 text-right">
            <p className="text-xs font-medium uppercase tracking-[0.14em] text-[#687789]">
              Base Currency
            </p>
            <p className="mt-1 text-lg font-semibold text-[#102033]">
              {portfolio.baseCurrency}
            </p>
          </div>
        </div>
      </header>

      <main className="mx-auto flex max-w-7xl flex-col gap-8 px-4 py-8 sm:px-6 lg:px-8">
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

        <section aria-labelledby="positions-heading">
          <div className="mb-4">
            <h2 id="positions-heading" className="text-xl font-semibold text-[#102033]">
              Pozisyonlar
            </h2>
          </div>

          {positions.length === 0 ? (
            <EmptyPanel text="Henüz açık pozisyon yok." />
          ) : (
            <div className="overflow-hidden rounded-lg border border-[#d8dee8] bg-white shadow-sm">
              <div className="overflow-x-auto">
                <table className="w-full min-w-[980px] border-collapse text-left text-sm">
                  <thead className="bg-[#f7f9fc] text-xs uppercase tracking-[0.12em] text-[#64748b]">
                    <tr>
                      <TableHeader>Symbol</TableHeader>
                      <TableHeader>Type</TableHeader>
                      <TableHeader align="right">Quantity</TableHeader>
                      <TableHeader align="right">Average Cost</TableHeader>
                      <TableHeader align="right">Current Price</TableHeader>
                      <TableHeader align="right">Market Value</TableHeader>
                      <TableHeader align="right">Unrealized P/L</TableHeader>
                      <TableHeader align="right">Realized P/L</TableHeader>
                      <TableHeader align="right">Return %</TableHeader>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#e2e8f0]">
                    {positions.map((position) => (
                      <tr key={position.assetId} className="hover:bg-[#fafcff]">
                        <TableCell>
                          <span className="font-semibold text-[#102033]">
                            {position.assetSymbol}
                          </span>
                        </TableCell>
                        <TableCell>
                          <span className="rounded-md bg-[#edf2f7] px-2 py-1 text-xs font-medium text-[#334155]">
                            {position.assetType}
                          </span>
                        </TableCell>
                        <TableCell align="right">{formatQuantity(position.quantity)}</TableCell>
                        <TableCell align="right">
                          {formatCurrency(position.averageCost, position.currency, 8)}
                        </TableCell>
                        <TableCell align="right">
                          {formatCurrency(position.currentPrice, position.currency)}
                        </TableCell>
                        <TableCell align="right">
                          {formatCurrency(position.marketValue, position.currency)}
                        </TableCell>
                        <TableCell align="right" tone={position.unrealizedProfit}>
                          {formatSignedCurrency(position.unrealizedProfit, position.currency)}
                        </TableCell>
                        <TableCell align="right" tone={position.realizedProfit}>
                          {formatSignedCurrency(position.realizedProfit, position.currency)}
                        </TableCell>
                        <TableCell align="right" tone={position.unrealizedProfitPercentage}>
                          {formatPercentage(position.unrealizedProfitPercentage)}
                        </TableCell>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}

export function EmptyPortfolioState() {
  return (
    <CenteredState
      text="Dashboard, ilk portföy oluşturulduktan sonra burada görünecek."
      title="Henüz portföy oluşturulmamış."
    />
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

function TableHeader({
  align = "left",
  children,
}: {
  align?: "left" | "right";
  children: ReactNode;
}) {
  return (
    <th className={`px-4 py-3 font-semibold ${align === "right" ? "text-right" : "text-left"}`}>
      {children}
    </th>
  );
}

function TableCell({
  align = "left",
  children,
  tone,
}: {
  align?: "left" | "right";
  children: ReactNode;
  tone?: number;
}) {
  return (
    <td
      className={`whitespace-nowrap px-4 py-4 ${
        align === "right" ? "text-right" : "text-left"
      } ${toneClass(tone)}`}
    >
      {children}
    </td>
  );
}

function toneClass(value?: number) {
  if (value === undefined || value === 0) {
    return "text-[#334155]";
  }
  return value > 0 ? "text-[#15803d]" : "text-[#b42318]";
}
