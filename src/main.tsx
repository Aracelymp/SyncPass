import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { Amplify } from 'aws-amplify'
import { COGNITO_CONFIG } from './config/cognito'
import './styles/global.css'
import App from './App.tsx'

const fallbackOrigin =
  typeof window !== 'undefined' ? window.location.origin : 'http://localhost:5173';
const fallbackCallbackUrl = `${fallbackOrigin}/auth/callback`;
const fallbackLogoutUrl = `${fallbackOrigin}/login`;

const amplifyConfig = {
  Auth: {
    Cognito: {
      userPoolId: COGNITO_CONFIG.userPoolId,
      userPoolClientId: COGNITO_CONFIG.userPoolWebClientId,
      loginWith: {
        oauth: {
          domain: COGNITO_CONFIG.oauth?.domain || '',
          scopes: COGNITO_CONFIG.oauth?.scope || ['openid', 'email', 'profile'],
          // Incluye tanto la URL configurada como el origen actual. Esto permite
          // que Vercel Preview (URL dinámica) funcione aunque las variables de
          // entorno apunten a la URL de Production.
          redirectSignIn: [
            COGNITO_CONFIG.oauth?.redirectSignIn || fallbackCallbackUrl,
            fallbackCallbackUrl,
          ],
          redirectSignOut: [
            COGNITO_CONFIG.oauth?.redirectSignOut || fallbackLogoutUrl,
            fallbackLogoutUrl,
          ],
          responseType: 'code' as const,
        },
      },
    },
  },
}

Amplify.configure(amplifyConfig)

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
