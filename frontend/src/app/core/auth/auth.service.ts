import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { LoginResponse, Role } from '../models';

const STORAGE_KEY = 'scheduler.session';

/** La única llamada pública de la API: no debe llevar token y maneja sus propios errores. */
export const LOGIN_URL = '/api/auth/login';

export interface Session {
  token: string;
  username: string;
  role: Role;
  expiresAt: string;
}

/**
 * Guarda la sesión JWT. Vive en sessionStorage (se borra al cerrar la pestaña) y se descarta en cuanto el token
 * expira. El backend aplica todos los permisos; el rol aquí solo adapta la interfaz.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly session = signal<Session | null>(this.restore());
  private expiryTimer?: ReturnType<typeof setTimeout>;

  readonly user = this.session.asReadonly();
  readonly isLoggedIn = computed(() => this.session() !== null);
  readonly isAdmin = computed(() => this.session()?.role === 'ADMIN');

  constructor() {
    const current = this.session();
    if (current) this.scheduleExpiry(current);
  }

  get token(): string | null {
    return this.session()?.token ?? null;
  }

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(LOGIN_URL, { username, password }).pipe(
      tap((response) => {
        const session: Session = { ...response };
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
        this.session.set(session);
        this.scheduleExpiry(session);
      }),
    );
  }

  logout(reason?: 'expired'): void {
    clearTimeout(this.expiryTimer);
    sessionStorage.removeItem(STORAGE_KEY);
    this.session.set(null);
    void this.router.navigate(['/login'], reason ? { queryParams: { reason } } : undefined);
  }

  private scheduleExpiry(session: Session): void {
    clearTimeout(this.expiryTimer);
    const ms = new Date(session.expiresAt).getTime() - Date.now();
    this.expiryTimer = setTimeout(() => this.logout('expired'), Math.max(ms, 0));
  }

  private restore(): Session | null {
    try {
      const raw = sessionStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      const session = JSON.parse(raw) as Session;
      if (!session.token || new Date(session.expiresAt).getTime() <= Date.now()) {
        sessionStorage.removeItem(STORAGE_KEY);
        return null;
      }
      return session;
    } catch {
      return null;
    }
  }
}
