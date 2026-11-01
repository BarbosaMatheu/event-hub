// Client-side checks mirror the server's Bean Validation rules so users get instant feedback.
// The server remains the source of truth: its field errors are shown too.

export function validateLogin(value: string): string | null {
  if (!value.trim()) return 'Login is required';
  if (value.length < 3 || value.length > 50) return 'Login must be between 3 and 50 characters';
  if (!/^[A-Za-z0-9._-]+$/.test(value)) return "Only letters, digits, '.', '_' and '-' are allowed";
  return null;
}

export function validatePassword(value: string): string | null {
  if (!value) return 'Password is required';
  if (value.length < 8) return 'Password must have at least 8 characters';
  // BCrypt only uses the first 72 bytes, so the server rejects anything longer.
  if (new TextEncoder().encode(value).length > 72) return 'Password is too long (72 bytes at most)';
  return null;
}

export interface EventFormValues {
  title: string;
  description: string;
  location: string;
  /** Value of an <input type="datetime-local">, e.g. "2030-06-01T20:00". */
  startsAt: string;
  capacity: string;
  price: string;
  imageUrl: string;
  categoryId: string;
}

export type FormErrors<T> = Partial<Record<keyof T, string>>;

export function validateEventForm(values: EventFormValues): FormErrors<EventFormValues> {
  const errors: FormErrors<EventFormValues> = {};

  if (!values.title.trim()) errors.title = 'Title is required';
  else if (values.title.trim().length > 150) errors.title = 'Title must have at most 150 characters';

  if (values.description.length > 2000) errors.description = 'Description must have at most 2000 characters';

  if (!values.location.trim()) errors.location = 'Location is required';
  else if (values.location.trim().length > 200) errors.location = 'Location must have at most 200 characters';

  if (!values.startsAt) errors.startsAt = 'Start date and time are required';

  const capacity = Number(values.capacity);
  if (values.capacity.trim() === '' || !Number.isInteger(capacity) || capacity < 1 || capacity > 100000) {
    errors.capacity = 'Capacity must be a whole number between 1 and 100000';
  }

  const price = Number(values.price);
  if (values.price.trim() === '' || Number.isNaN(price) || price < 0) {
    errors.price = 'Price must be zero or more';
  } else if (!/^\d+(\.\d{1,2})?$/.test(values.price.trim())) {
    errors.price = 'Price can have at most 2 decimal places';
  }

  if (values.imageUrl.trim() && !/^https?:\/\/\S+$/.test(values.imageUrl.trim())) {
    errors.imageUrl = 'Image must be an http(s) URL';
  }

  if (!values.categoryId) errors.categoryId = 'Choose a category';

  return errors;
}

/** "2030-06-01T20:00" (datetime-local) -> "2030-06-01T20:00:00" (what the API expects). */
export function toApiDateTime(local: string): string {
  return local.length === 16 ? `${local}:00` : local;
}

/** "2030-06-01T20:00:00" (API) -> "2030-06-01T20:00" (datetime-local). */
export function toInputDateTime(iso: string): string {
  return iso.slice(0, 16);
}
