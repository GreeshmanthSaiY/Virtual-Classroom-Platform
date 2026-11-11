import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { Navbar } from './components/common/Navbar';
import { Footer } from './components/common/Footer';
import { ProtectedRoute } from './components/common/ProtectedRoute';

import { LandingPage } from './pages/LandingPage';
import { AboutPage } from './pages/AboutPage';
import { LoginPage } from './pages/LoginPage';
import { StudentRegisterPage } from './pages/StudentRegisterPage';
import { FacultyRegisterPage } from './pages/FacultyRegisterPage';
import { StudentDashboard } from './pages/student/StudentDashboard';
import { FacultyDashboard } from './pages/faculty/FacultyDashboard';
import { AdminDashboard } from './pages/admin/AdminDashboard';
import { CourseMaterialsPage } from './pages/classroom/CourseMaterialsPage';
import { VideoLecturesPage } from './pages/classroom/VideoLecturesPage';
import { QnAForumPage } from './pages/classroom/QnAForumPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <AuthProvider>
          <BrowserRouter>
            <div className="min-h-screen flex flex-col bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 antialiased selection:bg-primary-500 selection:text-white transition-colors duration-200">
              <Navbar />

              <div className="flex-1">
                <Routes>
                  {/* Public Academic Routes */}
                  <Route path="/" element={<LandingPage />} />
                  <Route path="/about" element={<AboutPage />} />
                  <Route path="/login" element={<LoginPage />} />
                  <Route path="/register/student" element={<StudentRegisterPage />} />
                  <Route path="/register/faculty" element={<FacultyRegisterPage />} />
                  <Route path="/materials" element={<CourseMaterialsPage />} />
                  <Route path="/lectures" element={<VideoLecturesPage />} />
                  <Route path="/forum" element={<QnAForumPage />} />

                  {/* Student Portal Protected Routes */}
                  <Route
                    path="/student/*"
                    element={
                      <ProtectedRoute allowedRoles={['ROLE_STUDENT', 'ROLE_ADMIN']}>
                        <StudentDashboard />
                      </ProtectedRoute>
                    }
                  />

                  {/* Faculty Portal Protected Routes */}
                  <Route
                    path="/faculty/*"
                    element={
                      <ProtectedRoute allowedRoles={['ROLE_FACULTY', 'ROLE_ADMIN']}>
                        <FacultyDashboard />
                      </ProtectedRoute>
                    }
                  />

                  {/* Administrator Console Protected Routes */}
                  <Route
                    path="/admin/*"
                    element={
                      <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                        <AdminDashboard />
                      </ProtectedRoute>
                    }
                  />

                  {/* Fallback */}
                  <Route path="*" element={<Navigate to="/" replace />} />
                </Routes>
              </div>

              <Footer />
            </div>
          </BrowserRouter>
        </AuthProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
};

