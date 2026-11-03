import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { useAuth } from '../../context/AuthContext';
import { isCognitoConfigured } from '../../config/cognito';
import { authStorage } from '../../utils/authStorage';
import './Login.css';

export const Login: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, isAuthenticated, isLoading, authError, clearAuthError } = useAuth();
  const [localError, setLocalError] = useState<string | null>(null);
  const cognitoReady = isCognitoConfigured();

  // Mensaje propagado por Callback cuando falla el intercambio con el backend
  const locationState = location.state as { authError?: unknown } | null;
  const stateAuthError =
    typeof locationState?.authError === 'string' && locationState.authError.trim().length > 0
      ? locationState.authError
      : null;

  useEffect(() => {
    if (!isLoading && authStorage.isAuthenticated() && isAuthenticated) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, isLoading, navigate]);

  useEffect(() => {
    return () => {
      clearAuthError();
    };
  }, [clearAuthError]);

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError('El inicio de sesión con usuario y contraseña no está habilitado. Usa "Iniciar sesión con Cognito".');
  };

  const handleCognitoLogin = async () => {
    setLocalError(null);
    clearAuthError();
    if (!cognitoReady) {
      setLocalError('Configura las variables VITE_COGNITO_* para habilitar el inicio de sesión con Cognito.');
      return;
    }
    try {
      await login();
    } catch (err) {
      const message =
        err instanceof Error
          ? err.message
          : 'No se pudo iniciar el flujo de autenticación con Cognito.';
      setLocalError(message);
    }
  };

  const displayError = stateAuthError || authError || localError;

  return (
    <div className="login-container">
      <div className="login-card-wrapper">
        <div className="login-header">
          <h1 className="login-logo">SyncPass</h1>
          <p className="login-subtitle">Gestión de Proyectos Comunitarios</p>
        </div>
        <Card>
          <form onSubmit={handleLogin} className="login-form">
            <div className="login-form-title">
              <h2>Iniciar Sesión</h2>
            </div>
            {displayError && (
              <p style={{ color: '#dc2626', fontSize: '0.875rem', margin: 0 }}>{displayError}</p>
            )}
            <Button
              type="button"
              variant="primary"
              className="login-button"
              onClick={handleCognitoLogin}
              disabled={!cognitoReady || isLoading}
            >
              Iniciar sesión con Cognito
            </Button>
            {!cognitoReady && (
              <p className="login-demo-note">Configura las variables VITE_COGNITO_* en .env.local para habilitarlo</p>
            )}
          </form>
        </Card>
        <p className="login-footer">
          Proyecto web independiente - Desarrollo Fullstack II
        </p>
      </div>
    </div>
  );
};
