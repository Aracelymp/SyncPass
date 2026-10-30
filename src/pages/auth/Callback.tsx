import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCurrentUser, signOut } from 'aws-amplify/auth';
import { useAuth } from '../../context/AuthContext';
import { authStorage } from '../../utils/authStorage';
import { exchangeCognitoToken } from '../../services/authExchange';
import { fetchAuthSession } from 'aws-amplify/auth';
import { runExchangeOnce } from '../../utils/authExchangeLock';
import './Login.css';

export const Callback: React.FC = () => {
  const navigate = useNavigate();
  const { checkAuth } = useAuth();
  const [error, setError] = useState<string | null>(null);
  // Garantiza una única ejecución del flujo, incluso con StrictMode
  // o con re-renders del provider que cambien la identidad de checkAuth.
  const startedRef = useRef(false);

  useEffect(() => {
    // Una sola ejecución por montaje. No se cancela en el cleanup porque el único
    // cleanup que ocurre aquí es el simulado por StrictMode (checkAuth y navigate
    // son estables); cancelar abortaría el flujo y dejaría la pantalla bloqueada.
    if (startedRef.current) return;
    startedRef.current = true;

    const handleCallback = async () => {
      try {
        await new Promise((resolve) => setTimeout(resolve, 500));
        try {
          await getCurrentUser();
        } catch {
          setError('No se pudo obtener la sesión de Cognito.');
          setTimeout(() => navigate('/login', { replace: true }), 1500);
          return;
        }

        try {
          const session = await fetchAuthSession({ forceRefresh: false });
          const idToken = session.tokens?.idToken?.toString();
          if (!idToken) {
            await signOut().catch(() => undefined);
            setError('No se pudo obtener el token de Cognito.');
            setTimeout(() => navigate('/login', { replace: true }), 1500);
            return;
          }
          await runExchangeOnce(() => exchangeCognitoToken(idToken));
        } catch (err) {
          const message =
            err instanceof Error
              ? err.message
              : 'No se pudo completar el inicio de sesión con SyncPass.';
          authStorage.clear(true);
          await signOut().catch(() => undefined);
          setError(message);
          setTimeout(
            () => navigate('/login', { replace: true, state: { authError: message } }),
            2000,
          );
          return;
        }

        await checkAuth();

        if (authStorage.isAuthenticated()) {
          navigate('/dashboard', { replace: true });
          return;
        }

        authStorage.clear(true);
        await signOut().catch(() => undefined);
        setError('No se pudo establecer la sesión de SyncPass.');
        setTimeout(() => navigate('/login', { replace: true }), 1500);
      } catch {
        authStorage.clear(true);
        await signOut().catch(() => undefined);
        setError('No se pudo completar el inicio de sesión con Cognito.');
        setTimeout(() => navigate('/login', { replace: true }), 1500);
      }
    };

    void handleCallback();
  }, [checkAuth, navigate]);

  return (
    <div className="login-container">
      <div className="login-card-wrapper">
        <div className="login-header">
          <h1 className="login-logo">SyncPass</h1>
          <p className="login-subtitle">Procesando autenticación...</p>
        </div>
        <div style={{ textAlign: 'center', padding: '1rem' }}>
          {error ? <p>{error}</p> : <p>Redirigiendo...</p>}
        </div>
      </div>
    </div>
  );
};

export default Callback;
