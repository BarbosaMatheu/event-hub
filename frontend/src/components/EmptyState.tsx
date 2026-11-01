import type { ReactNode } from 'react';

interface EmptyStateProps {
  title: string;
  description?: string;
  action?: ReactNode;
}

export function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <div className="rounded-xl border border-dashed border-slate-300 px-6 py-14 text-center dark:border-slate-700">
      <h3 className="text-lg font-semibold">{title}</h3>
      {description && <p className="mx-auto mt-2 max-w-md text-sm text-slate-600 dark:text-slate-400">{description}</p>}
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}
