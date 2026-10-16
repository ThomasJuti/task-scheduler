import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { AuthService } from '../../core/auth/auth.service';
import { problemMessage } from '../../core/http/problem';
import { notBlank } from '../../core/validation/validators';
import { FieldErrorPipe } from '../../shared/field-error.pipe';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, MatProgressBarModule, FieldErrorPipe],
  template: `
    <div class="login-page">
      <mat-card class="login-card" appearance="outlined">
        @if (loading()) {
          <mat-progress-bar mode="indeterminate" />
        }
        <mat-card-header>
          <mat-icon mat-card-avatar class="login-icon">event_available</mat-icon>
          <mat-card-title>Task Scheduler</mat-card-title>
          <mat-card-subtitle>Sign in to continue</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          @if (expired) {
            <p class="notice">Your session expired. Please sign in again.</p>
          }
          <form [formGroup]="form" (ngSubmit)="submit()" class="form-column">
            <mat-form-field appearance="outline">
              <mat-label>Username</mat-label>
              <input matInput formControlName="username" autocomplete="username" maxlength="50" />
              <mat-error>{{ form.controls.username.errors | fieldError }}</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Password</mat-label>
              <input matInput formControlName="password" [type]="showPassword() ? 'text' : 'password'"
                     autocomplete="current-password" maxlength="100" />
              <button mat-icon-button matSuffix type="button" (click)="showPassword.set(!showPassword())"
                      [attr.aria-label]="showPassword() ? 'Hide password' : 'Show password'">
                <mat-icon>{{ showPassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              <mat-error>{{ form.controls.password.errors | fieldError }}</mat-error>
            </mat-form-field>
            @if (error()) {
              <p class="form-error" role="alert">{{ error() }}</p>
            }
            <button mat-flat-button type="submit" [disabled]="loading()">Sign in</button>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly expired = inject(ActivatedRoute).snapshot.queryParamMap.get('reason') === 'expired';
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly showPassword = signal(false);
  protected readonly form = inject(NonNullableFormBuilder).group({
    username: ['', [Validators.required, Validators.maxLength(50), notBlank]],
    password: ['', [Validators.required, Validators.maxLength(100)]],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    const { username, password } = this.form.getRawValue();
    this.auth.login(username.trim(), password).subscribe({
      next: () => void this.router.navigate(['/tasks']),
      error: (err) => {
        this.loading.set(false);
        this.error.set(problemMessage(err));
      },
    });
  }
}
