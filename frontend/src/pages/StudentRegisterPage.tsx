import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link } from 'react-router-dom';
import { apiClient } from '../api/client';
import { ApiResponse, User } from '../types';
import {
  GraduationCap,
  Mail,
  Lock,
  User as UserIcon,
  Hash,
  BookOpen,
  Calendar,
  AlertCircle,
  CheckCircle2,
  ArrowRight,
} from 'lucide-react';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

const studentRegisterSchema = z.object({
  fullName: z.string().min(2, 'Full name must be at least 2 characters'),
  rollNumber: z.string().min(3, 'Roll number is required (e.g. CS-2025-001)'),
  email: z.string().email('Please enter a valid academic email address'),
  password: z.string().min(8, 'Password must be at least 8 characters with numbers and letters'),
  courseId: z.coerce.number().min(1, 'Please select your degree/course program'),
  year: z.coerce.number().min(1).max(4, 'Please select your current academic year'),
});

type StudentRegisterValues = z.infer<typeof studentRegisterSchema>;

export const StudentRegisterPage: React.FC = () => {
  const [submittedUser, setSubmittedUser] = useState<User | null>(null);
  const [serverError, setServerError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<StudentRegisterValues>({
    resolver: zodResolver(studentRegisterSchema),
    defaultValues: {
      fullName: '',
      rollNumber: '',
      email: '',
      password: '',
      courseId: 1,
      year: 1,
    },
  });

  const onSubmit = async (data: StudentRegisterValues) => {
    setServerError(null);
    setSubmitting(true);
    try {
      const res = await apiClient.post<ApiResponse<User>>('/auth/register/student', data);
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
          'Failed to submit registration. Please check your details.'
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
              Enrollment Submitted!
            </h2>
            <p className="text-sm text-slate-500 dark:text-slate-400 leading-relaxed">
              Your enrollment request for roll number{' '}
              <strong className="text-slate-900 dark:text-white">{submittedUser.rollNumber}</strong> is currently in{' '}
              <span className="text-amber-600 dark:text-amber-400 font-semibold">PENDING</span> status awaiting Administrator verification.
            </p>
          </div>

          <div className="p-4 bg-slate-50 dark:bg-slate-800/60 rounded-xl text-left text-xs space-y-1.5 text-slate-600 dark:text-slate-400">
            <p>• <strong>Name:</strong> {submittedUser.fullName}</p>
            <p>• <strong>Email:</strong> {submittedUser.email}</p>
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
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-primary-600 to-indigo-600 text-white flex items-center justify-center mx-auto shadow-md shadow-primary-500/20">
            <GraduationCap className="w-7 h-7" />
          </div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">
            Student Enrollment Registration
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400">
            Create your academic account to access lectures, course materials, and discussions
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
                Full Legal Name
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <UserIcon className="w-4 h-4" />
                </div>
                <input
                  type="text"
                  {...register('fullName')}
                  placeholder="e.g. Gaurav Shukla"
                  className="w-full pl-10 pr-3.5 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm"
                />
              </div>
              {errors.fullName && <p className="text-xs text-rose-500 mt-1">{errors.fullName.message}</p>}
            </div>

            {/* Roll Number & Email in Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1.5">
                  Roll / Student ID
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                    <Hash className="w-4 h-4" />
                  </div>
                  <input
                    type="text"
                    {...register('rollNumber')}
                    placeholder="e.g. CS-2025-101"
                    className="w-full pl-10 pr-3.5 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm"
                  />
                </div>
                {errors.rollNumber && <p className="text-xs text-rose-500 mt-1">{errors.rollNumber.message}</p>}
              </div>

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
                    placeholder="student@university.edu"
                    className="w-full pl-10 pr-3.5 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm"
                  />
                </div>
                {errors.email && <p className="text-xs text-rose-500 mt-1">{errors.email.message}</p>}
              </div>
            </div>

            {/* Program & Year in Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1.5">
                  Course Program
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                    <BookOpen className="w-4 h-4" />
                  </div>
                  <select
                    {...register('courseId')}
                    className="w-full pl-10 pr-3.5 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm"
                  >
                    <option value={1}>B.Tech Computer Science (CS-ENG)</option>
                    <option value={2}>B.Tech Electronics & Comm (ECE-ENG)</option>
                    <option value={3}>Master of Computer Applications (MCA-PG)</option>
                  </select>
                </div>
                {errors.courseId && <p className="text-xs text-rose-500 mt-1">{errors.courseId.message}</p>}
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1.5">
                  Current Year
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                    <Calendar className="w-4 h-4" />
                  </div>
                  <select
                    {...register('year')}
                    className="w-full pl-10 pr-3.5 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm"
                  >
                    <option value={1}>Year 1 (Freshman)</option>
                    <option value={2}>Year 2 (Sophomore)</option>
                    <option value={3}>Year 3 (Junior)</option>
                    <option value={4}>Year 4 (Senior)</option>
                  </select>
                </div>
                {errors.year && <p className="text-xs text-rose-500 mt-1">{errors.year.message}</p>}
              </div>
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
              className="w-full py-3 px-4 rounded-xl font-semibold text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 disabled:opacity-50 transition-all flex items-center justify-center gap-2 shadow-md shadow-primary-600/20"
            >
              {submitting ? (
                <LoadingSpinner size="sm" />
              ) : (
                <>
                  <span>Submit Enrollment Application</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          <p className="text-center text-xs text-slate-500 dark:text-slate-400">
            Already have an approved account?{' '}
            <Link to="/login" className="font-semibold text-primary-600 hover:text-primary-700">
              Sign In
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

