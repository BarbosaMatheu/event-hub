import { availabilityOf, soldPercent } from '../utils/format';

interface AvailabilityBarProps {
  sold: number;
  capacity: number;
  /** Hide the numbers under the bar (used in dense tables). */
  compact?: boolean;
}

export function AvailabilityBar({ sold, capacity, compact = false }: AvailabilityBarProps) {
  const percent = soldPercent(sold, capacity);
  const status = availabilityOf(capacity - sold, capacity);
  const color =
    status === 'sold-out' ? 'bg-rose-500' : status === 'almost-sold-out' ? 'bg-amber-500' : 'bg-indigo-500';

  return (
    <div>
      <div
        role="progressbar"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={percent}
        aria-label="Tickets sold"
        className="h-1.5 w-full overflow-hidden rounded-full bg-slate-200 dark:bg-slate-800"
      >
        <div className={`h-full rounded-full transition-all ${color}`} style={{ width: `${percent}%` }} />
      </div>
      {!compact && (
        <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
          {sold} / {capacity} sold
        </p>
      )}
    </div>
  );
}
