"use client";

import type { ReactNode } from "react";
import { useActionState } from "react";
import { useMemo, useState } from "react";
import { useFormStatus } from "react-dom";

import { deleteTransactionAction, type DeleteTransactionState } from "@/app/dashboard/actions";
import { FilterResetButton } from "@/components/filter-reset-button";
import { formatCurrency, formatDateTime, formatQuantity } from "@/lib/format";
import type { Asset, PortfolioTransaction, TransactionType } from "@/types/api";

type TransactionHistoryProps = {
  assets: Asset[];
  baseCurrency: string;
  portfolioId: number;
  transactions: PortfolioTransaction[];
};

type TransactionTypeFilter = "ALL" | TransactionType;

export function TransactionHistory({
  assets,
  baseCurrency,
  portfolioId,
  transactions,
}: TransactionHistoryProps) {
  const [assetFilter, setAssetFilter] = useState("ALL");
  const [typeFilter, setTypeFilter] = useState<TransactionTypeFilter>("ALL");
  const hasActiveFilters = assetFilter !== "ALL" || typeFilter !== "ALL";
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

            <FilterResetButton disabled={!hasActiveFilters} onClick={resetFilters} />
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
                  <TableHeader align="right">Action</TableHeader>
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
                      <TableCell align="right">
                        <DeleteTransactionForm portfolioId={portfolioId} transaction={transaction} />
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

  function resetFilters() {
    setAssetFilter("ALL");
    setTypeFilter("ALL");
  }
}

const initialDeleteState: DeleteTransactionState = {
  message: "",
  status: "idle",
};

function DeleteTransactionForm({
  portfolioId,
  transaction,
}: {
  portfolioId: number;
  transaction: PortfolioTransaction;
}) {
  const [state, formAction] = useActionState(deleteTransactionAction, initialDeleteState);

  return (
    <form
      action={formAction}
      className="flex flex-col items-end gap-1"
      onSubmit={(event) => {
        if (!window.confirm(`${transaction.assetSymbol} işlemi silinsin mi?`)) {
          event.preventDefault();
        }
      }}
    >
      <input name="portfolioId" type="hidden" value={portfolioId} />
      <input name="transactionId" type="hidden" value={transaction.id} />
      <DeleteButton />
      {state.status === "error" ? (
        <span className="max-w-40 whitespace-normal text-right text-xs font-medium text-[#b42318]">
          {state.message}
        </span>
      ) : null}
    </form>
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
