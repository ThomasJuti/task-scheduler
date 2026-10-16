import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { WorkersApi } from '../../core/api/workers-api';
import { Worker, WorkerRequest } from '../../core/models';
import {
  AVAILABILITY_MAX,
  AVAILABILITY_MIN,
  WORKER_NAME_MAX,
  WORKER_SKILLS_MAX,
} from '../../core/validation/limits';
import { integer, notBlank, skillsValidator } from '../../core/validation/validators';
import { submitDialogForm } from '../../shared/dialog-form';
import { FieldErrorPipe } from '../../shared/field-error.pipe';
import { SkillsInput } from '../../shared/skills-input';

export interface WorkerDialogData {
  worker?: Worker;
  skillOptions: string[];
}

@Component({
  selector: 'app-worker-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    SkillsInput,
    FieldErrorPipe,
  ],
  templateUrl: './worker-dialog.html',
})
export class WorkerDialog {
  protected readonly data = inject<WorkerDialogData>(MAT_DIALOG_DATA);
  private readonly api = inject(WorkersApi);
  private readonly dialogRef = inject(MatDialogRef<WorkerDialog, Worker>);
  protected readonly saving = signal(false);
  protected readonly limits = { WORKER_NAME_MAX, AVAILABILITY_MIN, AVAILABILITY_MAX };

  protected readonly form = inject(NonNullableFormBuilder).group({
    name: [
      this.data.worker?.name ?? '',
      [Validators.required, Validators.maxLength(WORKER_NAME_MAX), notBlank],
    ],
    availabilityHoursPerDay: [
      this.data.worker?.availabilityHoursPerDay ?? 8,
      [
        Validators.required,
        Validators.min(AVAILABILITY_MIN),
        Validators.max(AVAILABILITY_MAX),
        integer,
      ],
    ],
    skills: [this.data.worker?.skills ?? ([] as string[]), [skillsValidator(WORKER_SKILLS_MAX)]],
  });

  protected save(): void {
    submitDialogForm(
      this.form,
      () => {
        const value = this.form.getRawValue();
        const request: WorkerRequest = { ...value, name: value.name.trim() };
        return this.data.worker
          ? this.api.update(this.data.worker.id, request)
          : this.api.create(request);
      },
      this.dialogRef,
      this.saving,
    );
  }
}
