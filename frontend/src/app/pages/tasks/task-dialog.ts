import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { TasksApi } from '../../core/api/tasks-api';
import { Task, TaskRequest, TaskStatus } from '../../core/models';
import {
  EFFORT_MAX,
  EFFORT_MIN,
  TASK_NAME_MAX,
  TASK_SKILLS_MAX,
} from '../../core/validation/limits';
import { integer, notBlank, skillsValidator } from '../../core/validation/validators';
import { submitDialogForm } from '../../shared/dialog-form';
import { FieldErrorPipe } from '../../shared/field-error.pipe';
import { SkillsInput } from '../../shared/skills-input';

export interface TaskDialogData {
  task?: Task;
  skillOptions: string[];
}

@Component({
  selector: 'app-task-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    SkillsInput,
    FieldErrorPipe,
  ],
  templateUrl: './task-dialog.html',
})
export class TaskDialog {
  protected readonly data = inject<TaskDialogData>(MAT_DIALOG_DATA);
  private readonly api = inject(TasksApi);
  private readonly dialogRef = inject(MatDialogRef<TaskDialog, Task>);
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly saving = signal(false);
  protected readonly limits = { TASK_NAME_MAX, EFFORT_MIN, EFFORT_MAX };

  protected readonly form = this.fb.group({
    name: [
      this.data.task?.name ?? '',
      [Validators.required, Validators.maxLength(TASK_NAME_MAX), notBlank],
    ],
    // vacío en una tarea nueva, para que el usuario tenga que escribir un valor
    effortHours: this.fb.control<number | null>(this.data.task?.effortHours ?? null, [
      Validators.required,
      Validators.min(EFFORT_MIN),
      Validators.max(EFFORT_MAX),
      integer,
    ]),
    skills: [this.data.task?.skills ?? ([] as string[]), [skillsValidator(TASK_SKILLS_MAX)]],
    status: [this.data.task?.status ?? ('NOT_DONE' as TaskStatus), Validators.required],
  });

  protected save(): void {
    submitDialogForm(
      this.form,
      () => {
        const request = this.toRequest();
        return this.data.task
          ? this.api.update(this.data.task.id, request)
          : this.api.create(request);
      },
      this.dialogRef,
      this.saving,
    );
  }

  /** Solo se llama con el formulario válido, donde el validador required garantiza un esfuerzo. */
  private toRequest(): TaskRequest {
    const { name, effortHours, skills, status } = this.form.getRawValue();
    if (effortHours === null) throw new Error('effortHours is required');
    return { name: name.trim(), effortHours, skills, status };
  }
}
