import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { categoriesApi, eventsApi } from '../api/endpoints';
import { EmptyState } from '../components/EmptyState';
import { ErrorBanner } from '../components/ErrorBanner';
import { EventCard } from '../components/EventCard';
import { Spinner } from '../components/Spinner';
import { useAsync } from '../hooks/useAsync';
import { useDocumentTitle } from '../hooks/useDocumentTitle';

const STEPS = [
  { title: 'Discover', text: 'Search by name, category, price or date to find events you will love.' },
  { title: 'Reserve', text: 'Pick your tickets. They are held for you while you complete the payment.' },
  { title: 'Enjoy', text: 'Your order is confirmed instantly and always available under “My orders”.' },
];

export default function Home() {
  useDocumentTitle('');
  const navigate = useNavigate();
  const [query, setQuery] = useState('');

  const categories = useAsync((signal) => categoriesApi.list(signal), []);
  const upcoming = useAsync((signal) => eventsApi.search({ size: 6, sort: 'startsAt,asc' }, signal), []);

  function onSearch(event: FormEvent) {
    event.preventDefault();
    const text = query.trim();
    navigate(text ? `/events?q=${encodeURIComponent(text)}` : '/events');
  }

  return (
    <>
      <section className="bg-gradient-to-br from-indigo-600 via-indigo-600 to-violet-600 text-white">
        <div className="container-page py-16 sm:py-24">
          <h1 className="max-w-2xl text-4xl font-extrabold sm:text-5xl">Find your next unforgettable event</h1>
          <p className="mt-4 max-w-xl text-lg text-indigo-100">
            Concerts, workshops, meetups and more. Browse what is happening and book your tickets in seconds.
          </p>

          <form onSubmit={onSearch} className="mt-8 flex max-w-xl flex-col gap-3 sm:flex-row" role="search">
            <label htmlFor="hero-search" className="sr-only">
              Search events
            </label>
            <input
              id="hero-search"
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search by title, place or topic"
              className="input flex-1 border-transparent py-3 text-base"
            />
            <button type="submit" className="btn bg-white py-3 text-indigo-700 hover:bg-indigo-50">
              Search events
            </button>
          </form>

          {categories.data && categories.data.length > 0 && (
            <div className="mt-6 flex flex-wrap gap-2">
              {categories.data.map((category) => (
                <Link
                  key={category.id}
                  to={`/events?category=${category.id}`}
                  className="chip bg-white/15 px-3 py-1 text-sm text-white hover:bg-white/25"
                >
                  {category.name}
                </Link>
              ))}
            </div>
          )}
        </div>
      </section>

      <section className="container-page py-12">
        <div className="mb-6 flex items-end justify-between gap-4">
          <h2 className="text-2xl font-bold">Coming up</h2>
          <Link to="/events" className="text-sm font-semibold text-indigo-600 hover:underline dark:text-indigo-400">
            View all events →
          </Link>
        </div>

        {upcoming.loading && !upcoming.data && <Spinner label="Loading events" />}
        {upcoming.error && <ErrorBanner error={upcoming.error} onRetry={upcoming.reload} />}
        {upcoming.data && upcoming.data.content.length === 0 && (
          <EmptyState title="No upcoming events yet" description="Check back soon, new events are added regularly." />
        )}
        {upcoming.data && upcoming.data.content.length > 0 && (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {upcoming.data.content.map((event) => (
              <EventCard key={event.id} event={event} />
            ))}
          </div>
        )}
      </section>

      <section className="container-page pb-4">
        <h2 className="mb-6 text-2xl font-bold">How it works</h2>
        <ol className="grid gap-6 sm:grid-cols-3">
          {STEPS.map((step, index) => (
            <li key={step.title} className="card p-5">
              <span className="flex h-9 w-9 items-center justify-center rounded-full bg-indigo-100 text-sm font-bold text-indigo-700 dark:bg-indigo-950 dark:text-indigo-300">
                {index + 1}
              </span>
              <h3 className="mt-3 font-semibold">{step.title}</h3>
              <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{step.text}</p>
            </li>
          ))}
        </ol>
      </section>
    </>
  );
}
