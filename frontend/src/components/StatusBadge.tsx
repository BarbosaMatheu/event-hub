import type { OrderStatus } from '../api/types';

const STYLES: Record<OrderStatus, string> = {
  PENDING: 'bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300',
  COMPLETED: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300',
  CANCELLED: 'bg-slate-200 text-slate-700 dark:bg-slate-800 dark:text-slate-300',
};

const LABELS: Record<OrderStatus, string> = {
  PENDING: 'Awaiting payment',
  COMPLETED: 'Confirmed',
  CANCELLED: 'Cancelled',
};

export function StatusBadge({ status }: { status: OrderStatus }) {
  return <span className={`chip ${STYLES[status]}`}>{LABELS[status]}</span>;
}
