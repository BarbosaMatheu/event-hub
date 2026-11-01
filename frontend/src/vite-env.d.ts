/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Base URL of the API. Defaults to "/api/v1" (same origin; proxied by Vite in dev, nginx in production). */
  readonly VITE_API_URL?: string;
  /** ISO 4217 currency code used to display prices. Defaults to EUR. */
  readonly VITE_CURRENCY?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
