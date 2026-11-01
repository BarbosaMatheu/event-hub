import { Suspense } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { Spinner } from '../../components/Spinner';
import { useDocumentTitle } from '../../hooks/useDocumentTitle';

const TABS = [
  { to: '/admin', label: 'Dashboard', end: true },
  { to: '/admin/events', label: 'Events', end: false },
  { to: '/admin/categories', label: 'Categories', end: false },
  { to: '/admin/orders', label: 'Orders', end: false },
];

export default function AdminLayout() {
  useDocumentTitle('Admin');
  return (
    <div className="container-page py-8">
      <h1 className="text-3xl font-bold">Admin</h1>

      <nav aria-label="Admin sections" className="mt-4 flex gap-1 overflow-x-auto border-b border-slate-200 dark:border-slate-800">
        {TABS.map((tab) => (
          <NavLink
            key={tab.to}
            to={tab.to}
            end={tab.end}
            className={({ isActive }) =>
              `-mb-px whitespace-nowrap border-b-2 px-4 py-2.5 text-sm font-medium transition ${
                isActive
                  ? 'border-indigo-600 text-indigo-700 dark:border-indigo-400 dark:text-indigo-300'
                  : 'border-transparent text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-100'
              }`
            }
          >
            {tab.label}
          </NavLink>
        ))}
      </nav>

      <div className="mt-6">
        <Suspense fallback={<Spinner />}>
          <Outlet />
        </Suspense>
      </div>
    </div>
  );
}
