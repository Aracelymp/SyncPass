import { describe, it, expect } from 'vitest';
import { API_CONFIG, API_ENDPOINTS } from './api';

describe('API_CONFIG', () => {
  it('debe tener un timeout definido', () => {
    expect(API_CONFIG.timeout).toBeGreaterThan(0);
  });

  it('debe incluir el header Content-Type por defecto', () => {
    expect(API_CONFIG.headers['Content-Type']).toBe('application/json');
  });
});

describe('API_ENDPOINTS', () => {
  it('debe definir los endpoints de autenticación', () => {
    expect(API_ENDPOINTS.auth.login).toBe('/api/auth/login');
    expect(API_ENDPOINTS.auth.me).toBe('/api/auth/me');
  });

  it('debe definir los endpoints principales', () => {
    expect(API_ENDPOINTS.health).toBe('/api/health');
    expect(API_ENDPOINTS.projects).toBe('/api/projects');
    expect(API_ENDPOINTS.events).toBe('/api/events');
    expect(API_ENDPOINTS.reports).toBe('/api/reports/impact');
  });

  it('debe definir los endpoints de sincronización', () => {
    expect(API_ENDPOINTS.sync.batch).toBe('/api/sync/batch');
    expect(API_ENDPOINTS.sync.pull).toBe('/api/sync/pull');
  });
});
