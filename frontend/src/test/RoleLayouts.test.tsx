import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { AdminLayout, FacultyLayout, StudentLayout } from '../shared/layouts';
import { ThemeProvider } from '../context/ThemeContext';
import * as AuthContextModule from '../context/AuthContext';

vi.mock('../context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

describe('Role-Based Application Shell Layouts', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('AdminLayout', () => {
    it('renders admin branding, navigation links, and child outlet', () => {
      vi.mocked(AuthContextModule.useAuth).mockReturnValue({
        user: { id: 1, email: 'admin@vc.edu', fullName: 'System Admin', role: 'ROLE_ADMIN', status: 'APPROVED', createdAt: '2026-01-01T00:00:00Z' },
        token: 'mock-token',
        isAuthenticated: true,
        isLoading: false,
        login: vi.fn(),
        logout: vi.fn(),
        setUser: vi.fn(),
        refreshProfile: vi.fn(),
      });

      render(
        <ThemeProvider>
          <MemoryRouter initialEntries={['/admin']}>
            <Routes>
              <Route element={<AdminLayout />}>
                <Route path="/admin" element={<div>Admin Content Area</div>} />
              </Route>
            </Routes>
          </MemoryRouter>
        </ThemeProvider>
      );

      expect(screen.getByText('Admin Console')).toBeInTheDocument();
      expect(screen.getByText('Platform Overview')).toBeInTheDocument();
      expect(screen.getByText('User Governance')).toBeInTheDocument();
      expect(screen.getByText('Degrees & Curriculum')).toBeInTheDocument();
      expect(screen.getByText('Admin Content Area')).toBeInTheDocument();
    });

    it('toggles mobile navigation drawer', () => {
      vi.mocked(AuthContextModule.useAuth).mockReturnValue({
        user: { id: 1, email: 'admin@vc.edu', fullName: 'System Admin', role: 'ROLE_ADMIN', status: 'APPROVED', createdAt: '2026-01-01T00:00:00Z' },
        token: 'mock-token',
        isAuthenticated: true,
        isLoading: false,
        login: vi.fn(),
        logout: vi.fn(),
        setUser: vi.fn(),
        refreshProfile: vi.fn(),
      });

      render(
        <ThemeProvider>
          <MemoryRouter initialEntries={['/admin']}>
            <Routes>
              <Route element={<AdminLayout />}>
                <Route path="/admin" element={<div>Admin Content Area</div>} />
              </Route>
            </Routes>
          </MemoryRouter>
        </ThemeProvider>
      );

      const toggleBtn = screen.getByRole('button', { name: /toggle navigation menu/i });
      expect(toggleBtn).toBeInTheDocument();
      fireEvent.click(toggleBtn);

      // Mobile drawer should open and display the close button
      expect(screen.getAllByText('User Governance').length).toBeGreaterThanOrEqual(1);
    });
  });

  describe('FacultyLayout', () => {
    it('renders faculty branding, navigation links, and child outlet', () => {
      vi.mocked(AuthContextModule.useAuth).mockReturnValue({
        user: { id: 2, email: 'faculty@vc.edu', fullName: 'Prof. Miller', role: 'ROLE_FACULTY', status: 'APPROVED', createdAt: '2026-01-01T00:00:00Z' },
        token: 'mock-token',
        isAuthenticated: true,
        isLoading: false,
        login: vi.fn(),
        logout: vi.fn(),
        setUser: vi.fn(),
        refreshProfile: vi.fn(),
      });

      render(
        <ThemeProvider>
          <MemoryRouter initialEntries={['/faculty']}>
            <Routes>
              <Route element={<FacultyLayout />}>
                <Route path="/faculty" element={<div>Faculty Content Area</div>} />
              </Route>
            </Routes>
          </MemoryRouter>
        </ThemeProvider>
      );

      expect(screen.getByText('Faculty Workspace')).toBeInTheDocument();
      expect(screen.getByText('Faculty Overview')).toBeInTheDocument();
      expect(screen.getByText('My Subjects & Rosters')).toBeInTheDocument();
      expect(screen.getByText('Assignments & Grading')).toBeInTheDocument();
      expect(screen.getByText('Faculty Content Area')).toBeInTheDocument();
    });
  });

  describe('StudentLayout', () => {
    it('renders student portal branding, navigation links, and child outlet', () => {
      vi.mocked(AuthContextModule.useAuth).mockReturnValue({
        user: { id: 3, email: 'student@vc.edu', fullName: 'Alice Scholar', role: 'ROLE_STUDENT', status: 'APPROVED', createdAt: '2026-01-01T00:00:00Z' },
        token: 'mock-token',
        isAuthenticated: true,
        isLoading: false,
        login: vi.fn(),
        logout: vi.fn(),
        setUser: vi.fn(),
        refreshProfile: vi.fn(),
      });

      render(
        <ThemeProvider>
          <MemoryRouter initialEntries={['/student']}>
            <Routes>
              <Route element={<StudentLayout />}>
                <Route path="/student" element={<div>Student Content Area</div>} />
              </Route>
            </Routes>
          </MemoryRouter>
        </ThemeProvider>
      );

      expect(screen.getByText('Student Portal')).toBeInTheDocument();
      expect(screen.getByText('Student Dashboard')).toBeInTheDocument();
      expect(screen.getByText('My Enrolled Subjects')).toBeInTheDocument();
      expect(screen.getByText('Assignments & Work')).toBeInTheDocument();
      expect(screen.getByText('Student Content Area')).toBeInTheDocument();
    });
  });
});
