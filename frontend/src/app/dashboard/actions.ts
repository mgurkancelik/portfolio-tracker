"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";

import {
  createAsset,
  createPortfolio,
  createPortfolioTransaction,
  deletePortfolioTransaction,
  updatePortfolioTransaction,
} from "@/lib/api";
import type { AssetType, TransactionType } from "@/types/api";

export type PortfolioFormState = {
  message: string;
  status: "idle" | "success" | "error";
};

export type TransactionFormState = {
  message: string;
  status: "idle" | "success" | "error";
};

export type DeleteTransactionState = {
  message: string;
  status: "idle" | "success" | "error";
};

export type UpdateTransactionState = {
  message: string;
  status: "idle" | "success" | "error";
};

export type AssetFormState = {
  message: string;
  status: "idle" | "success" | "error";
};

export async function createPortfolioAction(
  _previousState: PortfolioFormState,
  formData: FormData,
): Promise<PortfolioFormState> {
  const name = String(formData.get("name") ?? "").trim();
  const baseCurrency = String(formData.get("baseCurrency") ?? "").trim();

  if (!name || name.length > 100 || !/^[A-Za-z]{3}$/.test(baseCurrency)) {
    return { message: "Portfoy alanlarini kontrol et.", status: "error" };
  }

  let nextUrl: string;
  try {
    const portfolio = await createPortfolio({
      baseCurrency,
      name,
    });
    nextUrl = `/dashboard?portfolioId=${portfolio.id}`;
  } catch (error) {
    return {
      message: error instanceof Error ? error.message : "Portfoy kaydedilemedi.",
      status: "error",
    };
  }

  revalidatePath("/dashboard");
  redirect(nextUrl);
}

export async function createAssetAction(
  _previousState: AssetFormState,
  formData: FormData,
): Promise<AssetFormState> {
  const symbol = String(formData.get("symbol") ?? "").trim();
  const name = String(formData.get("name") ?? "").trim();
  const assetType = String(formData.get("assetType")) as AssetType;
  const currency = String(formData.get("currency") ?? "").trim();

  if (!symbol || !name || !isAssetType(assetType) || !/^[A-Za-z]{3}$/.test(currency)) {
    return { message: "Varlik alanlarini kontrol et.", status: "error" };
  }

  try {
    await createAsset({
      assetType,
      currency,
      name,
      symbol,
    });
    revalidatePath("/dashboard");
    return { message: "Varlik kaydedildi.", status: "success" };
  } catch (error) {
    return {
      message: error instanceof Error ? error.message : "Varlik kaydedilemedi.",
      status: "error",
    };
  }
}

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

export async function deleteTransactionAction(
  _previousState: DeleteTransactionState,
  formData: FormData,
): Promise<DeleteTransactionState> {
  const portfolioId = Number(formData.get("portfolioId"));
  const transactionId = Number(formData.get("transactionId"));

  if (!portfolioId || !transactionId) {
    return { message: "Silinecek islem bulunamadi.", status: "error" };
  }

  try {
    await deletePortfolioTransaction(portfolioId, transactionId);
    revalidatePath("/dashboard");
    return { message: "Islem silindi.", status: "success" };
  } catch (error) {
    return {
      message: error instanceof Error ? error.message : "Islem silinemedi.",
      status: "error",
    };
  }
}

export async function updateTransactionAction(
  _previousState: UpdateTransactionState,
  formData: FormData,
): Promise<UpdateTransactionState> {
  const portfolioId = Number(formData.get("portfolioId"));
  const transactionId = Number(formData.get("transactionId"));
  const assetId = Number(formData.get("assetId"));
  const transactionType = String(formData.get("transactionType")) as TransactionType;
  const quantity = Number(formData.get("quantity"));
  const unitPrice = Number(formData.get("unitPrice"));
  const fee = Number(formData.get("fee"));
  const transactionDateInput = String(formData.get("transactionDate"));

  if (!portfolioId || !transactionId || !assetId || !isTransactionType(transactionType)) {
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
    await updatePortfolioTransaction(portfolioId, transactionId, {
      assetId,
      fee,
      quantity,
      transactionDate,
      transactionType,
      unitPrice,
    });
    revalidatePath("/dashboard");
    return { message: "Islem guncellendi.", status: "success" };
  } catch (error) {
    return {
      message: error instanceof Error ? error.message : "Islem guncellenemedi.",
      status: "error",
    };
  }
}

function isAssetType(value: string): value is AssetType {
  return value === "STOCK" || value === "CRYPTO" || value === "FOREX";
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
