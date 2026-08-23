import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import { axiosClient } from '../api/axiosClient';

vi.mock('axios', async (importOriginal) => {
  const actual = await importOriginal<typeof import('axios')>();
  return {
    ...actual,
    default: {
      ...actual.default,
      create: actual.default.create,
      post: vi.fn(),
    },
  };
});

describe('Authentication Concurrency & Token Refresh Queue', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('queues concurrent 401 responses and dispatches only a single /auth/refresh call', async () => {
    localStorage.setItem('fininsight_token', 'initial-expired-token');

    let refreshCallCount = 0;
    vi.mocked(axios.post).mockImplementation(async (url: string) => {
      if (url.includes('/auth/refresh')) {
        refreshCallCount++;
        // Simulate a slight delay in network refresh
        await new Promise((resolve) => setTimeout(resolve, 30));
        return {
          data: {
            success: true,
            data: {
              accessToken: 'new-rotated-access-token',
              tokenType: 'Bearer',
              expiresIn: 900000,
              user: { id: 'u-1', email: 'test@user.com', role: 'ROLE_USER' },
            },
          },
        };
      }
      return { data: {} };
    });

    // Provide custom adapter for axiosClient so retried requests resolve immediately without network calls
    axiosClient.defaults.adapter = async (config) => {
      return {
        data: { success: true, url: config.url },
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      };
    };

    // @ts-ignore
    const responseInterceptor: (error: any) => Promise<any> = axiosClient.interceptors.response.handlers[0].rejected;

    const req1 = {
      config: { url: '/transactions', headers: {} as Record<string, string>, _retry: false },
      response: { status: 401 },
    };
    const req2 = {
      config: { url: '/budgets', headers: {} as Record<string, string>, _retry: false },
      response: { status: 401 },
    };
    const req3 = {
      config: { url: '/analytics/summary', headers: {} as Record<string, string>, _retry: false },
      response: { status: 401 },
    };

    // Fire 3 concurrent 401 handler executions
    const p1 = responseInterceptor(req1);
    const p2 = responseInterceptor(req2);
    const p3 = responseInterceptor(req3);

    const results = await Promise.all([p1, p2, p3]);

    // Verify only 1 refresh call was made
    expect(refreshCallCount).toBe(1);
    expect(localStorage.getItem('fininsight_token')).toBe('new-rotated-access-token');
    expect(results.length).toBe(3);
    expect(results[0].status).toBe(200);
  });

  it('clears storage and rejects queue if /auth/refresh fails with invalid refresh token', async () => {
    localStorage.setItem('fininsight_token', 'stale-token');
    localStorage.setItem('fininsight_user', JSON.stringify({ id: '1', email: 'test@user.com' }));

    vi.mocked(axios.post).mockRejectedValue(new Error('Refresh token revoked'));

    // @ts-ignore
    const responseInterceptor: (error: any) => Promise<any> = axiosClient.interceptors.response.handlers[0].rejected;

    const req = {
      config: { url: '/categories', headers: {} as Record<string, string>, _retry: false },
      response: { status: 401 },
    };

    await expect(responseInterceptor(req)).rejects.toThrow('Refresh token revoked');

    expect(localStorage.getItem('fininsight_token')).toBeNull();
    expect(localStorage.getItem('fininsight_user')).toBeNull();
  });
});
