import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ordersApi } from '../api/endpoints';
import type { Order } from '../api/types';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { EmptyState } from '../components/EmptyState';
import { ErrorBanner } from '../components/ErrorBanner';
import { Pagination } from '../components/Pagination';
import { Spinner } from '../components/Spinner';
import { StatusBadge } from '../components/StatusBadge';
import { useToast } from '../components/Toast';
import { useAsync } from '../hooks/useAsync';
import { useCountdown } from '../hooks/useCountdown';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { errorMessage } from '../utils/errors';
import { formatCountdown, formatDateTime, formatMoney, pluralize } from '../utils/format';

interface OrderCardProps {
  order: Order;
  busy: boolean;
  onPay: (order: Order) => void;
  onCancel: (order: Order) => void;
  onExpired: () => void;
}

function OrderCard({ order, busy, onPay, onCancel, onExpired }: OrderCardProps) {
  const remaining = useCountdown(order.status === 'PENDING' ? order.expiresInSeconds : null);
  const expired = order.status === 'PENDING' && remaining === 0;
  const tickets = order.items.reduce((sum, item) => sum + item.quantity, 0);

  // The server releases expired reservations on a schedule; refresh to pick that up.
  useEffect(() => {
    if (!expired) return;
    const id = window.setTimeout(onExpired, 5000);
    return () => window.clearTimeout(id);
  }, [expired, onExpired]);

  return (
    <li className="card p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <StatusBadge status={order.status} />
          <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">
            Placed {formatCreated(order.createdAt)} · {pluralize(tickets, 'ticket')}
          </p>
        </div>
        <p className="text-xl font-bold">{formatMoney(order.totalAmount)}</p>
      </div>

      <ul className="mt-4 divide-y divide-slate-100 dark:divide-slate-800">
        {order.items.map((item) => (
          <li key={item.eventId} className="flex items-center justify-between gap-4 py-2 text-sm">
            <div className="min-w-0">
              <Link to={`/events/${item.eventId}`} className="block truncate font-medium hover:text-indigo-600 dark:hover:text-indigo-400">
                {item.eventTitle}
              </Link>
              <p className="truncate text-slate-500 dark:text-slate-400">
                {formatDateTime(item.eventStartsAt)} · {item.eventLocation}
              </p>
            </div>
            <p className="shrink-0 text-slate-600 dark:text-slate-300">
              {item.quantity} × {formatMoney(item.unitPrice)}
            </p>
          </li>
        ))}
      </ul>

      {order.status === 'PENDING' && (
        <div className="mt-4 flex flex-col gap-3 border-t border-slate-200 pt-4 dark:border-slate-800 sm:flex-row sm:items-center sm:justify-between">
          <p className={`text-sm font-medium ${expired ? 'text-rose-600' : 'text-amber-700 dark:text-amber-400'}`}>
            {expired
              ? 'Reservation expired. The tickets are being released.'
              : remaining !== null
                ? `Tickets held for ${formatCountdown(remaining)}`
                : 'Complete the payment to confirm your tickets'}
          </p>
          <div className="flex gap-2">
            <button type="button" className="btn btn-secondary btn-sm" disabled={busy} onClick={() => onCancel(order)}>
              Cancel
            </button>
            <button type="button" className="btn btn-primary btn-sm" disabled={busy || expired} onClick={() => onPay(order)}>
              Pay now
            </button>
          </div>
        </div>
      )}
    </li>
  );
}

function formatCreated(iso: string): string {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(iso));
}

export default function MyOrders() {
  useDocumentTitle('My orders');
  const toast = useToast();
  const [page, setPage] = useState(0);
  const { data, error, loading, reload } = useAsync((signal) => ordersApi.mine(page, signal), [page]);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [toCancel, setToCancel] = useState<Order | null>(null);

  async function run(order: Order, action: () => Promise<unknown>, successMessage: string) {
    setBusyId(order.id);
    try {
      await action();
      toast.push(successMessage, 'success');
    } catch (cause) {
      toast.push(errorMessage(cause), 'error');
    } finally {
      setBusyId(null);
      setToCancel(null);
      reload();
    }
  }

  return (
    <div className="container-page py-8">
      <h1 className="text-3xl font-bold">My orders</h1>
      <p className="mt-1 text-slate-600 dark:text-slate-400">
        Unpaid orders hold your tickets for a limited time, then release them automatically.
      </p>

      <div className="mt-6">
        {error && <ErrorBanner error={error} onRetry={reload} />}
        {loading && !data && <Spinner label="Loading your orders" />}

        {data && data.content.length === 0 && (
          <EmptyState
            title="You have no orders yet"
            description="When you buy tickets they will show up here."
            action={
              <Link to="/events" className="btn btn-primary">
                Find an event
              </Link>
            }
          />
        )}

        {data && data.content.length > 0 && (
          <ul className={`space-y-4 ${loading ? 'opacity-60 transition-opacity' : ''}`}>
            {data.content.map((order) => (
              <OrderCard
                key={order.id}
                order={order}
                busy={busyId === order.id}
                onPay={(o) => run(o, () => ordersApi.pay(o.id), 'Payment received. Your tickets are confirmed!')}
                onCancel={setToCancel}
                onExpired={reload}
              />
            ))}
          </ul>
        )}

        {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />}
      </div>

      {toCancel && (
        <ConfirmDialog
          title="Cancel this order?"
          message="Your reserved tickets will be released and become available to other people."
          confirmLabel="Cancel order"
          danger
          busy={busyId === toCancel.id}
          onCancel={() => setToCancel(null)}
          onConfirm={() => run(toCancel, () => ordersApi.cancel(toCancel.id), 'Order cancelled.')}
        />
      )}
    </div>
  );
}
