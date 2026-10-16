import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { TasksApi } from '../../core/api/tasks-api';
import { applyServerErrors } from '../../core/http/problem';
import { Task, TaskRequest, TaskStatus } from '../../core/models';
import { integer, notBlank, skillsValidator } from '../../core/validation/validators';
import { FieldErrorPipe } from '../../shared/field-error.pipe';
import { SkillsInput } from '../../shared/skills-input';

export interface TaskDialogData {
  task?: Task;
  skillOptions: string[];
}

@Component({
  selector: 'app-task-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, SkillsInput, FieldErrorPipe],
  template: `
    <h2 mat-dialog-title>{{ data.task ? 'Edit task' : 'New task' }}</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content class="form-column">
        <mat-form-field appearance="outline">
          <mat-label>Name</mat-label>
          <input matInput formControlName="name" maxlength="120" cdkFocusInitial />
          <mat-hint align="end">{{ form.controls.name.value.length }}/120</mat-hint>
          <mat-error>{{ form.controls.name.errors | fieldError }}</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Effort (hours)</mat-label>
          <input matInput type="number" formControlName="effortHours" min="1" max="1000" step="1" />
          <mat-hint>Whole hours, 1–1000</mat-hint>
          <mat-error>{{ form.controls.effortHours.errors | fieldError }}</mat-error>
        </mat-form-field>
        <app-skills-input [control]="form.controls.skills" [options]="data.skillOptions" />
        <mat-form-field appearance="outline">
          <mat-label>Status</mat-label>
          <mat-select formControlName="status">
            <mat-option value="NOT_DONE">Not done</mat-option>
            <mat-option value="DONE">Done</mat-option>
          </mat-select>
        </mat-form-field>
        @if (data.task?.assignment) {
          <p class="notice">This task is assigned to {{ data.task?.assignment?.workerName }}: effort and skills can only
            change after unassigning it.</p>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button type="submit" [disabled]="saving()">Save</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class TaskDialog {
  protected readonly data = inject<TaskDialogData>(MAT_DIALOG_DATA);
  private readonly api = inject(TasksApi);
  private readonly dialogRef = inject(MatDialogRef<TaskDialog, Task>);
  protected readonly saving = signal(false);

  protected readonly form = inject(NonNullableFormBuilder).group({
    name: [this.data.task?.name ?? '', [Validators.required, Validators.maxLength(120), notBlank]],
    effortHours: [this.data.task?.effortHours ?? (null as unknown as number),
      [Validators.required, Validators.min(1), Validators.max(1000), integer]],
    skills: [this.data.task?.skills ?? ([] as string[]), [skillsValidator(20)]],
    status: [this.data.task?.status ?? ('NOT_DONE' as TaskStatus), Validators.required],
  });

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const request: TaskRequest = { ...value, name: value.name.trim(), effortHours: Number(value.effortHours) };
    this.saving.set(true);
    const call = this.data.task ? this.api.update(this.data.task.id, request) : this.api.create(request);
    call.subscribe({
      next: (task) => this.dialogRef.close(task),
      error: (err) => {
        this.saving.set(false);
        applyServerErrors(this.form, err);
      },
    });
  }
}
