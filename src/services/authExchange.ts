/**
 * Servicio de intercambio Cognito ID Token -> JWT propio de SyncPass
 */

import { API_CONFIG } from '../config/api';
import { authStorage, type SyncPassSession } from '../utils/authStorage';

export interface CognitoExchangeResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: {
    id: number;
    username: string;
    email: string | null;
    fullName: string | null;
    role: string;
  };
}

export class AuthExchangeError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'AuthExchangeError';
  }
}

export async function exchangeCognitoToken(idToken: string): Promise<SyncPassSession> {
  if (!idToken) {
    throw new AuthExchangeError('ID token de Cognito no disponible');
  }

  const url = `${API_CONFIG.baseURL}/api/auth/cognito/exchange`;

  let response: Response;
  try {
    response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ idToken }),
    });
  } catch {
    throw new AuthExchangeError('No se pudo conectar con el servidor para completar el inicio de sesión');
  }

  if (!response.ok) {
    if (response.status === 401) {
      throw new AuthExchangeError('Token de Cognito inválido');
    }
    if (response.status === 403) {
      throw new AuthExchangeError('Cuenta no vinculada o usuario inactivo');
    }
    throw new AuthExchangeError('No se pudo completar el intercambio de autenticación');
  }

  let data: CognitoExchangeResponse;
  try {
    data = (await response.json()) as CognitoExchangeResponse;
  } catch {
    throw new AuthExchangeError('Respuesta inválida del servidor');
  }

  if (!data.accessToken || !data.user || data.tokenType !== 'Bearer') {
    throw new AuthExchangeError('Respuesta inválida del servidor');
  }

  const session: SyncPassSession = {
    accessToken: data.accessToken,
    tokenType: data.tokenType,
    expiresIn: data.expiresIn,
    user: {
      id: data.user.id,
      username: data.user.username,
      email: data.user.email ?? null,
      fullName: data.user.fullName ?? null,
      role: data.user.role,
    },
  };

  authStorage.setSession(session);
  return session;
}

export async function fetchCurrentUser(): Promise<SyncPassSession['user']> {
  const token = authStorage.getAccessToken();
  if (!token) {
    throw new AuthExchangeError('No hay sesión propia activa');
  }

  const url = `${API_CONFIG.baseURL}/api/auth/me`;

  let response: Response;
  try {
    response = await fetch(url, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
    });
  } catch {
    throw new AuthExchangeError('No se pudo conectar con el servidor');
  }

  if (!response.ok) {
    if (response.status === 401) {
      authStorage.clear();
      throw new AuthExchangeError('Sesión expirada');
    }
    throw new AuthExchangeError('No se pudo obtener la información del usuario');
  }

  let user: SyncPassSession['user'];
  try {
    user = (await response.json()) as SyncPassSession['user'];
  } catch {
    throw new AuthExchangeError('Respuesta inválida del servidor');
  }

  if (!user || user.id == null || !user.username || !user.role) {
    throw new AuthExchangeError('Respuesta inválida del servidor');
  }

  const currentSession = authStorage.getSession();
  if (currentSession) {
    authStorage.setSession({
      ...currentSession,
      user,
    });
  }

  return user;
}
