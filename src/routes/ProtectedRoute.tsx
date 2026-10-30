/**
 * Ruta Protegida - Integración con AuthContext (Cognito) + JWT propio SyncPass
 *
 * IMPORTANTE:
 * - Solo permite acceso si hay sesión propia válida (JWT propio)
 * - No confiar solo en controles visuales para autorizar
 * - Autorización real debe validarse en servidor
 */

import React from 'react';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { authStorage } from '../utils/authStorage';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRole?: 'DOCENTE_COORDINADOR' | 'ESTUDIANTE_VOLUNTARIO_MONITOR';
}

const ROLE_LABELS: Record<string, string> = {
  DOCENTE_COORDINADOR: 'Docente · Coordinador',
  ESTUDIANTE_VOLUNTARIO_MONITOR: 'Estudiante · Voluntario · Monitor',
};

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, requiredRole }) => {
  const { isAuthenticated, isLoading, user } = useAuth();

  if (isLoading) {
    return (
      <div
        style={{
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: '#f9fafb',
        }}
      >
        <p style={{ color: '#374151', fontSize: '0.95rem' }}>Verificando sesión...</p>
      </div>
    );
  }

  if (!isAuthenticated || !authStorage.isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  if (requiredRole && user?.role !== requiredRole) {
    return (
      <div
        style={{
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: '#f9fafb',
          padding: '2rem',
        }}
      >
        <div
          style={{
            maxWidth: '520px',
            width: '100%',
            backgroundColor: '#ffffff',
            border: '1px solid #e5e7eb',
            borderRadius: '12px',
            padding: '2rem',
            textAlign: 'center',
            boxShadow: '0 4px 12px rgba(0, 0, 0, 0.06)',
          }}
        >
          <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#7c1d3b', marginBottom: '0.75rem' }}>
            Acceso denegado
          </h1>
          <p style={{ color: '#4b5563', fontSize: '0.95rem', lineHeight: 1.5, marginBottom: '1.5rem' }}>
            Tu rol actual no tiene permiso para ver esta sección. Se requiere el rol{' '}
            <strong>{ROLE_LABELS[requiredRole] ?? requiredRole}</strong>. Si crees que es un error,
            contacta a la coordinación del proyecto.
          </p>
          <Link
            to="/dashboard"
            style={{
              display: 'inline-block',
              padding: '0.625rem 1.5rem',
              backgroundColor: '#7c1d3b',
              color: '#ffffff',
              borderRadius: '8px',
              fontWeight: 600,
              textDecoration: 'none',
              fontSize: '0.9rem',
            }}
          >
            Volver al panel
          </Link>
        </div>
      </div>
    );
  }

  return <>{children}</>;
};
