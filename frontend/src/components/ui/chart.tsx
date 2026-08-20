"use client";

import type { CSSProperties, ReactElement, ReactNode } from "react";
import { ResponsiveContainer, Tooltip } from "recharts";

export type ChartConfig = Record<
  string,
  {
    color: string;
    label: string;
  }
>;

type ChartContainerProps = {
  children: ReactElement;
  className?: string;
  config: ChartConfig;
};

type TooltipPayloadItem = {
  color?: string;
  name?: string;
  payload?: {
    currency?: string;
    fill?: string;
    name?: string;
    value?: number;
  };
  value?: number | string;
};

type ChartTooltipContentProps = {
  active?: boolean;
  payload?: TooltipPayloadItem[];
  valueFormatter?: (value: number, payload: TooltipPayloadItem) => ReactNode;
};

export const ChartTooltip = Tooltip;

export function ChartContainer({ children, className = "", config }: ChartContainerProps) {
  const chartVars = Object.fromEntries(
    Object.entries(config).map(([key, item]) => [`--color-${key}`, item.color]),
  ) as CSSProperties;

  return (
    <div className={`h-full w-full ${className}`} style={chartVars}>
      <ResponsiveContainer height="100%" width="100%">
        {children}
      </ResponsiveContainer>
    </div>
  );
}

export function ChartTooltipContent({
  active,
  payload,
  valueFormatter,
}: ChartTooltipContentProps) {
  if (!active || !payload?.length) {
    return null;
  }

  return (
    <div className="min-w-36 rounded-lg border border-slate-200/80 bg-white px-3 py-2 text-sm shadow-sm dark:border-zinc-800 dark:bg-zinc-900">
      <div className="flex flex-col gap-1.5">
        {payload.map((item) => {
          const value = Number(item.payload?.value ?? item.value ?? 0);
          const name = item.payload?.name ?? item.name;
          const color = item.payload?.fill ?? item.color;

          return (
            <div key={`${name}-${value}`} className="flex items-center justify-between gap-5">
              <div className="flex min-w-0 items-center gap-2">
                <span
                  aria-hidden="true"
                  className="h-2.5 w-2.5 shrink-0 rounded-full"
                  style={{ backgroundColor: color }}
                />
                <span className="truncate text-slate-500 dark:text-zinc-400">{name}</span>
              </div>
              <span className="font-medium text-slate-950 dark:text-zinc-50">
                {valueFormatter ? valueFormatter(value, item) : value}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
