import React from 'react';
import { GraduationCap, ShieldCheck } from 'lucide-react';
import { Link } from 'react-router-dom';

export const Footer: React.FC = () => {
  return (
    <footer className="bg-white dark:bg-slate-900 border-t border-slate-200 dark:border-slate-800 transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-lg bg-primary-600 flex items-center justify-center text-white">
                <GraduationCap className="w-5 h-5" />
              </div>
              <span className="font-bold text-slate-900 dark:text-white text-base">
                Virtual<span className="text-primary-600">Classroom</span>
              </span>
            </div>
            <p className="text-sm text-slate-500 dark:text-slate-400 leading-relaxed">
              An enterprise-grade Virtual Classroom & Learning Management Platform built with modern Spring Boot and React.
            </p>
          </div>

          <div>
            <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-900 dark:text-white mb-3">
              Platform Features
            </h4>
            <ul className="space-y-2 text-sm text-slate-600 dark:text-slate-400">
              <li><Link to="/materials" className="hover:text-primary-600">Curated Materials</Link></li>
              <li><Link to="/lectures" className="hover:text-primary-600">Video Lectures</Link></li>
              <li><Link to="/forum" className="hover:text-primary-600">Academic Q&A Forum</Link></li>
              <li><Link to="/about" className="hover:text-primary-600">Architecture Overview</Link></li>
            </ul>
          </div>

          <div>
            <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-900 dark:text-white mb-3">
              Access Portals
            </h4>
            <ul className="space-y-2 text-sm text-slate-600 dark:text-slate-400">
              <li><Link to="/login" className="hover:text-primary-600">Secure Sign In</Link></li>
              <li><Link to="/register/student" className="hover:text-primary-600">Student Enrollment</Link></li>
              <li><Link to="/register/faculty" className="hover:text-primary-600">Faculty Registration</Link></li>
            </ul>
          </div>

          <div>
            <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-900 dark:text-white mb-3">
              Security & Reliability
            </h4>
            <div className="flex items-start gap-2 text-xs text-slate-500 dark:text-slate-400">
              <ShieldCheck className="w-5 h-5 text-emerald-500 shrink-0 mt-0.5" />
              <span>
                Protected with stateless JWT tokens, BCrypt salted hashing, and role-based access control.
              </span>
            </div>
          </div>
        </div>

        <div className="pt-8 border-t border-slate-100 dark:border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-slate-500 dark:text-slate-400">
          <p>© {new Date().getFullYear()} Virtual Classroom Platform. Open-source educational platform.</p>
          <p className="flex items-center gap-1">
            Built for modern academic institutions.
          </p>
        </div>
      </div>
    </footer>
  );
};

