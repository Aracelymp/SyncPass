import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { AuthProvider, useAuth } from './AuthContext';
import { authStorage } from '../utils/authStorage';

const mocks = vi.hoisted(() => ({
  getCurrentUser: vi.fn(),
  fetchAuthSession: vi.fn(),
  signInWithRedirect: vi.fn(),
  signOut: vi.fn(),
}));

vi.mock('aws-amplify/auth', () => mocks);

const TestConsumer: React.FC = () => {
  const { isAuthenticated, isLoading, user, login, logout } = useAuth();
  return (
    <div>
      <span data-testid="loading">{isLoading ? 'cargando' : 'listo'}</span>
      <span data-testid="authenticated">{isAuthenticated ? 'si' : 'no'}</span>
      <span data-testid="user">{user?.username ?? 'sin-usuario'}</span>
      <button data-testid="login" onClick={() => void login()}>
        Iniciar sesión
      </button>
      <button data-testid="logout" onClick={() => void logout()}>
        Cerrar sesión
      </button>
    </div>
  );
};

describe('AuthContext', () => {
  beforeEach(() => {
    authStorage.clear(false);
    vi.clearAllMocks();
    mocks.getCurrentUser.mockRejectedValue(new Error('No current user'));
    mocks.fetchAuthSession.mockResolvedValue({ tokens: { idToken: undefined } });
    mocks.signOut.mockResolvedValue(undefined);
  });

  it('debe lanzar error si useAuth se usa fuera del AuthProvider', () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});

    expect(() => render(<TestConsumer />)).toThrow();

    consoleError.mockRestore();
  });

  it('debe renderizar los hijos y finalizar la carga cuando no hay sesión', async () => {
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>,
    );

    expect(screen.getByTestId('loading')).toHaveTextContent('cargando');

    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('listo'));

    expect(screen.getByTestId('authenticated')).toHaveTextContent('no');
    expect(screen.getByTestId('user')).toHaveTextContent('sin-usuario');
  });

  it('debe llamar a signInWithRedirect cuando se invoca login', async () => {
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>,
    );

    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('listo'));

    fireEvent.click(screen.getByTestId('login'));

    await waitFor(() => expect(mocks.signInWithRedirect).toHaveBeenCalledTimes(1));
  });

  it('debe llamar a signOut y limpiar la sesión cuando se invoca logout', async () => {
    authStorage.setSession({
      accessToken: 'token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: {
        id: 1,
        username: 'user',
        email: null,
        fullName: null,
        role: 'DOCENTE_COORDINADOR',
      },
    });

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>,
    );

    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('listo'));

    fireEvent.click(screen.getByTestId('logout'));

    await waitFor(() => expect(mocks.signOut).toHaveBeenCalledTimes(1));
    expect(authStorage.isAuthenticated()).toBe(false);
  });
});
