import { useState, type FormEvent } from 'react';
import { categoriesApi } from '../../api/endpoints';
import type { Category } from '../../api/types';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { EmptyState } from '../../components/EmptyState';
import { ErrorBanner } from '../../components/ErrorBanner';
import { Spinner } from '../../components/Spinner';
import { useToast } from '../../components/Toast';
import { useAsync } from '../../hooks/useAsync';
import { errorMessage } from '../../utils/errors';

export default function AdminCategories() {
  const toast = useToast();
  const { data, error, loading, reload } = useAsync((signal) => categoriesApi.list(signal), []);

  const [newName, setNewName] = useState('');
  const [adding, setAdding] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editingName, setEditingName] = useState('');
  const [toDelete, setToDelete] = useState<Category | null>(null);
  const [busy, setBusy] = useState(false);

  async function add(e: FormEvent) {
    e.preventDefault();
    const name = newName.trim();
    if (!name) return;
    setAdding(true);
    try {
      await categoriesApi.create(name);
      setNewName('');
      toast.push('Category created.', 'success');
      reload();
    } catch (cause) {
      toast.push(errorMessage(cause), 'error');
    } finally {
      setAdding(false);
    }
  }

  async function saveRename(category: Category) {
    const name = editingName.trim();
    if (!name || name === category.name) {
      setEditingId(null);
      return;
    }
    setBusy(true);
    try {
      await categoriesApi.update(category.id, name);
      toast.push('Category renamed.', 'success');
      setEditingId(null);
      reload();
    } catch (cause) {
      toast.push(errorMessage(cause), 'error');
    } finally {
      setBusy(false);
    }
  }

  async function confirmDelete() {
    if (!toDelete) return;
    setBusy(true);
    try {
      await categoriesApi.remove(toDelete.id);
      toast.push('Category deleted.', 'success');
      reload();
    } catch (cause) {
      toast.push(errorMessage(cause), 'error');
    } finally {
      setBusy(false);
      setToDelete(null);
    }
  }

  return (
    <div className="max-w-2xl">
      <form onSubmit={add} className="flex gap-2">
        <label htmlFor="new-category" className="sr-only">
          New category name
        </label>
        <input
          id="new-category"
          className="input"
          placeholder="New category name"
          maxLength={100}
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
        />
        <button type="submit" className="btn btn-primary shrink-0" disabled={adding || !newName.trim()}>
          {adding ? 'Adding…' : 'Add category'}
        </button>
      </form>

      <div className="mt-6">
        {error && <ErrorBanner error={error} onRetry={reload} />}
        {loading && !data && <Spinner label="Loading categories" />}
        {data && data.length === 0 && (
          <EmptyState title="No categories yet" description="Create your first category to start adding events." />
        )}

        {data && data.length > 0 && (
          <ul className="card divide-y divide-slate-100 dark:divide-slate-800">
            {data.map((category) => (
              <li key={category.id} className="flex items-center justify-between gap-3 px-4 py-3">
                {editingId === category.id ? (
                  <form
                    className="flex flex-1 gap-2"
                    onSubmit={(e) => {
                      e.preventDefault();
                      void saveRename(category);
                    }}
                  >
                    <label htmlFor={`rename-${category.id}`} className="sr-only">
                      Category name
                    </label>
                    <input
                      id={`rename-${category.id}`}
                      className="input py-1.5"
                      value={editingName}
                      maxLength={100}
                      autoFocus
                      onChange={(e) => setEditingName(e.target.value)}
                    />
                    <button type="submit" className="btn btn-primary btn-sm" disabled={busy}>
                      Save
                    </button>
                    <button type="button" className="btn btn-secondary btn-sm" onClick={() => setEditingId(null)}>
                      Cancel
                    </button>
                  </form>
                ) : (
                  <>
                    <span className="font-medium">{category.name}</span>
                    <div className="flex gap-1">
                      <button
                        type="button"
                        className="btn btn-ghost btn-sm"
                        onClick={() => {
                          setEditingId(category.id);
                          setEditingName(category.name);
                        }}
                      >
                        Rename
                      </button>
                      <button
                        type="button"
                        className="btn btn-ghost btn-sm text-rose-600 dark:text-rose-400"
                        onClick={() => setToDelete(category)}
                      >
                        Delete
                      </button>
                    </div>
                  </>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>

      {toDelete && (
        <ConfirmDialog
          title="Delete this category?"
          message={`“${toDelete.name}” will be removed. Categories that still have events cannot be deleted.`}
          confirmLabel="Delete category"
          danger
          busy={busy}
          onCancel={() => setToDelete(null)}
          onConfirm={confirmDelete}
        />
      )}
    </div>
  );
}
