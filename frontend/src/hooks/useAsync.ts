import { useCallback, useEffect, useState } from 'react';

export interface AsyncState<T> {
  data: T | null;
  error: Error | null;
  loading: boolean;
  /** Runs the task again (e.g. after a mutation). Previous data stays visible meanwhile. */
  reload: () => void;
}

/**
 * Runs an async task on mount and whenever `deps` change. In-flight requests are aborted when
 * the deps change or the component unmounts, so a slow old response can never overwrite a newer one.
 */
export function useAsync<T>(task: (signal: AbortSignal) => Promise<T>, deps: ReadonlyArray<unknown>): AsyncState<T> {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<Error | null>(null);
  const [loading, setLoading] = useState(true);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    task(controller.signal)
      .then((result) => {
        if (controller.signal.aborted) return;
        setData(result);
        setLoading(false);
      })
      .catch((cause: unknown) => {
        if (controller.signal.aborted) return;
        setError(cause instanceof Error ? cause : new Error(String(cause)));
        setLoading(false);
      });
    return () => controller.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, version]);

  const reload = useCallback(() => setVersion((v) => v + 1), []);

  return { data, error, loading, reload };
}
