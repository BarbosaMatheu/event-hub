const DEFAULT_CURRENCY = import.meta.env.VITE_CURRENCY ?? 'EUR';

/** "Free" for zero, otherwise a localised currency string. */
export function formatPrice(value: number, currency: string = DEFAULT_CURRENCY, locale?: string): string {
  if (value === 0) return 'Free';
  return new Intl.NumberFormat(locale, { style: 'currency', currency }).format(value);
}

/** Like formatPrice, but zero is shown as an amount (for totals). */
export function formatMoney(value: number, currency: string = DEFAULT_CURRENCY, locale?: string): string {
  return new Intl.NumberFormat(locale, { style: 'currency', currency }).format(value);
}

/** Parses "2030-06-01T20:00:00" (no timezone) as venue-local wall-clock time. */
function parseLocal(iso: string): Date {
  return new Date(iso);
}

export function formatDate(iso: string, locale?: string): string {
  return new Intl.DateTimeFormat(locale, { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' }).format(
    parseLocal(iso),
  );
}

export function formatTime(iso: string, locale?: string): string {
  return new Intl.DateTimeFormat(locale, { hour: '2-digit', minute: '2-digit' }).format(parseLocal(iso));
}

export function formatDateTime(iso: string, locale?: string): string {
  return `${formatDate(iso, locale)}, ${formatTime(iso, locale)}`;
}

/** Day number and short month for the calendar badge on event cards. */
export function dayParts(iso: string, locale?: string): { day: string; month: string } {
  const date = parseLocal(iso);
  return {
    day: new Intl.DateTimeFormat(locale, { day: 'numeric' }).format(date),
    month: new Intl.DateTimeFormat(locale, { month: 'short' }).format(date),
  };
}

/** 125 -> "02:05" */
export function formatCountdown(totalSeconds: number): string {
  const safe = Math.max(0, Math.floor(totalSeconds));
  const minutes = Math.floor(safe / 60);
  const seconds = safe % 60;
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
}

export type Availability = 'sold-out' | 'almost-sold-out' | 'available';

/** "Almost sold out" once 15% or fewer of the tickets (and at most 20) are left. */
export function availabilityOf(available: number, capacity: number): Availability {
  if (available <= 0) return 'sold-out';
  if (available <= 20 && available / capacity <= 0.15) return 'almost-sold-out';
  return 'available';
}

/** Share of tickets sold, as a whole percentage between 0 and 100. */
export function soldPercent(sold: number, capacity: number): number {
  if (capacity <= 0) return 0;
  return Math.min(100, Math.round((sold / capacity) * 100));
}

export function pluralize(count: number, singular: string, plural = `${singular}s`): string {
  return `${count} ${count === 1 ? singular : plural}`;
}
