import { adminApi } from '../../api/endpoints';
import { AvailabilityBar } from '../../components/AvailabilityBar';
import { EmptyState } from '../../components/EmptyState';
import { ErrorBanner } from '../../components/ErrorBanner';
import { Spinner } from '../../components/Spinner';
import { useAsync } from '../../hooks/useAsync';
import { formatMoney, formatPrice } from '../../utils/format';

function StatCard({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div className="card p-5">
      <p className="text-sm text-slate-500 dark:text-slate-400">{label}</p>
      <p className="mt-1 text-3xl font-bold">{value}</p>
      {hint && <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{hint}</p>}
    </div>
  );
}

export default function Dashboard() {
  const { data, error, loading, reload } = useAsync((signal) => adminApi.stats(signal), []);

  if (loading && !data) return <Spinner label="Loading dashboard" />;
  if (error && !data) return <ErrorBanner error={error} onRetry={reload} />;
  if (!data) return null;

  return (
    <div className="space-y-8">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Revenue" value={formatMoney(data.revenue)} hint="From confirmed orders" />
        <StatCard label="Tickets sold" value={String(data.ticketsSold)} hint="Across all events" />
        <StatCard label="Upcoming events" value={String(data.upcomingEvents)} hint={`${data.totalEvents} in total`} />
        <StatCard label="Registered users" value={String(data.totalUsers)} hint={`${data.totalCategories} categories`} />
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <section className="card p-5 lg:col-span-1">
          <h2 className="font-semibold">Orders</h2>
          <dl className="mt-4 space-y-3 text-sm">
            <div className="flex items-center justify-between">
              <dt className="text-slate-600 dark:text-slate-400">Awaiting payment</dt>
              <dd className="chip bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300">{data.pendingOrders}</dd>
            </div>
            <div className="flex items-center justify-between">
              <dt className="text-slate-600 dark:text-slate-400">Confirmed</dt>
              <dd className="chip bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300">{data.completedOrders}</dd>
            </div>
            <div className="flex items-center justify-between">
              <dt className="text-slate-600 dark:text-slate-400">Cancelled</dt>
              <dd className="chip bg-slate-200 text-slate-700 dark:bg-slate-800 dark:text-slate-300">{data.cancelledOrders}</dd>
            </div>
          </dl>
        </section>

        <section className="card p-5 lg:col-span-2">
          <h2 className="font-semibold">Best sellers</h2>
          {data.topEvents.length === 0 ? (
            <div className="mt-4">
              <EmptyState title="No sales yet" description="Top events will appear here once tickets are sold." />
            </div>
          ) : (
            <ol className="mt-4 space-y-4">
              {data.topEvents.map((event, index) => (
                <li key={event.id} className="flex items-center gap-4">
                  <span className="w-5 text-sm font-semibold text-slate-400">{index + 1}</span>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-baseline justify-between gap-3">
                      <p className="truncate text-sm font-medium">{event.title}</p>
                      <p className="shrink-0 text-xs text-slate-500 dark:text-slate-400">
                        {event.ticketsSold}/{event.capacity} · {formatPrice(event.price)}
                      </p>
                    </div>
                    <div className="mt-1.5">
                      <AvailabilityBar sold={event.ticketsSold} capacity={event.capacity} compact />
                    </div>
                  </div>
                </li>
              ))}
            </ol>
          )}
        </section>
      </div>
    </div>
  );
}
