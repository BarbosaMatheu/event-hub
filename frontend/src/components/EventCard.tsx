import { useState } from 'react';
import { Link } from 'react-router-dom';
import type { EventDto } from '../api/types';
import { availabilityOf, dayParts, formatPrice, formatTime } from '../utils/format';
import { gradientFor } from '../utils/gradient';
import { AvailabilityBar } from './AvailabilityBar';

export function EventCard({ event }: { event: EventDto }) {
  const [imageFailed, setImageFailed] = useState(false);
  const { day, month } = dayParts(event.startsAt);
  const availability = availabilityOf(event.availableTickets, event.capacity);

  return (
    <Link
      to={`/events/${event.id}`}
      className="card group flex flex-col overflow-hidden transition hover:-translate-y-0.5 hover:shadow-lg focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500"
    >
      <div className={`relative h-36 bg-gradient-to-br ${gradientFor(event.category.name)}`}>
        {event.imageUrl && !imageFailed && (
          <img
            src={event.imageUrl}
            alt=""
            loading="lazy"
            onError={() => setImageFailed(true)}
            className="h-full w-full object-cover"
          />
        )}
        <div className="absolute left-3 top-3 rounded-lg bg-white px-2.5 py-1.5 text-center shadow dark:bg-slate-900">
          <div className="text-lg font-bold leading-none text-slate-900 dark:text-white">{day}</div>
          <div className="text-[10px] font-semibold uppercase tracking-wide text-indigo-600 dark:text-indigo-400">{month}</div>
        </div>
        {availability === 'sold-out' && (
          <span className="chip absolute right-3 top-3 bg-rose-600 text-white">Sold out</span>
        )}
        {availability === 'almost-sold-out' && (
          <span className="chip absolute right-3 top-3 bg-amber-400 text-amber-950">Few left</span>
        )}
        <span className="chip absolute bottom-3 left-3 bg-black/40 text-white backdrop-blur">{event.category.name}</span>
      </div>

      <div className="flex flex-1 flex-col p-4">
        <h3 className="line-clamp-2 text-base font-semibold group-hover:text-indigo-600 dark:group-hover:text-indigo-400">
          {event.title}
        </h3>
        <p className="mt-1 truncate text-sm text-slate-600 dark:text-slate-400">
          {formatTime(event.startsAt)} · {event.location}
        </p>
        <div className="mt-auto pt-4">
          <div className="mb-2 flex items-center justify-between">
            <span className="text-base font-bold">{formatPrice(event.price)}</span>
            <span className="text-xs text-slate-500 dark:text-slate-400">{event.availableTickets} left</span>
          </div>
          <AvailabilityBar sold={event.ticketsSold} capacity={event.capacity} compact />
        </div>
      </div>
    </Link>
  );
}
