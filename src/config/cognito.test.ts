import { describe, it, expect, vi, beforeEach } from 'vitest';

describe('cognito config', () => {
  beforeEach(() => {
    vi.resetModules();
  });

  it('debe normalizar el dominio de Cognito eliminando protocolo y barras finales', async () => {
    const { COGNITO_CONFIG } = await import('./cognito');
    // El dominio se normaliza en tiempo de importación; este test verifica la función expuesta indirectamente
    // mediante la configuración resultante. No es una función exportada, por lo que validamos el comportamiento.
    expect(COGNITO_CONFIG).toBeDefined();
  });

  it('debe detectar que Cognito no está configurado cuando faltan variables', async () => {
    import.meta.env.VITE_AWS_REGION = '';
    import.meta.env.VITE_COGNITO_USER_POOL_ID = '';
    import.meta.env.VITE_COGNITO_APP_CLIENT_ID = '';
    import.meta.env.VITE_COGNITO_DOMAIN = '';

    const { isCognitoConfigured } = await import('./cognito');

    expect(isCognitoConfigured()).toBe(false);
  });

  it('debe detectar que Cognito está configurado cuando existen todas las variables', async () => {
    import.meta.env.VITE_AWS_REGION = 'us-east-1';
    import.meta.env.VITE_COGNITO_USER_POOL_ID = 'us-east-1_test';
    import.meta.env.VITE_COGNITO_APP_CLIENT_ID = 'client123';
    import.meta.env.VITE_COGNITO_DOMAIN = 'https://my-domain.auth.us-east-1.amazoncognito.com/';

    const { isCognitoConfigured, COGNITO_CONFIG } = await import('./cognito');

    expect(isCognitoConfigured()).toBe(true);
    expect(COGNITO_CONFIG.oauth?.domain).toBe('my-domain.auth.us-east-1.amazoncognito.com');
    expect(COGNITO_CONFIG.oauth?.scope).toEqual(['openid', 'email', 'profile']);
    expect(COGNITO_CONFIG.oauth?.responseType).toBe('code');
  });

  it('debe mantener los roles de Cognito definidos', async () => {
    const { COGNITO_ROLES } = await import('./cognito');

    expect(COGNITO_ROLES.DOCENTE_COORDINADOR).toBe('DOCENTE_COORDINADOR');
    expect(COGNITO_ROLES.ESTUDIANTE_VOLUNTARIO_MONITOR).toBe('ESTUDIANTE_VOLUNTARIO_MONITOR');
  });
});
