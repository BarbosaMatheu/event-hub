import { describe, expect, it } from 'vitest';
import {
  type EventFormValues,
  toApiDateTime,
  toInputDateTime,
  validateEventForm,
  validateLogin,
  validatePassword,
} from './validation';

describe('validateLogin', () => {
  it('accepts valid logins', () => {
    expect(validateLogin('ann')).toBeNull();
    expect(validateLogin('ann.smith_99-x')).toBeNull();
  });

  it('rejects empty, short and invalid characters', () => {
    expect(validateLogin('')).not.toBeNull();
    expect(validateLogin('ab')).not.toBeNull();
    expect(validateLogin('a b c')).not.toBeNull();
    expect(validateLogin('x'.repeat(51))).not.toBeNull();
  });
});

describe('validatePassword', () => {
  it('requires at least 8 characters', () => {
    expect(validatePassword('1234567')).not.toBeNull();
    expect(validatePassword('12345678')).toBeNull();
  });

  it('enforces the 72-byte BCrypt limit (bytes, not characters)', () => {
    expect(validatePassword('a'.repeat(72))).toBeNull();
    expect(validatePassword('a'.repeat(73))).not.toBeNull();
    expect(validatePassword('é'.repeat(37))).not.toBeNull(); // 74 bytes
  });
});

describe('validateEventForm', () => {
  const valid: EventFormValues = {
    title: 'Jazz night',
    description: '',
    location: 'Main hall',
    startsAt: '2030-06-01T20:00',
    capacity: '100',
    price: '25.50',
    imageUrl: '',
    categoryId: 'abc',
  };

  it('accepts a valid form', () => {
    expect(validateEventForm(valid)).toEqual({});
  });

  it('reports every invalid field', () => {
    const errors = validateEventForm({
      ...valid,
      title: ' ',
      capacity: '0',
      price: '-1',
      imageUrl: 'ftp://x',
      categoryId: '',
      startsAt: '',
    });
    expect(Object.keys(errors).sort()).toEqual(['capacity', 'categoryId', 'imageUrl', 'price', 'startsAt', 'title']);
  });

  it('limits prices to two decimals and capacity to whole numbers', () => {
    expect(validateEventForm({ ...valid, price: '10.999' }).price).toBeDefined();
    expect(validateEventForm({ ...valid, capacity: '10.5' }).capacity).toBeDefined();
    expect(validateEventForm({ ...valid, price: '0' }).price).toBeUndefined();
  });
});

describe('date conversions', () => {
  it('adds seconds for the API and removes them for the input', () => {
    expect(toApiDateTime('2030-06-01T20:00')).toBe('2030-06-01T20:00:00');
    expect(toApiDateTime('2030-06-01T20:00:30')).toBe('2030-06-01T20:00:30');
    expect(toInputDateTime('2030-06-01T20:00:00')).toBe('2030-06-01T20:00');
  });
});
