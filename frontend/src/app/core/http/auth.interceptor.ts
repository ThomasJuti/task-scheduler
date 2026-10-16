import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { AuthService, LOGIN_URL } from '../auth/auth.service';

/** Adds the bearer token to API calls and ends the session when the API answers 401. */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const isApi = request.url.startsWith('/api/');
  const isLogin = request.url === LOGIN_URL;
  const token = auth.token;

  const authorized =
    isApi && !isLogin && token
      ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : request;

  return next(authorized).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        !isLogin &&
        auth.isLoggedIn()
      ) {
        auth.logout('expired');
      }
      return throwError(() => error);
    }),
  );
};
