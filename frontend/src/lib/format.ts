const localeByCurrency: Record<string, string> = {
  TRY: "tr-TR",
  USD: "en-US",
};

export function formatCurrency(value: number, currency: string, maximumFractionDigits = 2) {
  try {
    return new Intl.NumberFormat(localeByCurrency[currency] ?? "en-US", {
      currency,
      currencyDisplay: currency === "USD" || currency === "TRY" ? "symbol" : "code",
      maximumFractionDigits,
      minimumFractionDigits: 2,
      style: "currency",
    }).format(value);
  } catch {
    return `${currency} ${formatNumber(value, maximumFractionDigits)}`;
  }
}

export function formatSignedCurrency(value: number, currency: string, maximumFractionDigits = 2) {
  const prefix = value > 0 ? "+" : "";
  return `${prefix}${formatCurrency(value, currency, maximumFractionDigits)}`;
}

export function formatQuantity(value: number) {
  return formatNumber(value, 8);
}

export function formatPercentage(value: number) {
  const prefix = value > 0 ? "+" : "";
  return `${prefix}${formatNumber(value, 2)}%`;
}

function formatNumber(value: number, maximumFractionDigits: number) {
  return new Intl.NumberFormat("en-US", {
    maximumFractionDigits,
    minimumFractionDigits: 0,
  }).format(value);
}
