import React, { forwardRef } from 'react';

export interface TextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  helperText?: string;
  error?: string;
  showCount?: boolean;
  maxLength?: number;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ label, helperText, error, showCount, maxLength, className = '', id, disabled, value, ...props }, ref) => {
    const textareaId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);
    const currentLength = typeof value === 'string' ? value.length : 0;

    return (
      <div className="w-full">
        {label && (
          <div className="flex justify-between items-center mb-1.5">
            <label htmlFor={textareaId} className="block text-sm font-medium text-slate-700 dark:text-slate-300">
              {label}
              {props.required && <span className="text-rose-500 ml-1" aria-hidden="true">*</span>}
            </label>
            {showCount && maxLength && (
              <span className="text-xs text-slate-400 dark:text-slate-500">
                {currentLength}/{maxLength}
              </span>
            )}
          </div>
        )}
        <div className="relative rounded-lg shadow-sm">
          <textarea
            ref={ref}
            id={textareaId}
            disabled={disabled}
            maxLength={maxLength}
            value={value}
            aria-invalid={error ? 'true' : 'false'}
            aria-describedby={error ? `${textareaId}-error` : helperText ? `${textareaId}-helper` : undefined}
            className={`block w-full rounded-lg border text-sm transition-colors duration-150 focus:outline-none focus:ring-2 focus:ring-offset-0 disabled:bg-slate-100 disabled:dark:bg-slate-800 disabled:text-slate-400 disabled:cursor-not-allowed p-3 ${
              error
                ? 'border-rose-300 dark:border-rose-700 bg-rose-50/50 dark:bg-rose-950/20 text-rose-900 dark:text-rose-100 focus:border-rose-500 focus:ring-rose-500/20'
                : 'border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 focus:border-indigo-600 dark:focus:border-indigo-400 focus:ring-indigo-500/20'
            } ${className}`}
            rows={props.rows || 4}
            {...props}
          />
        </div>
        {error ? (
          <p id={`${textareaId}-error`} className="mt-1.5 text-xs text-rose-600 dark:text-rose-400" role="alert">
            {error}
          </p>
        ) : helperText ? (
          <p id={`${textareaId}-helper`} className="mt-1.5 text-xs text-slate-500 dark:text-slate-400">
            {helperText}
          </p>
        ) : null}
      </div>
    );
  }
);

Textarea.displayName = 'Textarea';
