import { Link } from 'react-router-dom';
import { useDocumentTitle } from '../hooks/useDocumentTitle';

export default function NotFound() {
  useDocumentTitle('Page not found');
  return (
    <div className="container-page py-24 text-center">
      <p className="text-sm font-semibold text-indigo-600">404</p>
      <h1 className="mt-2 text-3xl font-bold">We could not find that page</h1>
      <p className="mt-3 text-slate-600 dark:text-slate-400">
        The page may have been moved, or the event no longer exists.
      </p>
      <Link to="/events" className="btn btn-primary mt-6">
        Browse events
      </Link>
    </div>
  );
}
