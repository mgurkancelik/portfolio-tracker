type DashboardOverviewProps = {
  assetCount: number;
  openPositionCount: number;
  portfolioCount: number;
  transactionCount: number;
};

export function DashboardOverview({
  assetCount,
  openPositionCount,
  portfolioCount,
  transactionCount,
}: DashboardOverviewProps) {
  return (
    <section aria-labelledby="overview-heading">
      <h2 id="overview-heading" className="sr-only">
        Genel Durum
      </h2>
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <OverviewMetric label="Portföy" value={portfolioCount} />
        <OverviewMetric label="Varlık" value={assetCount} />
        <OverviewMetric label="İşlem" value={transactionCount} />
        <OverviewMetric label="Açık Pozisyon" value={openPositionCount} />
      </div>
    </section>
  );
}

function OverviewMetric({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg border border-[#d8dee8] bg-white px-5 py-4 shadow-sm">
      <p className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">{label}</p>
      <p className="mt-2 text-2xl font-semibold text-[#102033]">{value}</p>
    </div>
  );
}
