"use client";

import { Fragment, type ReactNode } from "react";
import { useActionState } from "react";
import { useMemo, useState } from "react";
import { useFormStatus } from "react-dom";

import {
  deleteTransactionAction,
  updateTransactionAction,
  type DeleteTransactionState,
  type UpdateTransactionState,
} from "@/app/dashboard/actions";
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
type TransactionSortKey =
  | "assetSymbol"
  | "fee"
  | "quantity"
  | "transactionDate"
  | "transactionType"
  | "unitPrice";
type SortDirection = "ASC" | "DESC";

export function TransactionHistory({
  assets,
  baseCurrency,
  portfolioId,
  transactions,
}: TransactionHistoryProps) {
  const [assetFilter, setAssetFilter] = useState("ALL");
  const [typeFilter, setTypeFilter] = useState<TransactionTypeFilter>("ALL");
  const [sortKey, setSortKey] = useState<TransactionSortKey>("transactionDate");
  const [sortDirection, setSortDirection] = useState<SortDirection>("DESC");
  const [editingTransactionId, setEditingTransactionId] = useState<number | null>(null);
  const hasActiveControls =
    assetFilter !== "ALL" ||
    typeFilter !== "ALL" ||
    sortKey !== "transactionDate" ||
    sortDirection !== "DESC";
  const currencyByAssetId = useMemo(
    () => new Map(assets.map((asset) => [asset.id, asset.currency])),
    [assets],
  );

  const filteredTransactions = transactions.filter((transaction) => {
    const matchesAsset = assetFilter === "ALL" || transaction.assetId === Number(assetFilter);
    const matchesType = typeFilter === "ALL" || transaction.transactionType === typeFilter;
    return matchesAsset && matchesType;
  });
  const sortedTransactions = [...filteredTransactions].sort((left, right) =>
    compareTransactions(left, right, sortKey, sortDirection),
  );

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
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-[minmax(208px,1fr)_140px_160px_130px_auto] xl:items-end">
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

            <label className="flex min-w-40 flex-col gap-2">
              <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
                Sort
              </span>
              <select
                className="h-11 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
                onChange={(event) => setSortKey(event.target.value as TransactionSortKey)}
                value={sortKey}
              >
                <option value="transactionDate">Date</option>
                <option value="assetSymbol">Symbol</option>
                <option value="transactionType">Type</option>
                <option value="quantity">Quantity</option>
                <option value="unitPrice">Unit price</option>
                <option value="fee">Fee</option>
              </select>
            </label>

            <label className="flex min-w-32 flex-col gap-2">
              <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
                Direction
              </span>
              <select
                className="h-11 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
                onChange={(event) => setSortDirection(event.target.value as SortDirection)}
                value={sortDirection}
              >
                <option value="DESC">Desc</option>
                <option value="ASC">Asc</option>
              </select>
            </label>

            <FilterResetButton disabled={!hasActiveControls} onClick={resetControls} />
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
            <table className="w-full min-w-[980px] border-collapse text-left text-sm">
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
                {sortedTransactions.map((transaction) => {
                  const currency = currencyByAssetId.get(transaction.assetId) ?? baseCurrency;
                  const isEditing = editingTransactionId === transaction.id;

                  return (
                    <Fragment key={transaction.id}>
                      <tr className="hover:bg-[#fafcff]">
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
                          <div className="flex flex-col items-end gap-2">
                            <div className="flex justify-end gap-2">
                              <button
                                className="h-9 rounded-md border border-[#bfdbfe] bg-[#eff6ff] px-3 text-xs font-semibold text-[#1d4ed8] transition hover:bg-[#dbeafe]"
                                onClick={() =>
                                  setEditingTransactionId(isEditing ? null : transaction.id)
                                }
                                type="button"
                              >
                                {isEditing ? "Kapat" : "Düzenle"}
                              </button>
                              <DeleteTransactionForm
                                portfolioId={portfolioId}
                                transaction={transaction}
                              />
                            </div>
                          </div>
                        </TableCell>
                      </tr>
                      {isEditing ? (
                        <tr className="bg-[#f8fafc]">
                          <td className="px-4 py-4" colSpan={7}>
                            <UpdateTransactionForm
                              assets={assets}
                              onCancel={() => setEditingTransactionId(null)}
                              portfolioId={portfolioId}
                              transaction={transaction}
                            />
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

  function resetControls() {
    setAssetFilter("ALL");
    setTypeFilter("ALL");
    setSortKey("transactionDate");
    setSortDirection("DESC");
  }
}

const initialDeleteState: DeleteTransactionState = {
  message: "",
  status: "idle",
};

const initialUpdateState: UpdateTransactionState = {
  message: "",
  status: "idle",
};

function UpdateTransactionForm({
  assets,
  onCancel,
  portfolioId,
  transaction,
}: {
  assets: Asset[];
  onCancel: () => void;
  portfolioId: number;
  transaction: PortfolioTransaction;
}) {
  const [state, formAction] = useActionState(updateTransactionAction, initialUpdateState);

  return (
    <form action={formAction} className="rounded-md border border-[#d8dee8] bg-white p-4">
      <input name="portfolioId" type="hidden" value={portfolioId} />
      <input name="transactionId" type="hidden" value={transaction.id} />

      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-7">
        <label className="flex flex-col gap-2 xl:col-span-2">
          <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
            Asset
          </span>
          <select
            className="h-10 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
            defaultValue={transaction.assetId}
            name="assetId"
            required
          >
            {assets.map((asset) => (
              <option key={asset.id} value={asset.id}>
                {asset.symbol} - {asset.assetType} - {asset.currency}
              </option>
            ))}
          </select>
        </label>

        <fieldset className="flex flex-col gap-2">
          <legend className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
            Type
          </legend>
          <div className="grid h-10 grid-cols-2 overflow-hidden rounded-md border border-[#cbd5e1] bg-[#f8fafc] p-1">
            <label className="flex cursor-pointer items-center justify-center rounded-sm text-xs font-semibold text-[#15803d] has-[:checked]:bg-white has-[:checked]:shadow-sm">
              <input
                className="sr-only"
                defaultChecked={transaction.transactionType === "BUY"}
                name="transactionType"
                type="radio"
                value="BUY"
              />
              BUY
            </label>
            <label className="flex cursor-pointer items-center justify-center rounded-sm text-xs font-semibold text-[#b42318] has-[:checked]:bg-white has-[:checked]:shadow-sm">
              <input
                className="sr-only"
                defaultChecked={transaction.transactionType === "SELL"}
                name="transactionType"
                type="radio"
                value="SELL"
              />
              SELL
            </label>
          </div>
        </fieldset>

        <EditNumberField
          defaultValue={transaction.quantity}
          label="Quantity"
          name="quantity"
          step="0.00000001"
        />
        <EditNumberField
          defaultValue={transaction.unitPrice}
          label="Unit Price"
          name="unitPrice"
          step="0.00000001"
        />
        <EditNumberField
          defaultValue={transaction.fee}
          label="Fee"
          min="0"
          name="fee"
          step="0.00000001"
        />

        <label className="flex flex-col gap-2 xl:col-span-1">
          <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
            Date
          </span>
          <input
            className="h-10 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
            defaultValue={toDateTimeLocalValue(transaction.transactionDate)}
            name="transactionDate"
            required
            type="datetime-local"
          />
        </label>
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

function EditNumberField({
  defaultValue,
  label,
  min = "0.00000001",
  name,
  step,
}: {
  defaultValue: number;
  label: string;
  min?: string;
  name: string;
  step: string;
}) {
  return (
    <label className="flex flex-col gap-2">
      <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
        {label}
      </span>
      <input
        className="h-10 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
        defaultValue={String(defaultValue)}
        min={min}
        name={name}
        required
        step={step}
        type="number"
      />
    </label>
  );
}

function FormMessage({ state }: { state: UpdateTransactionState }) {
  if (state.status === "idle") {
    return <span className="text-sm text-[#64748b]">İşlem bilgilerini düzenle.</span>;
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

function toDateTimeLocalValue(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "";
  }
  const offsetMs = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16);
}

function compareTransactions(
  left: PortfolioTransaction,
  right: PortfolioTransaction,
  sortKey: TransactionSortKey,
  sortDirection: SortDirection,
) {
  let result: number;

  if (sortKey === "transactionDate") {
    result = Date.parse(left.transactionDate) - Date.parse(right.transactionDate);
  } else if (sortKey === "assetSymbol" || sortKey === "transactionType") {
    result = left[sortKey].localeCompare(right[sortKey], "en-US", {
      numeric: true,
      sensitivity: "base",
    });
  } else {
    result = left[sortKey] - right[sortKey];
  }

  if (result === 0) {
    result = left.id - right.id;
  }

  return sortDirection === "ASC" ? result : -result;
}
