/**
 * Contexto de Autenticación - Integración Amazon Cognito (Amplify v6) + JWT propio SyncPass
 *
 * IMPORTANTE:
 * - Usa flujo Authorization Code + PKCE
 * - El usuario solo se considera autenticado tras intercambio Cognito -> JWT propio exitoso
 * - JWT propio almacenado solo en memoria (authStorage)
 * - No se loggean tokens ni secretos
 * - Autorización real debe validarse en servidor
 */

import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { getCurrentUser, fetchAuthSession, signInWithRedirect, signOut } from 'aws-amplify/auth';
import { authStorage, type SyncPassUser } from '../utils/authStorage';
import { exchangeCognitoToken, fetchCurrentUser } from '../services/authExchange';
import { runExchangeOnce, clearExchangeLock } from '../utils/authExchangeLock';

export type UserRole = string | null;

export interface AuthUser {
  id: number | string;
  username: string;
  email?: string | null;
  fullName?: string | null;
  role: UserRole;
}

export interface AuthContextType {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  authError: string | null;
  login: () => Promise<void>;
  logout: () => Promise<void>;
  checkAuth: () => Promise<void>;
  clearAuthError: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: React.ReactNode;
}

function mapSyncPassUser(user: SyncPassUser): AuthUser {
  return {
    id: user.id,
    username: user.username,
    email: user.email,
    fullName: user.fullName,
    role: user.role,
  };
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [authError, setAuthError] = useState<string | null>(null);



  useEffect(() => {
    const unsubscribe = authStorage.onAuthInvalidated(() => {
      setUser(null);
      setAuthError(null);
    });
    return () => {
      unsubscribe();
    };
  }, []);

  const checkAuth = useCallback(async (): Promise<void> => {
    setIsLoading(true);
    setAuthError(null);
    try {
      // Si ya tenemos sesión propia válida, intentar recargar usuario desde backend
      if (authStorage.isAuthenticated()) {
        try {
          const updatedUser = await fetchCurrentUser();
          setUser(mapSyncPassUser(updatedUser));
          return;
        } catch (err) {
          // Solo limpiar si es sesión expirada (401) - no por error de red
          if (err instanceof Error && err.message === 'Sesión expirada') {
            authStorage.clear(true);
            setUser(null);
          } else {
            // Error de red u otro - mantener sesión propia si existe
            const storedUser = authStorage.getUser();
            if (storedUser) {
              setUser(mapSyncPassUser(storedUser));
            }
            return;
          }
        }
      }

      // Verificar sesión Cognito
      try {
        await getCurrentUser();
      } catch {
        authStorage.clear(true);
        setUser(null);
        return;
      }

      // Intentar obtener idToken de Cognito para intercambio (con protección contra duplicados)
      try {
        const session = await fetchAuthSession({ forceRefresh: false });
        const idToken = session.tokens?.idToken?.toString();
        if (!idToken) {
          authStorage.clear(true);
          setUser(null);
          return;
        }
        const syncSession = await runExchangeOnce(() => exchangeCognitoToken(idToken));
        setUser(mapSyncPassUser(syncSession.user));
      } catch (err) {
        authStorage.clear(true);
        await signOut().catch(() => undefined);
        setUser(null);
        if (err instanceof Error) {
          setAuthError(err.message);
        } else {
          setAuthError('No se pudo completar el inicio de sesión con SyncPass');
        }
      }
    } catch {
      authStorage.clear(true);
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void checkAuth();
  }, [checkAuth]);

  const login = useCallback(async (): Promise<void> => {
    setAuthError(null);
    await signInWithRedirect();
  }, []);

  const logout = useCallback(async (): Promise<void> => {
    setIsLoading(true);
    setAuthError(null);
    try {
      await signOut();
    } catch {
      // Ignorar errores de logout de Cognito
    } finally {
      clearExchangeLock();
      authStorage.clear(true);
      setUser(null);
      setIsLoading(false);
    }
  }, []);

  const clearAuthError = useCallback((): void => {
    setAuthError(null);
  }, []);

  const value: AuthContextType = {
    user,
    isAuthenticated: authStorage.isAuthenticated() && !!user,
    isLoading,
    authError,
    login,
    logout,
    checkAuth,
    clearAuthError,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth debe usarse dentro de AuthProvider');
  }
  return context;
};
