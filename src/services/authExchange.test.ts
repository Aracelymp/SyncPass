import { describe, it, expect, beforeEach, vi, type Mock } from 'vitest';
import {
  exchangeCognitoToken,
  fetchCurrentUser,
  AuthExchangeError,
  type CognitoExchangeResponse,
} from './authExchange';
import { authStorage } from '../utils/authStorage';

const mockExchangeResponse: CognitoExchangeResponse = {
  accessToken: 'syncpass-token',
  tokenType: 'Bearer',
  expiresIn: 3600,
  user: {
    id: 1,
    username: 'monitor',
    email: 'monitor@example.com',
    fullName: 'Monitor Prueba',
    role: 'ESTUDIANTE_VOLUNTARIO_MONITOR',
  },
};

describe('AuthExchangeError', () => {
  it('debe crear un error con el nombre correcto', () => {
    const error = new AuthExchangeError('fallo');
    expect(error.message).toBe('fallo');
    expect(error.name).toBe('AuthExchangeError');
  });
});

describe('exchangeCognitoToken', () => {
  beforeEach(() => {
    authStorage.clear(false);
    vi.stubGlobal('fetch', vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  const mockFetch = (): Mock => globalThis.fetch as Mock;

  it('debe lanzar error si no hay idToken', async () => {
    await expect(exchangeCognitoToken('')).rejects.toThrow('ID token de Cognito no disponible');
  });

  it('debe lanzar error de conexión si fetch falla', async () => {
    mockFetch().mockRejectedValueOnce(new Error('network'));

    await expect(exchangeCognitoToken('id-token')).rejects.toThrow(
      'No se pudo conectar con el servidor para completar el inicio de sesión',
    );
  });

  it('debe lanzar error si el token Cognito es inválido (401)', async () => {
    mockFetch().mockResolvedValueOnce(new Response('Unauthorized', { status: 401 }));

    await expect(exchangeCognitoToken('id-token')).rejects.toThrow('Token de Cognito inválido');
  });

  it('debe lanzar error si la cuenta no está vinculada (403)', async () => {
    mockFetch().mockResolvedValueOnce(new Response('Forbidden', { status: 403 }));

    await expect(exchangeCognitoToken('id-token')).rejects.toThrow(
      'Cuenta no vinculada o usuario inactivo',
    );
  });

  it('debe lanzar error genérico para otros códigos de error', async () => {
    mockFetch().mockResolvedValueOnce(new Response('Error', { status: 500 }));

    await expect(exchangeCognitoToken('id-token')).rejects.toThrow(
      'No se pudo completar el intercambio de autenticación',
    );
  });

  it('debe lanzar error si la respuesta no es JSON válido', async () => {
    mockFetch().mockResolvedValueOnce(
      new Response('not-json', { status: 200, headers: { 'content-type': 'application/json' } }),
    );

    await expect(exchangeCognitoToken('id-token')).rejects.toThrow('Respuesta inválida del servidor');
  });

  it('debe lanzar error si faltan campos obligatorios en la respuesta', async () => {
    mockFetch().mockResolvedValueOnce(
      new Response(JSON.stringify({ accessToken: '', user: null }), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      }),
    );

    await expect(exchangeCognitoToken('id-token')).rejects.toThrow('Respuesta inválida del servidor');
  });

  it('debe almacenar la sesión y devolverla cuando el intercambio es exitoso', async () => {
    mockFetch().mockResolvedValueOnce(
      new Response(JSON.stringify(mockExchangeResponse), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      }),
    );

    const session = await exchangeCognitoToken('id-token');

    expect(session.accessToken).toBe('syncpass-token');
    expect(session.user.role).toBe('ESTUDIANTE_VOLUNTARIO_MONITOR');
    expect(authStorage.getAccessToken()).toBe('syncpass-token');
  });
});

describe('fetchCurrentUser', () => {
  beforeEach(() => {
    authStorage.clear(false);
    vi.stubGlobal('fetch', vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  const mockFetch = (): Mock => globalThis.fetch as Mock;

  it('debe lanzar error si no hay token propio', async () => {
    await expect(fetchCurrentUser()).rejects.toThrow('No hay sesión propia activa');
  });

  it('debe lanzar error de conexión si fetch falla', async () => {
    authStorage.setSession({
      accessToken: 'token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: mockExchangeResponse.user,
    });
    mockFetch().mockRejectedValueOnce(new Error('network'));

    await expect(fetchCurrentUser()).rejects.toThrow('No se pudo conectar con el servidor');
  });

  it('debe limpiar la sesión si el servidor responde 401', async () => {
    authStorage.setSession({
      accessToken: 'expired',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: mockExchangeResponse.user,
    });
    mockFetch().mockResolvedValueOnce(new Response('Unauthorized', { status: 401 }));

    await expect(fetchCurrentUser()).rejects.toThrow('Sesión expirada');
    expect(authStorage.isAuthenticated()).toBe(false);
  });

  it('debe actualizar el usuario en la sesión actual cuando la consulta es exitosa', async () => {
    authStorage.setSession({
      accessToken: 'token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: mockExchangeResponse.user,
    });

    const updatedUser = { ...mockExchangeResponse.user, fullName: 'Nombre Actualizado' };
    mockFetch().mockResolvedValueOnce(
      new Response(JSON.stringify(updatedUser), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      }),
    );

    const user = await fetchCurrentUser();

    expect(user.fullName).toBe('Nombre Actualizado');
    expect(authStorage.getUser()?.fullName).toBe('Nombre Actualizado');
  });
});
