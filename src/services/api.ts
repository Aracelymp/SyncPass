/**
 * Cliente HTTP base para API
 * 
 * PENDIENTE: No hay llamadas HTTP implementadas en esta etapa.
 * Estructura preparada para futura integración con API Gateway/Backend.
 * 
 * IMPORTANTE:
 * - No se realizan solicitudes en esta etapa
 * - Requiere autorización para instalar cliente HTTP (axios/fetch wrapper)
 * - Debe agregar Authorization header cuando haya tokens
 * - Errores 401/403 deben manejarse correctamente
 * - Nunca confiar solo en frontend para autorización
 */

import { API_CONFIG } from '../config/api';

export class ApiClient {
  private baseURL: string;
  private timeout: number;

  constructor() {
    this.baseURL = API_CONFIG.baseURL;
    this.timeout = API_CONFIG.timeout;
    void this.baseURL;
    void this.timeout;
  }

  /**
   * Método base - NO IMPLEMENTADO
   * Pendiente de implementación con fetch/axios tras autorización
   */
  async request<T>(_endpoint: string, _options?: RequestInit): Promise<T> {
    throw new Error('API client no implementado. Pendiente de integración con backend.');
  }

  /**
   * GET - NO IMPLEMENTADO
   */
  async get<T>(_endpoint: string): Promise<T> {
    throw new Error('Método GET no implementado. Pendiente de integración.');
  }

  /**
   * POST - NO IMPLEMENTADO
   */
  async post<T>(_endpoint: string, _data?: unknown): Promise<T> {
    throw new Error('Método POST no implementado. Pendiente de integración.');
  }

  /**
   * PUT - NO IMPLEMENTADO
   */
  async put<T>(_endpoint: string, _data?: unknown): Promise<T> {
    throw new Error('Método PUT no implementado. Pendiente de integración.');
  }

  /**
   * DELETE - NO IMPLEMENTADO
   */
  async delete<T>(_endpoint: string): Promise<T> {
    throw new Error('Método DELETE no implementado. Pendiente de integración.');
  }
}

export const apiClient = new ApiClient();
