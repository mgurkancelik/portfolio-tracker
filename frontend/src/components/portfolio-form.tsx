"use client";

import { useActionState } from "react";
import { useFormStatus } from "react-dom";

import { createPortfolioAction, type PortfolioFormState } from "@/app/dashboard/actions";
import { CollapsibleFormSection } from "@/components/collapsible-form-section";

const initialState: PortfolioFormState = {
  message: "",
  status: "idle",
};

type PortfolioFormProps = {
  defaultOpen?: boolean;
};

export function PortfolioForm({ defaultOpen = false }: PortfolioFormProps) {
  const [state, formAction] = useActionState(createPortfolioAction, initialState);

  return (
    <CollapsibleFormSection
      defaultOpen={defaultOpen}
      description="Farklı stratejileri ayrı takip etmek için yeni portföy aç."
      headingId="portfolio-form-heading"
      title="Portföy Oluştur"
    >
      <form action={formAction}>
        <div className="grid gap-4 md:grid-cols-[minmax(0,1fr)_180px]">
          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
              Name
            </span>
            <input
              className="h-11 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
              maxLength={100}
              name="name"
              placeholder="Uzun Vadeli"
              required
            />
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
              Base Currency
            </span>
            <input
              className="h-11 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm uppercase text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
              maxLength={3}
              minLength={3}
              name="baseCurrency"
              pattern="[A-Za-z]{3}"
              placeholder="TRY"
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

function FormMessage({ state }: { state: PortfolioFormState }) {
  if (state.status === "idle") {
    return <span className="text-sm text-[#64748b]">Portföy server action ile kaydedilir.</span>;
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
      {pending ? "Kaydediliyor" : "Portföy Oluştur"}
    </button>
  );
}
