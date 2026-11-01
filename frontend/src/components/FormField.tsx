import type { ReactNode } from 'react';

interface FormFieldProps {
  label: string;
  htmlFor: string;
  error?: string;
  hint?: string;
  children: ReactNode;
}

/** Label + control + hint/error, wired for screen readers through the shared id convention. */
export function FormField({ label, htmlFor, error, hint, children }: FormFieldProps) {
  return (
    <div>
      <label htmlFor={htmlFor} className="label">
        {label}
      </label>
      {children}
      {hint && !error && <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{hint}</p>}
      {error && (
        <p id={`${htmlFor}-error`} role="alert" className="mt-1 text-xs font-medium text-rose-600 dark:text-rose-400">
          {error}
        </p>
      )}
    </div>
  );
}
