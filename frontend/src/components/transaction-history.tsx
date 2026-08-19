"use client";

import type { ReactNode } from "react";
import { useMemo, useState } from "react";

import { formatCurrency, formatDateTime, formatQuantity } from "@/lib/format";
import type { Asset, PortfolioTransaction, TransactionType } from "@/types/api";

type TransactionHistoryProps = {
  assets: Asset[];
  baseCurrency: string;
  transactions: PortfolioTransaction[];
};

type TransactionTypeFilter = "ALL" | TransactionType;

export function TransactionHistory({
  assets,
  baseCurrency,
  transactions,
}: TransactionHistoryProps) {
  const [assetFilter, setAssetFilter] = useState("ALL");
  const [typeFilter, setTypeFilter] = useState<TransactionTypeFilter>("ALL");
  const currencyByAssetId = useMemo(
    () => new Map(assets.map((asset) => [asset.id, asset.currency])),
    [assets],
  );

  const filteredTransactions = transactions.filter((transaction) => {
    const matchesAsset = assetFilter === "ALL" || transaction.assetId === Number(assetFilter);
    const matchesType = typeFilter === "ALL" || transaction.transactionType === typeFilter;
    return matchesAsset && matchesType;
  });

  return (
    <section aria-labelledby="transactions-heading">
      <div className="mb-4 flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h2 id="transactions-heading" className="text-xl font-semibold text-[#102033]">
            İşlem Geçmişi
          </h2>
          <p className="text-sm text-[#64748b]">
            Gösterilen işlem: {filteredTransactions.length} / {transactions.length}
          </p>
        </div>

        {transactions.length > 0 ? (
          <div className="grid gap-3 sm:grid-cols-2">
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
                {assets.map((asset) => (
                  <option key={asset.id} value={asset.id}>
                    {asset.symbol} - {asset.assetType}
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
                onChange={(event) => setTypeFilter(event.target.value as TransactionTypeFilter)}
                value={typeFilter}
              >
                <option value="ALL">All types</option>
                <option value="BUY">BUY</option>
                <option value="SELL">SELL</option>
              </select>
            </label>
          </div>
        ) : null}
      </div>

      {transactions.length === 0 ? (
        <EmptyPanel text="Henüz işlem kaydı yok." />
      ) : filteredTransactions.length === 0 ? (
        <EmptyPanel text="Bu filtrelere uygun işlem yok." />
      ) : (
        <div className="overflow-hidden rounded-lg border border-[#d8dee8] bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[860px] border-collapse text-left text-sm">
              <thead className="bg-[#f7f9fc] text-xs uppercase tracking-[0.12em] text-[#64748b]">
                <tr>
                  <TableHeader>Date</TableHeader>
                  <TableHeader>Symbol</TableHeader>
                  <TableHeader>Type</TableHeader>
                  <TableHeader align="right">Quantity</TableHeader>
                  <TableHeader align="right">Unit Price</TableHeader>
                  <TableHeader align="right">Fee</TableHeader>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#e2e8f0]">
                {filteredTransactions.map((transaction) => {
                  const currency = currencyByAssetId.get(transaction.assetId) ?? baseCurrency;

                  return (
                    <tr key={transaction.id} className="hover:bg-[#fafcff]">
                      <TableCell>{formatDateTime(transaction.transactionDate)}</TableCell>
                      <TableCell>
                        <span className="font-semibold text-[#102033]">
                          {transaction.assetSymbol}
                        </span>
                      </TableCell>
                      <TableCell>
                        <TransactionTypeBadge type={transaction.transactionType} />
                      </TableCell>
                      <TableCell align="right">{formatQuantity(transaction.quantity)}</TableCell>
                      <TableCell align="right">
                        {formatCurrency(transaction.unitPrice, currency, 8)}
                      </TableCell>
                      <TableCell align="right">
                        {formatCurrency(transaction.fee, currency, 8)}
                      </TableCell>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </section>
  );
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
}: {
  align?: "left" | "right";
  children: ReactNode;
}) {
  return (
    <td className={`whitespace-nowrap px-4 py-4 ${align === "right" ? "text-right" : "text-left"}`}>
      {children}
    </td>
  );
}

function TransactionTypeBadge({ type }: { type: PortfolioTransaction["transactionType"] }) {
  const className =
    type === "BUY" ? "bg-[#eef7f1] text-[#257447]" : "bg-[#fff1f1] text-[#b42318]";

  return (
    <span className={`rounded-md px-2 py-1 text-xs font-semibold ${className}`}>
      {type}
    </span>
  );
}
