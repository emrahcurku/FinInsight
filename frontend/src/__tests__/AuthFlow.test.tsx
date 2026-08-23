import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider, useAuth } from '../context/AuthContext';
import { RegisterForm } from '../features/auth/RegisterForm';
import { authApi } from '../api/authApi';

vi.mock('../api/authApi', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    getCurrentUser: vi.fn(),
    refresh: vi.fn(),
  },
}));

const AuthStateDisplay = () => {
  const { user, isAuthenticated, logout } = useAuth();
  return (
    <div>
      <span data-testid="auth-status">{isAuthenticated ? 'Authenticated' : 'Guest'}</span>
      {user && <span data-testid="user-email">{user.email}</span>}
      <button onClick={logout}>Logout Button</button>
    </div>
  );
};

describe('Authentication Flow Integration', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('renders all required registration fields matching backend DTO', () => {
    render(
      <BrowserRouter>
        <AuthProvider>
          <RegisterForm />
        </AuthProvider>
      </BrowserRouter>
    );

    expect(screen.getByLabelText(/First Name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Last Name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Email Address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Password/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Confirm Password/i)).toBeInTheDocument();
  });

  it('successfully registers and automatically authenticates the user', async () => {
    const mockUserSummary = {
      id: 'uuid-123',
      email: 'john.doe@example.com',
      firstName: 'John',
      lastName: 'Doe',
      role: 'ROLE_USER' as const,
    };

    const mockAuthResponse = {
      accessToken: 'jwt-access-token-xyz',
      tokenType: 'Bearer',
      expiresIn: 900000,
      user: mockUserSummary,
    };

    vi.mocked(authApi.register).mockResolvedValue(mockUserSummary);
    vi.mocked(authApi.login).mockResolvedValue(mockAuthResponse);

    render(
      <BrowserRouter>
        <AuthProvider>
          <RegisterForm />
          <AuthStateDisplay />
        </AuthProvider>
      </BrowserRouter>
    );

    fireEvent.change(screen.getByLabelText(/First Name/i), { target: { value: 'John' } });
    fireEvent.change(screen.getByLabelText(/Last Name/i), { target: { value: 'Doe' } });
    fireEvent.change(screen.getByLabelText(/Email Address/i), { target: { value: 'john.doe@example.com' } });
    fireEvent.change(screen.getByLabelText(/^Password/i), { target: { value: 'Secret123!' } });
    fireEvent.change(screen.getByLabelText(/Confirm Password/i), { target: { value: 'Secret123!' } });

    fireEvent.click(screen.getByRole('button', { name: /Create Account/i }));

    await waitFor(() => {
      expect(authApi.register).toHaveBeenCalledWith({
        firstName: 'John',
        lastName: 'Doe',
        email: 'john.doe@example.com',
        password: 'Secret123!',
      });
      expect(authApi.login).toHaveBeenCalledWith({
        email: 'john.doe@example.com',
        password: 'Secret123!',
      });
      expect(screen.getByTestId('auth-status')).toHaveTextContent('Authenticated');
      expect(screen.getByTestId('user-email')).toHaveTextContent('john.doe@example.com');
      expect(localStorage.getItem('fininsight_token')).toBe('jwt-access-token-xyz');
    });
  });

  it('clears credentials and local state on logout', async () => {
    localStorage.setItem('fininsight_token', 'active-token');
    localStorage.setItem('fininsight_user', JSON.stringify({ id: '1', email: 'test@user.com', role: 'ROLE_USER' }));

    vi.mocked(authApi.logout).mockResolvedValue(undefined);

    render(
      <BrowserRouter>
        <AuthProvider>
          <AuthStateDisplay />
        </AuthProvider>
      </BrowserRouter>
    );

    expect(screen.getByTestId('auth-status')).toHaveTextContent('Authenticated');

    fireEvent.click(screen.getByText('Logout Button'));

    await waitFor(() => {
      expect(authApi.logout).toHaveBeenCalled();
      expect(screen.getByTestId('auth-status')).toHaveTextContent('Guest');
      expect(localStorage.getItem('fininsight_token')).toBeNull();
      expect(localStorage.getItem('fininsight_user')).toBeNull();
    });
  });
});
