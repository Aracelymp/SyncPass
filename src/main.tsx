import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { Amplify } from 'aws-amplify'
import { COGNITO_CONFIG } from './config/cognito'
import './styles/global.css'
import App from './App.tsx'

const amplifyConfig = {
  Auth: {
    Cognito: {
      userPoolId: COGNITO_CONFIG.userPoolId,
      userPoolClientId: COGNITO_CONFIG.userPoolWebClientId,
      loginWith: {
        oauth: {
          domain: COGNITO_CONFIG.oauth?.domain || '',
          scopes: COGNITO_CONFIG.oauth?.scope || ['openid', 'email', 'profile'],
          redirectSignIn: [COGNITO_CONFIG.oauth?.redirectSignIn || 'http://localhost:5173/auth/callback'],
          redirectSignOut: [COGNITO_CONFIG.oauth?.redirectSignOut || 'http://localhost:5173/login'],
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
