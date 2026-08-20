import type { ComponentPropsWithoutRef } from "react";

export function Input({ className = "", ...props }: ComponentPropsWithoutRef<"input">) {
  return (
    <input
      className={`h-11 rounded-md border border-slate-200 bg-white px-3 text-sm text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe] dark:border-zinc-800 dark:bg-zinc-950 dark:text-zinc-50 ${className}`}
      {...props}
    />
  );
}
