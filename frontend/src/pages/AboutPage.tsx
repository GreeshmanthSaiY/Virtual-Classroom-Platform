import React from 'react';
import {
  ShieldCheck,
  CheckCircle,
  Database,
  Layers,
  Code2,
  Workflow,
  Server,
} from 'lucide-react';

export const AboutPage: React.FC = () => {
  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-12 space-y-12">
      <div className="text-center space-y-4">
        <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-primary-50 dark:bg-primary-950/60 border border-primary-200 dark:border-primary-800 text-xs font-semibold text-primary-700 dark:text-primary-300">
          <Workflow className="w-4 h-4" />
          Technical & Architectural Overview
        </div>
        <h1 className="text-3xl sm:text-5xl font-extrabold text-slate-900 dark:text-white tracking-tight">
          About the Virtual Classroom Platform
        </h1>
        <p className="text-slate-600 dark:text-slate-400 max-w-2xl mx-auto text-base sm:text-lg">
          An enterprise-grade academic platform re-engineered from the ground up to provide seamless collaboration,
          scalable infrastructure, and ironclad security.
        </p>
      </div>

      {/* Modernization Journey */}
      <section className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-8 space-y-6 shadow-sm">
        <h2 className="text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
          <Layers className="w-6 h-6 text-primary-600" />
          The Modernization Architecture
        </h2>
        <p className="text-sm text-slate-600 dark:text-slate-400 leading-relaxed">
          The legacy Virtual Classroom platform originally relied on monolithic, raw Java Server Pages (JSP)
          executing database queries directly within scriptlets, unhashed plaintext passwords, and tightly coupled presentation logic.
          This complete rebuild replaces the entire application with a verified, decoupled architecture designed for high availability and maintainability.
        </p>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
          <div className="p-4 rounded-xl bg-slate-50 dark:bg-slate-800/60 border border-slate-100 dark:border-slate-800 space-y-1">
            <span className="text-xs font-bold uppercase tracking-wider text-rose-600 dark:text-rose-400">Legacy Architecture (1.5 / 10)</span>
            <p className="text-xs text-slate-600 dark:text-slate-400">
              Raw JSP scriptlets, SQL string concatenation, no connection pooling, plain-text credentials, unvalidated uploads.
            </p>
          </div>
          <div className="p-4 rounded-xl bg-slate-50 dark:bg-slate-800/60 border border-slate-100 dark:border-slate-800 space-y-1">
            <span className="text-xs font-bold uppercase tracking-wider text-emerald-600 dark:text-emerald-400">Target Rebuild (9.5 / 10)</span>
            <p className="text-xs text-slate-600 dark:text-slate-400">
              Spring Boot 3.4 REST APIs, React 18 + Vite SPA, Flyway migrations, BCrypt salted hashing, stateless JWT, 100% type-safe.
            </p>
          </div>
        </div>
      </section>

      {/* Technology Stack Grid */}
      <section className="space-y-6">
        <h2 className="text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
          <Code2 className="w-6 h-6 text-indigo-600" />
          Verified Technology Stack
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="p-6 bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 space-y-3">
            <div className="w-10 h-10 rounded-xl bg-sky-50 dark:bg-sky-950/60 text-sky-600 dark:text-sky-400 flex items-center justify-center">
              <Code2 className="w-5 h-5" />
            </div>
            <h3 className="font-bold text-slate-900 dark:text-white">Frontend Application</h3>
            <ul className="text-xs text-slate-600 dark:text-slate-400 space-y-1.5">
              <li>• React 18 + TypeScript 5.6</li>
              <li>• Vite 6 build engine</li>
              <li>• Tailwind CSS 3.4 design system</li>
              <li>• React Router 6 declarative routes</li>
              <li>• TanStack Query v5 state sync</li>
            </ul>
          </div>

          <div className="p-6 bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 space-y-3">
            <div className="w-10 h-10 rounded-xl bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 flex items-center justify-center">
              <Server className="w-5 h-5" />
            </div>
            <h3 className="font-bold text-slate-900 dark:text-white">Backend Micro-Monolith</h3>
            <ul className="text-xs text-slate-600 dark:text-slate-400 space-y-1.5">
              <li>• Java 21 LTS + Spring Boot 3.4.3</li>
              <li>• Spring Security 6 stateless filter</li>
              <li>• Spring Data JPA + Hibernate</li>
              <li>• HMAC-SHA256 JWT tokens</li>
              <li>• Springdoc OpenAPI 2.8.5</li>
            </ul>
          </div>

          <div className="p-6 bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 space-y-3">
            <div className="w-10 h-10 rounded-xl bg-purple-50 dark:bg-purple-950/60 text-purple-600 dark:text-purple-400 flex items-center justify-center">
              <Database className="w-5 h-5" />
            </div>
            <h3 className="font-bold text-slate-900 dark:text-white">Database & Ops</h3>
            <ul className="text-xs text-slate-600 dark:text-slate-400 space-y-1.5">
              <li>• Supabase PostgreSQL 15+</li>
              <li>• Versioned Flyway migrations</li>
              <li>• Multi-stage Docker containers</li>
              <li>• GitHub Actions CI/CD</li>
              <li>• Vercel (UI) + Render (API)</li>
            </ul>
          </div>
        </div>
      </section>

      {/* Security Principles */}
      <section className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-8 space-y-4">
        <h2 className="text-xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
          <ShieldCheck className="w-6 h-6 text-emerald-600" />
          Zero-Trust Security Model
        </h2>
        <div className="space-y-2 text-sm text-slate-600 dark:text-slate-400">
          <p className="flex items-start gap-2">
            <CheckCircle className="w-4 h-4 text-emerald-500 shrink-0 mt-0.5" />
            <span><strong>Mandatory Administrator Approval:</strong> All new student and faculty registrations start in a PENDING status and cannot authenticate until explicitly reviewed.</span>
          </p>
          <p className="flex items-start gap-2">
            <CheckCircle className="w-4 h-4 text-emerald-500 shrink-0 mt-0.5" />
            <span><strong>Strict Salted Password Encryption:</strong> Passwords are hashed with BCrypt (12 rounds) with salted entropy, preventing rainbow table attacks.</span>
          </p>
          <p className="flex items-start gap-2">
            <CheckCircle className="w-4 h-4 text-emerald-500 shrink-0 mt-0.5" />
            <span><strong>Comprehensive Audit Logging:</strong> Critical operations (logins, role approvals, content deletions) are persisted to an immutable audit trail table.</span>
          </p>
        </div>
      </section>
    </div>
  );
};
