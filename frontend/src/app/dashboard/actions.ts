"use server";

import { revalidatePath } from "next/cache";

import { createPortfolioTransaction } from "@/lib/api";
import type { TransactionType } from "@/types/api";

export type TransactionFormState = {
  message: string;
  status: "idle" | "success" | "error";
};

export async function createTransactionAction(
  _previousState: TransactionFormState,
  formData: FormData,
): Promise<TransactionFormState> {
  const portfolioId = Number(formData.get("portfolioId"));
  const assetId = Number(formData.get("assetId"));
  const transactionType = String(formData.get("transactionType")) as TransactionType;
  const quantity = Number(formData.get("quantity"));
  const unitPrice = Number(formData.get("unitPrice"));
  const fee = Number(formData.get("fee"));
  const transactionDateInput = String(formData.get("transactionDate"));

  if (!portfolioId || !assetId || !isTransactionType(transactionType)) {
    return { message: "Portfolio, varlik veya islem tipi eksik.", status: "error" };
  }

  if (!isPositive(quantity) || !isPositive(unitPrice) || !isPositiveOrZero(fee)) {
    return { message: "Miktar, fiyat ve komisyon alanlarini kontrol et.", status: "error" };
  }

  const transactionDate = toIsoDateTime(transactionDateInput);
  if (!transactionDate) {
    return { message: "Islem tarihi gecersiz.", status: "error" };
  }

  try {
    await createPortfolioTransaction(portfolioId, {
      assetId,
      fee,
      quantity,
      transactionDate,
      transactionType,
      unitPrice,
    });
    revalidatePath("/dashboard");
    return { message: "Islem kaydedildi.", status: "success" };
  } catch (error) {
    return {
      message: error instanceof Error ? error.message : "Islem kaydedilemedi.",
      status: "error",
    };
  }
}

function isTransactionType(value: string): value is TransactionType {
  return value === "BUY" || value === "SELL";
}

function isPositive(value: number) {
  return Number.isFinite(value) && value > 0;
}

function isPositiveOrZero(value: number) {
  return Number.isFinite(value) && value >= 0;
}

function toIsoDateTime(value: string) {
  if (!value) {
    return null;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return null;
  }
  return date.toISOString();
}
