const localeByCurrency: Record<string, string> = {
  TRY: "tr-TR",
  USD: "en-US",
};

type NumericValue = number | string | null | undefined;

export function formatCurrency(
  value: NumericValue,
  currency: string | null | undefined,
  maximumFractionDigits = 2,
) {
  const safeValue = toSafeNumber(value);
  const safeCurrency = getSafeCurrency(currency);

  try {
    return new Intl.NumberFormat(localeByCurrency[safeCurrency] ?? "en-US", {
      currency: safeCurrency,
      currencyDisplay: safeCurrency === "USD" || safeCurrency === "TRY" ? "symbol" : "code",
      maximumFractionDigits,
      minimumFractionDigits: 2,
      style: "currency",
    }).format(safeValue);
  } catch {
    return `${safeCurrency} ${formatNumber(safeValue, maximumFractionDigits)}`;
  }
}

export function formatSignedCurrency(
  value: NumericValue,
  currency: string | null | undefined,
  maximumFractionDigits = 2,
) {
  const safeValue = toSafeNumber(value);
  const prefix = safeValue > 0 ? "+" : "";
  return `${prefix}${formatCurrency(safeValue, currency, maximumFractionDigits)}`;
}

export function formatQuantity(value: NumericValue) {
  return formatNumber(toSafeNumber(value), 8);
}

export function formatPercentage(value: NumericValue) {
  const safeValue = toSafeNumber(value);
  const prefix = safeValue > 0 ? "+" : "";
  return `${prefix}${formatNumber(safeValue, 2)}%`;
}

export function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("tr-TR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function formatNumber(value: number, maximumFractionDigits: number) {
  return new Intl.NumberFormat("en-US", {
    maximumFractionDigits,
    minimumFractionDigits: 0,
  }).format(value);
}

export function toSafeNumber(value: NumericValue) {
  const parsedValue = Number(value);
  return Number.isFinite(parsedValue) ? parsedValue : 0;
}

function getSafeCurrency(currency: string | null | undefined) {
  if (!currency || !/^[A-Za-z]{3}$/.test(currency)) {
    return "TRY";
  }
  return currency.toUpperCase();
}
