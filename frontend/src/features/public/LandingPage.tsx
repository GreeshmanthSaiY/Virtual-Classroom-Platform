import React from 'react';
import { Link } from 'react-router-dom';
import {
  GraduationCap,
  BookOpen,
  Video,
  MessageSquare,
  ShieldCheck,
  CheckCircle2,
  ArrowRight,
  Database,
  Lock,
} from 'lucide-react';

export const LandingPage: React.FC = () => {
  return (
    <div className="space-y-24 py-12 md:py-20">
      {/* Hero Section */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center space-y-8">
        <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-primary-50 dark:bg-primary-950/60 border border-primary-200 dark:border-primary-800 text-xs font-semibold text-primary-700 dark:text-primary-300 animate-fade-in">
          <ShieldCheck className="w-4 h-4" />
          Enterprise-Grade Academic Management Architecture
        </div>

        <h1 className="text-4xl sm:text-6xl font-extrabold text-slate-900 dark:text-white tracking-tight leading-tight max-w-4xl mx-auto">
          Modern Learning Experience,{' '}
          <span className="bg-gradient-to-r from-primary-600 to-indigo-600 bg-clip-text text-transparent">
            Uncompromised Engineering.
          </span>
        </h1>

        <p className="text-lg sm:text-xl text-slate-600 dark:text-slate-300 max-w-2xl mx-auto leading-relaxed">
          A full-stack Virtual Classroom platform connecting students, verified faculty, and administrators
          with structured coursework, video lectures, and real-time academic discussion forums.
        </p>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4">
          <Link
            to="/register/student"
            className="w-full sm:w-auto px-8 py-3.5 rounded-xl font-semibold text-white bg-primary-600 hover:bg-primary-700 shadow-lg shadow-primary-600/25 transition-all hover:scale-105 flex items-center justify-center gap-2"
          >
            <span>Enroll as Student</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
          <Link
            to="/login"
            className="w-full sm:w-auto px-8 py-3.5 rounded-xl font-semibold text-slate-700 dark:text-slate-200 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-700/50 shadow-sm transition-all"
          >
            Access Portal
          </Link>
        </div>

        {/* Feature metrics */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-6 pt-12 max-w-4xl mx-auto border-t border-slate-200 dark:border-slate-800">
          <div>
            <p className="text-3xl font-bold text-primary-600 dark:text-primary-400">100%</p>
            <p className="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wider mt-1">Verified Users</p>
          </div>
          <div>
            <p className="text-3xl font-bold text-primary-600 dark:text-primary-400">10+</p>
            <p className="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wider mt-1">Curriculum Subjects</p>
          </div>
          <div>
            <p className="text-3xl font-bold text-primary-600 dark:text-primary-400">24/7</p>
            <p className="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wider mt-1">Academic Q&A</p>
          </div>
          <div>
            <p className="text-3xl font-bold text-primary-600 dark:text-primary-400">Zero</p>
            <p className="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wider mt-1">SQL Injection Risk</p>
          </div>
        </div>
      </section>

      {/* Feature Pillars */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-12">
        <div className="text-center space-y-3">
          <h2 className="text-3xl font-bold text-slate-900 dark:text-white">
            Designed for Academic Excellence
          </h2>
          <p className="text-slate-500 dark:text-slate-400 max-w-xl mx-auto">
            Comprehensive tools supporting students throughout their degree programs and giving faculty robust teaching resources.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {/* Card 1 */}
          <div className="p-8 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm hover:shadow-md transition-shadow space-y-4">
            <div className="w-12 h-12 rounded-xl bg-primary-100 dark:bg-primary-950/80 text-primary-600 dark:text-primary-400 flex items-center justify-center">
              <BookOpen className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-slate-900 dark:text-white">Curated Course Materials</h3>
            <p className="text-sm text-slate-600 dark:text-slate-400 leading-relaxed">
              Access lecture notes, assignments, syllabus documentation, and reference materials organized cleanly by degree, course, and academic year.
            </p>
            <Link to="/materials" className="inline-flex items-center gap-1.5 text-sm font-semibold text-primary-600 hover:text-primary-700">
              Browse materials <ArrowRight className="w-4 h-4" />
            </Link>
          </div>

          {/* Card 2 */}
          <div className="p-8 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm hover:shadow-md transition-shadow space-y-4">
            <div className="w-12 h-12 rounded-xl bg-indigo-100 dark:bg-indigo-950/80 text-indigo-600 dark:text-indigo-400 flex items-center justify-center">
              <Video className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-slate-900 dark:text-white">On-Demand Video Lectures</h3>
            <p className="text-sm text-slate-600 dark:text-slate-400 leading-relaxed">
              Watch recorded classroom lectures published directly by faculty members. Learn at your own pace with responsive playback on any device.
            </p>
            <Link to="/lectures" className="inline-flex items-center gap-1.5 text-sm font-semibold text-indigo-600 hover:text-indigo-700">
              Watch lectures <ArrowRight className="w-4 h-4" />
            </Link>
          </div>

          {/* Card 3 */}
          <div className="p-8 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm hover:shadow-md transition-shadow space-y-4">
            <div className="w-12 h-12 rounded-xl bg-emerald-100 dark:bg-emerald-950/80 text-emerald-600 dark:text-emerald-400 flex items-center justify-center">
              <MessageSquare className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-slate-900 dark:text-white">Subject-Linked Q&A Forum</h3>
            <p className="text-sm text-slate-600 dark:text-slate-400 leading-relaxed">
              Ask questions tied to specific subjects. Receive answers from peers and verified faculty with official answer acceptances.
            </p>
            <Link to="/forum" className="inline-flex items-center gap-1.5 text-sm font-semibold text-emerald-600 hover:text-emerald-700">
              Join discussion <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>
      </section>

      {/* Engineering Architecture Section */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="bg-gradient-to-br from-slate-900 to-slate-800 rounded-3xl p-8 sm:p-12 text-white shadow-xl space-y-8">
          <div className="space-y-3">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-slate-800 border border-slate-700 text-xs font-medium text-primary-300">
              <ShieldCheck className="w-3.5 h-3.5" /> Security & Architecture
            </div>
            <h2 className="text-2xl sm:text-3xl font-bold">
              Engineered with Enterprise Standards
            </h2>
            <p className="text-slate-300 text-sm sm:text-base max-w-2xl leading-relaxed">
              Replacing legacy JSP scripts with a decoupled Spring Boot 3 + React 18 architecture, full Flyway database migrations, and strict audit logging.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 pt-4">
            <div className="p-4 rounded-xl bg-slate-800/60 border border-slate-700/60 space-y-2">
              <Lock className="w-5 h-5 text-emerald-400" />
              <h4 className="font-semibold text-sm">Stateless JWT Auth</h4>
              <p className="text-xs text-slate-400">HMAC-SHA256 signed tokens with expiration and strict CORS protection.</p>
            </div>
            <div className="p-4 rounded-xl bg-slate-800/60 border border-slate-700/60 space-y-2">
              <Database className="w-5 h-5 text-sky-400" />
              <h4 className="font-semibold text-sm">Supabase PostgreSQL</h4>
              <p className="text-xs text-slate-400">Normalized relational schema versioned with repeatable Flyway migrations.</p>
            </div>
            <div className="p-4 rounded-xl bg-slate-800/60 border border-slate-700/60 space-y-2">
              <CheckCircle2 className="w-5 h-5 text-amber-400" />
              <h4 className="font-semibold text-sm">Two-Stage Approval</h4>
              <p className="text-xs text-slate-400">Zero unauthorized logins; all faculty and students require admin clearance.</p>
            </div>
            <div className="p-4 rounded-xl bg-slate-800/60 border border-slate-700/60 space-y-2">
              <GraduationCap className="w-5 h-5 text-indigo-400" />
              <h4 className="font-semibold text-sm">Automated Auditing</h4>
              <p className="text-xs text-slate-400">Immutable audit logs record authentication, approvals, and content additions.</p>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center space-y-6">
        <h2 className="text-3xl font-bold text-slate-900 dark:text-white">
          Ready to Modernize Your Academic Experience?
        </h2>
        <p className="text-slate-500 dark:text-slate-400 max-w-xl mx-auto text-sm sm:text-base">
          Join students and faculty collaborating daily on the Virtual Classroom platform.
        </p>
        <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-2">
          <Link
            to="/register/student"
            className="w-full sm:w-auto px-6 py-3 rounded-xl font-semibold text-white bg-primary-600 hover:bg-primary-700 shadow-md transition-all"
          >
            Enroll as Student
          </Link>
          <Link
            to="/register/faculty"
            className="w-full sm:w-auto px-6 py-3 rounded-xl font-semibold text-slate-700 dark:text-slate-200 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-700/50 transition-all"
          >
            Faculty Portal Registration
          </Link>
        </div>
      </section>
    </div>
  );
};

