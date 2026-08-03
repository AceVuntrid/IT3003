import axios, { AxiosError } from 'axios';

export interface ApiEnvelope<T> {
  success: boolean;
  message?: string;
  data: T;
  errors?: Record<string, string>;
  timestamp?: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';

export const api = axios.create({ baseURL: BASE_URL });

const ACCESS_KEY = 'uam.accessToken';
const REFRESH_KEY = 'uam.refreshToken';

export const tokenStore = {
  get access() {
    return localStorage.getItem(ACCESS_KEY);
  },
  get refresh() {
    return localStorage.getItem(REFRESH_KEY);
  },
  set(access: string, refresh: string) {
    localStorage.setItem(ACCESS_KEY, access);
    localStorage.setItem(REFRESH_KEY, refresh);
  },
  clear() {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
};

api.interceptors.request.use((config) => {
  const token = tokenStore.access;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshing: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = tokenStore.refresh;
  if (!refreshToken) return null;
  try {
    const response = await axios.post<ApiEnvelope<{ accessToken: string; refreshToken: string }>>(
      `${BASE_URL}/auth/refresh`,
      { refreshToken },
    );
    const { accessToken, refreshToken: newRefresh } = response.data.data;
    tokenStore.set(accessToken, newRefresh);
    return accessToken;
  } catch {
    tokenStore.clear();
    return null;
  }
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as typeof error.config & { _retried?: boolean };
    if (error.response?.status === 401 && original && !original._retried
        && !original.url?.includes('/auth/login') && !original.url?.includes('/auth/refresh')) {
      original._retried = true;
      refreshing = refreshing ?? refreshAccessToken();
      const token = await refreshing;
      refreshing = null;
      if (token) {
        original.headers = original.headers ?? {};
        original.headers.Authorization = `Bearer ${token}`;
        return api.request(original);
      }
      window.dispatchEvent(new Event('uam:logout'));
    }
    return Promise.reject(error);
  },
);

/** Extracts a readable message from an API error for toasts and forms. */
export function errorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const body = error.response?.data as ApiEnvelope<unknown> | undefined;
    if (body?.errors && Object.keys(body.errors).length > 0) {
      return Object.values(body.errors).join('. ');
    }
    if (body?.message) return body.message;
    if (error.response?.status === 403) return 'You do not have permission to perform this action';
    if (!error.response) return 'Cannot reach the server. Check your connection.';
  }
  return 'Something went wrong. Please try again.';
}
