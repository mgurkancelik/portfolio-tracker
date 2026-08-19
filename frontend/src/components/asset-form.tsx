"use client";

import { useActionState } from "react";
import { useFormStatus } from "react-dom";

import { createAssetAction, type AssetFormState } from "@/app/dashboard/actions";
import { CollapsibleFormSection } from "@/components/collapsible-form-section";

const initialState: AssetFormState = {
  message: "",
  status: "idle",
};

type AssetFormProps = {
  defaultOpen?: boolean;
};

export function AssetForm({ defaultOpen = false }: AssetFormProps) {
  const [state, formAction] = useActionState(createAssetAction, initialState);

  return (
    <CollapsibleFormSection
      defaultOpen={defaultOpen}
      description="İşlem girebilmek için hisse, kripto veya parite tanımı oluştur."
      headingId="asset-form-heading"
      title="Varlık Ekle"
    >
      <form action={formAction}>
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
              Symbol
            </span>
            <input
              className="h-11 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm uppercase text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
              maxLength={20}
              name="symbol"
              pattern="[A-Za-z0-9./-]+"
              placeholder="AAPL"
              required
            />
          </label>

          <label className="flex flex-col gap-2 md:col-span-2 xl:col-span-2">
            <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
              Name
            </span>
            <input
              className="h-11 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
              maxLength={150}
              name="name"
              placeholder="Apple Inc."
              required
            />
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
              Type
            </span>
            <select
              className="h-11 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
              name="assetType"
              required
            >
              <option value="STOCK">STOCK</option>
              <option value="CRYPTO">CRYPTO</option>
              <option value="FOREX">FOREX</option>
            </select>
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
              Currency
            </span>
            <input
              className="h-11 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm uppercase text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
              maxLength={3}
              minLength={3}
              name="currency"
              pattern="[A-Za-z]{3}"
              placeholder="USD"
              required
            />
          </label>
        </div>

        <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <FormMessage state={state} />
          <SubmitButton />
        </div>
      </form>
    </CollapsibleFormSection>
  );
}

function FormMessage({ state }: { state: AssetFormState }) {
  if (state.status === "idle") {
    return <span className="text-sm text-[#64748b]">Varlık server action ile kaydedilir.</span>;
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

function SubmitButton() {
  const { pending } = useFormStatus();

  return (
    <button
      className="inline-flex h-11 items-center justify-center rounded-md bg-[#1f4f82] px-5 text-sm font-semibold text-white shadow-sm transition hover:bg-[#183f68] disabled:cursor-not-allowed disabled:bg-[#94a3b8]"
      disabled={pending}
      type="submit"
    >
      {pending ? "Kaydediliyor" : "Varlık Ekle"}
    </button>
  );
}
