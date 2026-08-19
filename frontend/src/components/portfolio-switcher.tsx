"use client";

import { useRouter } from "next/navigation";

import type { Portfolio } from "@/types/api";

type PortfolioSwitcherProps = {
  portfolios: Portfolio[];
  selectedPortfolioId: number;
};

export function PortfolioSwitcher({ portfolios, selectedPortfolioId }: PortfolioSwitcherProps) {
  const router = useRouter();

  return (
    <label className="flex min-w-64 flex-col gap-2">
      <span className="text-xs font-medium uppercase tracking-[0.14em] text-[#687789]">
        Portfolio
      </span>
      <select
        className="h-12 rounded-md border border-[#cfd8e3] bg-white px-3 text-sm font-semibold text-[#102033] outline-none focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
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
