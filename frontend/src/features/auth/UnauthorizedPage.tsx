import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ShieldAlert, ArrowLeft, Home } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/shared/components/Button';

export const UnauthorizedPage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const getRoleDashboard = () => {
    if (!user) return '/login';
    switch (user.role) {
      case 'ROLE_ADMIN':
        return '/admin';
      case 'ROLE_FACULTY':
        return '/faculty';
      default:
        return '/student';
    }
  };

  return (
    <div className="min-h-[75vh] flex items-center justify-center px-4 py-12">
      <div className="max-w-md w-full text-center bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-8 shadow-xl">
        <div className="w-16 h-16 rounded-2xl bg-rose-100 dark:bg-rose-950/50 text-rose-600 dark:text-rose-400 flex items-center justify-center mx-auto mb-5 shadow-inner">
          <ShieldAlert className="w-8 h-8" />
        </div>

        <h1 className="text-2xl font-bold text-slate-900 dark:text-white tracking-tight">Access Restricted</h1>
        <p className="mt-2 text-sm text-slate-600 dark:text-slate-400 leading-relaxed">
          You do not possess the required academic security permissions to access this specific portal or operation.
        </p>

        {user && (
          <div className="mt-4 p-3 bg-slate-50 dark:bg-slate-800/60 rounded-lg text-xs text-slate-600 dark:text-slate-300">
            Signed in as: <span className="font-semibold text-slate-800 dark:text-slate-100">{user.email}</span>
            <div className="mt-1">
              Active Role: <span className="font-mono uppercase text-indigo-600 dark:text-indigo-400 font-bold">{user.role}</span>
            </div>
          </div>
        )}

        <div className="mt-6 flex flex-col sm:flex-row gap-3 justify-center">
          <Button variant="outline" size="sm" onClick={() => navigate(-1)}>
            <ArrowLeft className="w-4 h-4 mr-1.5" />
            Go Back
          </Button>
          <Link to={getRoleDashboard()}>
            <Button variant="primary" size="sm" className="w-full sm:w-auto">
              <Home className="w-4 h-4 mr-1.5" />
              Return to My Portal
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
};
