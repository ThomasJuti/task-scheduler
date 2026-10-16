import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';

import { AuthService } from '../core/auth/auth.service';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatToolbarModule, MatButtonModule, MatIconModule, MatMenuModule, MatTooltipModule],
  template: `
    <mat-toolbar class="topbar">
      <span class="brand"><mat-icon>event_available</mat-icon> Task Scheduler</span>
      <nav class="nav">
        <a mat-button routerLink="/tasks" routerLinkActive="active"><mat-icon>checklist</mat-icon> Tasks</a>
        <a mat-button routerLink="/workers" routerLinkActive="active"><mat-icon>groups</mat-icon> Workers</a>
        <a mat-button routerLink="/assignments" routerLinkActive="active"><mat-icon>calendar_month</mat-icon> Assignments</a>
      </nav>
      <span class="spacer"></span>
      @if (auth.user(); as user) {
        <button mat-button [matMenuTriggerFor]="menu" class="user-button">
          <mat-icon>account_circle</mat-icon>
          {{ user.username }}
          <span class="role-badge" [class.admin]="user.role === 'ADMIN'">{{ user.role }}</span>
        </button>
        <mat-menu #menu="matMenu">
          <button mat-menu-item (click)="auth.logout()"><mat-icon>logout</mat-icon> Log out</button>
        </mat-menu>
      }
    </mat-toolbar>
    <main class="content">
      <router-outlet />
    </main>
  `,
})
export class Shell {
  protected readonly auth = inject(AuthService);
}
