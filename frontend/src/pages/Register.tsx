import { useEffect, useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { FormField } from '../components/FormField';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { errorMessage, fieldErrors } from '../utils/errors';
import { validateLogin, validatePassword } from '../utils/validation';

type Errors = { login?: string; password?: string; confirm?: string; form?: string };

export default function Register() {
  useDocumentTitle('Create account');
  const { register, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? '/';

  const [loginName, setLoginName] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [errors, setErrors] = useState<Errors>({});
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (user) navigate(from, { replace: true });
  }, [user, from, navigate]);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();

    const next: Errors = {};
    const loginError = validateLogin(loginName);
    const passwordError = validatePassword(password);
    if (loginError) next.login = loginError;
    if (passwordError) next.password = passwordError;
    if (!passwordError && confirm !== password) next.confirm = 'Passwords do not match';
    setErrors(next);
    if (Object.keys(next).length > 0) return;

    setSubmitting(true);
    try {
      await register(loginName, password);
    } catch (cause) {
      const server = fieldErrors(cause);
      setErrors({
        login: server.login,
        password: server.password,
        // A 409 (login taken) has no field list, so show it under the login field.
        ...(Object.keys(server).length === 0 ? { login: errorMessage(cause) } : {}),
      });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="container-page flex justify-center py-12">
      <div className="card w-full max-w-md p-6 sm:p-8">
        <h1 className="text-2xl font-bold">Create your account</h1>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">It takes less than a minute.</p>

        <form onSubmit={onSubmit} className="mt-6 space-y-4" noValidate>
          <FormField label="Login" htmlFor="reg-login" error={errors.login} hint="Letters, digits, '.', '_' or '-' (3 to 50 characters)">
            <input
              id="reg-login"
              className={`input ${errors.login ? 'input-error' : ''}`}
              autoComplete="username"
              autoCapitalize="none"
              aria-invalid={Boolean(errors.login)}
              aria-describedby={errors.login ? 'reg-login-error' : undefined}
              value={loginName}
              onChange={(e) => setLoginName(e.target.value)}
            />
          </FormField>

          <FormField label="Password" htmlFor="reg-password" error={errors.password} hint="At least 8 characters">
            <input
              id="reg-password"
              className={`input ${errors.password ? 'input-error' : ''}`}
              type="password"
              autoComplete="new-password"
              aria-invalid={Boolean(errors.password)}
              aria-describedby={errors.password ? 'reg-password-error' : undefined}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </FormField>

          <FormField label="Confirm password" htmlFor="reg-confirm" error={errors.confirm}>
            <input
              id="reg-confirm"
              className={`input ${errors.confirm ? 'input-error' : ''}`}
              type="password"
              autoComplete="new-password"
              aria-invalid={Boolean(errors.confirm)}
              aria-describedby={errors.confirm ? 'reg-confirm-error' : undefined}
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
            />
          </FormField>

          <button type="submit" disabled={submitting} className="btn btn-primary w-full py-2.5">
            {submitting ? 'Creating account…' : 'Create account'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-600 dark:text-slate-400">
          Already registered?{' '}
          <Link to="/login" state={{ from }} className="font-semibold text-indigo-600 hover:underline dark:text-indigo-400">
            Log in
          </Link>
        </p>
      </div>
    </div>
  );
}
