import { useState, type ChangeEvent, type FormEvent } from 'react';
import { categoriesApi, eventsApi } from '../../api/endpoints';
import type { Category, EventDto } from '../../api/types';
import { AvailabilityBar } from '../../components/AvailabilityBar';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { EmptyState } from '../../components/EmptyState';
import { ErrorBanner } from '../../components/ErrorBanner';
import { FormField } from '../../components/FormField';
import { Modal } from '../../components/Modal';
import { Pagination } from '../../components/Pagination';
import { Spinner } from '../../components/Spinner';
import { useToast } from '../../components/Toast';
import { useAsync } from '../../hooks/useAsync';
import { errorMessage, fieldErrors } from '../../utils/errors';
import { formatDateTime, formatPrice } from '../../utils/format';
import {
  toApiDateTime,
  toInputDateTime,
  validateEventForm,
  type EventFormValues,
  type FormErrors,
} from '../../utils/validation';

const EMPTY_FORM: EventFormValues = {
  title: '',
  description: '',
  location: '',
  startsAt: '',
  capacity: '',
  price: '',
  imageUrl: '',
  categoryId: '',
};

function valuesFor(event: EventDto | null): EventFormValues {
  if (!event) return EMPTY_FORM;
  return {
    title: event.title,
    description: event.description ?? '',
    location: event.location,
    startsAt: toInputDateTime(event.startsAt),
    capacity: String(event.capacity),
    price: String(event.price),
    imageUrl: event.imageUrl ?? '',
    categoryId: event.category.id,
  };
}

interface EventFormModalProps {
  event: EventDto | null;
  categories: Category[];
  onClose: () => void;
  onSaved: () => void;
}

function EventFormModal({ event, categories, onClose, onSaved }: EventFormModalProps) {
  const toast = useToast();
  const [values, setValues] = useState<EventFormValues>(() => valuesFor(event));
  const [errors, setErrors] = useState<FormErrors<EventFormValues>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const bind =
    (field: keyof EventFormValues) =>
    (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) =>
      setValues((current) => ({ ...current, [field]: e.target.value }));

  const cls = (field: keyof EventFormValues) => `input ${errors[field] ? 'input-error' : ''}`;

  async function submit(e: FormEvent) {
    e.preventDefault();
    const found = validateEventForm(values);
    setErrors(found);
    setFormError(null);
    if (Object.keys(found).length > 0) return;

    const payload = {
      title: values.title.trim(),
      description: values.description,
      location: values.location.trim(),
      startsAt: toApiDateTime(values.startsAt),
      capacity: Number(values.capacity),
      price: Number(values.price),
      imageUrl: values.imageUrl.trim(),
      categoryId: values.categoryId,
    };

    setSaving(true);
    try {
      if (event) await eventsApi.update(event.id, payload);
      else await eventsApi.create(payload);
      toast.push(event ? 'Event updated.' : 'Event created.', 'success');
      onSaved();
    } catch (cause) {
      const server = fieldErrors(cause);
      if (Object.keys(server).length > 0) setErrors(server as FormErrors<EventFormValues>);
      else setFormError(errorMessage(cause));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      wide
      title={event ? 'Edit event' : 'New event'}
      onClose={onClose}
      footer={
        <>
          <button type="button" className="btn btn-secondary" onClick={onClose} disabled={saving}>
            Cancel
          </button>
          <button type="submit" form="event-form" className="btn btn-primary" disabled={saving}>
            {saving ? 'Saving…' : event ? 'Save changes' : 'Create event'}
          </button>
        </>
      }
    >
      <form id="event-form" onSubmit={submit} noValidate className="grid gap-4 sm:grid-cols-2">
        {formError && (
          <div role="alert" className="rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-800 dark:bg-rose-950 dark:text-rose-200 sm:col-span-2">
            {formError}
          </div>
        )}

        <div className="sm:col-span-2">
          <FormField label="Title" htmlFor="ev-title" error={errors.title}>
            <input id="ev-title" className={cls('title')} value={values.title} onChange={bind('title')} maxLength={150} />
          </FormField>
        </div>

        <div className="sm:col-span-2">
          <FormField label="Description" htmlFor="ev-description" error={errors.description} hint="Optional">
            <textarea
              id="ev-description"
              rows={4}
              className={cls('description')}
              value={values.description}
              onChange={bind('description')}
              maxLength={2000}
            />
          </FormField>
        </div>

        <FormField label="Location" htmlFor="ev-location" error={errors.location}>
          <input id="ev-location" className={cls('location')} value={values.location} onChange={bind('location')} maxLength={200} />
        </FormField>

        <FormField label="Starts at" htmlFor="ev-starts" error={errors.startsAt} hint="Local time at the venue">
          <input id="ev-starts" type="datetime-local" className={cls('startsAt')} value={values.startsAt} onChange={bind('startsAt')} />
        </FormField>

        <FormField label="Category" htmlFor="ev-category" error={errors.categoryId}>
          <select id="ev-category" className={cls('categoryId')} value={values.categoryId} onChange={bind('categoryId')}>
            <option value="">Select…</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </FormField>

        <div className="grid grid-cols-2 gap-4">
          <FormField label="Capacity" htmlFor="ev-capacity" error={errors.capacity}>
            <input id="ev-capacity" type="number" min="1" step="1" className={cls('capacity')} value={values.capacity} onChange={bind('capacity')} />
          </FormField>
          <FormField label="Price" htmlFor="ev-price" error={errors.price}>
            <input id="ev-price" type="number" min="0" step="0.01" className={cls('price')} value={values.price} onChange={bind('price')} />
          </FormField>
        </div>

        <div className="sm:col-span-2">
          <FormField label="Image URL" htmlFor="ev-image" error={errors.imageUrl} hint="Optional. A colourful gradient is used when empty.">
            <input id="ev-image" className={cls('imageUrl')} value={values.imageUrl} onChange={bind('imageUrl')} placeholder="https://…" />
          </FormField>
        </div>
      </form>
    </Modal>
  );
}

export default function AdminEvents() {
  const toast = useToast();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [submittedSearch, setSubmittedSearch] = useState('');
  const [editing, setEditing] = useState<EventDto | 'new' | null>(null);
  const [toDelete, setToDelete] = useState<EventDto | null>(null);
  const [deleting, setDeleting] = useState(false);

  const categories = useAsync((signal) => categoriesApi.list(signal), []);
  const events = useAsync(
    (signal) =>
      eventsApi.search({ search: submittedSearch, includePast: true, sort: 'startsAt,desc', page, size: 10 }, signal),
    [page, submittedSearch],
  );

  async function confirmDelete() {
    if (!toDelete) return;
    setDeleting(true);
    try {
      await eventsApi.remove(toDelete.id);
      toast.push('Event deleted.', 'success');
      events.reload();
    } catch (cause) {
      toast.push(errorMessage(cause), 'error');
    } finally {
      setDeleting(false);
      setToDelete(null);
    }
  }

  return (
    <div>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <form
          role="search"
          className="flex flex-1 gap-2 sm:max-w-sm"
          onSubmit={(e) => {
            e.preventDefault();
            setPage(0);
            setSubmittedSearch(search.trim());
          }}
        >
          <label htmlFor="admin-event-search" className="sr-only">
            Search events
          </label>
          <input
            id="admin-event-search"
            type="search"
            className="input"
            placeholder="Search events"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <button type="submit" className="btn btn-secondary">
            Search
          </button>
        </form>
        <button type="button" className="btn btn-primary" onClick={() => setEditing('new')} disabled={!categories.data?.length}>
          New event
        </button>
      </div>
      {categories.data && categories.data.length === 0 && (
        <p className="mt-2 text-sm text-amber-700 dark:text-amber-400">Create a category first: every event belongs to one.</p>
      )}

      <div className="mt-6">
        {events.error && <ErrorBanner error={events.error} onRetry={events.reload} />}
        {events.loading && !events.data && <Spinner label="Loading events" />}
        {events.data && events.data.content.length === 0 && <EmptyState title="No events found" />}

        {events.data && events.data.content.length > 0 && (
          <div className={`card overflow-x-auto ${events.loading ? 'opacity-60' : ''}`}>
            <table className="w-full min-w-[720px] text-left text-sm">
              <thead className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500 dark:border-slate-800 dark:text-slate-400">
                <tr>
                  <th scope="col" className="px-4 py-3 font-semibold">Event</th>
                  <th scope="col" className="px-4 py-3 font-semibold">When</th>
                  <th scope="col" className="px-4 py-3 font-semibold">Sold</th>
                  <th scope="col" className="px-4 py-3 font-semibold">Price</th>
                  <th scope="col" className="px-4 py-3 text-right font-semibold">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {events.data.content.map((event) => {
                  const past = new Date(event.startsAt).getTime() < Date.now();
                  return (
                    <tr key={event.id}>
                      <td className="px-4 py-3">
                        <p className="font-medium">{event.title}</p>
                        <p className="text-xs text-slate-500 dark:text-slate-400">{event.category.name}</p>
                      </td>
                      <td className="whitespace-nowrap px-4 py-3">
                        {formatDateTime(event.startsAt)}
                        {past && <span className="chip ml-2 bg-slate-200 text-slate-700 dark:bg-slate-800 dark:text-slate-300">Past</span>}
                      </td>
                      <td className="w-40 px-4 py-3">
                        <AvailabilityBar sold={event.ticketsSold} capacity={event.capacity} />
                      </td>
                      <td className="whitespace-nowrap px-4 py-3">{formatPrice(event.price)}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-right">
                        <button type="button" className="btn btn-ghost btn-sm" onClick={() => setEditing(event)}>
                          Edit
                        </button>
                        <button
                          type="button"
                          className="btn btn-ghost btn-sm text-rose-600 dark:text-rose-400"
                          onClick={() => setToDelete(event)}
                        >
                          Delete
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {events.data && <Pagination page={events.data.page} totalPages={events.data.totalPages} onChange={setPage} />}
      </div>

      {editing && categories.data && (
        <EventFormModal
          event={editing === 'new' ? null : editing}
          categories={categories.data}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null);
            events.reload();
          }}
        />
      )}

      {toDelete && (
        <ConfirmDialog
          title="Delete this event?"
          message={`“${toDelete.title}” will be permanently removed. Events that already have orders cannot be deleted.`}
          confirmLabel="Delete event"
          danger
          busy={deleting}
          onCancel={() => setToDelete(null)}
          onConfirm={confirmDelete}
        />
      )}
    </div>
  );
}
