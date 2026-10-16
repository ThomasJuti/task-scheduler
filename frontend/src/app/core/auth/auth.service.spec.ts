import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { authInterceptor } from '../http/auth.interceptor';
import { AuthService } from './auth.service';

describe('AuthService + authInterceptor', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => http.verify());

  function loginAs(role: 'ADMIN' | 'USER'): void {
    auth.login('ana', 'secret').subscribe();
    http.expectOne('/api/auth/login').flush({
      token: 'jwt-token', username: 'ana', role, expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
    });
  }

  it('stores the session and exposes the role', () => {
    loginAs('USER');

    expect(auth.isLoggedIn()).toBe(true);
    expect(auth.isAdmin()).toBe(false);
    expect(JSON.parse(sessionStorage.getItem('scheduler.session')!).username).toBe('ana');
  });

  it('adds the bearer token to API calls but not to the login call', () => {
    auth.login('ana', 'secret').subscribe();
    const loginRequest = http.expectOne('/api/auth/login');
    expect(loginRequest.request.headers.has('Authorization')).toBe(false);
    loginRequest.flush({ token: 'jwt-token', username: 'ana', role: 'USER', expiresAt: new Date(Date.now() + 60_000).toISOString() });

    TestBed.inject(HttpClient).get('/api/tasks').subscribe();
    expect(http.expectOne('/api/tasks').request.headers.get('Authorization')).toBe('Bearer jwt-token');
  });

  it('logs out when the API answers 401', () => {
    loginAs('ADMIN');
    expect(auth.isAdmin()).toBe(true);

    TestBed.inject(HttpClient).get('/api/tasks').subscribe({ error: () => undefined });
    http.expectOne('/api/tasks').flush({ status: 401 }, { status: 401, statusText: 'Unauthorized' });

    expect(auth.isLoggedIn()).toBe(false);
    expect(sessionStorage.getItem('scheduler.session')).toBeNull();
  });
});
