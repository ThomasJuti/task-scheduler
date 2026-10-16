import { Directive, TemplateRef, ViewContainerRef, effect, inject, input } from '@angular/core';

import { Role } from '../models';
import { AuthService } from './auth.service';

/** Renders the element only for the given role, e.g. {@code <button *appHasRole="'ADMIN'">}. */
@Directive({ selector: '[appHasRole]' })
export class HasRoleDirective {
  readonly appHasRole = input.required<Role>();

  private readonly auth = inject(AuthService);
  private readonly template = inject(TemplateRef<unknown>);
  private readonly container = inject(ViewContainerRef);
  private rendered = false;

  constructor() {
    effect(() => {
      const allowed = this.auth.user()?.role === this.appHasRole() || this.auth.isAdmin();
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
