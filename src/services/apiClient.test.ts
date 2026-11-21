import { describe, it, expect, beforeEach, vi, type Mock } from 'vitest';
import { ApiError, apiRequest } from './apiClient';
import { authStorage } from '../utils/authStorage';

describe('ApiError', () => {
  it('debe crear un error con nombre y status', () => {
    const error = new ApiError('Error de prueba', 500);

    expect(error.message).toBe('Error de prueba');
    expect(error.name).toBe('ApiError');
    expect(error.status).toBe(500);
  });
});

describe('apiRequest', () => {
  beforeEach(() => {
    authStorage.clear(false);
    vi.stubGlobal('fetch', vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  const mockFetch = (): Mock => globalThis.fetch as Mock;

  it('debe realizar una solicitud sin autenticación cuando requireAuth es false', async () => {
    mockFetch().mockResolvedValueOnce(
      new Response(JSON.stringify({ ok: true }), { status: 200, headers: { 'content-type': 'application/json' } }),
    );

    const result = await apiRequest('/api/health', { requireAuth: false });

    expect(mockFetch()).toHaveBeenCalledWith(
      '/api/health',
      expect.objectContaining({
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    expect(result).toEqual({ ok: true });
  });

  it('debe enviar el header Authorization cuando hay sesión', async () => {
    authStorage.setSession({
      accessToken: 'abc123',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: { id: 1, username: 'user', email: null, fullName: null, role: 'ESTUDIANTE_VOLUNTARIO_MONITOR' },
    });

    mockFetch().mockResolvedValueOnce(
      new Response(JSON.stringify({ id: 1 }), { status: 200, headers: { 'content-type': 'application/json' } }),
    );

    await apiRequest('/api/events');

    expect(mockFetch()).toHaveBeenCalledWith(
      '/api/events',
      expect.objectContaining({
        headers: {
          'Content-Type': 'application/json',
          Authorization: 'Bearer abc123',
        },
      }),
    );
  });

  it('debe lanzar error 401 si no hay sesión y se requiere autenticación', async () => {
    await expect(apiRequest('/api/events')).rejects.toThrow(ApiError);
    await expect(apiRequest('/api/events')).rejects.toThrow('No hay sesión activa');

    try {
      await apiRequest('/api/events');
    } catch (error) {
      expect((error as ApiError).status).toBe(401);
    }
  });

  it('debe lanzar error de conexión cuando fetch falla', async () => {
    mockFetch().mockRejectedValueOnce(new Error('Network error'));

    try {
      await apiRequest('/api/health', { requireAuth: false });
      expect.fail('Debería haber lanzado un error');
    } catch (error) {
      expect(error).toBeInstanceOf(ApiError);
      expect((error as ApiError).message).toBe('No se pudo conectar con el servidor');
      expect((error as ApiError).status).toBe(0);
    }
  });

  it('debe limpiar la sesión y lanzar error 401 si el servidor responde 401', async () => {
    authStorage.setSession({
      accessToken: 'expired',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: { id: 1, username: 'user', email: null, fullName: null, role: 'ESTUDIANTE_VOLUNTARIO_MONITOR' },
    });

    mockFetch().mockResolvedValueOnce(new Response('Unauthorized', { status: 401 }));

    await expect(apiRequest('/api/events')).rejects.toThrow('Sesión expirada');
    expect(authStorage.isAuthenticated()).toBe(false);
  });

  it('debe lanzar ApiError con el status del servidor para otros errores', async () => {
    mockFetch().mockResolvedValueOnce(new Response('Bad request', { status: 400 }));

    try {
      await apiRequest('/api/health', { requireAuth: false });
      expect.fail('Debería haber lanzado un error');
    } catch (error) {
      expect(error).toBeInstanceOf(ApiError);
      expect((error as ApiError).message).toBe('Error en la solicitud');
      expect((error as ApiError).status).toBe(400);
    }
  });

  it('debe devolver null para respuestas sin contenido JSON', async () => {
    mockFetch().mockResolvedValueOnce(new Response(null, { status: 204 }));

    const result = await apiRequest('/api/health', { requireAuth: false });

    expect(result).toBeNull();
  });

  it('debe permitir URLs absolutas', async () => {
    mockFetch().mockResolvedValueOnce(
      new Response(JSON.stringify({}), { status: 200, headers: { 'content-type': 'application/json' } }),
    );

    await apiRequest('https://example.com/api/health', { requireAuth: false });

    expect(mockFetch()).toHaveBeenCalledWith('https://example.com/api/health', expect.any(Object));
  });
});
