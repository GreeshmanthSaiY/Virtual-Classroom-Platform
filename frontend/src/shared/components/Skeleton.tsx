import React from 'react';

export interface SkeletonProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'rectangular' | 'circular' | 'text';
  width?: string | number;
  height?: string | number;
}

export const Skeleton: React.FC<SkeletonProps> = ({
  variant = 'rectangular',
  width,
  height,
  className = '',
  style,
  ...props
}) => {
  const getVariantClasses = () => {
    switch (variant) {
      case 'circular':
        return 'rounded-full';
      case 'text':
        return 'rounded h-4 my-1';
      case 'rectangular':
      default:
        return 'rounded-lg';
    }
  };

  return (
    <div
      className={`animate-pulse bg-slate-200 dark:bg-slate-800 ${getVariantClasses()} ${className}`}
      style={{
        width,
        height,
        ...style,
      }}
      aria-hidden="true"
      {...props}
    />
  );
};

export const CardSkeleton: React.FC = () => {
  return (
    <div className="p-6 border border-slate-200 dark:border-slate-800 rounded-xl bg-white dark:bg-slate-900 space-y-4">
      <Skeleton height={24} width="40%" />
      <Skeleton height={16} width="80%" />
      <Skeleton height={16} width="60%" />
      <div className="pt-4 flex justify-between items-center">
        <Skeleton height={32} width={80} />
        <Skeleton height={32} width={100} />
      </div>
    </div>
  );
};

export const TableSkeleton: React.FC<{ rows?: number }> = ({ rows = 5 }) => {
  return (
    <div className="w-full space-y-3 p-4 border border-slate-200 dark:border-slate-800 rounded-xl bg-white dark:bg-slate-900">
      <Skeleton height={36} className="w-full" />
      {Array.from({ length: rows }).map((_, i) => (
        <Skeleton key={i} height={48} className="w-full" />
      ))}
    </div>
  );
};
