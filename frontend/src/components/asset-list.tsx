"use client";

import type { ReactNode } from "react";
import { useMemo, useState } from "react";

import { FilterResetButton } from "@/components/filter-reset-button";
import type { Asset, AssetType } from "@/types/api";

type AssetListProps = {
  assets: Asset[];
};

type AssetTypeFilter = "ALL" | AssetType;

export function AssetList({ assets }: AssetListProps) {
  const [searchTerm, setSearchTerm] = useState("");
  const [assetTypeFilter, setAssetTypeFilter] = useState<AssetTypeFilter>("ALL");
  const [currencyFilter, setCurrencyFilter] = useState("ALL");
  const hasActiveFilters =
    searchTerm.trim() !== "" || assetTypeFilter !== "ALL" || currencyFilter !== "ALL";
  const availableAssetTypes = useMemo(
    () => Array.from(new Set(assets.map((asset) => asset.assetType))).sort(),
    [assets],
  );
  const availableCurrencies = useMemo(
    () => Array.from(new Set(assets.map((asset) => asset.currency))).sort(),
    [assets],
  );

  const normalizedSearchTerm = searchTerm.trim().toLocaleUpperCase("en-US");
  const filteredAssets = assets.filter((asset) => {
    const matchesSearch =
      !normalizedSearchTerm ||
      asset.symbol.toLocaleUpperCase("en-US").includes(normalizedSearchTerm) ||
      asset.name.toLocaleUpperCase("en-US").includes(normalizedSearchTerm);
    const matchesType = assetTypeFilter === "ALL" || asset.assetType === assetTypeFilter;
    const matchesCurrency = currencyFilter === "ALL" || asset.currency === currencyFilter;
    return matchesSearch && matchesType && matchesCurrency;
  });

  return (
    <section aria-labelledby="assets-heading">
      <div className="mb-4 flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h2 id="assets-heading" className="text-xl font-semibold text-[#102033]">
            Varlıklar
          </h2>
          <p className="text-sm text-[#64748b]">
            Gösterilen varlık: {filteredAssets.length} / {assets.length}
          </p>
        </div>

        {assets.length > 0 ? (
          <div className="grid gap-3 sm:grid-cols-[minmax(180px,1fr)_160px_160px] xl:grid-cols-[minmax(180px,1fr)_160px_160px_auto] xl:items-end">
            <label className="flex min-w-52 flex-col gap-2">
              <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
                Search
              </span>
              <input
                className="h-11 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
                onChange={(event) => setSearchTerm(event.target.value)}
                placeholder="AAPL veya Apple"
                type="search"
                value={searchTerm}
              />
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

            <label className="flex min-w-40 flex-col gap-2">
              <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
                Currency
              </span>
              <select
                className="h-11 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
                onChange={(event) => setCurrencyFilter(event.target.value)}
                value={currencyFilter}
              >
                <option value="ALL">All currencies</option>
                {availableCurrencies.map((currency) => (
                  <option key={currency} value={currency}>
                    {currency}
                  </option>
                ))}
              </select>
            </label>

            <FilterResetButton disabled={!hasActiveFilters} onClick={resetFilters} />
          </div>
        ) : null}
      </div>

      {assets.length === 0 ? (
        <div className="rounded-lg border border-dashed border-[#cbd5e1] bg-white px-5 py-8 text-center text-sm text-[#64748b]">
          Henüz varlık tanımı yok.
        </div>
      ) : filteredAssets.length === 0 ? (
        <div className="rounded-lg border border-dashed border-[#cbd5e1] bg-white px-5 py-8 text-center text-sm text-[#64748b]">
          Bu filtrelere uygun varlık yok.
        </div>
      ) : (
        <div className="overflow-hidden rounded-lg border border-[#d8dee8] bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[720px] border-collapse text-left text-sm">
              <thead className="bg-[#f7f9fc] text-xs uppercase tracking-[0.12em] text-[#64748b]">
                <tr>
                  <TableHeader>Symbol</TableHeader>
                  <TableHeader>Name</TableHeader>
                  <TableHeader>Type</TableHeader>
                  <TableHeader>Currency</TableHeader>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#e2e8f0]">
                {filteredAssets.map((asset) => (
                  <tr key={asset.id} className="hover:bg-[#fafcff]">
                    <TableCell>
                      <span className="font-semibold text-[#102033]">{asset.symbol}</span>
                    </TableCell>
                    <TableCell>{asset.name}</TableCell>
                    <TableCell>
                      <span className="rounded-md bg-[#edf2f7] px-2 py-1 text-xs font-medium text-[#334155]">
                        {asset.assetType}
                      </span>
                    </TableCell>
                    <TableCell>
                      <span className="font-medium text-[#334155]">{asset.currency}</span>
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
    setSearchTerm("");
    setAssetTypeFilter("ALL");
    setCurrencyFilter("ALL");
  }
}

function TableHeader({ children }: { children: ReactNode }) {
  return <th className="px-4 py-3 font-semibold text-left">{children}</th>;
}

function TableCell({ children }: { children: ReactNode }) {
  return <td className="whitespace-nowrap px-4 py-4 text-[#334155]">{children}</td>;
}
