export interface ApiResponse<T> {
  success: boolean;
  message?: string | null;
  data: T;
  timestamp: string;
}

export interface ErrorResponse {
  success: boolean;
  message: string;
  status: number;
  path: string;
  correlationId?: string;
  errors?: Record<string, string>;
  timestamp: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

// Alias for backward compatibility if needed
export type PageResponse<T> = PagedResponse<T>;
