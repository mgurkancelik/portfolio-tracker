"use client";

import { useRouter } from "next/navigation";

import type { Portfolio } from "@/types/api";

type PortfolioSwitcherProps = {
  labelClassName?: string;
  portfolios: Portfolio[];
  selectedPortfolioId: number;
  selectClassName?: string;
};

export function PortfolioSwitcher({
  labelClassName = "text-[#687789]",
  portfolios,
  selectedPortfolioId,
  selectClassName = "border-[#cfd8e3] bg-white text-[#102033] focus:border-[#2563eb] focus:ring-[#bfdbfe]",
}: PortfolioSwitcherProps) {
  const router = useRouter();

  return (
    <label className="flex min-w-64 flex-col gap-2">
      <span className={`text-xs font-medium uppercase tracking-[0.14em] ${labelClassName}`}>
        Portfolio
      </span>
      <select
        className={`h-12 rounded-md border px-3 text-sm font-semibold outline-none focus:ring-2 ${selectClassName}`}
        onChange={(event) => router.push(`/dashboard?portfolioId=${event.target.value}`)}
        value={selectedPortfolioId}
      >
        {portfolios.map((portfolio) => (
          <option key={portfolio.id} value={portfolio.id}>
            {portfolio.name}
          </option>
        ))}
      </select>
    </label>
  );
}
