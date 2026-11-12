import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import { ApiResponse, AuthResponse } from '@/types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export class ApiError extends Error {
  status: number;
  validationErrors?: Record<string, string>;
  path?: string;

  constructor(status: number, message: string, validationErrors?: Record<string, string>, path?: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.validationErrors = validationErrors;
    this.path = path;
  }
}

export function normalizeError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error;
  }

  if (axios.isAxiosError(error)) {
    const status = error.response?.status || 500;
    const data = error.response?.data;

    // Standard RFC 7807 or backend ApiResponse structure
    const message =
      data?.message ||
      data?.error ||
      (status === 403
        ? 'Access forbidden: You do not possess the required academic permissions'
        : status === 401
        ? 'Session expired or unauthenticated. Please sign in.'
        : error.message || 'An unexpected error occurred');

    const validationErrors = data?.validationErrors || undefined;
    const path = data?.path || undefined;

    return new ApiError(status, message, validationErrors, path);
  }

  if (error instanceof Error) {
    return new ApiError(500, error.message);
  }

  return new ApiError(500, 'Unknown operational failure');
}

// Memory-backed token accessor with localStorage synchronization
let memoryToken: string | null = typeof window !== 'undefined' ? localStorage.getItem('vc_token') : null;

export const setAccessToken = (token: string | null) => {
  memoryToken = token;
  if (typeof window !== 'undefined') {
    if (token) {
      localStorage.setItem('vc_token', token);
    } else {
      localStorage.removeItem('vc_token');
    }
  }
};

export const getAccessToken = (): string | null => {
  if (!memoryToken && typeof window !== 'undefined') {
    memoryToken = localStorage.getItem('vc_token');
  }
  return memoryToken;
};

// Event listener for session expiration
type AuthStateCallback = () => void;
const sessionExpiredListeners: AuthStateCallback[] = [];

export const onSessionExpired = (cb: AuthStateCallback) => {
  sessionExpiredListeners.push(cb);
  return () => {
    const idx = sessionExpiredListeners.indexOf(cb);
    if (idx !== -1) sessionExpiredListeners.splice(idx, 1);
  };
};

const notifySessionExpired = () => {
  sessionExpiredListeners.forEach((cb) => {
    try {
      cb();
    } catch (err) {
      console.error('Error executing session expiration listener', err);
    }
  });
};

export const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true, // Enables browser HttpOnly cookie transport for refresh tokens
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
});

// Request Interceptor: inject Bearer token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getAccessToken();
    if (token && !config.headers.Authorization) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Concurrency lock and queue for single-flight token rotation
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Response Interceptor: 401 handling, auto-refresh with cookies, and error normalization
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // Ignore 401 for login or refresh itself to prevent infinite loops
    const requestUrl = originalRequest?.url || '';
    const isAuthEndpoint =
      requestUrl.includes('/auth/login') ||
      requestUrl.includes('/auth/refresh') ||
      requestUrl.includes('/auth/logout');

    if (error.response?.status === 401 && !originalRequest._retry && !isAuthEndpoint) {
      if (isRefreshing) {
        // Enqueue concurrent 401s while refresh completes
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((newToken) => {
            if (newToken && originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${newToken}`;
            }
            return apiClient(originalRequest);
          })
          .catch((err) => Promise.reject(normalizeError(err)));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // Attempt silent token rotation using browser HttpOnly cookie
        const res = await axios.post<ApiResponse<AuthResponse>>(
          `${API_BASE_URL}/auth/refresh`,
          {},
          { withCredentials: true }
        );

        const newAuth = res.data?.data;
        if (newAuth?.token) {
          setAccessToken(newAuth.token);
          processQueue(null, newAuth.token);

          if (originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${newAuth.token}`;
          }
          return apiClient(originalRequest);
        } else {
          throw new Error('Refresh response missing access token');
        }
      } catch (refreshErr) {
        processQueue(refreshErr, null);
        setAccessToken(null);
        if (typeof window !== 'undefined') {
          localStorage.removeItem('vc_user');
        }
        notifySessionExpired();
        return Promise.reject(normalizeError(refreshErr));
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(normalizeError(error));
  }
);
