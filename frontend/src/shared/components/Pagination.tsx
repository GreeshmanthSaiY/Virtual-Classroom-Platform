import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from './Button';

export interface PaginationProps {
  currentPage: number;
  totalPages: number;
  totalItems?: number;
  pageSize?: number;
  onPageChange: (page: number) => void;
  className?: string;
}

export const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  totalItems,
  pageSize,
  onPageChange,
  className = '',
}) => {
  if (totalPages <= 1) return null;

  const startItem = totalItems !== undefined && pageSize !== undefined ? currentPage * pageSize + 1 : undefined;
  const endItem =
    totalItems !== undefined && pageSize !== undefined
      ? Math.min((currentPage + 1) * pageSize, totalItems)
      : undefined;

  return (
    <div className={`flex flex-col sm:flex-row items-center justify-between gap-4 py-3 ${className}`}>
      {totalItems !== undefined && (
        <p className="text-xs text-slate-500 dark:text-slate-400">
          Showing <span className="font-medium text-slate-700 dark:text-slate-200">{startItem}</span> to{' '}
          <span className="font-medium text-slate-700 dark:text-slate-200">{endItem}</span> of{' '}
          <span className="font-medium text-slate-700 dark:text-slate-200">{totalItems}</span> entries
        </p>
      )}

      <div className="flex items-center space-x-1.5 ml-auto">
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(Math.max(0, currentPage - 1))}
          disabled={currentPage === 0}
          aria-label="Previous Page"
        >
          <ChevronLeft className="w-4 h-4 mr-1" />
          Previous
        </Button>

        <span className="text-xs font-medium px-3 py-1.5 rounded-md bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300">
          Page {currentPage + 1} of {totalPages}
        </span>

        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(Math.min(totalPages - 1, currentPage + 1))}
          disabled={currentPage >= totalPages - 1}
          aria-label="Next Page"
        >
          Next
          <ChevronRight className="w-4 h-4 ml-1" />
        </Button>
      </div>
    </div>
  );
};
