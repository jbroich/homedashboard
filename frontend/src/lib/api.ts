import { ChartRange } from '@/components/dashboard/range-control';
import { RoomId } from '@/lib/theme';

export type Measurement = {
  id: number;
  room: string;
  temperature: number | null;
  humidity: number | null;
  batteryPercentage: number | null;
  ts: string | null;
};

export type ChartPoint = {
  timestamp: string;
  temperature: number | null;
  humidity: number | null;
};

export type ChartDataResponse = {
  room: string;
  range: ChartRange;
  from: string;
  to: string;
  resolution: string;
  points: ChartPoint[];
};

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status?: number,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

function normalizeBaseUrl(value: string) {
  const trimmed = value.trim();

  if (trimmed === '/' || trimmed === './') {
    return '';
  }

  return trimmed.replace(/\/+$/, '');
}

export const API_BASE_URL = normalizeBaseUrl(
  process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8081',
);

function apiPath(path: string) {
  return `${API_BASE_URL}${path.startsWith('/') ? path : `/${path}`}`;
}

async function readJson<T>(url: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(url, { signal });

  if (!response.ok) {
    throw new ApiError(`Request failed with HTTP ${response.status}`, response.status);
  }

  return response.json() as Promise<T>;
}

export async function fetchLatestMeasurement(room: RoomId, signal?: AbortSignal) {
  const url = apiPath(`/api/measurements/${encodeURIComponent(room)}/latest`);
  const response = await fetch(url, { signal });

  if (response.status === 404) {
    return null;
  }

  if (!response.ok) {
    throw new ApiError(`Latest measurement failed with HTTP ${response.status}`, response.status);
  }

  return response.json() as Promise<Measurement>;
}

export function buildChartUrl(room: RoomId, range: ChartRange, to?: string) {
  const basePath = apiPath(
    `/api/measurements/${encodeURIComponent(room)}/chart/${encodeURIComponent(range)}`,
  );

  return to ? `${basePath}?to=${encodeURIComponent(to)}` : basePath;
}

export async function fetchChartData(
  room: RoomId,
  range: ChartRange,
  to: string,
  signal?: AbortSignal,
) {
  return readJson<ChartDataResponse>(buildChartUrl(room, range, to), signal);
}
