import type { ReactNode } from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';

interface ProtectedRouteProps {
  children: ReactNode;
  /** When set to "ADMIN", regular users get a 403 page instead of the content. */
  role?: 'ADMIN';
}

export function ProtectedRoute({ children, role }: ProtectedRouteProps) {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) {
    // Remember where the user was going, so login can send them back there.
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  }

  if (role === 'ADMIN' && user.role !== 'ADMIN') {
    return (
      <div className="container-page py-24 text-center">
        <p className="text-sm font-semibold text-indigo-600">403</p>
        <h1 className="mt-2 text-3xl font-bold">You don&apos;t have access to this page</h1>
        <p className="mt-3 text-slate-600 dark:text-slate-400">This area is only available to administrators.</p>
        <Link to="/" className="btn btn-primary mt-6">
          Back to home
        </Link>
      </div>
    );
  }

  return <>{children}</>;
}
