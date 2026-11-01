import { Component, type ErrorInfo, type ReactNode } from 'react';

interface State {
  error: Error | null;
}

/** Last line of defence: a rendering bug shows a friendly page instead of a blank screen. */
export class ErrorBoundary extends Component<{ children: ReactNode }, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    console.error('Unhandled UI error', error, info.componentStack);
  }

  render() {
    if (this.state.error) {
      return (
        <div className="container-page py-24 text-center">
          <h1 className="text-3xl font-bold">Something went wrong</h1>
          <p className="mt-3 text-slate-600 dark:text-slate-400">An unexpected error occurred. Reloading the page usually fixes it.</p>
          <button type="button" className="btn btn-primary mt-6" onClick={() => window.location.assign('/')}>
            Back to home
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}
