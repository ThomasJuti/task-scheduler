import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { Role } from '../models';
import { AuthService } from './auth.service';
import { RequiresRoleDirective } from './requires-role.directive';

@Component({
  imports: [RequiresRoleDirective],
  template: `
    <span *appRequiresRole="'ADMIN'">admin-only</span>
    <span *appRequiresRole="'USER'">user-or-admin</span>
  `,
})
class Host {}

describe('RequiresRoleDirective', () => {
  const user = signal<{ role: Role } | null>(null);

  function render(role: Role | null): string {
    user.set(role ? { role } : null);
    const fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [{ provide: AuthService, useValue: { user } }] });
  });

  it('shows everything to an ADMIN, since ADMIN includes the USER permissions', () => {
    const text = render('ADMIN');
    expect(text).toContain('admin-only');
    expect(text).toContain('user-or-admin');
  });

  it('hides ADMIN content from a USER', () => {
    const text = render('USER');
    expect(text).not.toContain('admin-only');
    expect(text).toContain('user-or-admin');
  });

  it('shows nothing without a session', () => {
    expect(render(null).trim()).toBe('');
  });
});
