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
  Sparkles,
} from 'lucide-react';
import { NotificationCenter } from '../components/NotificationCenter';
import { UserMenu } from '../components/UserMenu';
import { Badge } from '../components/Badge';
import { ThemeToggle } from '../components/ThemeToggle';

interface NavItem {
  to: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  end?: boolean;
}

export const AdminLayout: React.FC = () => {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const coreNavLinks: NavItem[] = [
    { to: '/admin', label: 'Platform Overview', icon: LayoutDashboard, end: true },
    { to: '/admin/users', label: 'User Governance', icon: Users },
    { to: '/admin/courses', label: 'Degrees & Curriculum', icon: Layers },
  ];

  const academicNavLinks: NavItem[] = [
    { to: '/materials', label: 'Course Materials', icon: BookOpen },
    { to: '/lectures', label: 'Video Lectures', icon: Video },
    { to: '/forum', label: 'Q&A Forum', icon: MessageSquare },
  ];

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 antialiased selection:bg-primary-500 selection:text-white transition-colors duration-200">
      {/* Top Header */}
      <header className="sticky top-0 z-40 bg-white/80 dark:bg-slate-900/80 backdrop-blur-md border-b border-slate-200/80 dark:border-slate-800/80 transition-colors">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <button
              onClick={() => setSidebarOpen(!sidebarOpen)}
              className="lg:hidden p-2 rounded-xl text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-800/80 transition-colors"
              aria-label="Toggle navigation menu"
            >
              {sidebarOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
            <Link to="/admin" className="flex items-center gap-2.5 group">
              <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-rose-600 to-indigo-600 flex items-center justify-center text-white shadow-soft-md shadow-rose-500/20 group-hover:scale-105 transition-transform">
                <GraduationCap className="w-6 h-6" />
              </div>
              <div className="flex flex-col">
                <span className="font-bold text-sm sm:text-base tracking-tight text-slate-900 dark:text-white leading-tight">
                  Virtual<span className="text-primary-600 dark:text-primary-400">Classroom</span>
                </span>
                <span className="text-[10px] font-bold text-rose-600 dark:text-rose-400 uppercase tracking-wider flex items-center gap-1">
                  <ShieldCheck className="w-3 h-3" /> Admin Console
                </span>
              </div>
            </Link>
          </div>

          <div className="flex items-center gap-2 sm:gap-3">
            <Badge variant="danger" size="sm" className="hidden sm:inline-flex shadow-soft-sm font-semibold">
              INSTITUTIONAL ADMIN
            </Badge>
            <ThemeToggle />
            <NotificationCenter />
            <div className="h-6 w-px bg-slate-200 dark:bg-slate-800" />
            <UserMenu />
          </div>
        </div>
      </header>

      {/* Body with Sidebar */}
      <div className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8 flex gap-6 lg:gap-8">
        {/* Desktop Sidebar */}
        <aside className="hidden lg:block w-64 shrink-0">
          <nav className="sticky top-24 bg-white/90 dark:bg-slate-900/90 backdrop-blur-sm border border-slate-200/80 dark:border-slate-800/80 rounded-2xl p-3.5 shadow-soft-sm space-y-4">
            <div>
              <div className="px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">
                Governance & Control
              </div>
              <div className="space-y-1 mt-1">
                {coreNavLinks.map((link) => {
                  const Icon = link.icon;
                  return (
                    <NavLink
                      key={link.to}
                      to={link.to}
                      end={link.end}
                      className={({ isActive }) =>
                        `flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-all relative ${
                          isActive
                            ? 'bg-rose-50 text-rose-700 dark:bg-rose-950/50 dark:text-rose-300 font-bold shadow-soft-sm'
                            : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100/70 dark:text-slate-400 dark:hover:text-slate-100 dark:hover:bg-slate-800/60'
                        }`
                      }
                    >
                      <Icon className="w-4 h-4 shrink-0" />
                      <span>{link.label}</span>
                    </NavLink>
                  );
                })}
              </div>
            </div>

            <div>
              <div className="px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">
                Academic Commons
              </div>
              <div className="space-y-1 mt-1">
                {academicNavLinks.map((link) => {
                  const Icon = link.icon;
                  return (
                    <NavLink
                      key={link.to}
                      to={link.to}
                      end={link.end}
                      className={({ isActive }) =>
                        `flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-all ${
                          isActive
                            ? 'bg-primary-50 text-primary-700 dark:bg-primary-950/50 dark:text-primary-300 font-bold shadow-soft-sm'
                            : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100/70 dark:text-slate-400 dark:hover:text-slate-100 dark:hover:bg-slate-800/60'
                        }`
                      }
                    >
                      <Icon className="w-4 h-4 shrink-0" />
                      <span>{link.label}</span>
                    </NavLink>
                  );
                })}
              </div>
            </div>

            {/* Institutional Status Badge in Sidebar */}
            <div className="pt-2 border-t border-slate-100 dark:border-slate-800/80 px-3 py-2 text-[11px] text-slate-500 dark:text-slate-400 flex items-center justify-between">
              <span className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
                Live Term 2026
              </span>
              <Sparkles className="w-3.5 h-3.5 text-amber-500" />
            </div>
          </nav>
        </aside>

        {/* Mobile Sidebar Modal */}
        {sidebarOpen && (
          <div className="fixed inset-0 z-50 lg:hidden">
            <div
              className="fixed inset-0 bg-slate-950/60 backdrop-blur-sm transition-opacity"
              onClick={() => setSidebarOpen(false)}
            />
            <div className="fixed top-0 bottom-0 left-0 w-72 max-w-[85vw] bg-white dark:bg-slate-900 p-5 shadow-soft-xl z-50 flex flex-col justify-between overflow-y-auto border-r border-slate-200 dark:border-slate-800">
              <div className="space-y-5">
                <div className="flex items-center justify-between pb-3.5 border-b border-slate-100 dark:border-slate-800">
                  <div className="flex items-center gap-2.5">
                    <div className="w-8 h-8 rounded-lg bg-rose-600 text-white flex items-center justify-center font-bold">
                      <GraduationCap className="w-4 h-4" />
                    </div>
                    <span className="font-bold text-sm text-slate-900 dark:text-white">Admin Console</span>
                  </div>
                  <button
                    onClick={() => setSidebarOpen(false)}
                    className="p-1.5 rounded-lg text-slate-500 hover:text-slate-900 hover:bg-slate-100 dark:text-slate-400 dark:hover:text-white dark:hover:bg-slate-800 transition-colors"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>

                <div className="space-y-4">
                  <div>
                    <div className="px-2 py-1 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                      Governance
                    </div>
                    <div className="space-y-1 mt-1">
                      {coreNavLinks.map((link) => {
                        const Icon = link.icon;
                        return (
                          <NavLink
                            key={link.to}
                            to={link.to}
                            end={link.end}
                            onClick={() => setSidebarOpen(false)}
                            className={({ isActive }) =>
                              `flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-semibold transition-all ${
                                isActive
                                  ? 'bg-rose-50 text-rose-700 dark:bg-rose-950/60 dark:text-rose-300 font-bold'
                                  : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50 dark:text-slate-400 dark:hover:text-slate-200 dark:hover:bg-slate-800/60'
                              }`
                            }
                          >
                            <Icon className="w-4 h-4 shrink-0" />
                            <span>{link.label}</span>
                          </NavLink>
                        );
                      })}
                    </div>
                  </div>

                  <div>
                    <div className="px-2 py-1 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                      Academic Commons
                    </div>
                    <div className="space-y-1 mt-1">
                      {academicNavLinks.map((link) => {
                        const Icon = link.icon;
                        return (
                          <NavLink
                            key={link.to}
                            to={link.to}
                            end={link.end}
                            onClick={() => setSidebarOpen(false)}
                            className={({ isActive }) =>
                              `flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-semibold transition-all ${
                                isActive
                                  ? 'bg-primary-50 text-primary-700 dark:bg-primary-950/60 dark:text-primary-300 font-bold'
                                  : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50 dark:text-slate-400 dark:hover:text-slate-200 dark:hover:bg-slate-800/60'
                              }`
                            }
                          >
                            <Icon className="w-4 h-4 shrink-0" />
                            <span>{link.label}</span>
                          </NavLink>
                        );
                      })}
                    </div>
                  </div>
                </div>
              </div>

              <div className="pt-4 border-t border-slate-100 dark:border-slate-800 text-xs text-slate-400">
                Institutional Security Level: Active
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
