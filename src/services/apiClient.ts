/**
 * Cliente HTTP para llamadas a la API de SyncPass
 * Usa el JWT propio almacenado en memoria
 */

import { API_CONFIG } from '../config/api';
import { authStorage } from '../utils/authStorage';

export class ApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

interface RequestOptions extends RequestInit {
  requireAuth?: boolean;
}

export async function apiRequest<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
  const { requireAuth = true, headers = {}, ...rest } = options;

  const url = endpoint.startsWith('http') ? endpoint : `${API_CONFIG.baseURL}${endpoint}`;

  const requestHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(headers as Record<string, string>),
  };

  if (requireAuth) {
    const token = authStorage.getAccessToken();
    if (!token) {
      throw new ApiError('No hay sesión activa', 401);
    }
    requestHeaders['Authorization'] = `Bearer ${token}`;
  }

  let response: Response;
  try {
    response = await fetch(url, {
      ...rest,
      headers: requestHeaders,
    });
  } catch {
    throw new ApiError('No se pudo conectar con el servidor', 0);
  }

  if (!response.ok) {
    if (response.status === 401) {
      authStorage.clear(true);
      throw new ApiError('Sesión expirada', 401);
    }
    throw new ApiError('Error en la solicitud', response.status);
  }

  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    return (await response.json()) as T;
  }

  return null as T;
}
