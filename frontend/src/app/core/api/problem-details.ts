import { HttpErrorResponse } from '@angular/common/http';
import { ProblemDetails } from './api.models';


export function problemFromError(error: unknown): ProblemDetails | null {
  if (!(error instanceof HttpErrorResponse)) {
    return null;
  }

  const body: unknown = error.error;
  if (!body || typeof body !== 'object') {
    return { status: error.status };
  }

  const record = body as Record<string, unknown>;
  const text = (value: unknown): string | undefined =>
    typeof value === 'string' && value.length > 0 ? value : undefined;

  const problem: ProblemDetails = {
    type: text(record['type']),
    title: text(record['title']),
    status: typeof record['status'] === 'number' ? record['status'] : error.status,
    detail: text(record['detail']),
    correlationId: text(record['correlationId']),
  };

  return problem;
}

/** Short user-facing description of an HTTP failure for the error states. */
export function describeError(error: unknown): string {
  const problem = problemFromError(error);
  if (problem?.detail) {
    return problem.detail;
  }
  if (problem?.title) {
    return problem.title;
  }
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'The API could not be reached.';
    }
    return `${error.status} ${error.statusText || 'Request failed'}`.trim();
  }
  return 'An unexpected error occurred.';
}
