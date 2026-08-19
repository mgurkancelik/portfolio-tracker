import type { AnchorHTMLAttributes, ReactNode } from "react";

type NavLinkVariant = "cta" | "onDark";

type NavLinkProps = AnchorHTMLAttributes<HTMLAnchorElement> & {
  children: ReactNode;
  variant?: NavLinkVariant;
};

const variantClassNames: Record<NavLinkVariant, string> = {
  cta: "bg-[#facc15] text-[#102033] shadow-sm hover:bg-[#fde047] focus-visible:ring-[#fef08a] focus-visible:ring-offset-[#102033]",
  onDark:
    "text-[#dbeafe] hover:bg-white/10 hover:text-white focus-visible:bg-white/10 focus-visible:text-white focus-visible:ring-[#93c5fd]",
};

export function NavLink({
  children,
  className = "",
  variant = "onDark",
  ...props
}: NavLinkProps) {
  return (
    <a
      className={`inline-flex h-10 shrink-0 items-center justify-center rounded-md px-3 text-sm font-semibold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 ${variantClassNames[variant]} ${className}`}
      {...props}
    >
      {children}
    </a>
  );
}
