type FilterResetButtonProps = {
  disabled: boolean;
  onClick: () => void;
};

export function FilterResetButton({ disabled, onClick }: FilterResetButtonProps) {
  return (
    <button
      className="h-11 rounded-md border border-[#cbd5e1] bg-[#f8fafc] px-4 text-sm font-semibold text-[#334155] transition hover:bg-[#edf2f7] disabled:cursor-not-allowed disabled:opacity-50"
      disabled={disabled}
      onClick={onClick}
      type="button"
    >
      Filtreleri Temizle
    </button>
  );
}
