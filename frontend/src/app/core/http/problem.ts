import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormGroup } from '@angular/forms';

import { Problem } from '../models';

export function toProblem(error: unknown): Problem {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return { status: 0, detail: 'Cannot reach the server. Is the backend running?' };
    }
    const body = error.error as Partial<Problem> | null;
    if (body && typeof body === 'object') {
      return { status: error.status, title: body.title, detail: body.detail, errors: body.errors };
    }
    return { status: error.status, detail: error.statusText || 'Unexpected error' };
  }
  return { status: -1, detail: 'Unexpected error' };
}

export function problemMessage(error: unknown): string {
  return toProblem(error).detail ?? 'Unexpected error';
}

export function applyServerErrors(form: FormGroup, error: unknown): boolean {
  const errors = toProblem(error).errors ?? {};
  let matched = false;
  for (const [path, message] of Object.entries(errors)) {
    const field = path.split(/[.[]/)[0];
    const control: AbstractControl | null = form.get(field);
    if (control) {
      control.setErrors({ ...(control.errors ?? {}), server: message });
      control.markAsTouched();
      matched = true;
    }
  }
  return matched;
}
