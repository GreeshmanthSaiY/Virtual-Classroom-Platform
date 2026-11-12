import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import { LoginPage, sanitizeRedirectPath } from '../pages/LoginPage';
import { AuthProvider } from '../context/AuthContext';
import { ThemeProvider } from '../context/ThemeContext';

describe('LoginPage Component & Security Gates', () => {
  it('renders login form inputs and academic sign-in interface', () => {
    render(
      <ThemeProvider>
        <AuthProvider>
          <BrowserRouter>
            <LoginPage />
          </BrowserRouter>
        </AuthProvider>
      </ThemeProvider>
    );

    expect(screen.getByText('Sign In to Virtual Classroom')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('e.g. gaurav.shukla@virtualclassroom.edu')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('••••••••••••')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });

  it('strictly hides demo credentials bar when VITE_ENABLE_DEMO_CREDENTIALS is not enabled', () => {
    render(
      <ThemeProvider>
        <AuthProvider>
          <BrowserRouter>
            <LoginPage />
          </BrowserRouter>
        </AuthProvider>
      </ThemeProvider>
    );

    expect(screen.queryByText('Quick-Fill Demo Credentials (Dev Only):')).not.toBeInTheDocument();
  });

  describe('Open Redirect Sanitizer', () => {
    it('allows valid relative application paths', () => {
      expect(sanitizeRedirectPath('/admin/users')).toBe('/admin/users');
      expect(sanitizeRedirectPath('/faculty/exams')).toBe('/faculty/exams');
      expect(sanitizeRedirectPath('/student/assignments?page=2')).toBe('/student/assignments?page=2');
    });

    it('rejects external URL schemes and open redirect vectors', () => {
      expect(sanitizeRedirectPath('https://evil.com')).toBeNull();
      expect(sanitizeRedirectPath('http://malicious.org/phish')).toBeNull();
      expect(sanitizeRedirectPath('//attacker.com/bypass')).toBeNull();
      expect(sanitizeRedirectPath('javascript:alert(1)')).toBeNull();
      expect(sanitizeRedirectPath('\\evil.com')).toBeNull();
      expect(sanitizeRedirectPath('')).toBeNull();
      expect(sanitizeRedirectPath(null)).toBeNull();
      expect(sanitizeRedirectPath(undefined)).toBeNull();
    });
  });
});

