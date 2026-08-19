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

const dashboardSections = [
  { href: "#overview", label: "Genel" },
  { href: "#summary", label: "Özet" },
  { href: "#portfolio", label: "Portföy" },
  { href: "#assets", label: "Varlıklar" },
  { href: "#transactions", label: "İşlemler" },
  { href: "#positions", label: "Pozisyonlar" },
];

export function PortfolioHeader({ portfolio, portfolios }: PortfolioHeaderProps) {
  const [isEditing, setIsEditing] = useState(false);

  return (
    <header className="border-b border-[#1d3554] bg-[#102033] text-white shadow-sm">
      <div className="mx-auto flex max-w-7xl flex-col gap-4 px-4 py-5 sm:px-6 lg:px-8">
        <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
          <div className="min-w-0 flex-1">
            <p className="text-sm font-medium uppercase tracking-[0.16em] text-[#bfdbfe]">
              Portfolio Tracker
            </p>
            <div className="mt-2 flex flex-col gap-3 sm:flex-row sm:items-center">
              <h1 className="min-w-0 break-words text-2xl font-semibold text-white sm:text-3xl">
                {portfolio.name}
              </h1>
              <div className="flex gap-2">
                <button
                  className="h-9 w-fit rounded-md border border-white/25 bg-white/10 px-3 text-xs font-semibold text-white transition hover:bg-white/15 focus:outline-none focus:ring-2 focus:ring-[#93c5fd]"
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
            <PortfolioSwitcher
              labelClassName="text-[#bfdbfe]"
              portfolios={portfolios}
              selectedPortfolioId={portfolio.id}
              selectClassName="border-white/25 bg-white text-[#102033] focus:border-[#93c5fd] focus:ring-[#93c5fd]"
            />
            <div className="rounded-md border border-white/20 bg-white/10 px-4 py-3 text-left sm:text-right">
              <p className="text-xs font-medium uppercase tracking-[0.14em] text-[#bfdbfe]">
                Base Currency
              </p>
              <p className="mt-1 text-lg font-semibold text-white">{portfolio.baseCurrency}</p>
            </div>
          </div>
        </div>

        <nav
          aria-label="Dashboard bölümleri"
          className="flex flex-col gap-3 border-t border-white/10 pt-3 lg:flex-row lg:items-center lg:justify-between"
        >
          <div className="flex gap-2 overflow-x-auto pb-1 lg:pb-0">
            {dashboardSections.map((section) => (
              <a
                key={section.href}
                className="whitespace-nowrap rounded-md px-3 py-2 text-sm font-semibold text-[#dbeafe] transition hover:bg-white/10 hover:text-white focus:bg-white/10 focus:text-white focus:outline-none focus:ring-2 focus:ring-[#93c5fd]"
                href={section.href}
              >
                {section.label}
              </a>
            ))}
          </div>
          <a
            className="inline-flex h-10 shrink-0 items-center justify-center rounded-md bg-[#facc15] px-5 text-sm font-semibold text-[#102033] shadow-sm transition hover:bg-[#fde047] focus:outline-none focus:ring-2 focus:ring-[#fef08a] focus:ring-offset-2 focus:ring-offset-[#102033]"
            href="#portfolio"
          >
            Kayıt Ol
          </a>
        </nav>
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
        <span className="max-w-56 text-xs font-medium text-[#fecaca]">{state.message}</span>
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
    <form action={formAction} className="mt-4 rounded-md border border-white/20 bg-white/10 p-4">
      <input name="portfolioId" type="hidden" value={portfolio.id} />

      <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_160px]">
        <label className="flex flex-col gap-2">
          <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#bfdbfe]">
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
          <span className="text-xs font-medium uppercase tracking-[0.12em] text-[#bfdbfe]">
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
    return <span className="text-sm text-[#dbeafe]">Portföy bilgilerini düzenle.</span>;
  }

  return (
    <span
      className={`text-sm font-medium ${
        state.status === "success" ? "text-[#bbf7d0]" : "text-[#fecaca]"
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
      className="h-10 rounded-md bg-[#facc15] px-4 text-sm font-semibold text-[#102033] shadow-sm transition hover:bg-[#fde047] disabled:cursor-not-allowed disabled:bg-[#94a3b8]"
      disabled={pending}
      type="submit"
    >
      {pending ? "Güncelleniyor" : "Güncelle"}
    </button>
  );
}
