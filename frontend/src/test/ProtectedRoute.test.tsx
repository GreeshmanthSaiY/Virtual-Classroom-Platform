import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { ProtectedRoute } from '../components/navigation/ProtectedRoute';
import * as AuthContextModule from '../context/AuthContext';

vi.mock('../context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

describe('ProtectedRoute Access Control', () => {
  it('redirects unauthenticated users to /login', () => {
    vi.mocked(AuthContextModule.useAuth).mockReturnValue({
      user: null,
      token: null,
      isAuthenticated: false,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      setUser: vi.fn(),
      refreshProfile: vi.fn(),
    });

    render(
      <MemoryRouter initialEntries={['/admin']}>
        <Routes>
          <Route path="/login" element={<div>Sign In Screen</div>} />
          <Route
            path="/admin"
            element={
              <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                <div>Admin Secret Console</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Sign In Screen')).toBeInTheDocument();
    expect(screen.queryByText('Admin Secret Console')).not.toBeInTheDocument();
  });

  it('redirects authenticated user with wrong role to /unauthorized', () => {
    vi.mocked(AuthContextModule.useAuth).mockReturnValue({
      user: {
        id: 1,
        email: 'student@vc.edu',
        fullName: 'Student User',
        role: 'ROLE_STUDENT',
        status: 'APPROVED',
        createdAt: '2026-01-01',
      },
      token: 'valid-token',
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      setUser: vi.fn(),
      refreshProfile: vi.fn(),
    });

    render(
      <MemoryRouter initialEntries={['/admin']}>
        <Routes>
          <Route path="/unauthorized" element={<div>Access Forbidden Screen</div>} />
          <Route
            path="/admin"
            element={
              <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                <div>Admin Secret Console</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Access Forbidden Screen')).toBeInTheDocument();
    expect(screen.queryByText('Admin Secret Console')).not.toBeInTheDocument();
  });

  it('permits authorized user into protected content', () => {
    vi.mocked(AuthContextModule.useAuth).mockReturnValue({
      user: {
        id: 99,
        email: 'admin@vc.edu',
        fullName: 'Platform Admin',
        role: 'ROLE_ADMIN',
        status: 'APPROVED',
        createdAt: '2026-01-01',
      },
      token: 'admin-token',
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      setUser: vi.fn(),
      refreshProfile: vi.fn(),
    });

    render(
      <MemoryRouter initialEntries={['/admin']}>
        <Routes>
          <Route
            path="/admin"
            element={
              <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                <div>Admin Secret Console</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Admin Secret Console')).toBeInTheDocument();
  });
});
