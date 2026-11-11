import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  LayoutDashboard,
  BookOpen,
  Video,
  MessageSquare,
  Users,
  CheckCircle2,
  Bell,
  Settings,
  ShieldCheck,
} from 'lucide-react';

interface SidebarProps {
  className?: string;
}

export const Sidebar: React.FC<SidebarProps> = ({ className = '' }) => {
  const { user } = useAuth();
  if (!user) return null;

  const role = user.role;

  const getLinks = () => {
    if (role === 'ROLE_ADMIN') {
      return [
        { to: '/admin', label: 'Platform Overview', icon: LayoutDashboard },
        { to: '/admin/approvals', label: 'Pending Approvals', icon: CheckCircle2 },
        { to: '/admin/users', label: 'User Directory', icon: Users },
        { to: '/materials', label: 'Course Materials', icon: BookOpen },
        { to: '/lectures', label: 'Video Lectures', icon: Video },
        { to: '/forum', label: 'Q&A Forum', icon: MessageSquare },
        { to: '/admin/audit', label: 'Security & Audit Logs', icon: ShieldCheck },
      ];
    }
    if (role === 'ROLE_FACULTY') {
      return [
        { to: '/faculty', label: 'Faculty Dashboard', icon: LayoutDashboard },
        { to: '/materials', label: 'Course Materials', icon: BookOpen },
        { to: '/lectures', label: 'Manage Lectures', icon: Video },
        { to: '/forum', label: 'Q&A Forum', icon: MessageSquare },
        { to: '/faculty/assignments', label: 'Subject Assignments', icon: Settings },
      ];
    }
    // Student
    return [
      { to: '/student', label: 'Student Dashboard', icon: LayoutDashboard },
      { to: '/materials', label: 'Study Materials', icon: BookOpen },
      { to: '/lectures', label: 'Video Lectures', icon: Video },
      { to: '/forum', label: 'Q&A Forum', icon: MessageSquare },
      { to: '/student/notifications', label: 'Notifications', icon: Bell },
    ];
  };

  const links = getLinks();

  return (
    <aside
      className={`w-64 bg-white dark:bg-slate-900 border-r border-slate-200 dark:border-slate-800 flex flex-col justify-between p-4 transition-colors ${className}`}
    >
      <div className="space-y-1">
        <p className="px-3 text-xs font-semibold uppercase tracking-wider text-slate-400 dark:text-slate-500 mb-3">
          Academic Portal
        </p>
        {links.map((link) => {
          const Icon = link.icon;
          return (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.to === '/admin' || link.to === '/faculty' || link.to === '/student'}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all ${
                  isActive
                    ? 'bg-primary-50 text-primary-700 dark:bg-primary-950/60 dark:text-primary-300 shadow-sm'
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

      <div className="pt-4 border-t border-slate-100 dark:border-slate-800">
        <div className="p-3 bg-slate-50 dark:bg-slate-800/60 rounded-xl">
          <p className="text-xs font-semibold text-slate-900 dark:text-white truncate">
            {user.fullName}
          </p>
          <p className="text-xs text-slate-500 dark:text-slate-400 truncate mt-0.5">
            {user.email}
          </p>
        </div>
      </div>
    </aside>
  );
};
