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
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { filter, forkJoin } from 'rxjs';

import { AssignmentsApi } from '../../core/api/assignments-api';
import { TasksApi } from '../../core/api/tasks-api';
import { WorkersApi } from '../../core/api/workers-api';
import { RequiresRoleDirective } from '../../core/auth/requires-role.directive';
import { isWorkingDay, maxPlanningDate, nextWorkingDay, startOfToday, toIsoDate } from '../../core/dates';
import { Assignment, AutoAssignResult, Page, Task, Worker } from '../../core/models';
import { missingSkills } from '../../core/skills';
import { PAGE_SIZE, PAGE_SIZE_MAX } from '../../core/validation/limits';
import { confirmThen } from '../../shared/confirm-dialog';
import { FieldErrorPipe } from '../../shared/field-error.pipe';
import { SkillChips } from '../../shared/skill-chips';
import { AssignmentsTable } from './assignments-table';
import { AutoAssignDialog } from './auto-assign-dialog';
import { ReassignDialog, ReassignDialogData } from './reassign-dialog';

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
    MatPaginatorModule,
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
  protected readonly maxDate = maxPlanningDate();
  protected readonly isWorkingDay = isWorkingDay;
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly workers = signal<Worker[]>([]);
  /** Tareas pendientes sin asignar (las primeras PAGE_SIZE_MAX; pendingTotal dice cuántas hay en total). */
  protected readonly pendingTasks = signal<Task[]>([]);
  protected readonly pendingTotal = signal(0);
  protected readonly assignments = signal<Assignment[]>([]);
  protected readonly assignmentsTotal = signal(0);
  protected readonly assignmentsPage = signal(0);
  protected readonly assignmentsPageSize = signal(PAGE_SIZE);
  protected readonly pageSizeOptions = [10, 20, 50, PAGE_SIZE_MAX];
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
  protected readonly selectedHours = computed(() => {
    const ids = new Set(this.formValue().taskIds ?? []);
    return this.pendingTasks()
      .filter((t) => ids.has(t.id))
      .reduce((sum, t) => sum + t.effortHours, 0);
  });

  constructor() {
    this.load();
    // Al cambiar de trabajador se descartan las tareas que el nuevo no puede tomar.
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
    // mantiene el orden mostrado en la lista, que es el orden en que el trabajador las hará
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

  protected onWorkerFilter(workerId: number | null): void {
    this.workerFilter.set(workerId);
    this.assignmentsPage.set(0);
    this.loadAssignments();
  }

  protected onAssignmentsPage(event: PageEvent): void {
    this.assignmentsPage.set(event.pageIndex);
    this.assignmentsPageSize.set(event.pageSize);
    this.loadAssignments();
  }

  protected reassign(assignment: Assignment): void {
    const taskSkills = assignment.taskSkills;
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
      pending: this.tasksApi.list({ status: 'NOT_DONE', unassigned: true, size: PAGE_SIZE_MAX }),
      assignments: this.assignmentsApi.list(this.assignmentsQuery()),
    }).subscribe({
      next: ({ workers, pending, assignments }) => {
        this.workers.set(workers);
        this.pendingTasks.set(pending.content);
        this.pendingTotal.set(pending.totalElements);
        this.setAssignments(assignments);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  private loadAssignments(): void {
    this.loading.set(true);
    this.assignmentsApi.list(this.assignmentsQuery()).subscribe({
      next: (page) => {
        this.setAssignments(page);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  private assignmentsQuery() {
    return { workerId: this.workerFilter(), page: this.assignmentsPage(), size: this.assignmentsPageSize() };
  }

  private setAssignments(page: Page<Assignment>): void {
    this.assignments.set(page.content);
    this.assignmentsTotal.set(page.totalElements);
  }
}
