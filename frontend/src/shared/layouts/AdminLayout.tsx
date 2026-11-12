import React, { useState } from 'react';
import { NavLink, Outlet, Link } from 'react-router-dom';
import {
  GraduationCap,
  LayoutDashboard,
  Users,
  BookOpen,
  Video,
  MessageSquare,
  ShieldCheck,
  Menu,
  X,
  Layers,
} from 'lucide-react';
import { NotificationCenter } from '../components/NotificationCenter';
import { UserMenu } from '../components/UserMenu';
import { Badge } from '../components/Badge';

export const AdminLayout: React.FC = () => {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const navLinks = [
    { to: '/admin', label: 'Platform Metrics', icon: LayoutDashboard, end: true },
    { to: '/admin/users', label: 'User Governance', icon: Users },
    { to: '/admin/courses', label: 'Degrees & Courses', icon: Layers },
    { to: '/materials', label: 'Course Materials', icon: BookOpen },
    { to: '/lectures', label: 'Video Lectures', icon: Video },
    { to: '/forum', label: 'Q&A Forum', icon: MessageSquare },
  ];

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100">
      {/* Top Header */}
      <header className="sticky top-0 z-40 bg-white/90 dark:bg-slate-900/90 backdrop-blur-md border-b border-slate-200 dark:border-slate-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <button
              onClick={() => setSidebarOpen(!sidebarOpen)}
              className="lg:hidden p-2 rounded-lg text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800"
              aria-label="Toggle navigation menu"
            >
              {sidebarOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
            <Link to="/admin" className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-indigo-600 to-indigo-800 flex items-center justify-center text-white shadow-md">
                <GraduationCap className="w-5 h-5" />
              </div>
              <div className="flex flex-col">
                <span className="font-bold text-sm tracking-tight text-slate-900 dark:text-white">
                  Virtual<span className="text-indigo-600 dark:text-indigo-400">Classroom</span>
                </span>
                <span className="text-[10px] font-semibold text-rose-600 dark:text-rose-400 uppercase tracking-wider flex items-center gap-1">
                  <ShieldCheck className="w-3 h-3" /> Admin Console
                </span>
              </div>
            </Link>
          </div>

          <div className="flex items-center gap-3">
            <Badge variant="danger" size="sm" className="hidden sm:inline-flex">
              ADMINISTRATOR
            </Badge>
            <NotificationCenter />
            <div className="h-6 w-px bg-slate-200 dark:bg-slate-800" />
            <UserMenu />
          </div>
        </div>
      </header>

      {/* Body with Sidebar */}
      <div className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 flex gap-6">
        {/* Desktop Sidebar */}
        <aside className="hidden lg:block w-64 flex-shrink-0">
          <nav className="sticky top-24 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-3 shadow-sm space-y-1">
            <div className="px-3 py-2 text-[11px] font-bold uppercase tracking-wider text-slate-400">
              Admin Governance
            </div>
            {navLinks.map((link) => {
              const Icon = link.icon;
              return (
                <NavLink
                  key={link.to}
                  to={link.to}
                  end={link.end}
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-all ${
                      isActive
                        ? 'bg-indigo-50 text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300 shadow-sm'
                        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50 dark:text-slate-400 dark:hover:text-slate-200 dark:hover:bg-slate-800/60'
                    }`
                  }
                >
                  <Icon className="w-4 h-4 flex-shrink-0" />
                  <span>{link.label}</span>
                </NavLink>
              );
            })}
          </nav>
        </aside>

        {/* Mobile Sidebar Modal */}
        {sidebarOpen && (
          <div className="fixed inset-0 z-50 lg:hidden">
            <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm" onClick={() => setSidebarOpen(false)} />
            <div className="fixed top-0 bottom-0 left-0 w-64 bg-white dark:bg-slate-900 p-4 shadow-xl z-50 flex flex-col justify-between">
              <div className="space-y-4">
                <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
                  <div className="flex items-center gap-2">
                    <GraduationCap className="w-5 h-5 text-indigo-600" />
                    <span className="font-bold text-sm">Admin Navigation</span>
                  </div>
                  <button onClick={() => setSidebarOpen(false)} className="p-1 rounded-lg hover:bg-slate-100">
                    <X className="w-5 h-5" />
                  </button>
                </div>
                <div className="space-y-1">
                  {navLinks.map((link) => {
                    const Icon = link.icon;
                    return (
                      <NavLink
                        key={link.to}
                        to={link.to}
                        end={link.end}
                        onClick={() => setSidebarOpen(false)}
                        className={({ isActive }) =>
                          `flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-all ${
                            isActive
                              ? 'bg-indigo-50 text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300 shadow-sm'
                              : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50 dark:text-slate-400 dark:hover:text-slate-200 dark:hover:bg-slate-800/60'
                          }`
                        }
                      >
                        <Icon className="w-4 h-4" />
                        <span>{link.label}</span>
                      </NavLink>
                    );
                  })}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Main Workspace */}
        <main className="flex-1 min-w-0">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
