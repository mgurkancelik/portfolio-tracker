"use client";

import { useActionState, useMemo } from "react";
import { useFormStatus } from "react-dom";

import { createTransactionAction, type TransactionFormState } from "@/app/dashboard/actions";
import { CollapsibleFormSection } from "@/components/collapsible-form-section";
import type { Asset } from "@/types/api";

type TransactionFormProps = {
  assets: Asset[];
  defaultOpen?: boolean;
  portfolioId: number;
};

const initialState: TransactionFormState = {
  message: "",
  status: "idle",
};

export function TransactionForm({
  assets,
  defaultOpen = false,
  portfolioId,
}: TransactionFormProps) {
  const [state, formAction] = useActionState(createTransactionAction, initialState);
  const defaultDateTime = useMemo(() => toDateTimeLocalValue(new Date()), []);
  const hasAssets = assets.length > 0;

  return (
    <CollapsibleFormSection
      defaultOpen={defaultOpen}
      description="Mevcut varlıklardan birine BUY veya SELL işlemi gir."
      headingId="transaction-form-heading"
      title="İşlem Ekle"
    >
      <form action={formAction}>
        <input name="portfolioId" type="hidden" value={portfolioId} />

        {!hasAssets ? (
          <div className="rounded-md border border-dashed border-[#cbd5e1] bg-[#f8fafc] px-4 py-6 text-center text-sm text-[#64748b]">
            İşlem eklemek için önce en az bir varlık gerekir.
          </div>
        ) : (
          <>
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-6">
              <label className="flex flex-col gap-2 xl:col-span-2">
                <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
                  Asset
                </span>
                <select
                  className="h-11 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
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
                <div className="grid h-11 grid-cols-2 overflow-hidden rounded-md border border-[#cbd5e1] bg-[#f8fafc] p-1">
                  <label className="flex cursor-pointer items-center justify-center rounded-sm text-sm font-semibold text-[#15803d] has-[:checked]:bg-white has-[:checked]:shadow-sm">
                    <input
                      className="sr-only"
                      defaultChecked
                      name="transactionType"
                      type="radio"
                      value="BUY"
                    />
                    BUY
                  </label>
                  <label className="flex cursor-pointer items-center justify-center rounded-sm text-sm font-semibold text-[#b42318] has-[:checked]:bg-white has-[:checked]:shadow-sm">
                    <input className="sr-only" name="transactionType" type="radio" value="SELL" />
                    SELL
                  </label>
                </div>
              </fieldset>

              <NumberField label="Quantity" name="quantity" step="0.00000001" />
              <NumberField label="Unit Price" name="unitPrice" step="0.00000001" />
              <NumberField defaultValue="0" label="Fee" min="0" name="fee" step="0.00000001" />

              <label className="flex flex-col gap-2 md:col-span-2 xl:col-span-2">
                <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
                  Date
                </span>
                <input
                  className="h-11 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
                  defaultValue={defaultDateTime}
                  name="transactionDate"
                  required
                  type="datetime-local"
                />
              </label>
            </div>

            <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <FormMessage state={state} />
              <SubmitButton />
            </div>
          </>
        )}
      </form>
    </CollapsibleFormSection>
  );
}

function NumberField({
  defaultValue,
  label,
  min = "0.00000001",
  name,
  step,
}: {
  defaultValue?: string;
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
        className="h-11 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
        defaultValue={defaultValue}
        inputMode="decimal"
        min={min}
        name={name}
        required
        step={step}
        type="number"
      />
    </label>
  );
}

function FormMessage({ state }: { state: TransactionFormState }) {
  if (state.status === "idle") {
    return <span className="text-sm text-[#64748b]">İşlem server action ile kaydedilir.</span>;
  }

  if (state.status === "error") {
    return (
      <div
        className="rounded-md border border-[#fecaca] bg-[#fff1f1] px-4 py-3 text-sm font-medium text-[#b42318]"
        role="alert"
      >
        {state.message}
      </div>
    );
  }

  return (
    <span className="text-sm font-medium text-[#15803d]" role="status">
      {state.message}
    </span>
  );
}

function SubmitButton() {
  const { pending } = useFormStatus();

  return (
    <button
      className="inline-flex h-11 items-center justify-center rounded-md bg-[#1f4f82] px-5 text-sm font-semibold text-white shadow-sm transition hover:bg-[#183f68] disabled:cursor-not-allowed disabled:bg-[#94a3b8]"
      disabled={pending}
      type="submit"
    >
      {pending ? "İşleniyor..." : "İşlem Ekle"}
    </button>
  );
}

function toDateTimeLocalValue(date: Date) {
  const offsetMs = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16);
}
