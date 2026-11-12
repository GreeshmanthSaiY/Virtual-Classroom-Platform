import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { GraduationCap, Lock, Mail, AlertCircle, ArrowRight, ShieldCheck } from 'lucide-react';
import { LoadingSpinner } from '../../components/ui/LoadingSpinner';

const loginSchema = z.object({
  email: z.string().email('Please enter a valid academic email address'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
});

type LoginFormValues = z.infer<typeof loginSchema>;

// Strict sanitation preventing open redirect attacks
export const sanitizeRedirectPath = (path: unknown): string | null => {
  if (typeof path !== 'string') return null;
  const trimmed = path.trim();
  if (
    trimmed.startsWith('/') &&
    !trimmed.startsWith('//') &&
    !trimmed.includes(':') &&
    !trimmed.includes('\\')
  ) {
    return trimmed;
  }
  return null;
};

// Demo credentials helper strictly gated to DEV mode with explicit flag
const isDemoEnabled = import.meta.env.DEV && import.meta.env.VITE_ENABLE_DEMO_CREDENTIALS === 'true';

const DEMO_CREDENTIALS = isDemoEnabled
  ? {
      admin: {
        email: 'admin@virtualclassroom.edu',
        password: import.meta.env.VITE_DEMO_ADMIN_PASSWORD || 'Admin@2025!',
      },
      faculty: {
        email: 'dr.jeevan@virtualclassroom.edu',
        password: import.meta.env.VITE_DEMO_FACULTY_PASSWORD || 'Faculty@2025!',
      },
      student: {
        email: 'gaurav.shukla@virtualclassroom.edu',
        password: import.meta.env.VITE_DEMO_STUDENT_PASSWORD || 'Student@2025!',
      },
    }
  : null;

export const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [authError, setAuthError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  });

  const onSubmit = async (data: LoginFormValues) => {
    setAuthError(null);
    setSubmitting(true);
    try {
      const user = await login(data.email, data.password);
      const rawFrom = (location.state as { from?: { pathname: string } })?.from?.pathname;
      const safeFrom = sanitizeRedirectPath(rawFrom);
      if (safeFrom) {
        navigate(safeFrom, { replace: true });
        return;
      }

      switch (user.role) {
        case 'ROLE_ADMIN':
          navigate('/admin', { replace: true });
          break;
        case 'ROLE_FACULTY':
          navigate('/faculty', { replace: true });
          break;
        default:
          navigate('/student', { replace: true });
          break;
      }
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } }; message?: string };
      setAuthError(
        error.response?.data?.message ||
          error.message ||
          'Authentication failed. Please verify your email and password.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleDemoFill = (email: string, pass: string) => {
    setValue('email', email, { shouldValidate: true });
    setValue('password', pass, { shouldValidate: true });
    setAuthError(null);
  };

  return (
    <div className="min-h-[calc(100vh-8rem)] flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-md space-y-6">
        <div className="text-center space-y-2">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-primary-600 to-indigo-600 text-white flex items-center justify-center mx-auto shadow-md shadow-primary-500/20">
            <GraduationCap className="w-7 h-7" />
          </div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">
            Sign In to Virtual Classroom
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400">
            Enter your credentials to access your academic dashboard
          </p>
        </div>

        {/* Demo Accounts Bar - Strictly rendered in DEV environment when explicitly enabled */}
        {isDemoEnabled && DEMO_CREDENTIALS && (
          <div className="p-3 bg-slate-50 dark:bg-slate-800/80 rounded-xl border border-slate-200 dark:border-slate-700/80 space-y-2">
            <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-700 dark:text-slate-300">
              <ShieldCheck className="w-3.5 h-3.5 text-primary-600 dark:text-primary-400" />
              <span>Quick-Fill Demo Credentials (Dev Only):</span>
            </div>
            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                onClick={() => handleDemoFill(DEMO_CREDENTIALS.admin.email, DEMO_CREDENTIALS.admin.password)}
                className="px-2 py-1.5 text-xs font-medium rounded-lg bg-white dark:bg-slate-700 border border-slate-200 dark:border-slate-600 hover:border-primary-500 text-slate-700 dark:text-slate-200 transition-colors"
              >
                Admin
              </button>
              <button
                type="button"
                onClick={() => handleDemoFill(DEMO_CREDENTIALS.faculty.email, DEMO_CREDENTIALS.faculty.password)}
                className="px-2 py-1.5 text-xs font-medium rounded-lg bg-white dark:bg-slate-700 border border-slate-200 dark:border-slate-600 hover:border-primary-500 text-slate-700 dark:text-slate-200 transition-colors"
              >
                Faculty
              </button>
              <button
                type="button"
                onClick={() => handleDemoFill(DEMO_CREDENTIALS.student.email, DEMO_CREDENTIALS.student.password)}
                className="px-2 py-1.5 text-xs font-medium rounded-lg bg-white dark:bg-slate-700 border border-slate-200 dark:border-slate-600 hover:border-primary-500 text-slate-700 dark:text-slate-200 transition-colors"
              >
                Student
              </button>
            </div>
          </div>
        )}

        {/* Error Alert */}
        {authError && (
          <div className="p-4 rounded-xl bg-rose-50 dark:bg-rose-950/50 border border-rose-200 dark:border-rose-900/60 flex items-start gap-3 text-rose-800 dark:text-rose-200 text-sm">
            <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
            <span>{authError}</span>
          </div>
        )}

        {/* Form Card */}
        <div className="bg-white dark:bg-slate-900 p-8 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-6">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1.5">
                Academic Email
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <Mail className="w-4 h-4" />
                </div>
                <input
                  type="email"
                  {...register('email')}
                  placeholder="e.g. gaurav.shukla@virtualclassroom.edu"
                  className="w-full pl-10 pr-3.5 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm transition-colors"
                />
              </div>
              {errors.email && (
                <p className="text-xs text-rose-500 mt-1">{errors.email.message}</p>
              )}
            </div>

            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider">
                  Password
                </label>
                <Link to="/forgot-password" className="text-xs font-medium text-primary-600 hover:text-primary-700 dark:text-primary-400 hover:underline">
                  Forgot password?
                </Link>
              </div>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <Lock className="w-4 h-4" />
                </div>
                <input
                  type="password"
                  {...register('password')}
                  placeholder="••••••••••••"
                  className="w-full pl-10 pr-3.5 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm transition-colors"
                />
              </div>
              {errors.password && (
                <p className="text-xs text-rose-500 mt-1">{errors.password.message}</p>
              )}
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="w-full py-3 px-4 rounded-xl font-semibold text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 disabled:opacity-50 transition-all flex items-center justify-center gap-2 shadow-md shadow-primary-600/20"
            >
              {submitting ? (
                <LoadingSpinner size="sm" />
              ) : (
                <>
                  <span>Sign In</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          <div className="text-center space-y-2 pt-2 border-t border-slate-100 dark:border-slate-800">
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Need an academic account?
            </p>
            <div className="flex justify-center gap-4 text-xs font-semibold">
              <Link to="/register/student" className="text-primary-600 hover:text-primary-700">
                Enroll as Student
              </Link>
              <span className="text-slate-300 dark:text-slate-700">•</span>
              <Link to="/register/faculty" className="text-primary-600 hover:text-primary-700">
                Register as Faculty
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

