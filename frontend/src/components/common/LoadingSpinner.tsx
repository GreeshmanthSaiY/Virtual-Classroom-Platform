import React from 'react';

interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg';
  className?: string;
  label?: string;
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  size = 'md',
  className = '',
  label,
}) => {
  const sizeMap = {
    sm: 'w-4 h-4 border-2',
    md: 'w-8 h-8 border-3',
    lg: 'w-12 h-12 border-4',
  };

  return (
    <div className={`flex flex-col items-center justify-center gap-3 ${className}`}>
      <div
        className={`${sizeMap[size]} border-primary-200 border-t-primary-600 rounded-full animate-spin dark:border-slate-700 dark:border-t-primary-400`}
        role="status"
        aria-label={label || 'Loading...'}
      />
      {label && <p className="text-sm text-slate-500 dark:text-slate-400 animate-pulse">{label}</p>}
    </div>
  );
};

