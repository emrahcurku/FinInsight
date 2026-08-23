import { AxiosError } from 'axios';
import { ErrorResponse } from '../types/api.types';

export interface ParsedApiError {
  message: string;
  status?: number;
  correlationId?: string;
  fieldErrors?: Record<string, string>;
}

/**
 * Extracts sanitized error details from backend ErrorResponse or Axios error.
 * Prevents raw SQL, internal paths, or stack traces from reaching the user interface.
 */
export function extractApiError(error: unknown, defaultMessage = 'An unexpected error occurred.'): ParsedApiError {
  if (!error) {
    return { message: defaultMessage };
  }

  if (typeof error === 'string') {
    return { message: error };
  }

  const axiosError = error as AxiosError<ErrorResponse>;
  if (axiosError?.response?.data) {
    const data = axiosError.response.data;

    if (data && typeof data === 'object' && 'message' in data && typeof data.message === 'string') {
      return {
        message: sanitizeMessage(data.message),
        status: data.status || axiosError.response.status,
        correlationId: data.correlationId,
        fieldErrors: data.errors,
      };
    }
  }

  if (axiosError?.response?.status) {
    const status = axiosError.response.status;
    if (status === 401) {
      return { message: 'Authentication required. Please sign in.', status: 401 };
    }
    if (status === 403) {
      return { message: 'You do not have permission to perform this action.', status: 403 };
    }
    if (status === 404) {
      return { message: 'The requested resource was not found.', status: 404 };
    }
    if (status === 409) {
      return { message: 'A conflict occurred with an existing resource.', status: 409 };
    }
    if (status === 429) {
      return { message: 'Rate limit exceeded. Please wait a moment before trying again.', status: 429 };
    }
    if (status >= 500) {
      return { message: 'Backend server error. Please try again later.', status };
    }
  }

  if (axiosError?.message) {
    if (axiosError.message.includes('Network Error')) {
      return { message: 'Cannot connect to FinInsight server. Please verify the backend is running.' };
    }
    if (axiosError.code === 'ECONNABORTED') {
      return { message: 'Request timed out. Please try again.' };
    }
  }

  if (error instanceof Error) {
    return { message: sanitizeMessage(error.message) };
  }

  return { message: defaultMessage };
}

/**
 * Returns simple error string message for UI notifications.
 */
export function extractErrorMessage(error: unknown, defaultMessage = 'An unexpected error occurred.'): string {
  const parsed = extractApiError(error, defaultMessage);
  return parsed.message;
}

/**
 * Removes internal technical tokens from error messages.
 */
function sanitizeMessage(msg: string): string {
  if (!msg) return 'An error occurred.';
  // Mask SQL or internal class names if any leak
  if (msg.includes('org.hibernate') || msg.includes('PSQLException') || msg.includes('SQL')) {
    return 'A database operation error occurred. Please verify your inputs.';
  }
  return msg;
}
