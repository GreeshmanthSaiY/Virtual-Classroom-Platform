import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { User, AuthResponse, ApiResponse } from '@/types';
import { apiClient, setAccessToken, onSessionExpired, normalizeError } from '@/core/api/client';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<User>;
  logout: () => Promise<void>;
  setUser: (user: User | null) => void;
  refreshProfile: () => Promise<User | null>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  // Pure in-memory session state (no localStorage / sessionStorage persistence)
  const [user, setUserState] = useState<User | null>(null);
  const [token, setTokenState] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const setUser = useCallback((newUser: User | null) => {
    setUserState(newUser);
  }, []);

  const refreshProfile = useCallback(async (): Promise<User | null> => {
    try {
      const res = await apiClient.get<ApiResponse<User>>('/auth/me');
      if (res.data?.success && res.data.data) {
        const u = res.data.data;
        setUserState(u);
        return u;
      }
      return null;
    } catch {
      return null;
    }
  }, []);

  // Session verification on initial load via HttpOnly refresh cookie
  useEffect(() => {
    let mounted = true;

    const verifySession = async () => {
      try {
        const refreshRes = await apiClient.post<ApiResponse<AuthResponse>>('/auth/refresh', {});
        const authData = refreshRes.data?.data;
        if (authData?.token && mounted) {
          const newToken = authData.token;
          setAccessToken(newToken);
          setTokenState(newToken);
          await refreshProfile();
        }
      } catch {
        // No active session cookie; remain unauthenticated
        if (mounted) {
          setAccessToken(null);
          setTokenState(null);
          setUserState(null);
        }
      } finally {
        if (mounted) {
          setIsLoading(false);
        }
      }
    };

    verifySession();

    // Subscribe to session expiration events triggered by API client
    const unsubscribe = onSessionExpired(() => {
      if (mounted) {
        setAccessToken(null);
        setTokenState(null);
        setUserState(null);
      }
    });

    return () => {
      mounted = false;
      unsubscribe();
    };
  }, [refreshProfile]);

  const login = async (email: string, password: string): Promise<User> => {
    setIsLoading(true);
    try {
      const res = await apiClient.post<ApiResponse<AuthResponse>>('/auth/login', {
        email,
        password,
      });

      if (!res.data?.success || !res.data.data) {
        throw new Error(res.data?.message || 'Authentication failed');
      }

      const authData = res.data.data;
      setAccessToken(authData.token);
      setTokenState(authData.token);

      const userRes = await apiClient.get<ApiResponse<User>>('/auth/me');
      const currentUser = userRes.data.data;
      setUserState(currentUser);
      return currentUser;
    } catch (err) {
      throw normalizeError(err);
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async (): Promise<void> => {
    try {
      await apiClient.post('/auth/logout', {});
    } catch (err) {
      // Even if network fails, continue clearing local in-memory state
      console.warn('Server logout notice:', err);
    } finally {
      setAccessToken(null);
      setTokenState(null);
      setUserState(null);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token && !!user,
        isLoading,
        login,
        logout,
        setUser,
        refreshProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
