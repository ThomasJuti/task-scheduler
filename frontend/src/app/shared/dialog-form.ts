import { WritableSignal } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';

import { applyServerErrors } from '../core/http/problem';

/**
 * Submit flow shared by the edit dialogs: an invalid form only shows its errors; otherwise the request is
 * sent and the dialog closes with the saved entity, or the form is re-enabled with the backend field errors.
 */
export function submitDialogForm<T>(
  form: FormGroup,
  request: () => Observable<T>,
  dialogRef: { close(result: T): void },
  saving: WritableSignal<boolean>,
): void {
  if (form.invalid) {
    form.markAllAsTouched();
    return;
  }
  saving.set(true);
  request().subscribe({
    next: (saved) => dialogRef.close(saved),
    error: (err: unknown) => {
      saving.set(false);
      applyServerErrors(form, err);
    },
  });
}
