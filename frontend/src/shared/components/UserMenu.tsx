import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { User, LogOut, KeyRound, Sun, Moon } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { useTheme } from '@/context/ThemeContext';
import { Badge } from './Badge';
import { ChangePasswordModal } from '@/features/auth/ChangePasswordModal';
import { UserProfileModal } from '@/features/auth/UserProfileModal';

export const UserMenu: React.FC = () => {
  const { user, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();

  const [isOpen, setIsOpen] = useState(false);
  const [showProfileModal, setShowProfileModal] = useState(false);
  const [showPasswordModal, setShowPasswordModal] = useState(false);

  if (!user) return null;

  const handleLogout = async () => {
    setIsOpen(false);
    await logout();
    navigate('/login');
  };

  const getRoleVariant = (role: string) => {
    switch (role) {
      case 'ROLE_ADMIN':
        return 'danger';
      case 'ROLE_FACULTY':
        return 'primary';
      default:
        return 'success';
    }
  };

  const formatRole = (role: string) => {
    switch (role) {
      case 'ROLE_ADMIN':
        return 'Administrator';
      case 'ROLE_FACULTY':
        return 'Faculty Member';
      default:
        return 'Student';
    }
  };

  return (
    <>
      <div className="relative">
        <button
          onClick={() => setIsOpen(!isOpen)}
          className="flex items-center gap-2.5 p-1.5 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors focus:outline-none focus:ring-2 focus:ring-indigo-500"
          aria-label="User account menu"
          aria-expanded={isOpen}
        >
          <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-indigo-600 to-indigo-800 text-white flex items-center justify-center font-semibold text-xs shadow-sm">
            {user.fullName ? user.fullName.charAt(0).toUpperCase() : <User className="w-4 h-4" />}
          </div>
          <div className="hidden lg:flex flex-col text-left">
            <span className="text-xs font-semibold text-slate-800 dark:text-slate-200 truncate max-w-[120px]">
              {user.fullName}
            </span>
            <span className="text-[10px] text-slate-400 font-medium">{formatRole(user.role)}</span>
          </div>
        </button>

        {isOpen && (
          <>
            <div className="fixed inset-0 z-40" onClick={() => setIsOpen(false)} />
            <div className="absolute right-0 mt-2 w-64 rounded-2xl bg-white dark:bg-slate-900 shadow-2xl border border-slate-200 dark:border-slate-800 z-50 overflow-hidden animate-in fade-in zoom-in-95 duration-150">
              <div className="p-4 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/40">
                <p className="text-sm font-bold text-slate-900 dark:text-white truncate">{user.fullName}</p>
                <p className="text-xs text-slate-500 dark:text-slate-400 truncate">{user.email}</p>
                <div className="mt-2.5 flex items-center gap-2">
                  <Badge variant={getRoleVariant(user.role)} size="sm">
                    {formatRole(user.role)}
                  </Badge>
                  {user.status && (
                    <span className="text-[10px] font-medium text-emerald-600 dark:text-emerald-400">
                      • {user.status}
                    </span>
                  )}
                </div>
              </div>

              <div className="p-2 space-y-0.5">
                <button
                  onClick={() => {
                    setIsOpen(false);
                    setShowProfileModal(true);
                  }}
                  className="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-medium text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
                >
                  <User className="w-4 h-4 text-slate-400" />
                  My Academic Profile
                </button>

                <button
                  onClick={() => {
                    setIsOpen(false);
                    setShowPasswordModal(true);
                  }}
                  className="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-medium text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
                >
                  <KeyRound className="w-4 h-4 text-slate-400" />
                  Change Password
                </button>

                <button
                  onClick={() => {
                    toggleTheme();
                  }}
                  className="w-full flex items-center justify-between px-3 py-2 rounded-xl text-xs font-medium text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
                >
                  <div className="flex items-center gap-2.5">
                    {theme === 'dark' ? (
                      <Sun className="w-4 h-4 text-amber-500" />
                    ) : (
                      <Moon className="w-4 h-4 text-indigo-500" />
                    )}
                    <span>Theme</span>
                  </div>
                  <span className="text-[10px] uppercase font-semibold text-slate-400">{theme}</span>
                </button>
              </div>

              <div className="p-2 border-t border-slate-100 dark:border-slate-800">
                <button
                  onClick={handleLogout}
                  className="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-medium text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/40 transition-colors"
                >
                  <LogOut className="w-4 h-4" />
                  Sign Out
                </button>
              </div>
            </div>
          </>
        )}
      </div>

      <UserProfileModal isOpen={showProfileModal} onClose={() => setShowProfileModal(false)} />
      <ChangePasswordModal isOpen={showPasswordModal} onClose={() => setShowPasswordModal(false)} />
    </>
  );
};
