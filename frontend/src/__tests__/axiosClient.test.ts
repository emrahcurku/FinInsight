import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { axiosClient } from '../api/axiosClient';

describe('axiosClient interceptors and security', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('attaches Authorization header when token exists in localStorage', async () => {
    localStorage.setItem('fininsight_token', 'test-jwt-token-12345');

    // Spy on internal request interceptor
    const requestConfig = { headers: {} as Record<string, string> };
    // @ts-ignore
    const modifiedConfig = await axiosClient.interceptors.request.handlers[0].fulfilled(requestConfig);

    expect(modifiedConfig.headers.Authorization).toBe('Bearer test-jwt-token-12345');
    expect(modifiedConfig.headers['X-Correlation-ID']).toBeDefined();
  });

  it('generates a new X-Correlation-ID when none is present', async () => {
    const requestConfig = { headers: {} as Record<string, string> };
    // @ts-ignore
    const modifiedConfig = await axiosClient.interceptors.request.handlers[0].fulfilled(requestConfig);

    expect(modifiedConfig.headers['X-Correlation-ID']).toBeDefined();
    expect(typeof modifiedConfig.headers['X-Correlation-ID']).toBe('string');
  });

  it('preserves existing X-Correlation-ID header', async () => {
    const customCorrelationId = 'custom-trace-id-abc';
    const requestConfig = { headers: { 'X-Correlation-ID': customCorrelationId } as Record<string, string> };
    // @ts-ignore
    const modifiedConfig = await axiosClient.interceptors.request.handlers[0].fulfilled(requestConfig);

    expect(modifiedConfig.headers['X-Correlation-ID']).toBe(customCorrelationId);
  });
});
