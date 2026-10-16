import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { AssignmentsApi } from '../../core/api/assignments-api';
import { Assignment, Worker } from '../../core/models';
import { isWorkingDay, missingSkills, nextWorkingDay, startOfToday, toIsoDate } from '../../core/validation/validators';
import { FieldErrorPipe } from '../../shared/field-error.pipe';

export interface ReassignDialogData {
  assignment: Assignment;
  taskSkills: string[];
  workers: Worker[];
}

/** "Move" a task to another worker and/or start date (allowed for USER and ADMIN). */
@Component({
  selector: 'app-reassign-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatSelectModule, MatInputModule, MatDatepickerModule, MatButtonModule, FieldErrorPipe],
  template: `
    <h2 mat-dialog-title>Move "{{ data.assignment.taskName }}"</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content class="form-column">
        <p class="muted">Currently: {{ data.assignment.workerName }}, {{ data.assignment.startDate }} → {{ data.assignment.endDate }}</p>
        <mat-form-field appearance="outline">
          <mat-label>Worker</mat-label>
          <mat-select formControlName="workerId">
            @for (worker of data.workers; track worker.id) {
              @let missing = missingFor(worker);
              <mat-option [value]="worker.id" [disabled]="missing.length > 0">
                {{ worker.name }} · {{ worker.availabilityHoursPerDay }} h/day
                @if (missing.length) { <span class="muted"> — missing {{ missing.join(', ') }}</span> }
              </mat-option>
            }
          </mat-select>
          <mat-hint>Only workers with all the task skills can be selected</mat-hint>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Start date</mat-label>
          <input matInput [matDatepicker]="picker" formControlName="startDate" [min]="today" [matDatepickerFilter]="isWorkingDay" />
          <mat-datepicker-toggle matIconSuffix [for]="picker" />
          <mat-datepicker #picker />
          <mat-error>{{ form.controls.startDate.errors | fieldError }}</mat-error>
        </mat-form-field>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button type="submit" [disabled]="saving()">Move task</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class ReassignDialog {
  protected readonly data = inject<ReassignDialogData>(MAT_DIALOG_DATA);
  private readonly api = inject(AssignmentsApi);
  private readonly dialogRef = inject(MatDialogRef<ReassignDialog, Assignment>);
  protected readonly saving = signal(false);
  protected readonly today = startOfToday();
  protected readonly isWorkingDay = isWorkingDay;

  protected readonly form = inject(NonNullableFormBuilder).group({
    workerId: [this.data.assignment.workerId, Validators.required],
    startDate: [this.initialDate(), Validators.required],
  });

  protected missingFor(worker: Worker): string[] {
    return missingSkills(this.data.taskSkills, worker.skills);
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { workerId, startDate } = this.form.getRawValue();
    this.saving.set(true);
    this.api.reassign(this.data.assignment.id, { workerId, startDate: toIsoDate(startDate) }).subscribe({
      next: (assignment) => this.dialogRef.close(assignment),
      error: () => this.saving.set(false),
    });
  }

  private initialDate(): Date {
    const [y, m, d] = this.data.assignment.startDate.split('-').map(Number);
    const current = new Date(y, m - 1, d);
    return current < this.today ? nextWorkingDay() : current;
  }
}
