import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { WorkersApi } from '../../core/api/workers-api';
import { applyServerErrors } from '../../core/http/problem';
import { Worker, WorkerRequest } from '../../core/models';
import { integer, notBlank, skillsValidator } from '../../core/validation/validators';
import { FieldErrorPipe } from '../../shared/field-error.pipe';
import { SkillsInput } from '../../shared/skills-input';

export interface WorkerDialogData {
  worker?: Worker;
  skillOptions: string[];
}

@Component({
  selector: 'app-worker-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, SkillsInput, FieldErrorPipe],
  template: `
    <h2 mat-dialog-title>{{ data.worker ? 'Edit worker' : 'New worker' }}</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content class="form-column">
        <mat-form-field appearance="outline">
          <mat-label>Name</mat-label>
          <input matInput formControlName="name" maxlength="120" cdkFocusInitial />
          <mat-hint align="end">{{ form.controls.name.value.length }}/120</mat-hint>
          <mat-error>{{ form.controls.name.errors | fieldError }}</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Availability (hours/day)</mat-label>
          <input matInput type="number" formControlName="availabilityHoursPerDay" min="1" max="24" step="1" />
          <mat-hint>Hours per working day (Mon–Fri), 1–24</mat-hint>
          <mat-error>{{ form.controls.availabilityHoursPerDay.errors | fieldError }}</mat-error>
        </mat-form-field>
        <app-skills-input [control]="form.controls.skills" [options]="data.skillOptions" />
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button type="submit" [disabled]="saving()">Save</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class WorkerDialog {
  protected readonly data = inject<WorkerDialogData>(MAT_DIALOG_DATA);
  private readonly api = inject(WorkersApi);
  private readonly dialogRef = inject(MatDialogRef<WorkerDialog, Worker>);
  protected readonly saving = signal(false);

  protected readonly form = inject(NonNullableFormBuilder).group({
    name: [this.data.worker?.name ?? '', [Validators.required, Validators.maxLength(120), notBlank]],
    availabilityHoursPerDay: [this.data.worker?.availabilityHoursPerDay ?? 8,
      [Validators.required, Validators.min(1), Validators.max(24), integer]],
    skills: [this.data.worker?.skills ?? ([] as string[]), [skillsValidator(30)]],
  });

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const request: WorkerRequest = {
      ...value,
      name: value.name.trim(),
      availabilityHoursPerDay: Number(value.availabilityHoursPerDay),
    };
    this.saving.set(true);
    const call = this.data.worker ? this.api.update(this.data.worker.id, request) : this.api.create(request);
    call.subscribe({
      next: (worker) => this.dialogRef.close(worker),
      error: (err) => {
        this.saving.set(false);
        applyServerErrors(this.form, err);
      },
    });
  }
}
