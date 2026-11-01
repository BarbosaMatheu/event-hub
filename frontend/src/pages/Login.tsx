import { useEffect, useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { FormField } from '../components/FormField';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { errorMessage } from '../utils/errors';

export default function Login() {
  useDocumentTitle('Log in');
  const { login, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? '/';

  const [loginName, setLoginName] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Once authenticated (by this form or already), go where the user was heading.
  useEffect(() => {
    if (user) navigate(from, { replace: true });
  }, [user, from, navigate]);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    if (!loginName.trim() || !password) {
      setError('Enter your login and password.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await login(loginName.trim(), password);
    } catch (cause) {
      setError(errorMessage(cause));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="container-page flex justify-center py-12">
      <div className="card w-full max-w-md p-6 sm:p-8">
        <h1 className="text-2xl font-bold">Welcome back</h1>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">Log in to manage your tickets.</p>

        <form onSubmit={onSubmit} className="mt-6 space-y-4" noValidate>
          {error && (
            <div role="alert" className="rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-800 dark:bg-rose-950 dark:text-rose-200">
              {error}
            </div>
          )}

          <FormField label="Login" htmlFor="login">
            <input
              id="login"
              className="input"
              autoComplete="username"
              autoCapitalize="none"
              value={loginName}
              onChange={(e) => setLoginName(e.target.value)}
            />
          </FormField>

          <FormField label="Password" htmlFor="password">
            <div className="relative">
              <input
                id="password"
                className="input pr-16"
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <button
                type="button"
                onClick={() => setShowPassword((v) => !v)}
                className="absolute inset-y-0 right-2 my-auto h-7 rounded px-2 text-xs font-medium text-slate-500 hover:text-slate-800 dark:hover:text-slate-200"
              >
                {showPassword ? 'Hide' : 'Show'}
              </button>
            </div>
          </FormField>

          <button type="submit" disabled={submitting} className="btn btn-primary w-full py-2.5">
            {submitting ? 'Logging in…' : 'Log in'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-600 dark:text-slate-400">
          New here?{' '}
          <Link to="/register" state={{ from }} className="font-semibold text-indigo-600 hover:underline dark:text-indigo-400">
            Create an account
          </Link>
        </p>
      </div>
    </div>
  );
}
