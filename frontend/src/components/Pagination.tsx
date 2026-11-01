import { pageWindow } from '../utils/pagination';

interface PaginationProps {
  /** 0-based current page. */
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}

export function Pagination({ page, totalPages, onChange }: PaginationProps) {
  if (totalPages <= 1) return null;

  return (
    <nav aria-label="Pagination" className="mt-8 flex items-center justify-center gap-1">
      <button type="button" className="btn btn-secondary btn-sm" disabled={page === 0} onClick={() => onChange(page - 1)}>
        Previous
      </button>

      <ul className="hidden items-center gap-1 sm:flex">
        {pageWindow(page, totalPages).map((p, index) =>
          p === -1 ? (
            <li key={`gap-${index}`} className="px-1 text-slate-400" aria-hidden="true">
              …
            </li>
          ) : (
            <li key={p}>
              <button
                type="button"
                onClick={() => onChange(p)}
                aria-current={p === page ? 'page' : undefined}
                className={`btn btn-sm min-w-[2.25rem] ${p === page ? 'btn-primary' : 'btn-ghost'}`}
              >
                {p + 1}
              </button>
            </li>
          ),
        )}
      </ul>

      <span className="px-3 text-sm text-slate-600 dark:text-slate-400 sm:hidden">
        {page + 1} / {totalPages}
      </span>

      <button
        type="button"
        className="btn btn-secondary btn-sm"
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
      >
        Next
      </button>
    </nav>
  );
}
