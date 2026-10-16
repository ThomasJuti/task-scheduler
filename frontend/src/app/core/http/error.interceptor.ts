import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';

import { LOGIN_URL } from '../auth/auth.service';
import { toProblem } from './problem';

/**
 * Shows API errors in a snackbar. Field validation errors (400 with an {@code errors} map) and the login
 * call are left to the component, which displays them next to the inputs.
 */
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
