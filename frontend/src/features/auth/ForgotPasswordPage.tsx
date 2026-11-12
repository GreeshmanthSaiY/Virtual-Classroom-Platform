import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Mail, ArrowLeft, CheckCircle2 } from 'lucide-react';
import { authService } from '@/core/services/authService';
import { Input } from '@/shared/components/Input';
import { Button } from '@/shared/components/Button';

export const ForgotPasswordPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsLoading(true);

    try {
      await authService.forgotPassword({ email: email.trim() });
      setSubmitted(true);
    } catch {
      // Regardless of failure (e.g. account not found), show neutral confirmation to prevent enumeration
      setSubmitted(true);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4 py-12">
      <div className="max-w-md w-full bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-8 shadow-xl">
        <div className="text-center mb-6">
          <div className="w-12 h-12 rounded-xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 flex items-center justify-center mx-auto mb-3">
            <Mail className="w-6 h-6" />
          </div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white tracking-tight">Forgot Password</h1>
          <p className="mt-1.5 text-xs text-slate-500 dark:text-slate-400">
            Enter your registered academic email address to receive password reset instructions.
          </p>
        </div>

        {submitted ? (
          <div className="space-y-4">
            <div className="p-4 rounded-xl bg-emerald-50 dark:bg-emerald-950/30 border border-emerald-200 dark:border-emerald-800 text-emerald-800 dark:text-emerald-200 text-sm flex items-start gap-3">
              <CheckCircle2 className="w-5 h-5 text-emerald-600 dark:text-emerald-400 flex-shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold">Reset Instructions Dispatched</p>
                <p className="mt-1 text-xs text-emerald-700 dark:text-emerald-300">
                  If an active academic account matches that email address, password reset instructions and a verification token have been sent.
                </p>
              </div>
            </div>
            <div className="pt-2 text-center">
              <Link to="/login" className="inline-flex items-center text-sm font-medium text-indigo-600 dark:text-indigo-400 hover:underline">
                <ArrowLeft className="w-4 h-4 mr-1.5" />
                Return to Sign In
              </Link>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            {error && (
              <div className="p-3 rounded-lg bg-rose-50 dark:bg-rose-950/30 border border-rose-200 dark:border-rose-800 text-xs text-rose-700 dark:text-rose-300">
                {error}
              </div>
            )}

            <Input
              label="Academic Email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="e.g. yourname@university.edu"
              required
              icon={<Mail className="w-4 h-4" />}
            />

            <Button type="submit" variant="primary" size="md" className="w-full" isLoading={isLoading}>
              Send Reset Instructions
            </Button>

            <div className="pt-2 text-center">
              <Link to="/login" className="inline-flex items-center text-xs font-medium text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200 transition-colors">
                <ArrowLeft className="w-3.5 h-3.5 mr-1" />
                Back to Sign In
              </Link>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};
