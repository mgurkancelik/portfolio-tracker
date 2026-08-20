import type { ComponentPropsWithoutRef } from "react";

type ButtonProps = ComponentPropsWithoutRef<"button"> & {
  variant?: "primary" | "secondary";
};

export function Button({ className = "", variant = "primary", ...props }: ButtonProps) {
  const variantClass = variant === "primary"
    ? "bg-[#1f4f82] text-white shadow-sm hover:bg-[#183f68] disabled:bg-[#94a3b8]"
    : "border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 disabled:text-slate-400";

  return (
    <button
      className={`inline-flex h-11 items-center justify-center rounded-md px-5 text-sm font-semibold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#93c5fd] focus-visible:ring-offset-2 disabled:cursor-not-allowed ${variantClass} ${className}`}
      {...props}
    />
  );
}
