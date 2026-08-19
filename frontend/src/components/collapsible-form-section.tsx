import type { ReactNode } from "react";

type CollapsibleFormSectionProps = {
  children: ReactNode;
  defaultOpen?: boolean;
  description: string;
  headingId: string;
  title: string;
};

export function CollapsibleFormSection({
  children,
  defaultOpen = false,
  description,
  headingId,
  title,
}: CollapsibleFormSectionProps) {
  return (
    <section aria-labelledby={headingId}>
      <details
        className="group rounded-lg border border-[#d8dee8] bg-white shadow-sm"
        open={defaultOpen || undefined}
      >
        <summary className="flex cursor-pointer list-none items-center justify-between gap-4 px-5 py-4 [&::-webkit-details-marker]:hidden">
          <div>
            <h2 id={headingId} className="text-xl font-semibold text-[#102033]">
              {title}
            </h2>
            <p className="text-sm text-[#64748b]">{description}</p>
          </div>
          <span
            aria-hidden="true"
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-md border border-[#cbd5e1] bg-[#f8fafc] text-sm font-semibold text-[#334155] transition group-open:rotate-180"
          >
            v
          </span>
        </summary>
        <div className="border-t border-[#e2e8f0] p-5">{children}</div>
      </details>
    </section>
  );
}
