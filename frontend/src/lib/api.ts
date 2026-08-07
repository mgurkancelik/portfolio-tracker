import type { Portfolio, PortfolioSummary, Position } from "@/types/api";

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

export function getPortfolios() {
  return fetchBackend<Portfolio[]>("/api/portfolios");
}

export function getPortfolioSummary(portfolioId: number) {
  return fetchBackend<PortfolioSummary>(`/api/portfolios/${portfolioId}/summary`);
}

export function getPortfolioPositions(portfolioId: number) {
  return fetchBackend<Position[]>(`/api/portfolios/${portfolioId}/positions`);
}
