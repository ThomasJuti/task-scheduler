import { WritableSignal } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';

import { applyServerErrors } from '../core/http/problem';

/**
 * Flujo de envío compartido por los diálogos de edición: un formulario inválido solo muestra sus errores; si no,
 * se envía la petición y el diálogo se cierra con la entidad guardada, o el formulario se reactiva con los errores del backend.
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
