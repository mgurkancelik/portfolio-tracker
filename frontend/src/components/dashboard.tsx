import { formatCurrency, formatSignedCurrency } from "@/lib/format";
import { AssetList } from "@/components/asset-list";
import { AssetForm } from "@/components/asset-form";
import { DashboardOverview } from "@/components/dashboard-overview";
import { PortfolioForm } from "@/components/portfolio-form";
import { PortfolioHeader } from "@/components/portfolio-header";
import { PositionTable } from "@/components/position-table";
import { TransactionHistory } from "@/components/transaction-history";
import { TransactionForm } from "@/components/transaction-form";
import type { ProductMenuItem } from "@/data/navigation";
import type {
  Asset,
  AssetType,
  Portfolio,
  PortfolioSummary,
  PortfolioTransaction,
  Position,
} from "@/types/api";

type DashboardProps = {
  assets: Asset[];
  assetTypeFilter?: AssetType;
  currencyFilter?: string;
  portfolio: Portfolio;
  portfolios: Portfolio[];
  positions: Position[];
  selectedProduct?: ProductMenuItem | null;
  summary: PortfolioSummary;
  transactions: PortfolioTransaction[];
};

export function Dashboard({
  assets,
  assetTypeFilter,
  currencyFilter,
  portfolio,
  portfolios,
  positions,
  selectedProduct,
  summary,
  transactions,
}: DashboardProps) {
  return (
    <div className="min-h-screen bg-[#f5f7fa] text-[#1f2933]">
      <PortfolioHeader
        activeProductKey={selectedProduct?.key}
        portfolio={portfolio}
        portfolios={portfolios}
      />

      <main className="mx-auto flex max-w-7xl flex-col gap-8 px-4 py-8 sm:px-6 lg:px-8">
        <DashboardOverview
          assetCount={assets.length}
          openPositionCount={summary.openPositionCount}
          portfolioCount={portfolios.length}
          transactionCount={transactions.length}
        />

        {selectedProduct ? <ProductContextPanel product={selectedProduct} /> : null}

        <section id="summary" className="scroll-mt-24" aria-labelledby="summary-heading">
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
            <div className="grid items-stretch gap-4 lg:grid-cols-2">
              {summary.totalsByCurrency.map((currencySummary) => (
                <article
                  key={currencySummary.currency}
                  className="flex h-full flex-col rounded-lg border border-[#d8dee8] bg-white p-5 shadow-sm"
                >
                  <div className="mb-5 flex items-center justify-between gap-3">
                    <h3 className="text-lg font-semibold text-[#102033]">
                      {currencySummary.currency}
                    </h3>
                    <span className="rounded-md bg-[#eef7f1] px-2.5 py-1 text-xs font-semibold text-[#257447]">
                      Currency
                    </span>
                  </div>
                  <div className="grid flex-1 auto-rows-fr gap-3 sm:grid-cols-2">
                    <SummaryMetric
                      label="Cost Basis"
                      value={formatCurrency(currencySummary.costBasis, currencySummary.currency)}
                    />
                    <SummaryMetric
                      label="Market Value"
                      value={formatCurrency(
                        currencySummary.marketValue,
                        currencySummary.currency,
                      )}
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

        <div id="portfolio" className="scroll-mt-24">
          <PortfolioForm />
        </div>

        <div id="assets" className="flex scroll-mt-24 flex-col gap-8">
          <AssetForm />

          <AssetList
            assets={assets}
            key={`${assetTypeFilter ?? "ALL"}-${currencyFilter ?? "ALL"}-${selectedProduct?.key ?? "none"}`}
            initialAssetTypeFilter={assetTypeFilter}
            initialCurrencyFilter={currencyFilter}
            productFilterLabel={selectedProduct?.label}
          />
        </div>

        <div id="transactions" className="flex scroll-mt-24 flex-col gap-8">
          <TransactionForm assets={assets} portfolioId={portfolio.id} />

          <TransactionHistory
            assets={assets}
            baseCurrency={portfolio.baseCurrency}
            portfolioId={portfolio.id}
            transactions={transactions}
          />
        </div>

        <div id="positions" className="scroll-mt-24">
          <PositionTable positions={positions} />
        </div>
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
    <div
      className={`flex min-h-28 flex-col justify-between rounded-md border border-[#e2e8f0] bg-[#fbfcfe] p-4 ${className}`}
    >
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

function ProductContextPanel({ product }: { product: ProductMenuItem }) {
  return (
    <section
      aria-label={`${product.label} ürün bilgisi`}
      className="rounded-lg border border-[#d8dee8] bg-white px-5 py-4 shadow-sm"
    >
      <p className="text-xs font-semibold uppercase text-[#64748b]">
        Seçili Ürün
      </p>
      <div className="mt-2 flex flex-col gap-2 md:flex-row md:items-end md:justify-between">
        <div>
          <h2 className="text-xl font-semibold text-[#102033]">{product.label}</h2>
          <p className="mt-1 text-sm leading-6 text-[#64748b]">{product.description}</p>
        </div>
        {product.assetType || product.currency ? (
          <div className="flex flex-wrap gap-2">
            {product.assetType ? <FilterBadge text={product.assetType} /> : null}
            {product.currency ? <FilterBadge text={product.currency} /> : null}
          </div>
        ) : null}
      </div>
    </section>
  );
}

function FilterBadge({ text }: { text: string }) {
  return (
    <span className="rounded-md bg-[#eef2ff] px-3 py-1.5 text-xs font-semibold text-[#4f46e5]">
      {text}
    </span>
  );
}
