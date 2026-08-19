"use client";

import { useActionState, useState } from "react";
import { useFormStatus } from "react-dom";

import {
  deletePortfolioAction,
  updatePortfolioAction,
  type DeletePortfolioState,
  type UpdatePortfolioState,
} from "@/app/dashboard/actions";
import { PortfolioSwitcher } from "@/components/portfolio-switcher";
import type { Portfolio } from "@/types/api";

type PortfolioHeaderProps = {
  portfolio: Portfolio;
  portfolios: Portfolio[];
};

const initialUpdateState: UpdatePortfolioState = {
  message: "",
  status: "idle",
};

const initialDeleteState: DeletePortfolioState = {
  message: "",
  status: "idle",
};

export function PortfolioHeader({ portfolio, portfolios }: PortfolioHeaderProps) {
  const [isEditing, setIsEditing] = useState(false);

  return (
    <header className="border-b border-[#d8dee8] bg-white">
      <div className="mx-auto flex max-w-7xl flex-col gap-4 px-4 py-5 sm:px-6 lg:px-8 md:flex-row md:items-start md:justify-between">
        <div className="min-w-0 flex-1">
          <p className="text-sm font-medium uppercase tracking-[0.16em] text-[#5c6b7a]">
            Portfolio Tracker
          </p>
          <div className="mt-2 flex flex-col gap-3 sm:flex-row sm:items-center">
            <h1 className="min-w-0 break-words text-2xl font-semibold text-[#102033] sm:text-3xl">
              {portfolio.name}
            </h1>
            <div className="flex gap-2">
              <button
                className="h-9 w-fit rounded-md border border-[#bfdbfe] bg-[#eff6ff] px-3 text-xs font-semibold text-[#1d4ed8] transition hover:bg-[#dbeafe]"
                onClick={() => setIsEditing((value) => !value)}
                type="button"
              >
                {isEditing ? "Kapat" : "Düzenle"}
              </button>
              <DeletePortfolioForm portfolio={portfolio} />
            </div>
          </div>

          {isEditing ? (
            <UpdatePortfolioForm portfolio={portfolio} onCancel={() => setIsEditing(false)} />
          ) : null}
        </div>

        <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <PortfolioSwitcher portfolios={portfolios} selectedPortfolioId={portfolio.id} />
          <div className="rounded-md border border-[#cfd8e3] bg-[#f9fafb] px-4 py-3 text-left sm:text-right">
            <p className="text-xs font-medium uppercase tracking-[0.14em] text-[#687789]">
              Base Currency
            </p>
            <p className="mt-1 text-lg font-semibold text-[#102033]">
              {portfolio.baseCurrency}
            </p>
          </div>
        </div>
      </div>
    </header>
  );
}

function DeletePortfolioForm({ portfolio }: { portfolio: Portfolio }) {
  const [state, formAction] = useActionState(deletePortfolioAction, initialDeleteState);

  return (
    <form
      action={formAction}
      className="flex flex-col items-start gap-1"
      onSubmit={(event) => {
        if (!window.confirm(`${portfolio.name} portföyü silinsin mi?`)) {
          event.preventDefault();
        }
      }}
    >
      <input name="portfolioId" type="hidden" value={portfolio.id} />
      <DeleteButton />
      {state.status === "error" ? (
        <span className="max-w-56 text-xs font-medium text-[#b42318]">{state.message}</span>
      ) : null}
    </form>
  );
}

function UpdatePortfolioForm({
  onCancel,
  portfolio,
}: {
  onCancel: () => void;
  portfolio: Portfolio;
}) {
  const [state, formAction] = useActionState(updatePortfolioAction, initialUpdateState);

  return (
    <form action={formAction} className="mt-4 rounded-md border border-[#d8dee8] bg-[#f8fafc] p-4">
      <input name="portfolioId" type="hidden" value={portfolio.id} />

      <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_160px]">
        <label className="flex flex-col gap-2">
          <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
            Name
          </span>
          <input
            className="h-10 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
            defaultValue={portfolio.name}
            maxLength={100}
            name="name"
            required
          />
        </label>

        <label className="flex flex-col gap-2">
          <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#64748b]">
            Base Currency
          </span>
          <input
            className="h-10 rounded-md border border-[#cbd5e1] bg-white px-3 text-sm uppercase text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
            defaultValue={portfolio.baseCurrency}
            maxLength={3}
            minLength={3}
            name="baseCurrency"
            pattern="[A-Za-z]{3}"
            required
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

function FormMessage({ state }: { state: UpdatePortfolioState }) {
  if (state.status === "idle") {
    return <span className="text-sm text-[#64748b]">Portföy bilgilerini düzenle.</span>;
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
