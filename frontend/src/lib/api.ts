import type {
  Asset,
  CreateAssetInput,
  CreatePortfolioInput,
  CreatePortfolioTransactionInput,
  Portfolio,
  PortfolioSummary,
  PortfolioTransaction,
  Position,
  UpdateAssetInput,
  UpdatePortfolioTransactionInput,
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

async function sendBackend<T>(
  path: string,
  body: unknown,
  options: {
    conflictMessage?: string;
    method?: "POST" | "PUT";
  } = {},
): Promise<T> {
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
      method: options.method ?? "POST",
    });
  } catch {
    throw new Error("Backend'e ulasilamadi.");
  }

  if (!response.ok) {
    throw new Error(getErrorMessage(response.status, options.conflictMessage));
  }

  return response.json() as Promise<T>;
}

async function deleteBackend(path: string): Promise<void> {
  const url = `${getBackendBaseUrl()}${path}`;

  let response: Response;
  try {
    response = await fetch(url, {
      cache: "no-store",
      headers: jsonHeaders,
      method: "DELETE",
    });
  } catch {
    throw new Error("Backend'e ulasilamadi.");
  }

  if (!response.ok) {
    throw new Error(
      getErrorMessage(response.status, "Bu islem silinirse pozisyon gecmisi gecersiz olur."),
    );
  }
}

function getErrorMessage(status: number, conflictMessage = "Satis miktari mevcut pozisyondan fazla.") {
  if (status === 400) {
    return "Form alanlarini kontrol et.";
  }
  if (status === 404) {
    return "Portfoy veya varlik bulunamadi.";
  }
  if (status === 409) {
    return conflictMessage;
  }
  return `Backend API ${status} dondu.`;
}

export function getPortfolios() {
  return fetchBackend<Portfolio[]>("/api/portfolios");
}

export function createPortfolio(input: CreatePortfolioInput) {
  return sendBackend<Portfolio>("/api/portfolios", input);
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
  return sendBackend<PortfolioTransaction>(`/api/portfolios/${portfolioId}/transactions`, input);
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
  return deleteBackend(`/api/portfolios/${portfolioId}/transactions/${transactionId}`);
}
