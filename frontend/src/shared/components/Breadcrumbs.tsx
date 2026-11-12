import React from 'react';
import { Link } from 'react-router-dom';
import { ChevronRight, Home } from 'lucide-react';

export interface BreadcrumbItem {
  label: string;
  href?: string;
}

export interface BreadcrumbsProps {
  items: BreadcrumbItem[];
  className?: string;
}

export const Breadcrumbs: React.FC<BreadcrumbsProps> = ({ items, className = '' }) => {
  return (
    <nav className={`flex items-center space-x-1.5 text-xs text-slate-500 dark:text-slate-400 ${className}`} aria-label="Breadcrumb">
      <Link
        to="/"
        className="hover:text-slate-700 dark:hover:text-slate-200 transition-colors flex items-center"
      >
        <Home className="w-3.5 h-3.5" />
        <span className="sr-only">Home</span>
      </Link>
      {items.map((item, idx) => {
        const isLast = idx === items.length - 1;
        return (
          <React.Fragment key={idx}>
            <ChevronRight className="w-3.5 h-3.5 text-slate-400 dark:text-slate-600 flex-shrink-0" />
            {isLast || !item.href ? (
              <span className="font-medium text-slate-800 dark:text-slate-200 truncate max-w-xs" aria-current="page">
                {item.label}
              </span>
            ) : (
              <Link
                to={item.href}
                className="hover:text-slate-700 dark:hover:text-slate-200 transition-colors truncate max-w-xs"
              >
                {item.label}
              </Link>
            )}
          </React.Fragment>
        );
      })}
    </nav>
  );
};
