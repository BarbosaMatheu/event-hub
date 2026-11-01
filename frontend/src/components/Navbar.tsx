import { useState } from 'react';
import { Link, NavLink } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { ThemeToggle } from './ThemeToggle';

function linkClass({ isActive }: { isActive: boolean }): string {
  return `rounded-lg px-3 py-2 text-sm font-medium transition ${
    isActive
      ? 'bg-indigo-50 text-indigo-700 dark:bg-indigo-950 dark:text-indigo-300'
      : 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800'
  }`;
}

export function Navbar() {
  const { user, isAdmin, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const close = () => setOpen(false);

  return (
    <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/90 backdrop-blur dark:border-slate-800 dark:bg-slate-950/90">
      <div className="container-page flex h-16 items-center justify-between">
        <Link to="/" onClick={close} className="flex items-center gap-2 text-lg font-bold">
          <img src="/favicon.svg" alt="" className="h-7 w-7" />
          EventHub
        </Link>

        <nav aria-label="Main" className="hidden items-center gap-1 md:flex">
          <NavLink to="/events" className={linkClass}>
            Events
          </NavLink>
          {user && (
            <NavLink to="/orders" className={linkClass}>
              My orders
            </NavLink>
          )}
          {isAdmin && (
            <NavLink to="/admin" className={linkClass}>
              Admin
            </NavLink>
          )}
        </nav>

        <div className="hidden items-center gap-2 md:flex">
          <ThemeToggle />
          {user ? (
            <>
              <span className="max-w-[10rem] truncate text-sm text-slate-600 dark:text-slate-300" title={user.login}>
                {user.login}
              </span>
              <button type="button" onClick={logout} className="btn btn-secondary btn-sm">
                Log out
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn btn-ghost btn-sm">
                Log in
              </Link>
              <Link to="/register" className="btn btn-primary btn-sm">
                Sign up
              </Link>
            </>
          )}
        </div>

        <div className="flex items-center gap-1 md:hidden">
          <ThemeToggle />
          <button
            type="button"
            className="btn btn-ghost px-2.5"
            aria-label="Toggle menu"
            aria-expanded={open}
            aria-controls="mobile-menu"
            onClick={() => setOpen((value) => !value)}
          >
            <svg className="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
              {open ? (
                <path strokeLinecap="round" d="M6 6l12 12M18 6L6 18" />
              ) : (
                <path strokeLinecap="round" d="M4 7h16M4 12h16M4 17h16" />
              )}
            </svg>
          </button>
        </div>
      </div>

      {open && (
        <nav id="mobile-menu" aria-label="Mobile" className="border-t border-slate-200 px-4 py-3 dark:border-slate-800 md:hidden">
          <div className="flex flex-col gap-1">
            <NavLink to="/events" className={linkClass} onClick={close}>
              Events
            </NavLink>
            {user && (
              <NavLink to="/orders" className={linkClass} onClick={close}>
                My orders
              </NavLink>
            )}
            {isAdmin && (
              <NavLink to="/admin" className={linkClass} onClick={close}>
                Admin
              </NavLink>
            )}
            <div className="mt-2 border-t border-slate-200 pt-3 dark:border-slate-800">
              {user ? (
                <div className="flex items-center justify-between gap-3">
                  <span className="truncate text-sm text-slate-600 dark:text-slate-300">{user.login}</span>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => {
                      close();
                      logout();
                    }}
                  >
                    Log out
                  </button>
                </div>
              ) : (
                <div className="flex gap-2">
                  <Link to="/login" onClick={close} className="btn btn-secondary flex-1">
                    Log in
                  </Link>
                  <Link to="/register" onClick={close} className="btn btn-primary flex-1">
                    Sign up
                  </Link>
                </div>
              )}
            </div>
          </div>
        </nav>
      )}
    </header>
  );
}
