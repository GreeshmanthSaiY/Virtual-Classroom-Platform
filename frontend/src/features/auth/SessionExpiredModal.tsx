import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Clock, Lock, ArrowRight } from 'lucide-react';
import { onSessionExpired } from '@/core/api/client';
import { useAuth } from '@/context/AuthContext';
import { Modal } from '@/shared/components/Modal';
import { Input } from '@/shared/components/Input';
import { Button } from '@/shared/components/Button';

export const SessionExpiredModal: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { user, login } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const unsubscribe = onSessionExpired(() => {
      setIsOpen(true);
    });
    return unsubscribe;
  }, []);

  const handleReLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.email) {
      setIsOpen(false);
      navigate('/login');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      await login(user.email, password);
      setIsOpen(false);
      setPassword('');
    } catch (err: any) {
      setError(err?.message || 'Authentication failed. Please verify your password.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleFullLogout = () => {
    setIsOpen(false);
    navigate('/login');
  };

  if (!isOpen) return null;

  return (
    <Modal isOpen={isOpen} onClose={handleFullLogout} title="Session Expired" maxWidth="md">
      <div className="p-6 space-y-4">
        <div className="flex items-center space-x-3 p-3.5 rounded-xl bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800 text-amber-800 dark:text-amber-200 text-xs">
          <Clock className="w-5 h-5 text-amber-600 dark:text-amber-400 flex-shrink-0" />
          <p>
            Your authenticated academic session has expired due to inactivity. Enter your password to resume where you left off.
          </p>
        </div>

        {error && (
          <div className="p-3 rounded-lg bg-rose-50 dark:bg-rose-950/30 border border-rose-200 dark:border-rose-800 text-xs text-rose-700 dark:text-rose-300">
            {error}
          </div>
        )}

        {user ? (
          <form onSubmit={handleReLogin} className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-slate-500 dark:text-slate-400 mb-1">
                Account
              </label>
              <div className="text-sm font-semibold text-slate-800 dark:text-slate-200 bg-slate-50 dark:bg-slate-800 p-2.5 rounded-lg border border-slate-200 dark:border-slate-700">
                {user.email}
              </div>
            </div>

            <Input
              label="Password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your password"
              required
              autoFocus
              icon={<Lock className="w-4 h-4" />}
            />

            <div className="flex items-center justify-between pt-2">
              <Button type="button" variant="outline" size="sm" onClick={handleFullLogout}>
                Sign In As Another User
              </Button>
              <Button type="submit" variant="primary" size="sm" isLoading={isLoading}>
                Resume Session
                <ArrowRight className="w-4 h-4 ml-1.5" />
              </Button>
            </div>
          </form>
        ) : (
          <div className="text-center pt-2">
            <Button variant="primary" size="md" onClick={handleFullLogout} className="w-full">
              Proceed to Sign In
            </Button>
          </div>
        )}
      </div>
    </Modal>
  );
};
