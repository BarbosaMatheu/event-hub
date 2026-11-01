import { describe, expect, it } from 'vitest';
import {
  availabilityOf,
  dayParts,
  formatCountdown,
  formatDateTime,
  formatPrice,
  pluralize,
  soldPercent,
} from './format';

describe('formatPrice', () => {
  it('shows "Free" for zero', () => {
    expect(formatPrice(0, 'EUR', 'en-US')).toBe('Free');
  });

  it('formats money with the currency symbol', () => {
    expect(formatPrice(25.5, 'EUR', 'en-US')).toBe('€25.50');
    expect(formatPrice(1000, 'USD', 'en-US')).toBe('$1,000.00');
  });
});

describe('date formatting (venue-local wall-clock time)', () => {
  it('does not shift the time of day', () => {
    const text = formatDateTime('2030-06-01T20:05:00', 'en-US');
    expect(text).toContain('2030');
    expect(text).toContain('Jun');
    expect(text).toMatch(/8:05\s?PM/);
  });

  it('splits day and month for the calendar badge', () => {
    expect(dayParts('2030-12-24T10:00:00', 'en-US')).toEqual({ day: '24', month: 'Dec' });
  });
});

describe('formatCountdown', () => {
  it('formats minutes and seconds with leading zeros', () => {
    expect(formatCountdown(125)).toBe('02:05');
    expect(formatCountdown(9)).toBe('00:09');
    expect(formatCountdown(1800)).toBe('30:00');
  });

  it('never goes negative', () => {
    expect(formatCountdown(-5)).toBe('00:00');
  });
});

describe('availabilityOf', () => {
  it('detects sold out events', () => {
    expect(availabilityOf(0, 100)).toBe('sold-out');
  });

  it('flags events that are almost sold out', () => {
    expect(availabilityOf(10, 100)).toBe('almost-sold-out');
    expect(availabilityOf(15, 100)).toBe('almost-sold-out');
  });

  it('keeps large venues "available" until a small share is left', () => {
    expect(availabilityOf(30, 1000)).toBe('available');
    expect(availabilityOf(50, 100)).toBe('available');
  });
});

describe('soldPercent', () => {
  it('rounds and clamps', () => {
    expect(soldPercent(1, 3)).toBe(33);
    expect(soldPercent(5, 5)).toBe(100);
    expect(soldPercent(7, 5)).toBe(100);
    expect(soldPercent(0, 0)).toBe(0);
  });
});

describe('pluralize', () => {
  it('handles one and many', () => {
    expect(pluralize(1, 'ticket')).toBe('1 ticket');
    expect(pluralize(3, 'ticket')).toBe('3 tickets');
    expect(pluralize(2, 'category', 'categories')).toBe('2 categories');
  });
});
