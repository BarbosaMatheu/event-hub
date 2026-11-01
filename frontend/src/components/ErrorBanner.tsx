import { ApiError } from '../api/client';

interface ErrorBannerProps {
  error: Error;
  onRetry?: () => void;
}

export function ErrorBanner({ error, onRetry }: ErrorBannerProps) {
  const requestId = error instanceof ApiError ? error.requestId : null;
  return (
    <div
      role="alert"
      className="flex flex-col items-start gap-3 rounded-xl border border-rose-200 bg-rose-50 p-4 text-rose-900 dark:border-rose-900 dark:bg-rose-950 dark:text-rose-100 sm:flex-row sm:items-center sm:justify-between"
    >
      <div>
        <p className="font-medium">{error.message}</p>
        {requestId && <p className="mt-0.5 text-xs opacity-70">Reference: {requestId}</p>}
      </div>
      {onRetry && (
        <button type="button" onClick={onRetry} className="btn btn-secondary btn-sm">
          Try again
        </button>
      )}
    </div>
  );
}
