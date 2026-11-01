import { ApiError } from '../api/client';

/** A message that is safe and useful to show to the user for any thrown value. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError || error instanceof Error) return error.message;
  return 'Something went wrong. Please try again.';
}

/** Server-side field errors ({field: message}) from a failed request, if any. */
export function fieldErrors(error: unknown): Record<string, string> {
  if (!(error instanceof ApiError)) return {};
  const result: Record<string, string> = {};
  for (const violation of error.violations) result[violation.field] = violation.message;
  return result;
}
