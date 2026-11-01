import { useEffect, useState } from 'react';

/**
 * Counts down from `initialSeconds` (as reported by the server at fetch time) to zero.
 * Returns null when there is nothing to count (initialSeconds is null).
 */
export function useCountdown(initialSeconds: number | null): number | null {
  const [remaining, setRemaining] = useState<number | null>(initialSeconds);

  useEffect(() => {
    if (initialSeconds === null) {
      setRemaining(null);
      return;
    }
    const deadline = Date.now() + initialSeconds * 1000;
    const tick = () => setRemaining(Math.max(0, Math.round((deadline - Date.now()) / 1000)));
    tick();
    const id = window.setInterval(tick, 1000);
    return () => window.clearInterval(id);
  }, [initialSeconds]);

  return remaining;
}
