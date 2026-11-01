import { useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '../api/client';
import { eventsApi, ordersApi } from '../api/endpoints';
import { useAuth } from '../auth/AuthContext';
import { AvailabilityBar } from '../components/AvailabilityBar';
import { ErrorBanner } from '../components/ErrorBanner';
import { Spinner } from '../components/Spinner';
import { useToast } from '../components/Toast';
import { useAsync } from '../hooks/useAsync';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { availabilityOf, formatDateTime, formatMoney, formatPrice, pluralize } from '../utils/format';
import { gradientFor } from '../utils/gradient';
import { errorMessage } from '../utils/errors';
import NotFound from './NotFound';

const MAX_PER_ORDER = 10;

export default function EventDetail() {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();
  const { user } = useAuth();

  const { data: event, error, loading, reload } = useAsync((signal) => eventsApi.get(id, signal), [id]);
  const [quantity, setQuantity] = useState(1);
  const [buying, setBuying] = useState(false);
  const [imageFailed, setImageFailed] = useState(false);

  useDocumentTitle(event?.title ?? 'Event');

  if (error instanceof ApiError && error.status === 404) return <NotFound />;
  if (loading && !event) return <Spinner label="Loading event" />;
  if (error && !event) {
    return (
      <div className="container-page py-10">
        <ErrorBanner error={error} onRetry={reload} />
      </div>
    );
  }
  if (!event) return null;

  const started = new Date(event.startsAt).getTime() <= Date.now();
  const availability = availabilityOf(event.availableTickets, event.capacity);
  const maxQuantity = Math.max(1, Math.min(MAX_PER_ORDER, event.availableTickets));
  const selected = Math.min(quantity, maxQuantity);
  const canBuy = !started && event.availableTickets > 0;

  async function buy() {
    if (!event) return;
    if (!user) {
      navigate('/login', { state: { from: location.pathname } });
      return;
    }
    setBuying(true);
    try {
      await ordersApi.create(event.id, selected);
      toast.push('Tickets reserved! Complete the payment to confirm your order.', 'success');
      navigate('/orders');
    } catch (cause) {
      toast.push(errorMessage(cause), 'error');
      // Someone may have bought the last tickets meanwhile: refresh the availability.
      if (cause instanceof ApiError && cause.status === 409) reload();
    } finally {
      setBuying(false);
    }
  }

  return (
    <div className="container-page py-8">
      <Link to="/events" className="text-sm font-medium text-indigo-600 hover:underline dark:text-indigo-400">
        ← All events
      </Link>

      <div className="mt-4 overflow-hidden rounded-2xl">
        <div className={`relative h-48 bg-gradient-to-br sm:h-64 ${gradientFor(event.category.name)}`}>
          {event.imageUrl && !imageFailed && (
            <img src={event.imageUrl} alt="" onError={() => setImageFailed(true)} className="h-full w-full object-cover" />
          )}
          <span className="chip absolute bottom-4 left-4 bg-black/40 text-white backdrop-blur">{event.category.name}</span>
        </div>
      </div>

      <div className="mt-8 grid gap-8 lg:grid-cols-[minmax(0,1fr)_22rem]">
        <article>
          <h1 className="text-3xl font-bold sm:text-4xl">{event.title}</h1>

          <dl className="mt-6 grid gap-4 sm:grid-cols-2">
            <div className="card p-4">
              <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">When</dt>
              <dd className="mt-1 font-medium">{formatDateTime(event.startsAt)}</dd>
            </div>
            <div className="card p-4">
              <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">Where</dt>
              <dd className="mt-1 font-medium">{event.location}</dd>
            </div>
          </dl>

          {event.description && (
            <section className="mt-8">
              <h2 className="text-xl font-semibold">About this event</h2>
              <p className="mt-2 whitespace-pre-line text-slate-700 dark:text-slate-300">{event.description}</p>
            </section>
          )}
        </article>

        <aside>
          <div className="card p-5 lg:sticky lg:top-24">
            <p className="text-3xl font-extrabold">{formatPrice(event.price)}</p>
            <p className="text-sm text-slate-500 dark:text-slate-400">per ticket</p>

            <div className="mt-4">
              <AvailabilityBar sold={event.ticketsSold} capacity={event.capacity} />
              {availability === 'almost-sold-out' && (
                <p className="mt-2 text-sm font-medium text-amber-600 dark:text-amber-400">
                  Hurry, only {pluralize(event.availableTickets, 'ticket')} left!
                </p>
              )}
            </div>

            {canBuy ? (
              <>
                <div className="mt-5 flex items-center justify-between">
                  <label htmlFor="quantity" className="text-sm font-medium">
                    Tickets
                  </label>
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm h-8 w-8 px-0"
                      aria-label="Fewer tickets"
                      disabled={selected <= 1}
                      onClick={() => setQuantity(selected - 1)}
                    >
                      −
                    </button>
                    <output id="quantity" aria-live="polite" className="w-8 text-center font-semibold">
                      {selected}
                    </output>
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm h-8 w-8 px-0"
                      aria-label="More tickets"
                      disabled={selected >= maxQuantity}
                      onClick={() => setQuantity(selected + 1)}
                    >
                      +
                    </button>
                  </div>
                </div>

                <div className="mt-4 flex items-center justify-between border-t border-slate-200 pt-4 dark:border-slate-800">
                  <span className="text-sm text-slate-600 dark:text-slate-400">Total</span>
                  <span className="text-xl font-bold">{formatMoney(event.price * selected)}</span>
                </div>

                <button type="button" onClick={buy} disabled={buying} className="btn btn-primary mt-4 w-full py-3">
                  {buying ? 'Reserving…' : user ? 'Reserve tickets' : 'Log in to buy tickets'}
                </button>
                <p className="mt-2 text-center text-xs text-slate-500 dark:text-slate-400">
                  Tickets are held for you until the payment is completed. Max {MAX_PER_ORDER} per order.
                </p>
              </>
            ) : (
              <p className="mt-5 rounded-lg bg-slate-100 px-4 py-3 text-center text-sm font-medium dark:bg-slate-800">
                {started ? 'This event has already started.' : 'This event is sold out.'}
              </p>
            )}
          </div>
        </aside>
      </div>
    </div>
  );
}
