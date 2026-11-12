import React from 'react';

export const Table: React.FC<React.TableHTMLAttributes<HTMLTableElement>> = ({ children, className = '', ...props }) => {
  return (
    <div className="w-full overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-800">
      <table className={`w-full text-left text-sm text-slate-600 dark:text-slate-300 ${className}`} {...props}>
        {children}
      </table>
    </div>
  );
};

export const Thead: React.FC<React.HTMLAttributes<HTMLTableSectionElement>> = ({ children, className = '', ...props }) => {
  return (
    <thead
      className={`bg-slate-50 dark:bg-slate-800/60 text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400 border-b border-slate-200 dark:border-slate-800 ${className}`}
      {...props}
    >
      {children}
    </thead>
  );
};

export const Tbody: React.FC<React.HTMLAttributes<HTMLTableSectionElement>> = ({ children, className = '', ...props }) => {
  return (
    <tbody className={`divide-y divide-slate-200 dark:divide-slate-800 bg-white dark:bg-slate-900 ${className}`} {...props}>
      {children}
    </tbody>
  );
};

export const Tr: React.FC<React.HTMLAttributes<HTMLTableRowElement>> = ({ children, className = '', ...props }) => {
  return (
    <tr className={`hover:bg-slate-50/75 dark:hover:bg-slate-800/40 transition-colors ${className}`} {...props}>
      {children}
    </tr>
  );
};

export const Th: React.FC<React.ThHTMLAttributes<HTMLTableCellElement>> = ({ children, className = '', ...props }) => {
  return (
    <th className={`px-4 py-3.5 text-left font-semibold ${className}`} {...props}>
      {children}
    </th>
  );
};

export const Td: React.FC<React.TdHTMLAttributes<HTMLTableCellElement>> = ({ children, className = '', ...props }) => {
  return (
    <td className={`px-4 py-3.5 whitespace-nowrap text-slate-800 dark:text-slate-200 ${className}`} {...props}>
      {children}
    </td>
  );
};
