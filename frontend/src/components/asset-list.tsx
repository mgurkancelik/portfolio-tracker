"use client";

import { Fragment, type ReactNode } from "react";
import { useActionState } from "react";
import { useMemo, useState } from "react";
import { useFormStatus } from "react-dom";

import {
  deleteAssetAction,
  updateAssetAction,
  type DeleteAssetState,
  type UpdateAssetState,
} from "@/app/dashboard/actions";
import { FilterResetButton } from "@/components/filter-reset-button";
import type { Asset, AssetType } from "@/types/api";

type AssetListProps = {
  assets: Asset[];
  initialAssetTypeFilter?: AssetType;
  initialCurrencyFilter?: string;
  productFilterLabel?: string;
};

type AssetTypeFilter = "ALL" | AssetType;
type AssetSortKey = "assetType" | "currency" | "name" | "symbol";
type SortDirection = "ASC" | "DESC";

export function AssetList({
  assets,
  initialAssetTypeFilter,
  initialCurrencyFilter,
  productFilterLabel,
}: AssetListProps) {
  const [searchTerm, setSearchTerm] = useState("");
  const [assetTypeFilter, setAssetTypeFilter] = useState<AssetTypeFilter>(
    initialAssetTypeFilter ?? "ALL",
  );
  const [currencyFilter, setCurrencyFilter] = useState(initialCurrencyFilter ?? "ALL");
  const [sortKey, setSortKey] = useState<AssetSortKey>("symbol");
  const [sortDirection, setSortDirection] = useState<SortDirection>("ASC");
  const [editingAssetId, setEditingAssetId] = useState<number | null>(null);
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
  const sortedAssets = [...filteredAssets].sort((left, right) =>
    compareAssets(left, right, sortKey, sortDirection),
  );

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
          {productFilterLabel ? (
            <p className="mt-1 text-sm font-medium text-[#4f46e5]">
              Ürün filtresi: {productFilterLabel}
            </p>
          ) : null}
        </div>

        {assets.length > 0 ? (
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-[minmax(180px,1fr)_150px_150px_150px_120px_auto] xl:items-end">
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

            <label className="flex min-w-36 flex-col gap-2">
              <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
                Sort
              </span>
              <select
                className="h-11 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
                onChange={(event) => setSortKey(event.target.value as AssetSortKey)}
                value={sortKey}
              >
                <option value="symbol">Symbol</option>
                <option value="name">Name</option>
                <option value="assetType">Type</option>
                <option value="currency">Currency</option>
              </select>
            </label>

            <label className="flex min-w-28 flex-col gap-2">
              <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
                Direction
              </span>
              <select
                className="h-11 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
                onChange={(event) => setSortDirection(event.target.value as SortDirection)}
                value={sortDirection}
              >
                <option value="ASC">A-Z</option>
                <option value="DESC">Z-A</option>
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
            <table className="w-full min-w-[820px] border-collapse text-left text-sm">
              <thead className="bg-[#f7f9fc] text-xs uppercase tracking-[0.12em] text-[#64748b]">
                <tr>
                  <TableHeader>Symbol</TableHeader>
                  <TableHeader>Name</TableHeader>
                  <TableHeader>Type</TableHeader>
                  <TableHeader>Currency</TableHeader>
                  <TableHeader align="right">Action</TableHeader>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#e2e8f0]">
                {sortedAssets.map((asset) => {
                  const isEditing = editingAssetId === asset.id;

                  return (
                    <Fragment key={asset.id}>
                      <tr className="hover:bg-[#fafcff]">
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
                        <TableCell align="right">
                          <div className="flex flex-col items-end gap-2">
                            <div className="flex justify-end gap-2">
                              <button
                                className="h-9 rounded-md border border-[#bfdbfe] bg-[#eff6ff] px-3 text-xs font-semibold text-[#1d4ed8] transition hover:bg-[#dbeafe]"
                                onClick={() => setEditingAssetId(isEditing ? null : asset.id)}
                                type="button"
                              >
                                {isEditing ? "Kapat" : "Düzenle"}
                              </button>
                              <DeleteAssetForm asset={asset} />
                            </div>
                          </div>
                        </TableCell>
                      </tr>
                      {isEditing ? (
                        <tr className="bg-[#f8fafc]">
                          <td className="px-4 py-4" colSpan={5}>
                            <UpdateAssetForm asset={asset} onCancel={() => setEditingAssetId(null)} />
                          </td>
                        </tr>
                      ) : null}
                    </Fragment>
                  );
                })}
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

const initialUpdateState: UpdateAssetState = {
  message: "",
  status: "idle",
};

const initialDeleteState: DeleteAssetState = {
  message: "",
  status: "idle",
};

function UpdateAssetForm({ asset, onCancel }: { asset: Asset; onCancel: () => void }) {
  const [state, formAction] = useActionState(updateAssetAction, initialUpdateState);

  return (
    <form action={formAction} className="rounded-md border border-[#d8dee8] bg-white p-4">
      <input name="assetId" type="hidden" value={asset.id} />

      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-[160px_minmax(220px,1fr)_150px_120px]">
        <TextField defaultValue={asset.symbol} label="Symbol" maxLength={20} name="symbol" />
        <TextField defaultValue={asset.name} label="Name" maxLength={150} name="name" />

        <label className="flex flex-col gap-2">
          <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
            Type
          </span>
          <select
            className="h-10 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
            defaultValue={asset.assetType}
            name="assetType"
            required
          >
            <option value="STOCK">STOCK</option>
            <option value="CRYPTO">CRYPTO</option>
            <option value="FOREX">FOREX</option>
          </select>
        </label>

        <TextField defaultValue={asset.currency} label="Currency" maxLength={3} name="currency" />
      </div>

      <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <FormMessage state={state} />
        <div className="flex justify-end gap-2">
          <button
            className="h-10 rounded-md border border-[#cbd5e1] bg-white px-4 text-sm font-semibold text-[#334155] transition hover:bg-[#f8fafc]"
            onClick={onCancel}
            type="button"
          >
            Vazgeç
          </button>
          <UpdateButton />
        </div>
      </div>
    </form>
  );
}

function DeleteAssetForm({ asset }: { asset: Asset }) {
  const [state, formAction] = useActionState(deleteAssetAction, initialDeleteState);

  return (
    <form
      action={formAction}
      className="flex flex-col items-end gap-1"
      onSubmit={(event) => {
        if (!window.confirm(`${asset.symbol} varlığı silinsin mi?`)) {
          event.preventDefault();
        }
      }}
    >
      <input name="assetId" type="hidden" value={asset.id} />
      <DeleteButton />
      {state.status === "error" ? (
        <span className="max-w-48 whitespace-normal text-right text-xs font-medium text-[#b42318]">
          {state.message}
        </span>
      ) : null}
    </form>
  );
}

function TextField({
  defaultValue,
  label,
  maxLength,
  name,
}: {
  defaultValue: string;
  label: string;
  maxLength: number;
  name: string;
}) {
  return (
    <label className="flex flex-col gap-2">
      <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
        {label}
      </span>
      <input
        className="h-10 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
        defaultValue={defaultValue}
        maxLength={maxLength}
        name={name}
        required
      />
    </label>
  );
}

function FormMessage({ state }: { state: UpdateAssetState }) {
  if (state.status === "idle") {
    return <span className="text-sm text-[#64748b]">Varlık bilgilerini düzenle.</span>;
  }

  return (
    <span
      className={`text-sm font-medium ${
        state.status === "success" ? "text-[#15803d]" : "text-[#b42318]"
      }`}
    >
      {state.message}
    </span>
  );
}

function UpdateButton() {
  const { pending } = useFormStatus();

  return (
    <button
      className="h-10 rounded-md bg-[#1f4f82] px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-[#183f68] disabled:cursor-not-allowed disabled:bg-[#94a3b8]"
      disabled={pending}
      type="submit"
    >
      {pending ? "Güncelleniyor" : "Güncelle"}
    </button>
  );
}

function DeleteButton() {
  const { pending } = useFormStatus();

  return (
    <button
      className="h-9 rounded-md border border-[#fecaca] bg-[#fff1f1] px-3 text-xs font-semibold text-[#b42318] transition hover:bg-[#fee2e2] disabled:cursor-not-allowed disabled:opacity-60"
      disabled={pending}
      type="submit"
    >
      {pending ? "Siliniyor" : "Sil"}
    </button>
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
}: {
  align?: "left" | "right";
  children: ReactNode;
}) {
  return (
    <td className={`whitespace-nowrap px-4 py-4 text-[#334155] ${align === "right" ? "text-right" : "text-left"}`}>
      {children}
    </td>
  );
}

function compareAssets(
  left: Asset,
  right: Asset,
  sortKey: AssetSortKey,
  sortDirection: SortDirection,
) {
  const result = left[sortKey].localeCompare(right[sortKey], "en-US", {
    numeric: true,
    sensitivity: "base",
  });
  return sortDirection === "ASC" ? result : -result;
}
