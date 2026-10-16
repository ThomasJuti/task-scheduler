import { Directive, TemplateRef, ViewContainerRef, effect, inject, input } from '@angular/core';

import { Role } from '../models';
import { AuthService } from './auth.service';

/**
 * Renders the element only when the current user has at least the given role, e.g.
 * {@code <button *appRequiresRole="'ADMIN'">}. Roles are hierarchical: ADMIN includes every USER
 * permission, so {@code *appRequiresRole="'USER'"} is also shown to admins.
 */
@Directive({ selector: '[appRequiresRole]' })
export class RequiresRoleDirective {
  readonly appRequiresRole = input.required<Role>();

  private readonly auth = inject(AuthService);
  private readonly template = inject(TemplateRef<unknown>);
  private readonly container = inject(ViewContainerRef);
  private rendered = false;

  constructor() {
    effect(() => {
      const role = this.auth.user()?.role;
      const allowed = role === 'ADMIN' || role === this.appRequiresRole();
      if (allowed && !this.rendered) {
        this.container.createEmbeddedView(this.template);
        this.rendered = true;
      } else if (!allowed && this.rendered) {
        this.container.clear();
        this.rendered = false;
      }
    });
  }
}
