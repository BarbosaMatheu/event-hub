import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from 'react';

type ToastKind = 'success' | 'error' | 'info';

interface ToastItem {
  id: number;
  message: string;
  kind: ToastKind;
}

export interface ToastApi {
  push: (message: string, kind?: ToastKind) => void;
}

const ToastContext = createContext<ToastApi | null>(null);

const STYLES: Record<ToastKind, string> = {
  success: 'border-emerald-200 bg-emerald-50 text-emerald-900 dark:border-emerald-900 dark:bg-emerald-950 dark:text-emerald-100',
  error: 'border-rose-200 bg-rose-50 text-rose-900 dark:border-rose-900 dark:bg-rose-950 dark:text-rose-100',
  info: 'border-slate-200 bg-white text-slate-900 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100',
};

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([]);
  const nextId = useRef(1);

  const dismiss = useCallback((id: number) => {
    setItems((current) => current.filter((item) => item.id !== id));
  }, []);

  const push = useCallback(
    (message: string, kind: ToastKind = 'info') => {
      const id = nextId.current++;
      setItems((current) => [...current, { id, message, kind }]);
      window.setTimeout(() => dismiss(id), 5000);
    },
    [dismiss],
  );

  const api = useMemo<ToastApi>(() => ({ push }), [push]);

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div
        aria-live="polite"
        className="pointer-events-none fixed inset-x-0 bottom-4 z-50 mx-auto flex w-full max-w-sm flex-col gap-2 px-4 sm:inset-x-auto sm:right-4 sm:mx-0 sm:px-0"
      >
        {items.map((item) => (
          <div
            key={item.id}
            role={item.kind === 'error' ? 'alert' : 'status'}
            className={`pointer-events-auto flex items-start justify-between gap-3 rounded-lg border px-4 py-3 text-sm shadow-lg ${STYLES[item.kind]}`}
          >
            <span>{item.message}</span>
            <button
              type="button"
              onClick={() => dismiss(item.id)}
              aria-label="Dismiss notification"
              className="-mr-1 rounded p-0.5 opacity-60 hover:opacity-100"
            >
              ×
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastApi {
  const context = useContext(ToastContext);
  if (!context) throw new Error('useToast must be used inside <ToastProvider>');
  return context;
}
