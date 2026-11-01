import { useEffect, useRef, useState, type KeyboardEvent } from 'react';
import { useSearchParams } from 'react-router-dom';
import { categoriesApi, eventsApi } from '../api/endpoints';
import type { EventFilters } from '../api/types';
import { EmptyState } from '../components/EmptyState';
import { ErrorBanner } from '../components/ErrorBanner';
import { EventCard } from '../components/EventCard';
import { Pagination } from '../components/Pagination';
import { Spinner } from '../components/Spinner';
import { useAsync } from '../hooks/useAsync';
import { useDebouncedValue } from '../hooks/useDebouncedValue';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { pluralize } from '../utils/format';

const SORT_OPTIONS = [
  { value: 'startsAt,asc', label: 'Soonest first' },
  { value: 'price,asc', label: 'Price: low to high' },
  { value: 'price,desc', label: 'Price: high to low' },
  { value: 'createdAt,desc', label: 'Recently added' },
];

// URL parameters that count as "a filter" (page and sort do not).
const FILTER_KEYS = ['q', 'category', 'min', 'max', 'from', 'to', 'available'];

/**
 * The whole filter state lives in the URL (?q=jazz&category=...&page=2), so a search can be
 * bookmarked, shared, and survives a refresh or the browser back button.
 */
export default function Events() {
  useDocumentTitle('Events');
  const [params, setParams] = useSearchParams();
  const [filtersOpen, setFiltersOpen] = useState(false);

  const q = params.get('q') ?? '';
  const page = Math.max(0, Number(params.get('page') ?? '0') || 0);
  const sort = params.get('sort') ?? 'startsAt,asc';
  const hasFilters = FILTER_KEYS.some((key) => params.has(key));

  function update(patch: Record<string, string | null>, keepPage = false) {
    setParams(
      (current) => {
        const next = new URLSearchParams(current);
        for (const [key, value] of Object.entries(patch)) {
          if (value === null || value === '') next.delete(key);
          else next.set(key, value);
        }
        if (!keepPage) next.delete('page');
        return next;
      },
      { replace: true },
    );
  }

  // Text search: type freely, the URL (and therefore the request) updates after a short pause.
  const [searchText, setSearchText] = useState(q);
  const debouncedSearch = useDebouncedValue(searchText, 400);
  const lastPushed = useRef(q);
  const updateRef = useRef(update);
  updateRef.current = update;

  useEffect(() => {
    if (debouncedSearch !== lastPushed.current) {
      lastPushed.current = debouncedSearch;
      updateRef.current({ q: debouncedSearch });
    }
  }, [debouncedSearch]);

  // If the URL changes from outside (clear filters, a link), reflect it in the input.
  useEffect(() => {
    if (q !== lastPushed.current) {
      lastPushed.current = q;
      setSearchText(q);
    }
  }, [q]);

  const filters: EventFilters = {
    search: q || undefined,
    categoryId: params.get('category') ?? undefined,
    minPrice: params.get('min') ?? undefined,
    maxPrice: params.get('max') ?? undefined,
    from: params.get('from') ?? undefined,
    to: params.get('to') ?? undefined,
    onlyAvailable: params.get('available') === 'true',
    page,
    size: 12,
    sort,
  };

  const categories = useAsync((signal) => categoriesApi.list(signal), []);
  const events = useAsync((signal) => eventsApi.search(filters, signal), [params.toString()]);

  function clearFilters() {
    setSearchText('');
    lastPushed.current = '';
    setParams(
      (current) => {
        const next = new URLSearchParams(current);
        FILTER_KEYS.forEach((key) => next.delete(key));
        next.delete('page');
        return next;
      },
      { replace: true },
    );
  }

  function commitOnEnter(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter') event.currentTarget.blur();
  }

  const filterPanel = (
    <div className="space-y-5">
      <div>
        <label htmlFor="f-search" className="label">
          Search
        </label>
        <input
          id="f-search"
          type="search"
          className="input"
          placeholder="Title, place or topic"
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
        />
      </div>

      <div>
        <label htmlFor="f-category" className="label">
          Category
        </label>
        <select
          id="f-category"
          className="input"
          value={params.get('category') ?? ''}
          onChange={(e) => update({ category: e.target.value })}
        >
          <option value="">All categories</option>
          {categories.data?.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>
      </div>

      <fieldset>
        <legend className="label">Price</legend>
        <div className="flex items-center gap-2">
          <input
            key={`min-${params.get('min') ?? ''}`}
            aria-label="Minimum price"
            type="number"
            min="0"
            step="1"
            inputMode="decimal"
            placeholder="Min"
            className="input"
            defaultValue={params.get('min') ?? ''}
            onBlur={(e) => update({ min: e.target.value })}
            onKeyDown={commitOnEnter}
          />
          <span className="text-slate-400">–</span>
          <input
            key={`max-${params.get('max') ?? ''}`}
            aria-label="Maximum price"
            type="number"
            min="0"
            step="1"
            inputMode="decimal"
            placeholder="Max"
            className="input"
            defaultValue={params.get('max') ?? ''}
            onBlur={(e) => update({ max: e.target.value })}
            onKeyDown={commitOnEnter}
          />
        </div>
      </fieldset>

      <fieldset>
        <legend className="label">Date</legend>
        <div className="space-y-2">
          <input
            aria-label="From date"
            type="date"
            className="input"
            value={params.get('from') ?? ''}
            onChange={(e) => update({ from: e.target.value })}
          />
          <input
            aria-label="To date"
            type="date"
            className="input"
            value={params.get('to') ?? ''}
            onChange={(e) => update({ to: e.target.value })}
          />
        </div>
      </fieldset>

      <label className="flex cursor-pointer items-center gap-2 text-sm">
        <input
          type="checkbox"
          className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
          checked={params.get('available') === 'true'}
          onChange={(e) => update({ available: e.target.checked ? 'true' : null })}
        />
        Hide sold-out events
      </label>

      {hasFilters && (
        <button type="button" onClick={clearFilters} className="btn btn-secondary w-full">
          Clear all filters
        </button>
      )}
    </div>
  );

  return (
    <div className="container-page py-8">
      <h1 className="text-3xl font-bold">Events</h1>

      <div className="mt-6 lg:grid lg:grid-cols-[16rem_minmax(0,1fr)] lg:gap-8">
        <aside className="mb-6 lg:mb-0">
          <button
            type="button"
            className="btn btn-secondary w-full lg:hidden"
            aria-expanded={filtersOpen}
            onClick={() => setFiltersOpen((open) => !open)}
          >
            {filtersOpen ? 'Hide filters' : 'Show filters'}
            {hasFilters && <span className="chip bg-indigo-100 text-indigo-700">on</span>}
          </button>
          <div className={`${filtersOpen ? 'block' : 'hidden'} card mt-3 p-4 lg:sticky lg:top-24 lg:mt-0 lg:block`}>
            {filterPanel}
          </div>
        </aside>

        <section aria-live="polite">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <p className="text-sm text-slate-600 dark:text-slate-400">
              {events.data ? pluralize(events.data.totalElements, 'event') : 'Loading…'}
            </p>
            <div className="flex items-center gap-2">
              <label htmlFor="sort" className="text-sm text-slate-600 dark:text-slate-400">
                Sort by
              </label>
              <select id="sort" className="input w-auto py-1.5" value={sort} onChange={(e) => update({ sort: e.target.value })}>
                {SORT_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {events.error && <ErrorBanner error={events.error} onRetry={events.reload} />}
          {events.loading && !events.data && <Spinner label="Loading events" />}

          {events.data && events.data.content.length === 0 && !events.error && (
            <EmptyState
              title="No events match your search"
              description="Try removing some filters or searching for something else."
              action={
                hasFilters ? (
                  <button type="button" onClick={clearFilters} className="btn btn-primary">
                    Clear filters
                  </button>
                ) : undefined
              }
            />
          )}

          {events.data && events.data.content.length > 0 && (
            <div
              className={`grid gap-6 sm:grid-cols-2 xl:grid-cols-3 ${events.loading ? 'opacity-60 transition-opacity' : ''}`}
            >
              {events.data.content.map((event) => (
                <EventCard key={event.id} event={event} />
              ))}
            </div>
          )}

          {events.data && (
            <Pagination
              page={events.data.page}
              totalPages={events.data.totalPages}
              onChange={(next) => {
                update({ page: next === 0 ? null : String(next) }, true);
                window.scrollTo({ top: 0, behavior: 'smooth' });
              }}
            />
          )}
        </section>
      </div>
    </div>
  );
}
