import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { User, AuthResponse, ApiResponse } from '@/types';
import { apiClient, setAccessToken, getAccessToken, onSessionExpired, normalizeError } from '@/core/api/client';

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
  const [user, setUserState] = useState<User | null>(() => {
    if (typeof window === 'undefined') return null;
    const saved = localStorage.getItem('vc_user');
    return saved ? JSON.parse(saved) : null;
  });

  const [token, setTokenState] = useState<string | null>(() => getAccessToken());
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const setUser = useCallback((newUser: User | null) => {
    setUserState(newUser);
    if (typeof window !== 'undefined') {
      if (newUser) {
        localStorage.setItem('vc_user', JSON.stringify(newUser));
      } else {
        localStorage.removeItem('vc_user');
      }
    }
  }, []);

  const refreshProfile = useCallback(async (): Promise<User | null> => {
    try {
      const res = await apiClient.get<ApiResponse<User>>('/auth/me');
      if (res.data?.success && res.data.data) {
        const u = res.data.data;
        setUser(u);
        return u;
      }
      return null;
    } catch {
      return null;
    }
  }, [setUser]);

  // Session verification on initial load
  useEffect(() => {
    let mounted = true;

    const verifySession = async () => {
      const storedToken = getAccessToken();
      if (storedToken) {
        try {
          const profile = await refreshProfile();
          if (!profile && mounted) {
            setAccessToken(null);
            setTokenState(null);
            setUser(null);
          }
        } catch {
          if (mounted) {
            setAccessToken(null);
            setTokenState(null);
            setUser(null);
          }
        }
      } else {
        // Attempt cookie-based session recovery if cookie exists
        try {
          const refreshRes = await apiClient.post<ApiResponse<AuthResponse>>('/auth/refresh', {});
          if (refreshRes.data?.data?.token && mounted) {
            const newToken = refreshRes.data.data.token;
            setAccessToken(newToken);
            setTokenState(newToken);
            await refreshProfile();
          }
        } catch {
          // No active cookie session; remain unauthenticated
        }
      }

      if (mounted) {
        setIsLoading(false);
      }
    };

    verifySession();

    // Subscribe to session expiration events triggered by API client
    const unsubscribe = onSessionExpired(() => {
      if (mounted) {
        setTokenState(null);
        setUser(null);
      }
    });

    return () => {
      mounted = false;
      unsubscribe();
    };
  }, [refreshProfile, setUser]);

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

      // Retrieve full user profile
      const userRes = await apiClient.get<ApiResponse<User>>('/auth/me');
      const currentUser = userRes.data.data;
      setUser(currentUser);
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
      // Even if network fails, continue clearing local state
      console.warn('Server logout notice:', err);
    } finally {
      setAccessToken(null);
      setTokenState(null);
      setUser(null);
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
