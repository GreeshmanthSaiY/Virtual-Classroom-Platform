import React, { Suspense, lazy } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { ProtectedRoute } from './components/navigation/ProtectedRoute';
import { PublicLayout, AdminLayout, FacultyLayout, StudentLayout } from './shared/layouts';
import { SessionExpiredModal } from './features/auth/SessionExpiredModal';
import { LoadingSpinner } from './shared/components/LoadingSpinner';

// Lazy-loaded Public Pages
const LandingPage = lazy(() => import('./pages/LandingPage').then((m) => ({ default: m.LandingPage })));
const AboutPage = lazy(() => import('./pages/AboutPage').then((m) => ({ default: m.AboutPage })));
const LoginPage = lazy(() => import('./pages/LoginPage').then((m) => ({ default: m.LoginPage })));
const StudentRegisterPage = lazy(() =>
  import('./pages/StudentRegisterPage').then((m) => ({ default: m.StudentRegisterPage }))
);
const FacultyRegisterPage = lazy(() =>
  import('./pages/FacultyRegisterPage').then((m) => ({ default: m.FacultyRegisterPage }))
);
const ForgotPasswordPage = lazy(() =>
  import('./pages/ForgotPasswordPage').then((m) => ({ default: m.ForgotPasswordPage }))
);
const ResetPasswordPage = lazy(() =>
  import('./pages/ResetPasswordPage').then((m) => ({ default: m.ResetPasswordPage }))
);
const UnauthorizedPage = lazy(() =>
  import('./pages/UnauthorizedPage').then((m) => ({ default: m.UnauthorizedPage }))
);
const CourseMaterialsPage = lazy(() =>
  import('./pages/classroom/CourseMaterialsPage').then((m) => ({ default: m.CourseMaterialsPage }))
);
const VideoLecturesPage = lazy(() =>
  import('./pages/classroom/VideoLecturesPage').then((m) => ({ default: m.VideoLecturesPage }))
);
const QnAForumPage = lazy(() =>
  import('./pages/classroom/QnAForumPage').then((m) => ({ default: m.QnAForumPage }))
);

// Lazy-loaded Admin Pages
const AdminDashboard = lazy(() =>
  import('./pages/admin/AdminDashboard').then((m) => ({ default: m.AdminDashboard }))
);
const AdminUsersPage = lazy(() =>
  import('./pages/admin/AdminUsersPage').then((m) => ({ default: m.AdminUsersPage }))
);
const AdminCoursesPage = lazy(() =>
  import('./pages/admin/AdminCoursesPage').then((m) => ({ default: m.AdminCoursesPage }))
);

// Lazy-loaded Faculty Pages
const FacultyDashboard = lazy(() =>
  import('./pages/faculty/FacultyDashboard').then((m) => ({ default: m.FacultyDashboard }))
);
const FacultySubjectsPage = lazy(() =>
  import('./pages/faculty/FacultySubjectsPage').then((m) => ({ default: m.FacultySubjectsPage }))
);
const FacultyAssignmentsPage = lazy(() =>
  import('./pages/faculty/FacultyAssignmentsPage').then((m) => ({ default: m.FacultyAssignmentsPage }))
);
const FacultyExamsPage = lazy(() =>
  import('./pages/faculty/FacultyExamsPage').then((m) => ({ default: m.FacultyExamsPage }))
);
const FacultyLiveSessionsPage = lazy(() =>
  import('./pages/faculty/FacultyLiveSessionsPage').then((m) => ({ default: m.FacultyLiveSessionsPage }))
);

// Lazy-loaded Student Pages
const StudentDashboard = lazy(() =>
  import('./pages/student/StudentDashboard').then((m) => ({ default: m.StudentDashboard }))
);
const StudentSubjectsPage = lazy(() =>
  import('./pages/student/StudentSubjectsPage').then((m) => ({ default: m.StudentSubjectsPage }))
);
const StudentAssignmentsPage = lazy(() =>
  import('./pages/student/StudentAssignmentsPage').then((m) => ({ default: m.StudentAssignmentsPage }))
);
const StudentExamsPage = lazy(() =>
  import('./pages/student/StudentExamsPage').then((m) => ({ default: m.StudentExamsPage }))
);
const StudentLiveSessionsPage = lazy(() =>
  import('./pages/student/StudentLiveSessionsPage').then((m) => ({ default: m.StudentLiveSessionsPage }))
);

const PageFallback: React.FC = () => (
  <div className="min-h-[60vh] flex items-center justify-center p-8">
    <LoadingSpinner size="lg" label="Loading academic view..." />
  </div>
);

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
            <SessionExpiredModal />
            <Suspense fallback={<PageFallback />}>
              <Routes>
                {/* Public & Academic Showcase Routes */}
                <Route element={<PublicLayout />}>
                  <Route path="/" element={<LandingPage />} />
                  <Route path="/about" element={<AboutPage />} />
                  <Route path="/login" element={<LoginPage />} />
                  <Route path="/register/student" element={<StudentRegisterPage />} />
                  <Route path="/register/faculty" element={<FacultyRegisterPage />} />
                  <Route path="/forgot-password" element={<ForgotPasswordPage />} />
                  <Route path="/reset-password" element={<ResetPasswordPage />} />
                  <Route path="/unauthorized" element={<UnauthorizedPage />} />
                  <Route path="/materials" element={<CourseMaterialsPage />} />
                  <Route path="/lectures" element={<VideoLecturesPage />} />
                  <Route path="/forum" element={<QnAForumPage />} />
                  <Route path="/discussions" element={<Navigate to="/forum" replace />} />
                </Route>

                {/* Administrator Console Protected Routes */}
                <Route
                  path="/admin"
                  element={
                    <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                      <AdminLayout />
                    </ProtectedRoute>
                  }
                >
                  <Route index element={<AdminDashboard />} />
                  <Route path="users" element={<AdminUsersPage />} />
                  <Route path="courses" element={<AdminCoursesPage />} />
                </Route>

                {/* Faculty Workspace Protected Routes */}
                <Route
                  path="/faculty"
                  element={
                    <ProtectedRoute allowedRoles={['ROLE_FACULTY', 'ROLE_ADMIN']}>
                      <FacultyLayout />
                    </ProtectedRoute>
                  }
                >
                  <Route index element={<FacultyDashboard />} />
                  <Route path="subjects" element={<FacultySubjectsPage />} />
                  <Route path="assignments" element={<FacultyAssignmentsPage />} />
                  <Route path="examinations" element={<FacultyExamsPage />} />
                  <Route path="live-sessions" element={<FacultyLiveSessionsPage />} />
                </Route>

                {/* Student Portal Protected Routes */}
                <Route
                  path="/student"
                  element={
                    <ProtectedRoute allowedRoles={['ROLE_STUDENT', 'ROLE_ADMIN']}>
                      <StudentLayout />
                    </ProtectedRoute>
                  }
                >
                  <Route index element={<StudentDashboard />} />
                  <Route path="subjects" element={<StudentSubjectsPage />} />
                  <Route path="assignments" element={<StudentAssignmentsPage />} />
                  <Route path="examinations" element={<StudentExamsPage />} />
                  <Route path="live-sessions" element={<StudentLiveSessionsPage />} />
                </Route>

                {/* Global Fallback */}
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </Suspense>
          </BrowserRouter>
        </AuthProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
};
