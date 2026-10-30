/**
 * Almacenamiento en memoria del JWT propio de SyncPass
 * 
 * IMPORTANTE:
 * - Solo en memoria (no localStorage, sessionStorage ni cookies)
 * - Se pierde al recargar la página (limitación aceptada por ahora)
 * - No registrar tokens ni datos sensibles
 */

export interface SyncPassUser {
  id: number;
  username: string;
  email: string | null;
  fullName: string | null;
  role: string;
}

export interface SyncPassSession {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: SyncPassUser;
}

type AuthInvalidatedListener = () => void;

class AuthStorage {
  private session: SyncPassSession | null = null;
  private listeners: Set<AuthInvalidatedListener> = new Set();

  setSession(session: SyncPassSession): void {
    this.session = {
      accessToken: session.accessToken,
      tokenType: session.tokenType,
      expiresIn: session.expiresIn,
      user: session.user,
    };
  }

  getSession(): SyncPassSession | null {
    return this.session;
  }

  getAccessToken(): string | null {
    return this.session?.accessToken ?? null;
  }

  getUser(): SyncPassUser | null {
    return this.session?.user ?? null;
  }

  clear(notify: boolean = false): void {
    const hadSession = this.session !== null;
    this.session = null;
    if (notify && hadSession) {
      this.listeners.forEach((listener) => {
        try {
          listener();
        } catch {
          // Ignorar errores en listeners
        }
      });
    }
  }

  onAuthInvalidated(listener: AuthInvalidatedListener): () => void {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  }

  isAuthenticated(): boolean {
    return this.session !== null && !!this.session.accessToken && !!this.session.user;
  }
}

export const authStorage = new AuthStorage();
