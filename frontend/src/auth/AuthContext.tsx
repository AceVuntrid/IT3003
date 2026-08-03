import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { api, tokenStore } from '../api/client';
import type { ApiEnvelope } from '../api/client';
import type { UserProfile } from '../api/types';

interface AuthState {
  user: UserProfile | null;
  initializing: boolean;
  login: (email: string, password: string, rememberMe: boolean) => Promise<UserProfile>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
  hasPermission: (...codes: string[]) => boolean;
  hasRole: (...roles: string[]) => boolean;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [initializing, setInitializing] = useState(true);

  useEffect(() => {
    let cancelled = false;
    async function bootstrap() {
      if (!tokenStore.access) {
        setInitializing(false);
        return;
      }
      try {
        const response = await api.get<ApiEnvelope<UserProfile>>('/auth/me');
        if (!cancelled) setUser(response.data.data);
      } catch {
        tokenStore.clear();
      } finally {
        if (!cancelled) setInitializing(false);
      }
    }
    bootstrap();
    const onLogout = () => setUser(null);
    window.addEventListener('uam:logout', onLogout);
    return () => {
      cancelled = true;
      window.removeEventListener('uam:logout', onLogout);
    };
  }, []);

  const login = useCallback(async (email: string, password: string, rememberMe: boolean) => {
    const response = await api.post<ApiEnvelope<{
      accessToken: string;
      refreshToken: string;
      user: UserProfile;
    }>>('/auth/login', { email, password, rememberMe });
    const { accessToken, refreshToken, user: profile } = response.data.data;
    tokenStore.set(accessToken, refreshToken);
    setUser(profile);
    return profile;
  }, []);

  const logout = useCallback(async () => {
    try {
      await api.post('/auth/logout');
    } catch {
      // Local sign-out proceeds regardless.
    }
    tokenStore.clear();
    setUser(null);
  }, []);

  const refreshProfile = useCallback(async () => {
    const response = await api.get<ApiEnvelope<UserProfile>>('/auth/me');
    setUser(response.data.data);
  }, []);

  const value = useMemo<AuthState>(() => ({
    user,
    initializing,
    login,
    logout,
    refreshProfile,
    hasPermission: (...codes) =>
      !!user && codes.some((code) => user.permissions.includes(code)),
    hasRole: (...roles) => !!user && roles.some((role) => user.roles.includes(role)),
  }), [user, initializing, login, logout, refreshProfile]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
