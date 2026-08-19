import type {
  Asset,
  CreatePortfolioTransactionInput,
  Portfolio,
  PortfolioSummary,
  PortfolioTransaction,
  Position,
} from "@/types/api";

const jsonHeaders = {
  Accept: "application/json",
};

function getBackendBaseUrl() {
  const baseUrl = process.env.BACKEND_BASE_URL;
  if (!baseUrl) {
    throw new Error("BACKEND_BASE_URL is not configured.");
  }
  return baseUrl.replace(/\/$/, "");
}

async function fetchBackend<T>(path: string): Promise<T> {
  const url = `${getBackendBaseUrl()}${path}`;

  let response: Response;
  try {
    response = await fetch(url, {
      cache: "no-store",
      headers: jsonHeaders,
    });
  } catch {
    throw new Error("Backend'e ulasilamadi.");
  }

  if (!response.ok) {
    throw new Error(`Backend API ${response.status} dondu.`);
  }

  return response.json() as Promise<T>;
}

async function sendBackend<T>(path: string, body: unknown): Promise<T> {
  const url = `${getBackendBaseUrl()}${path}`;

  let response: Response;
  try {
    response = await fetch(url, {
      body: JSON.stringify(body),
      cache: "no-store",
      headers: {
        ...jsonHeaders,
        "Content-Type": "application/json",
      },
      method: "POST",
    });
  } catch {
    throw new Error("Backend'e ulasilamadi.");
  }

  if (!response.ok) {
    throw new Error(getErrorMessage(response.status));
  }

  return response.json() as Promise<T>;
}

function getErrorMessage(status: number) {
  if (status === 400) {
    return "Form alanlarini kontrol et.";
  }
  if (status === 404) {
    return "Portfoy veya varlik bulunamadi.";
  }
  if (status === 409) {
    return "Satis miktari mevcut pozisyondan fazla.";
  }
  return `Backend API ${status} dondu.`;
}

export function getPortfolios() {
  return fetchBackend<Portfolio[]>("/api/portfolios");
}

export function getAssets() {
  return fetchBackend<Asset[]>("/api/assets");
}

export function getPortfolioSummary(portfolioId: number) {
  return fetchBackend<PortfolioSummary>(`/api/portfolios/${portfolioId}/summary`);
}

export function getPortfolioPositions(portfolioId: number) {
  return fetchBackend<Position[]>(`/api/portfolios/${portfolioId}/positions`);
}

export function createPortfolioTransaction(
  portfolioId: number,
  input: CreatePortfolioTransactionInput,
) {
  return sendBackend<PortfolioTransaction>(`/api/portfolios/${portfolioId}/transactions`, input);
}
