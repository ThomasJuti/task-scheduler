import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { filter, forkJoin } from 'rxjs';

import { AssignmentsApi } from '../../core/api/assignments-api';
import { TasksApi } from '../../core/api/tasks-api';
import { WorkersApi } from '../../core/api/workers-api';
import { RequiresRoleDirective } from '../../core/auth/requires-role.directive';
import { isWorkingDay, nextWorkingDay, startOfToday, toIsoDate } from '../../core/dates';
import { Assignment, AutoAssignResult, Task, Worker } from '../../core/models';
import { missingSkills } from '../../core/skills';
import { confirmThen } from '../../shared/confirm-dialog';
import { FieldErrorPipe } from '../../shared/field-error.pipe';
import { SkillChips } from '../../shared/skill-chips';
import { AssignmentsTable } from './assignments-table';
import { AutoAssignDialog } from './auto-assign-dialog';
import { ReassignDialog, ReassignDialogData } from './reassign-dialog';

/** Manual assignment form, current assignments and the entry point to the auto-assignment. */
@Component({
  selector: 'app-assignments-page',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatListModule,
    MatProgressBarModule,
    MatSelectModule,
    RequiresRoleDirective,
    SkillChips,
    FieldErrorPipe,
    AssignmentsTable,
  ],
  templateUrl: './assignments-page.html',
})
export class AssignmentsPage {
  private readonly assignmentsApi = inject(AssignmentsApi);
  private readonly tasksApi = inject(TasksApi);
  private readonly workersApi = inject(WorkersApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly today = startOfToday();
  protected readonly isWorkingDay = isWorkingDay;
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly workers = signal<Worker[]>([]);
  protected readonly tasks = signal<Task[]>([]);
  protected readonly assignments = signal<Assignment[]>([]);
  protected readonly workerFilter = signal<number | null>(null);

  protected readonly form = this.fb.group({
    workerId: this.fb.control<number | null>(null, Validators.required),
    startDate: [nextWorkingDay(), Validators.required],
    taskIds: [[] as number[], Validators.required],
  });

  private readonly formValue = toSignal(this.form.valueChanges, {
    initialValue: this.form.getRawValue(),
  });
  protected readonly selectedWorker = computed(
    () => this.workers().find((w) => w.id === this.formValue().workerId) ?? null,
  );
  protected readonly pendingTasks = computed(() =>
    this.tasks().filter((t) => t.status === 'NOT_DONE' && !t.assignment),
  );
  protected readonly selectedHours = computed(() => {
    const ids = new Set(this.formValue().taskIds ?? []);
    return this.tasks()
      .filter((t) => ids.has(t.id))
      .reduce((sum, t) => sum + t.effortHours, 0);
  });
  protected readonly visibleAssignments = computed(() => {
    const workerId = this.workerFilter();
    return workerId
      ? this.assignments().filter((a) => a.workerId === workerId)
      : this.assignments();
  });

  constructor() {
    this.load();
    // Changing the worker clears tasks that the new worker cannot take.
    this.form.controls.workerId.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      const allowed = new Set(
        this.pendingTasks()
          .filter((t) => this.missingFor(t).length === 0)
          .map((t) => t.id),
      );
      const taskIds = this.form.controls.taskIds;
      taskIds.setValue(taskIds.value.filter((id) => allowed.has(id)));
    });
  }

  protected missingFor(task: Task): string[] {
    const worker = this.selectedWorker();
    return worker ? missingSkills(task.skills, worker.skills) : [];
  }

  protected assign(): void {
    const { workerId, startDate, taskIds } = this.form.getRawValue();
    if (this.form.invalid || workerId === null) {
      this.form.markAllAsTouched();
      return;
    }
    // keep the order shown in the list, which is the order the worker will do them
    const ordered = this.pendingTasks()
      .map((t) => t.id)
      .filter((id) => taskIds.includes(id));
    this.saving.set(true);
    this.assignmentsApi
      .assign({ workerId, taskIds: ordered, startDate: toIsoDate(startDate) })
      .subscribe({
        next: (created) => {
          this.saving.set(false);
          const last = created[created.length - 1];
          this.snackBar.open(
            `${created.length} task(s) assigned to ${last.workerName}, finishing ${last.endDate}`,
            undefined,
            { duration: 4000 },
          );
          this.form.controls.taskIds.reset([]);
          this.load();
        },
        error: () => this.saving.set(false),
      });
  }

  protected reassign(assignment: Assignment): void {
    const taskSkills = this.tasks().find((t) => t.id === assignment.taskId)?.skills ?? [];
    this.dialog
      .open<ReassignDialog, ReassignDialogData, Assignment>(ReassignDialog, {
        data: { assignment, taskSkills, workers: this.workers() },
        width: '520px',
      })
      .afterClosed()
      .pipe(filter(Boolean))
      .subscribe((moved) => {
        this.snackBar.open(`"${moved.taskName}" moved to ${moved.workerName}`, undefined, {
          duration: 3000,
        });
        this.load();
      });
  }

  protected unassign(assignment: Assignment): void {
    const data = {
      title: 'Unassign task',
      message: `Remove "${assignment.taskName}" from ${assignment.workerName}?`,
      confirmLabel: 'Unassign',
    };
    confirmThen(this.dialog, data, () => this.assignmentsApi.unassign(assignment.id)).subscribe(
      () => {
        this.snackBar.open('Task unassigned', undefined, { duration: 2500 });
        this.load();
      },
    );
  }

  protected openAutoAssign(): void {
    this.dialog
      .open<AutoAssignDialog, void, AutoAssignResult>(AutoAssignDialog, {
        width: '760px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .pipe(filter(Boolean))
      .subscribe((result) => {
        const skipped = result.unassigned.length
          ? `, ${result.unassigned.length} could not be assigned`
          : '';
        this.snackBar.open(`${result.assigned.length} task(s) auto-assigned${skipped}`, undefined, {
          duration: 4000,
        });
        this.load();
      });
  }

  private load(): void {
    this.loading.set(true);
    forkJoin({
      workers: this.workersApi.list(),
      tasks: this.tasksApi.list(),
      assignments: this.assignmentsApi.list(),
    }).subscribe({
      next: ({ workers, tasks, assignments }) => {
        this.workers.set(workers);
        this.tasks.set(tasks);
        this.assignments.set(assignments);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
