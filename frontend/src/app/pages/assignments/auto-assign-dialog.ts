import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { AssignmentsApi } from '../../core/api/assignments-api';
import { AutoAssignResult } from '../../core/models';
import { isWorkingDay, nextWorkingDay, startOfToday, toIsoDate } from '../../core/validation/validators';
import { FieldErrorPipe } from '../../shared/field-error.pipe';
import { SkillChips } from '../../shared/skill-chips';

/** Req4: previews the auto-assignment (dry run) and applies it after confirmation. ADMIN only. */
@Component({
  selector: 'app-auto-assign-dialog',
  imports: [DatePipe, ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatDatepickerModule,
    MatButtonModule, MatIconModule, MatProgressBarModule, FieldErrorPipe, SkillChips],
  template: `
    <h2 mat-dialog-title>Auto-assign pending tasks</h2>
    <mat-dialog-content>
      <p class="muted">
        Biggest tasks go first; each one goes to the compatible worker who would finish it earliest,
        given their availability and current workload. Nothing is saved until you confirm.
      </p>
      <mat-form-field appearance="outline">
        <mat-label>Start date</mat-label>
        <input matInput [matDatepicker]="picker" [formControl]="startDate" [min]="today" [matDatepickerFilter]="isWorkingDay"
               (dateChange)="preview()" />
        <mat-datepicker-toggle matIconSuffix [for]="picker" />
        <mat-datepicker #picker />
        <mat-error>{{ startDate.errors | fieldError }}</mat-error>
      </mat-form-field>

      @if (loading()) { <mat-progress-bar mode="indeterminate" /> }

      @if (result(); as r) {
        <h3 class="section-title"><mat-icon>check_circle</mat-icon> {{ r.assigned.length }} task(s) will be assigned</h3>
        @if (r.assigned.length) {
          <table class="simple-table">
            <thead><tr><th>Task</th><th>Effort</th><th>Worker</th><th>Start</th><th>End</th></tr></thead>
            <tbody>
              @for (item of r.assigned; track item.taskId) {
                <tr>
                  <td class="strong">{{ item.taskName }}</td>
                  <td>{{ item.effortHours }} h</td>
                  <td>{{ item.workerName }}</td>
                  <td>{{ item.startDate | date: 'EEE d MMM' }}</td>
                  <td>{{ item.endDate | date: 'EEE d MMM' }}</td>
                </tr>
              }
            </tbody>
          </table>
        } @else {
          <p class="muted">There are no pending tasks that can be assigned.</p>
        }
        @if (r.unassigned.length) {
          <h3 class="section-title warn"><mat-icon>warning</mat-icon> {{ r.unassigned.length }} task(s) cannot be assigned</h3>
          <ul class="reasons">
            @for (item of r.unassigned; track item.taskId) {
              <li><span class="strong">{{ item.taskName }}</span> — no worker has all of
                <app-skill-chips [skills]="item.skills" /></li>
            }
          </ul>
        }
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button (click)="confirm()" [disabled]="loading() || !result()?.assigned?.length">
        <mat-icon>bolt</mat-icon> Assign {{ result()?.assigned?.length ?? 0 }} task(s)
      </button>
    </mat-dialog-actions>
  `,
})
export class AutoAssignDialog {
  private readonly api = inject(AssignmentsApi);
  private readonly dialogRef = inject(MatDialogRef<AutoAssignDialog, AutoAssignResult>);

  protected readonly today = startOfToday();
  protected readonly isWorkingDay = isWorkingDay;
  protected readonly startDate = new FormControl<Date>(nextWorkingDay(), { nonNullable: true, validators: Validators.required });
  protected readonly loading = signal(false);
  protected readonly result = signal<AutoAssignResult | null>(null);

  constructor() {
    this.preview();
  }

  protected preview(): void {
    if (this.startDate.invalid) return;
    this.run(true);
  }

  protected confirm(): void {
    this.run(false);
  }

  private run(dryRun: boolean): void {
    this.loading.set(true);
    this.api.autoAssign(toIsoDate(this.startDate.value), dryRun).subscribe({
      next: (result) => {
        this.loading.set(false);
        if (dryRun) {
          this.result.set(result);
        } else {
          this.dialogRef.close(result);
        }
      },
      error: () => this.loading.set(false),
    });
  }
}
