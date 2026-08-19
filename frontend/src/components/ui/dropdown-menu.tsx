"use client";

import { useEffect, useRef, useState } from "react";

export type DropdownMenuItem = {
  href: string;
  isActive?: boolean;
  label: string;
};

type DropdownMenuProps = {
  ariaLabel: string;
  items: DropdownMenuItem[];
  label: string;
};

export function DropdownMenu({ ariaLabel, items, label }: DropdownMenuProps) {
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handlePointerDown(event: PointerEvent) {
      if (!menuRef.current?.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setIsOpen(false);
      }
    }

    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, []);

  return (
    <div ref={menuRef} className="relative shrink-0">
      <button
        aria-expanded={isOpen}
        aria-haspopup="menu"
        className="inline-flex h-10 items-center justify-center gap-2 rounded-full bg-[#eef2ff] px-4 text-sm font-semibold text-[#4f46e5] transition hover:bg-[#e0e7ff] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#93c5fd] focus-visible:ring-offset-2 focus-visible:ring-offset-[#102033]"
        onClick={() => setIsOpen((value) => !value)}
        type="button"
      >
        {label}
        <CaretIcon isOpen={isOpen} />
      </button>

      <div
        aria-label={ariaLabel}
        className="absolute left-0 top-12 z-30 w-64 rounded-lg border border-[#e5e7eb] bg-white p-3 text-[#2d2f3a] shadow-lg"
        hidden={!isOpen}
        role="menu"
      >
        <div className="flex flex-col gap-1">
          {items.map((item) => (
            <a
              key={`${item.href}-${item.label}`}
              className={`rounded-full px-4 py-3 text-sm font-semibold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#93c5fd] ${
                item.isActive
                  ? "bg-[#eef2ff] text-[#4f46e5]"
                  : "text-[#2d2f3a] hover:bg-[#f4f6fb] hover:text-[#4f46e5]"
              }`}
              href={item.href}
              onClick={() => setIsOpen(false)}
              role="menuitem"
            >
              {item.label}
            </a>
          ))}
        </div>
      </div>
    </div>
  );
}

function CaretIcon({ isOpen }: { isOpen: boolean }) {
  return (
    <svg
      aria-hidden="true"
      className={`h-4 w-4 transition-transform ${isOpen ? "rotate-180" : ""}`}
      fill="none"
      viewBox="0 0 24 24"
    >
      <path
        d="m7 10 5 5 5-5"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
      />
    </svg>
  );
}
