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
import { isWorkingDay, nextWorkingDay, startOfToday, toIsoDate } from '../../core/dates';
import { missingSkills } from '../../core/skills';
import { submitDialogForm } from '../../shared/dialog-form';
import { FieldErrorPipe } from '../../shared/field-error.pipe';

export interface ReassignDialogData {
  assignment: Assignment;
  taskSkills: string[];
  workers: Worker[];
}

@Component({
  selector: 'app-reassign-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatDatepickerModule,
    MatButtonModule,
    FieldErrorPipe,
  ],
  templateUrl: './reassign-dialog.html',
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
    submitDialogForm(
      this.form,
      () => {
        const { workerId, startDate } = this.form.getRawValue();
        return this.api.reassign(this.data.assignment.id, {
          workerId,
          startDate: toIsoDate(startDate),
        });
      },
      this.dialogRef,
      this.saving,
    );
  }

  private initialDate(): Date {
    const [y, m, d] = this.data.assignment.startDate.split('-').map(Number);
    const current = new Date(y, m - 1, d);
    return current < this.today ? nextWorkingDay() : current;
  }
}
