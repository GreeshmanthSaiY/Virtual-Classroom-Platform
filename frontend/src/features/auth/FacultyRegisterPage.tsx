import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link } from 'react-router-dom';
import { apiClient } from '../../services/api/client';
import { ApiResponse, User } from '../../types';
import {
  GraduationCap,
  Mail,
  Lock,
  User as UserIcon,
  Briefcase,
  Building,
  AlertCircle,
  CheckCircle2,
  ArrowRight,
} from 'lucide-react';
import { LoadingSpinner } from '../../components/ui/LoadingSpinner';

const facultyRegisterSchema = z.object({
  fullName: z.string().min(2, 'Full legal name must be at least 2 characters'),
  employeeId: z.string().min(3, 'Employee ID is required (e.g. FAC-001)'),
  email: z.string().email('Please enter an institutional email address'),
  department: z.string().min(2, 'Department is required'),
  password: z.string().min(8, 'Password must be at least 8 characters with numbers and letters'),
  subjectIds: z.array(z.number()).min(1, 'Please select at least one teaching subject'),
});

type FacultyRegisterValues = z.infer<typeof facultyRegisterSchema>;

export const FacultyRegisterPage: React.FC = () => {
  const [submittedUser, setSubmittedUser] = useState<User | null>(null);
  const [serverError, setServerError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FacultyRegisterValues>({
    resolver: zodResolver(facultyRegisterSchema),
    defaultValues: {
      fullName: '',
      employeeId: '',
      email: '',
      department: 'Computer Science & Engineering',
      password: '',
      subjectIds: [1, 2],
    },
  });

  const onSubmit = async (data: FacultyRegisterValues) => {
    setServerError(null);
    setSubmitting(true);
    try {
      const res = await apiClient.post<ApiResponse<User>>('/auth/register/faculty', data);
      if (res.data.success && res.data.data) {
        setSubmittedUser(res.data.data);
      } else {
        throw new Error(res.data.message || 'Registration failed');
      }
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } }; message?: string };
      setServerError(
        error.response?.data?.message ||
          error.message ||
          'Failed to submit faculty registration. Please verify details.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  if (submittedUser) {
    return (
      <div className="min-h-[calc(100vh-8rem)] flex items-center justify-center px-4 py-12">
        <div className="w-full max-w-md bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-8 text-center space-y-6 shadow-sm">
          <div className="w-16 h-16 rounded-full bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 flex items-center justify-center mx-auto">
            <CheckCircle2 className="w-10 h-10" />
          </div>

          <div className="space-y-2">
            <h2 className="text-2xl font-bold text-slate-900 dark:text-white">
              Faculty Application Received!
            </h2>
            <p className="text-sm text-slate-500 dark:text-slate-400 leading-relaxed">
              Faculty registration for Employee ID{' '}
              <strong className="text-slate-900 dark:text-white">{submittedUser.employeeId}</strong> has been logged in{' '}
              <span className="text-amber-600 dark:text-amber-400 font-semibold">PENDING</span> status. The Academic Dean / Administrator will review your department credentials.
            </p>
          </div>

          <div className="p-4 bg-slate-50 dark:bg-slate-800/60 rounded-xl text-left text-xs space-y-1.5 text-slate-600 dark:text-slate-400">
            <p>• <strong>Name:</strong> {submittedUser.fullName}</p>
            <p>• <strong>Email:</strong> {submittedUser.email}</p>
            <p>• <strong>Department:</strong> {submittedUser.department}</p>
            <p>• <strong>Status:</strong> Awaiting administrator approval</p>
          </div>

          <div className="pt-2">
            <Link
              to="/login"
              className="inline-flex items-center justify-center gap-2 w-full py-3 px-4 rounded-xl font-semibold text-white bg-primary-600 hover:bg-primary-700 shadow-md shadow-primary-600/20 transition-all"
            >
              <span>Back to Login</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-[calc(100vh-8rem)] flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-lg space-y-6">
        <div className="text-center space-y-2">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-indigo-600 to-primary-600 text-white flex items-center justify-center mx-auto shadow-md shadow-indigo-500/20">
            <GraduationCap className="w-7 h-7" />
          </div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">
            Faculty Portal Registration
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400">
            Register as an instructor to publish lecture materials, stream videos, and answer student questions
          </p>
        </div>

        {serverError && (
          <div className="p-4 rounded-xl bg-rose-50 dark:bg-rose-950/50 border border-rose-200 dark:border-rose-900/60 flex items-start gap-3 text-rose-800 dark:text-rose-200 text-sm">
            <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
            <span>{serverError}</span>
          </div>
        )}

        <div className="bg-white dark:bg-slate-900 p-8 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-6">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            {/* Full Name */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1.5">
                Full Academic Name (with Title)
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <UserIcon className="w-4 h-4" />
                </div>
                <input
                  type="text"
                  {...register('fullName')}
                  placeholder="e.g. Dr. Jeevan Kumar"
                  className="w-full pl-10 pr-3.5 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm"
                />
              </div>
              {errors.fullName && <p className="text-xs text-rose-500 mt-1">{errors.fullName.message}</p>}
            </div>

            {/* Employee ID & Institutional Email */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1.5">
                  Employee ID
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                    <Briefcase className="w-4 h-4" />
                  </div>
                  <input
                    type="text"
                    {...register('employeeId')}
                    placeholder="e.g. FAC-2025-01"
                    className="w-full pl-10 pr-3.5 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm"
                  />
                </div>
                {errors.employeeId && <p className="text-xs text-rose-500 mt-1">{errors.employeeId.message}</p>}
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1.5">
                  Institutional Email
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                    <Mail className="w-4 h-4" />
                  </div>
                  <input
                    type="email"
                    {...register('email')}
                    placeholder="faculty@university.edu"
                    className="w-full pl-10 pr-3.5 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm"
                  />
                </div>
                {errors.email && <p className="text-xs text-rose-500 mt-1">{errors.email.message}</p>}
              </div>
            </div>

            {/* Department */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1.5">
                Academic Department
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <Building className="w-4 h-4" />
                </div>
                <input
                  type="text"
                  {...register('department')}
                  placeholder="e.g. Computer Science & Engineering"
                  className="w-full pl-10 pr-3.5 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm"
                />
              </div>
              {errors.department && <p className="text-xs text-rose-500 mt-1">{errors.department.message}</p>}
            </div>

            {/* Teaching Subject Assignment */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-2">
                Primary Teaching Subjects
              </label>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
                <label className="flex items-center gap-2 p-2.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800/60 cursor-pointer">
                  <input type="checkbox" value={1} {...register('subjectIds', { valueAsNumber: true })} className="rounded text-primary-600" defaultChecked />
                  <span className="text-slate-800 dark:text-slate-200">Data Structures & Algo</span>
                </label>
                <label className="flex items-center gap-2 p-2.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800/60 cursor-pointer">
                  <input type="checkbox" value={2} {...register('subjectIds', { valueAsNumber: true })} className="rounded text-primary-600" defaultChecked />
                  <span className="text-slate-800 dark:text-slate-200">Database Mgmt Systems</span>
                </label>
                <label className="flex items-center gap-2 p-2.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800/60 cursor-pointer">
                  <input type="checkbox" value={3} {...register('subjectIds', { valueAsNumber: true })} className="rounded text-primary-600" />
                  <span className="text-slate-800 dark:text-slate-200">Operating Systems</span>
                </label>
                <label className="flex items-center gap-2 p-2.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800/60 cursor-pointer">
                  <input type="checkbox" value={4} {...register('subjectIds', { valueAsNumber: true })} className="rounded text-primary-600" />
                  <span className="text-slate-800 dark:text-slate-200">Computer Networks</span>
                </label>
              </div>
              {errors.subjectIds && <p className="text-xs text-rose-500 mt-1">{errors.subjectIds.message}</p>}
            </div>

            {/* Password */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1.5">
                Password
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <Lock className="w-4 h-4" />
                </div>
                <input
                  type="password"
                  {...register('password')}
                  placeholder="At least 8 characters"
                  className="w-full pl-10 pr-3.5 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm"
                />
              </div>
              {errors.password && <p className="text-xs text-rose-500 mt-1">{errors.password.message}</p>}
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="w-full py-3 px-4 rounded-xl font-semibold text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 disabled:opacity-50 transition-all flex items-center justify-center gap-2 shadow-md shadow-indigo-600/20"
            >
              {submitting ? (
                <LoadingSpinner size="sm" />
              ) : (
                <>
                  <span>Submit Faculty Application</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          <p className="text-center text-xs text-slate-500 dark:text-slate-400">
            Already have an active faculty account?{' '}
            <Link to="/login" className="font-semibold text-indigo-600 hover:text-indigo-700">
              Sign In
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

