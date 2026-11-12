import { describe, it, expect, beforeEach } from 'vitest';
import { normalizeError, ApiError, setAccessToken, getAccessToken } from '../core/api/client';

describe('API Client and Error Normalizer', () => {
  beforeEach(() => {
    setAccessToken(null);
  });

  it('manages access token in memory', () => {
    expect(getAccessToken()).toBeNull();
    setAccessToken('mock-jwt-token-12345');
    expect(getAccessToken()).toBe('mock-jwt-token-12345');
    setAccessToken(null);
    expect(getAccessToken()).toBeNull();
  });

  it('normalizes standard JavaScript errors', () => {
    const err = new Error('Network failure');
    const normalized = normalizeError(err);
    expect(normalized).toBeInstanceOf(ApiError);
    expect(normalized.status).toBe(500);
    expect(normalized.message).toBe('Network failure');
  });

  it('preserves existing ApiError instances', () => {
    const existing = new ApiError(403, 'Forbidden action', { reason: 'Unauthorized' }, '/api/v1/test');
    const normalized = normalizeError(existing);
    expect(normalized).toBe(existing);
    expect(normalized.status).toBe(403);
    expect(normalized.validationErrors?.reason).toBe('Unauthorized');
  });

  it('normalizes Axios 403 Forbidden responses into descriptive messages', () => {
    const axiosError = {
      isAxiosError: true,
      name: 'AxiosError',
      message: 'Request failed with status code 403',
      response: {
        status: 403,
        data: {},
      },
    } as any;

    const normalized = normalizeError(axiosError);
    expect(normalized.status).toBe(403);
    expect(normalized.message).toContain('Access forbidden');
  });
});
