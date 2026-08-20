import type {
  Asset,
  AuthInput,
  CreateAssetInput,
  CreatePortfolioInput,
  CreatePortfolioTransactionInput,
  Portfolio,
  PortfolioSummary,
  PortfolioTransaction,
  Position,
  TokenResponse,
  UpdateAssetInput,
  UpdatePortfolioInput,
  UpdatePortfolioTransactionInput,
} from "@/types/api";
import { getAuthToken } from "@/lib/auth-cookie";

const jsonHeaders = {
  Accept: "application/json",
};

export class AuthRequiredError extends Error {
  constructor() {
    super("Authentication is required.");
  }
}

export function isAuthRequiredError(error: unknown): error is AuthRequiredError {
  return error instanceof AuthRequiredError;
}

function getBackendBaseUrl() {
  const baseUrl = process.env.BACKEND_BASE_URL;
  if (!baseUrl) {
    throw new Error("BACKEND_BASE_URL is not configured.");
  }
  return baseUrl.replace(/\/$/, "");
}

async function getRequestHeaders(contentType?: "json") {
  const token = await getAuthToken();
  return {
    ...jsonHeaders,
    ...(contentType === "json" ? { "Content-Type": "application/json" } : {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

async function fetchBackend<T>(path: string): Promise<T> {
  const url = `${getBackendBaseUrl()}${path}`;

  let response: Response;
  try {
    response = await fetch(url, {
      cache: "no-store",
      headers: await getRequestHeaders(),
    });
  } catch {
    throw new Error("Backend'e ulasilamadi.");
  }

  if (!response.ok) {
    handleUnauthorized(response.status);
    throw new Error(`Backend API ${response.status} dondu.`);
  }

  return response.json() as Promise<T>;
}

async function sendBackend<T>(
  path: string,
  body: unknown,
  options: {
    conflictMessage?: string;
    method?: "POST" | "PUT";
    redirectOnUnauthorized?: boolean;
    unauthorizedMessage?: string;
  } = {},
): Promise<T> {
  const url = `${getBackendBaseUrl()}${path}`;

  let response: Response;
  try {
    response = await fetch(url, {
      body: JSON.stringify(body),
      cache: "no-store",
      headers: await getRequestHeaders("json"),
      method: options.method ?? "POST",
    });
  } catch {
    throw new Error("Backend'e ulasilamadi.");
  }

  if (!response.ok) {
    if (options.redirectOnUnauthorized !== false) {
      handleUnauthorized(response.status);
    }
    throw new Error(getErrorMessage(response.status, options.conflictMessage, options.unauthorizedMessage));
  }

  return response.json() as Promise<T>;
}

async function deleteBackend(
  path: string,
  options: {
    conflictMessage?: string;
  } = {},
): Promise<void> {
  const url = `${getBackendBaseUrl()}${path}`;

  let response: Response;
  try {
    response = await fetch(url, {
      cache: "no-store",
      headers: await getRequestHeaders(),
      method: "DELETE",
    });
  } catch {
    throw new Error("Backend'e ulasilamadi.");
  }

  if (!response.ok) {
    handleUnauthorized(response.status);
    throw new Error(getErrorMessage(response.status, options.conflictMessage));
  }
}

function handleUnauthorized(status: number) {
  if (status === 401 || status === 403) {
    throw new AuthRequiredError();
  }
}

function getErrorMessage(
  status: number,
  conflictMessage = "Satis miktari mevcut pozisyondan fazla.",
  unauthorizedMessage = "Oturum suresi doldu. Lutfen tekrar giris yap.",
) {
  if (status === 400) {
    return "Form alanlarini kontrol et.";
  }
  if (status === 404) {
    return "Portfoy veya varlik bulunamadi.";
  }
  if (status === 409) {
    return conflictMessage;
  }
  if (status === 401 || status === 403) {
    return unauthorizedMessage;
  }
  return `Backend API ${status} dondu.`;
}

export function login(input: AuthInput) {
  return sendBackend<TokenResponse>("/api/auth/login", input, {
    conflictMessage: "Email veya sifre hatali.",
    redirectOnUnauthorized: false,
    unauthorizedMessage: "Email veya sifre hatali.",
  });
}

export function register(input: AuthInput) {
  return sendBackend<TokenResponse>("/api/auth/register", input, {
    conflictMessage: "Bu email ile daha once kayit olunmus.",
    redirectOnUnauthorized: false,
  });
}

export function getPortfolios() {
  return fetchBackend<Portfolio[]>("/api/portfolios");
}

export function createPortfolio(input: CreatePortfolioInput) {
  return sendBackend<Portfolio>("/api/portfolios", input);
}

export function updatePortfolio(portfolioId: number, input: UpdatePortfolioInput) {
  return sendBackend<Portfolio>(`/api/portfolios/${portfolioId}`, input, {
    method: "PUT",
  });
}

export function deletePortfolio(portfolioId: number) {
  return deleteBackend(`/api/portfolios/${portfolioId}`, {
    conflictMessage: "Bu portfoy islem gecmisinde kullaniliyor.",
  });
}

export function getAssets() {
  return fetchBackend<Asset[]>("/api/assets");
}

export function createAsset(input: CreateAssetInput) {
  return sendBackend<Asset>("/api/assets", input, {
    conflictMessage: "Bu sembol ve varlik tipi zaten kayitli.",
  });
}

export function updateAsset(assetId: number, input: UpdateAssetInput) {
  return sendBackend<Asset>(`/api/assets/${assetId}`, input, {
    conflictMessage: "Bu sembol ve varlik tipi zaten kayitli.",
    method: "PUT",
  });
}

export function deleteAsset(assetId: number) {
  return deleteBackend(`/api/assets/${assetId}`, {
    conflictMessage: "Bu varlik islem gecmisinde kullaniliyor.",
  });
}

export function getPortfolioSummary(portfolioId: number) {
  return fetchBackend<PortfolioSummary>(`/api/portfolios/${portfolioId}/summary`);
}

export function getPortfolioPositions(portfolioId: number) {
  return fetchBackend<Position[]>(`/api/portfolios/${portfolioId}/positions`);
}

export function getPortfolioTransactions(portfolioId: number) {
  return fetchBackend<PortfolioTransaction[]>(`/api/portfolios/${portfolioId}/transactions`);
}

export function createPortfolioTransaction(
  portfolioId: number,
  input: CreatePortfolioTransactionInput,
) {
  return sendBackend<PortfolioTransaction>(`/api/portfolios/${portfolioId}/transactions`, input, {
    conflictMessage: "Yetersiz nakit bakiyesi. Lütfen önce portföyünüze CASH ekleyin veya tutarı düşürün.",
  });
}

export function updatePortfolioTransaction(
  portfolioId: number,
  transactionId: number,
  input: UpdatePortfolioTransactionInput,
) {
  return sendBackend<PortfolioTransaction>(
    `/api/portfolios/${portfolioId}/transactions/${transactionId}`,
    input,
    {
      conflictMessage: "Bu guncelleme pozisyon gecmisini gecersiz yapar.",
      method: "PUT",
    },
  );
}

export function deletePortfolioTransaction(portfolioId: number, transactionId: number) {
  return deleteBackend(`/api/portfolios/${portfolioId}/transactions/${transactionId}`, {
    conflictMessage: "Bu islem silinirse pozisyon gecmisi gecersiz olur.",
  });
}
