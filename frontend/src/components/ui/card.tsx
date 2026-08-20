import type { ComponentPropsWithoutRef } from "react";

export function Card({ className = "", ...props }: ComponentPropsWithoutRef<"div">) {
  return (
    <div
      className={`rounded-lg border border-slate-200/70 bg-white text-slate-950 shadow-[0_1px_2px_rgba(15,23,42,0.04)] dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-50 ${className}`}
      {...props}
    />
  );
}

export function CardHeader({ className = "", ...props }: ComponentPropsWithoutRef<"div">) {
  return <div className={`flex flex-col gap-1.5 p-6 ${className}`} {...props} />;
}

export function CardTitle({ className = "", ...props }: ComponentPropsWithoutRef<"h3">) {
  return (
    <h3
      className={`text-sm font-medium leading-none text-slate-500 dark:text-zinc-400 ${className}`}
      {...props}
    />
  );
}

export function CardContent({ className = "", ...props }: ComponentPropsWithoutRef<"div">) {
  return <div className={`p-6 pt-0 ${className}`} {...props} />;
}
