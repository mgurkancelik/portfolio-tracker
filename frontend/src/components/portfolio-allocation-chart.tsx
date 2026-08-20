"use client";

import { Cell, Label, Pie, PieChart } from "recharts";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart";
import { formatCurrency, toSafeNumber } from "@/lib/format";
import type { PortfolioSummary } from "@/types/api";

type PortfolioAllocationChartProps = {
  summary: PortfolioSummary;
};

type AllocationDatum = {
  currency: string;
  fill: string;
  name: string;
  value: number;
};

const chartColorKeys = ["chart-1", "chart-2", "chart-3", "chart-4", "chart-5", "chart-6"];

export function PortfolioAllocationChart({ summary }: PortfolioAllocationChartProps) {
  const chartData = getChartData(summary);
  const chartConfig = getChartConfig(chartData);
  const hasData = chartData.length > 0;
  const totalLabel = formatCurrency(summary.totalPortfolioValue, summary.baseCurrency);

  return (
    <Card className="min-h-[360px]">
      <CardHeader className="pb-0">
        <CardTitle>Portföy Dağılımı</CardTitle>
        <p className="text-sm text-slate-500 dark:text-zinc-400">
          Para birimine göre piyasa değeri
        </p>
      </CardHeader>
      <CardContent className="grid gap-6 pt-6 lg:grid-cols-[minmax(260px,0.9fr)_1fr] lg:items-center">
        {hasData ? (
          <>
            <ChartContainer className="mx-auto h-[260px] max-w-[360px]" config={chartConfig}>
              <PieChart>
                <ChartTooltip
                  content={
                    <ChartTooltipContent
                      valueFormatter={(value, item) =>
                        formatCurrency(value, item.payload?.currency ?? summary.baseCurrency)
                      }
                    />
                  }
                  cursor={false}
                />
                <Pie
                  data={chartData}
                  dataKey="value"
                  innerRadius={76}
                  nameKey="name"
                  outerRadius={108}
                  paddingAngle={3}
                  strokeWidth={0}
                >
                  {chartData.map((item) => (
                    <Cell key={item.currency} fill={item.fill} />
                  ))}
                  <Label content={(props) => <CenterLabel {...props} totalLabel={totalLabel} />} />
                </Pie>
              </PieChart>
            </ChartContainer>

            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-1">
              {chartData.map((item) => (
                <div
                  key={item.currency}
                  className="flex items-center justify-between gap-4 rounded-lg border border-slate-200/60 bg-slate-50/60 px-4 py-3 dark:border-zinc-800 dark:bg-zinc-950/60"
                >
                  <div className="flex min-w-0 items-center gap-3">
                    <span
                      aria-hidden="true"
                      className="h-3 w-3 shrink-0 rounded-full"
                      style={{ backgroundColor: item.fill }}
                    />
                    <span className="truncate text-sm font-medium text-slate-700 dark:text-zinc-300">
                      {item.currency}
                    </span>
                  </div>
                  <span className="text-sm font-semibold text-slate-950 dark:text-zinc-50">
                    {formatCurrency(item.value, item.currency)}
                  </span>
                </div>
              ))}
            </div>
          </>
        ) : (
          <div className="flex min-h-[240px] items-center justify-center rounded-lg border border-dashed border-slate-200 bg-slate-50/70 px-5 text-center text-sm text-slate-500 dark:border-zinc-800 dark:bg-zinc-950/50 dark:text-zinc-400 lg:col-span-2">
            Dağılım grafiği için henüz piyasa değeri yok.
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function getChartData(summary: PortfolioSummary): AllocationDatum[] {
  return (summary.totalsByCurrency ?? [])
    .filter((item) => toSafeNumber(item.marketValue) > 0)
    .map((item, index) => {
      const colorKey = chartColorKeys[index % chartColorKeys.length];

      return {
        currency: item.currency,
        fill: `var(--${colorKey})`,
        name: item.currency,
        value: toSafeNumber(item.marketValue),
      };
    });
}

function getChartConfig(chartData: AllocationDatum[]): ChartConfig {
  return Object.fromEntries(
    chartData.map((item, index) => [
      item.currency,
      {
        color: `var(--${chartColorKeys[index % chartColorKeys.length]})`,
        label: item.currency,
      },
    ]),
  );
}

function CenterLabel({ totalLabel, viewBox }: { totalLabel: string; viewBox?: unknown }) {
  const center = getCenter(viewBox);

  if (!center) {
    return null;
  }

  return (
    <text dominantBaseline="middle" textAnchor="middle" x={center.cx} y={center.cy}>
      <tspan
        className="fill-slate-500 text-[11px] font-medium dark:fill-zinc-400"
        x={center.cx}
        y={center.cy - 12}
      >
        Toplam Varlık
      </tspan>
      <tspan
        className="fill-slate-950 text-base font-semibold dark:fill-zinc-50"
        x={center.cx}
        y={center.cy + 14}
      >
        {totalLabel}
      </tspan>
    </text>
  );
}

function getCenter(viewBox: unknown) {
  if (
    typeof viewBox === "object" &&
    viewBox !== null &&
    "cx" in viewBox &&
    "cy" in viewBox &&
    typeof viewBox.cx === "number" &&
    typeof viewBox.cy === "number"
  ) {
    return {
      cx: viewBox.cx,
      cy: viewBox.cy,
    };
  }

  return null;
}
