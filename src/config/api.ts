/**
 * Configuración base de la API
 * 
 * PENDIENTE: No hay integración con backend aún.
 * Esta estructura está preparada para futura implementación.
 * 
 * IMPORTANTE:
 * - No se realizan llamadas HTTP en esta etapa
 * - El backend actual usa JWT propio para app Android
 * - API Gateway + Cognito están previstos para app web
 */

export const API_CONFIG = {
  baseURL: import.meta.env.VITE_API_BASE_URL || '',  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
} as const;

export const API_ENDPOINTS = {
  auth: {
    login: '/api/auth/login',
    me: '/api/auth/me',
  },
  health: '/api/health',
  projects: '/api/projects',
  events: '/api/events',
  reports: '/api/reports/impact',
  sync: {
    batch: '/api/sync/batch',
    pull: '/api/sync/pull',
  },
} as const;

export type ApiConfig = typeof API_CONFIG;
export type ApiEndpoints = typeof API_ENDPOINTS;
