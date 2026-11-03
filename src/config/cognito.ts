/**
 * Configuración para Amazon Cognito (Amplify v6)
 *
 * IMPORTANTE:
 * - Las variables VITE_* son públicas (no poner secretos)
 * - Usar flujo Authorization Code + PKCE (SPA sin secreto de cliente)
 * - El backend actual usa JWT propio para Android - NO asumir compatibilidad automática
 * - La autorización real DEBE validarse en el servidor
 * - Cognito Groups/Attributes se usarán para mapear roles
 */

export interface CognitoConfig {
  region: string;
  userPoolId: string;
  userPoolWebClientId: string;
  oauth?: {
    domain: string;
    scope: string[];
    redirectSignIn: string;
    redirectSignOut: string;
    responseType: 'code';
  };
}

/**
 * Amplify v6 construye las URLs de Cognito como `https://${domain}/oauth2/...`.
 * Si la variable de entorno incluye protocolo o barra final, la URL queda malformada.
 */
const normalizeCognitoDomain = (domain: string): string =>
  domain
    .trim()
    .replace(/^https?:\/\//, '')
    .replace(/\/+$/, '');

/**
 * Usa el origen actual como fallback para los redirects. Esto permite que cada
 * despliegue de Vercel (Preview con URL dinámica) use su propio callback sin
 * depender de que VITE_REDIRECT_SIGN_IN/_SIGN_OUT esté exactamente igual.
 */
const getCurrentOrigin = (): string =>
  typeof window !== 'undefined' ? window.location.origin : 'http://localhost:5173';

export const COGNITO_CONFIG: CognitoConfig = {
  region: import.meta.env.VITE_AWS_REGION || '',
  userPoolId: import.meta.env.VITE_COGNITO_USER_POOL_ID || '',
  userPoolWebClientId: import.meta.env.VITE_COGNITO_APP_CLIENT_ID || '',
  oauth: import.meta.env.VITE_COGNITO_DOMAIN
    ? {
        domain: normalizeCognitoDomain(import.meta.env.VITE_COGNITO_DOMAIN),
        scope: ['openid', 'email', 'profile'],
        redirectSignIn:
          import.meta.env.VITE_REDIRECT_SIGN_IN ||
          `${getCurrentOrigin()}/auth/callback`,
        redirectSignOut:
          import.meta.env.VITE_REDIRECT_SIGN_OUT || `${getCurrentOrigin()}/login`,
        responseType: 'code',
      }
    : undefined,
};

export const isCognitoConfigured = (): boolean => {
  const cfg = COGNITO_CONFIG;
  return Boolean(cfg.userPoolId && cfg.userPoolWebClientId && cfg.region && cfg.oauth?.domain);
};

export const COGNITO_ROLES = {
  DOCENTE_COORDINADOR: 'DOCENTE_COORDINADOR',
  ESTUDIANTE_VOLUNTARIO_MONITOR: 'ESTUDIANTE_VOLUNTARIO_MONITOR',
} as const;

export type CognitoRole = keyof typeof COGNITO_ROLES;
