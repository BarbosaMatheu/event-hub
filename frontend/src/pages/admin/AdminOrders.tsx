import { useState } from 'react';
import { adminApi } from '../../api/endpoints';
import type { Order, OrderStatus } from '../../api/types';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { EmptyState } from '../../components/EmptyState';
import { ErrorBanner } from '../../components/ErrorBanner';
import { Pagination } from '../../components/Pagination';
import { Spinner } from '../../components/Spinner';
import { StatusBadge } from '../../components/StatusBadge';
import { useToast } from '../../components/Toast';
import { useAsync } from '../../hooks/useAsync';
import { errorMessage } from '../../utils/errors';
import { formatMoney } from '../../utils/format';

const FILTERS: { value: OrderStatus | ''; label: string }[] = [
  { value: '', label: 'All' },
  { value: 'PENDING', label: 'Awaiting payment' },
  { value: 'COMPLETED', label: 'Confirmed' },
  { value: 'CANCELLED', label: 'Cancelled' },
];

function formatCreated(iso: string): string {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(iso));
}

export default function AdminOrders() {
  const toast = useToast();
  const [status, setStatus] = useState<OrderStatus | ''>('');
  const [page, setPage] = useState(0);
  const [toCancel, setToCancel] = useState<Order | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const { data, error, loading, reload } = useAsync((signal) => adminApi.orders(status, page, signal), [status, page]);

  async function setOrderStatus(order: Order, next: OrderStatus) {
    setBusyId(order.id);
    try {
      await adminApi.setOrderStatus(order.id, next);
      toast.push(next === 'COMPLETED' ? 'Order marked as paid.' : 'Order cancelled and tickets released.', 'success');
    } catch (cause) {
      toast.push(errorMessage(cause), 'error');
    } finally {
      setBusyId(null);
      setToCancel(null);
      reload();
    }
  }

  return (
    <div>
      <div role="group" aria-label="Filter by status" className="flex flex-wrap gap-2">
        {FILTERS.map((filter) => (
          <button
            key={filter.value || 'all'}
            type="button"
            aria-pressed={status === filter.value}
            onClick={() => {
              setStatus(filter.value);
              setPage(0);
            }}
            className={`btn btn-sm ${status === filter.value ? 'btn-primary' : 'btn-secondary'}`}
          >
            {filter.label}
          </button>
        ))}
      </div>

      <div className="mt-6">
        {error && <ErrorBanner error={error} onRetry={reload} />}
        {loading && !data && <Spinner label="Loading orders" />}
        {data && data.content.length === 0 && <EmptyState title="No orders found" description="Try another status filter." />}

        {data && data.content.length > 0 && (
          <div className={`card overflow-x-auto ${loading ? 'opacity-60' : ''}`}>
            <table className="w-full min-w-[760px] text-left text-sm">
              <thead className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500 dark:border-slate-800 dark:text-slate-400">
                <tr>
                  <th scope="col" className="px-4 py-3 font-semibold">Order</th>
                  <th scope="col" className="px-4 py-3 font-semibold">Customer</th>
                  <th scope="col" className="px-4 py-3 font-semibold">Tickets</th>
                  <th scope="col" className="px-4 py-3 font-semibold">Total</th>
                  <th scope="col" className="px-4 py-3 font-semibold">Status</th>
                  <th scope="col" className="px-4 py-3 text-right font-semibold">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {data.content.map((order) => (
                  <tr key={order.id}>
                    <td className="px-4 py-3">
                      <p className="font-mono text-xs">#{order.id.slice(0, 8)}</p>
                      <p className="text-xs text-slate-500 dark:text-slate-400">{formatCreated(order.createdAt)}</p>
                    </td>
                    <td className="px-4 py-3">{order.customerLogin}</td>
                    <td className="px-4 py-3">
                      {order.items.map((item) => (
                        <p key={item.eventId} className="max-w-[16rem] truncate">
                          {item.quantity} × {item.eventTitle}
                        </p>
                      ))}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3">{formatMoney(order.totalAmount)}</td>
                    <td className="px-4 py-3">
                      <StatusBadge status={order.status} />
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right">
                      {order.status === 'PENDING' && (
                        <>
                          <button
                            type="button"
                            className="btn btn-ghost btn-sm"
                            disabled={busyId === order.id}
                            onClick={() => setOrderStatus(order, 'COMPLETED')}
                          >
                            Mark paid
                          </button>
                          <button
                            type="button"
                            className="btn btn-ghost btn-sm text-rose-600 dark:text-rose-400"
                            disabled={busyId === order.id}
                            onClick={() => setToCancel(order)}
                          >
                            Cancel
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />}
      </div>

      {toCancel && (
        <ConfirmDialog
          title="Cancel this order?"
          message={`Order #${toCancel.id.slice(0, 8)} from ${toCancel.customerLogin} will be cancelled and its tickets released.`}
          confirmLabel="Cancel order"
          danger
          busy={busyId === toCancel.id}
          onCancel={() => setToCancel(null)}
          onConfirm={() => setOrderStatus(toCancel, 'CANCELLED')}
        />
      )}
    </div>
  );
}
