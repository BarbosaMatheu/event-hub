import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { configureClient } from '../api/client';
import { authApi } from '../api/endpoints';
import type { User } from '../api/types';
import { useToast } from '../components/Toast';
import { isTokenExpired, tokenExpiryMs } from '../utils/token';

const STORAGE_KEY = 'eventhub.auth';

interface Session {
  token: string;
  user: User;
}

// The token lives in localStorage so a refresh keeps you logged in. Trade-off: any XSS could read
// it. This app renders no raw HTML and sends no cookies; a production system handling money would
// prefer an httpOnly cookie plus CSRF protection.
function loadSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<Session>;
    if (!parsed.token || !parsed.user || isTokenExpired(parsed.token)) {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
    return { token: parsed.token, user: parsed.user };
  } catch {
    return null;
  }
}

function saveSession(session: Session | null): void {
  try {
    if (session) localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    else localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Storage can be unavailable (private mode, quota): the session just won't survive a reload.
  }
}

interface AuthContextValue {
  user: User | null;
  isAdmin: boolean;
  login: (login: string, password: string) => Promise<void>;
  register: (login: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(loadSession);
  const sessionRef = useRef(session);
  sessionRef.current = session;
  const toast = useToast();

  const logout = useCallback(() => {
    saveSession(null);
    setSession(null);
  }, []);

  // A layout effect runs before any child's useEffect, so the very first requests already
  // know how to read the token.
  useLayoutEffect(() => {
    configureClient({
      getToken: () => sessionRef.current?.token ?? null,
      onUnauthorized: () => {
        if (!sessionRef.current) return;
        logout();
        toast.push('Your session has expired. Please log in again.', 'info');
      },
    });
  }, [logout, toast]);

  // Log out exactly when the token expires.
  useEffect(() => {
    if (!session) return;
    const expiry = tokenExpiryMs(session.token);
    if (expiry === null) return;
    const delay = Math.min(Math.max(expiry - Date.now(), 0), 2_147_000_000);
    const id = window.setTimeout(() => {
      logout();
      toast.push('Your session has expired. Please log in again.', 'info');
    }, delay);
    return () => window.clearTimeout(id);
  }, [session, logout, toast]);

  // Keep several tabs in sync: logging out (or in) in one tab is reflected in the others.
  useEffect(() => {
    const onStorage = (event: StorageEvent) => {
      if (event.key === STORAGE_KEY) setSession(loadSession());
    };
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, []);

  const login = useCallback(async (loginName: string, password: string) => {
    const response = await authApi.login(loginName, password);
    const next: Session = { token: response.token, user: response.user };
    saveSession(next);
    setSession(next);
  }, []);

  const register = useCallback(
    async (loginName: string, password: string) => {
      await authApi.register(loginName, password);
      await login(loginName, password);
    },
    [login],
  );

  const value = useMemo<AuthContextValue>(
    () => ({
      user: session?.user ?? null,
      isAdmin: session?.user.role === 'ADMIN',
      login,
      register,
      logout,
    }),
    [session, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>');
  return context;
}
