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
import { isWorkingDay, nextWorkingDay, startOfToday, toIsoDate } from '../../core/dates';
import { FieldErrorPipe } from '../../shared/field-error.pipe';
import { SkillChips } from '../../shared/skill-chips';

/** Req4 (auto-assignment of the pending tasks): previews the auto-assignment (dry run) and applies it after confirmation. ADMIN only. */
@Component({
  selector: 'app-auto-assign-dialog',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    FieldErrorPipe,
    SkillChips,
  ],
  templateUrl: './auto-assign-dialog.html',
})
export class AutoAssignDialog {
  private readonly api = inject(AssignmentsApi);
  private readonly dialogRef = inject(MatDialogRef<AutoAssignDialog, AutoAssignResult>);

  protected readonly today = startOfToday();
  protected readonly isWorkingDay = isWorkingDay;
  protected readonly startDate = new FormControl<Date>(nextWorkingDay(), {
    nonNullable: true,
    validators: Validators.required,
  });
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
