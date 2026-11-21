import { describe, it, expect, beforeEach, vi } from 'vitest';
import { authStorage, type SyncPassSession } from './authStorage';

const mockSession: SyncPassSession = {
  accessToken: 'token-123',
  tokenType: 'Bearer',
  expiresIn: 3600,
  user: {
    id: 1,
    username: 'coordinador',
    email: 'test@example.com',
    fullName: 'Usuario Prueba',
    role: 'DOCENTE_COORDINADOR',
  },
};

describe('authStorage', () => {
  beforeEach(() => {
    authStorage.clear(false);
  });

  it('debe iniciar sin sesión', () => {
    expect(authStorage.getSession()).toBeNull();
    expect(authStorage.getAccessToken()).toBeNull();
    expect(authStorage.getUser()).toBeNull();
    expect(authStorage.isAuthenticated()).toBe(false);
  });

  it('debe almacenar y recuperar una sesión', () => {
    authStorage.setSession(mockSession);

    expect(authStorage.getSession()).toEqual(mockSession);
    expect(authStorage.getAccessToken()).toBe('token-123');
    expect(authStorage.getUser()).toEqual(mockSession.user);
    expect(authStorage.isAuthenticated()).toBe(true);
  });

  it('debe limpiar la sesión', () => {
    authStorage.setSession(mockSession);
    authStorage.clear(false);

    expect(authStorage.getSession()).toBeNull();
    expect(authStorage.isAuthenticated()).toBe(false);
  });

  it('debe notificar a los listeners cuando se limpia con notify=true', () => {
    const listener = vi.fn();
    const unsubscribe = authStorage.onAuthInvalidated(listener);

    authStorage.setSession(mockSession);
    authStorage.clear(true);

    expect(listener).toHaveBeenCalledTimes(1);

    unsubscribe();
  });

  it('no debe notificar a los listeners si no había sesión', () => {
    const listener = vi.fn();
    const unsubscribe = authStorage.onAuthInvalidated(listener);

    authStorage.clear(true);

    expect(listener).not.toHaveBeenCalled();

    unsubscribe();
  });

  it('debe permitir desuscribir un listener', () => {
    const listener = vi.fn();
    const unsubscribe = authStorage.onAuthInvalidated(listener);
    unsubscribe();

    authStorage.setSession(mockSession);
    authStorage.clear(true);

    expect(listener).not.toHaveBeenCalled();
  });

  it('debe considerar no autenticado si falta token o usuario', () => {
    authStorage.setSession({
      accessToken: '',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: mockSession.user,
    });

    expect(authStorage.isAuthenticated()).toBe(false);
  });
});
