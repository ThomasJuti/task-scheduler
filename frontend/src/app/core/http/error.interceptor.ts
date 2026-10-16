import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';

import { LOGIN_URL } from '../auth/auth.service';
import { toProblem } from './problem';

/** Muestra los errores de la API en un snackbar, salvo login y errores de validación de campo, que los componentes muestran junto a los inputs. */
export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const snackBar = inject(MatSnackBar);
  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && request.url !== LOGIN_URL) {
        const problem = toProblem(error);
        const fieldErrors =
          problem.status === 400 && problem.errors && Object.keys(problem.errors).length > 0;
        if (!fieldErrors && problem.status !== 401) {
          snackBar.open(problem.detail ?? 'Unexpected error', 'Close', {
            duration: 6000,
            panelClass: 'snack-error',
          });
        }
      }
      return throwError(() => error);
    }),
  );
};
