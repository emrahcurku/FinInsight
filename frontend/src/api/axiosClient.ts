import axios, { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import { ApiResponse } from '../types/api.types';
import { AuthResponse } from '../types/auth.types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export const axiosClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // Enables sending and receiving HttpOnly refresh_token cookies
  timeout: 10000,
});

// Request Interceptor: Attach JWT Token & Distributed Correlation ID
axiosClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('fininsight_token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // Attach or preserve X-Correlation-ID
    if (config.headers && !config.headers['X-Correlation-ID']) {
      config.headers['X-Correlation-ID'] = crypto.randomUUID();
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Concurrency-safe refresh queue state
let isRefreshing = false;
let refreshSubscribers: Array<(token: string) => void> = [];

const onTokenRefreshed = (newToken: string) => {
  refreshSubscribers.forEach((callback) => callback(newToken));
  refreshSubscribers = [];
};

const addRefreshSubscriber = (callback: (token: string) => void) => {
  refreshSubscribers.push(callback);
};

// Response Interceptor: Seamless Token Refresh & Error Handling
axiosClient.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error) => {
    const originalRequest = error.config;

    if (!originalRequest) {
      return Promise.reject(error);
    }

    const isAuthEndpoint =
      originalRequest.url?.includes('/auth/login') ||
      originalRequest.url?.includes('/auth/register') ||
      originalRequest.url?.includes('/auth/refresh');

    // 401 Unauthorized handling
    if (error.response?.status === 401 && !originalRequest._retry && !isAuthEndpoint) {
      if (isRefreshing) {
        // Queue concurrent requests until refresh completes
        return new Promise((resolve) => {
          addRefreshSubscriber((token: string) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            resolve(axiosClient(originalRequest));
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // Use bare axios instance for refresh to avoid interceptor recursion
        const refreshResponse = await axios.post<ApiResponse<AuthResponse>>(
          `${BASE_URL}/auth/refresh`,
          {},
          { withCredentials: true }
        );

        const newAuth = refreshResponse.data?.data;
        if (newAuth?.accessToken) {
          localStorage.setItem('fininsight_token', newAuth.accessToken);
          if (newAuth.user) {
            localStorage.setItem('fininsight_user', JSON.stringify(newAuth.user));
          }

          onTokenRefreshed(newAuth.accessToken);
          originalRequest.headers.Authorization = `Bearer ${newAuth.accessToken}`;
          return axiosClient(originalRequest);
        }
      } catch (refreshError) {
        // Refresh token failed or expired -> clear session and redirect
        localStorage.removeItem('fininsight_token');
        localStorage.removeItem('fininsight_user');
        refreshSubscribers = [];

        if (
          typeof window !== 'undefined' &&
          window.location.pathname !== '/login' &&
          window.location.pathname !== '/register'
        ) {
          window.location.href = '/login';
        }
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);
