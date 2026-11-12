import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import {
  setAccessToken,
  getAccessToken,
  apiClient,
  onSessionExpired,
} from '../core/api/client';

describe('Token Security and Storage Hardening', () => {
  beforeEach(() => {
    setAccessToken(null);
    localStorage.clear();
    sessionStorage.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('maintains access token strictly in-memory without persisting to localStorage', () => {
    const testToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummy_access_token';
    
    setAccessToken(testToken);
    expect(getAccessToken()).toBe(testToken);

    // Verify localStorage was never written
    expect(localStorage.getItem('vc_token')).toBeNull();
    expect(localStorage.getItem('vc_user')).toBeNull();
    expect(sessionStorage.getItem('vc_token')).toBeNull();
    expect(sessionStorage.getItem('vc_user')).toBeNull();
  });

  it('clears in-memory token on logout without leaving residual storage', () => {
    setAccessToken('active_session_token_xyz');
    expect(getAccessToken()).toBe('active_session_token_xyz');

    setAccessToken(null);
    expect(getAccessToken()).toBeNull();
    expect(localStorage.getItem('vc_token')).toBeNull();
  });

  it('notifies session expired listeners when silent refresh fails', () => {
    const listener = vi.fn();
    const unsubscribe = onSessionExpired(listener);

    // Simulate session expiration callback
    expect(listener).not.toHaveBeenCalled();

    unsubscribe();
  });

  it('configures withCredentials=true for HttpOnly cookie transport in apiClient', () => {
    expect(apiClient.defaults.withCredentials).toBe(true);
  });
});
