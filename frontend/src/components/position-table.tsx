"use client";

import type { ReactNode } from "react";
import { useMemo, useState } from "react";

import { FilterResetButton } from "@/components/filter-reset-button";
import {
  formatCurrency,
  formatPercentage,
  formatQuantity,
  formatSignedCurrency,
} from "@/lib/format";
import type { AssetType, Position } from "@/types/api";

type PositionTableProps = {
  positions: Position[];
};

type AssetTypeFilter = "ALL" | AssetType;

export function PositionTable({ positions }: PositionTableProps) {
  const [assetFilter, setAssetFilter] = useState("ALL");
  const [assetTypeFilter, setAssetTypeFilter] = useState<AssetTypeFilter>("ALL");
  const hasActiveFilters = assetFilter !== "ALL" || assetTypeFilter !== "ALL";
  const availableAssetTypes = useMemo(
    () => Array.from(new Set(positions.map((position) => position.assetType))).sort(),
    [positions],
  );

  const filteredPositions = positions.filter((position) => {
    const matchesAsset = assetFilter === "ALL" || position.assetId === Number(assetFilter);
    const matchesAssetType =
      assetTypeFilter === "ALL" || position.assetType === assetTypeFilter;
    return matchesAsset && matchesAssetType;
  });

  return (
    <section aria-labelledby="positions-heading">
      <div className="mb-4 flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h2 id="positions-heading" className="text-xl font-semibold text-[#102033]">
            Pozisyonlar
          </h2>
          <p className="text-sm text-[#64748b]">
            Gösterilen pozisyon: {filteredPositions.length} / {positions.length}
          </p>
        </div>

        {positions.length > 0 ? (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-[minmax(208px,1fr)_160px_auto] lg:items-end">
            <label className="flex min-w-52 flex-col gap-2">
              <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
                Asset
              </span>
              <select
                className="h-11 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
                onChange={(event) => setAssetFilter(event.target.value)}
                value={assetFilter}
              >
                <option value="ALL">All assets</option>
                {positions.map((position) => (
                  <option key={position.assetId} value={position.assetId}>
                    {position.assetSymbol} - {position.assetType}
                  </option>
                ))}
              </select>
            </label>

            <label className="flex min-w-40 flex-col gap-2">
              <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
                Type
              </span>
              <select
                className="h-11 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
                onChange={(event) => setAssetTypeFilter(event.target.value as AssetTypeFilter)}
                value={assetTypeFilter}
              >
                <option value="ALL">All types</option>
                {availableAssetTypes.map((assetType) => (
                  <option key={assetType} value={assetType}>
                    {assetType}
                  </option>
                ))}
              </select>
            </label>

            <FilterResetButton disabled={!hasActiveFilters} onClick={resetFilters} />
          </div>
        ) : null}
      </div>

      {positions.length === 0 ? (
        <EmptyPanel text="Henüz açık pozisyon yok." />
      ) : filteredPositions.length === 0 ? (
        <EmptyPanel text="Bu filtrelere uygun açık pozisyon yok." />
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
                {filteredPositions.map((position) => (
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
  );

  function resetFilters() {
    setAssetFilter("ALL");
    setAssetTypeFilter("ALL");
  }
}

function EmptyPanel({ text }: { text: string }) {
  return (
    <div className="rounded-lg border border-dashed border-[#cbd5e1] bg-white px-5 py-8 text-center text-sm text-[#64748b]">
      {text}
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
