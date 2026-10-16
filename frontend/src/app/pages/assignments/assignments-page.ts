import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
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
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { filter, forkJoin, switchMap } from 'rxjs';

import { AssignmentsApi } from '../../core/api/assignments-api';
import { TasksApi } from '../../core/api/tasks-api';
import { WorkersApi } from '../../core/api/workers-api';
import { HasRoleDirective } from '../../core/auth/has-role.directive';
import { Assignment, AutoAssignResult, Task, Worker } from '../../core/models';
import { isWorkingDay, missingSkills, nextWorkingDay, startOfToday, toIsoDate } from '../../core/validation/validators';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { FieldErrorPipe } from '../../shared/field-error.pipe';
import { SkillChips } from '../../shared/skill-chips';
import { AutoAssignDialog } from './auto-assign-dialog';
import { ReassignDialog, ReassignDialogData } from './reassign-dialog';

@Component({
  selector: 'app-assignments-page',
  imports: [DatePipe, ReactiveFormsModule, MatButtonModule, MatDatepickerModule, MatFormFieldModule, MatIconModule, MatInputModule,
    MatListModule, MatProgressBarModule, MatSelectModule, MatTableModule, MatTooltipModule, HasRoleDirective, SkillChips, FieldErrorPipe],
  template: `
    <header class="page-header">
      <div>
        <h1>Assignments</h1>
        <p class="subtitle">{{ assignments().length }} assigned · {{ pendingTasks().length }} pending tasks without worker</p>
      </div>
      <button mat-flat-button *appHasRole="'ADMIN'" (click)="openAutoAssign()" [disabled]="!pendingTasks().length">
        <mat-icon>bolt</mat-icon> Auto-assign
      </button>
    </header>

    <div class="grid-2">
      <section class="panel">
        <h2><mat-icon>assignment_ind</mat-icon> Assign tasks manually</h2>
        <form [formGroup]="form" (ngSubmit)="assign()" class="form-column">
          <mat-form-field appearance="outline">
            <mat-label>Worker</mat-label>
            <mat-select formControlName="workerId">
              @for (worker of workers(); track worker.id) {
                <mat-option [value]="worker.id">{{ worker.name }} · {{ worker.availabilityHoursPerDay }} h/day</mat-option>
              }
            </mat-select>
            <mat-error>{{ form.controls.workerId.errors | fieldError }}</mat-error>
          </mat-form-field>
          @if (selectedWorker(); as worker) {
            <div class="worker-skills"><span class="muted">Skills:</span> <app-skill-chips [skills]="worker.skills" /></div>
          }
          <mat-form-field appearance="outline">
            <mat-label>Start date</mat-label>
            <input matInput [matDatepicker]="picker" formControlName="startDate" [min]="today" [matDatepickerFilter]="isWorkingDay" />
            <mat-datepicker-toggle matIconSuffix [for]="picker" />
            <mat-datepicker #picker />
            <mat-hint>Working days only (Mon–Fri)</mat-hint>
            <mat-error>{{ form.controls.startDate.errors | fieldError }}</mat-error>
          </mat-form-field>

          <div class="task-picker">
            <div class="task-picker-header">
              <span>Pending tasks</span>
              <span class="muted">{{ selectedHours() }} h selected</span>
            </div>
            @if (!pendingTasks().length) {
              <p class="muted empty">No pending unassigned tasks.</p>
            }
            <mat-selection-list formControlName="taskIds" class="task-list">
              @for (task of pendingTasks(); track task.id) {
                @let missing = missingFor(task);
                <mat-list-option [value]="task.id" [disabled]="!selectedWorker() || missing.length > 0">
                  <span matListItemTitle>{{ task.name }} · {{ task.effortHours }} h</span>
                  <span matListItemLine>
                    <app-skill-chips [skills]="task.skills" [missing]="missing" />
                    @if (missing.length) { <span class="missing-text"> missing {{ missing.join(', ') }}</span> }
                  </span>
                </mat-list-option>
              }
            </mat-selection-list>
          </div>
          @if (form.controls.taskIds.touched && form.controls.taskIds.invalid) {
            <p class="form-error">Select at least one task</p>
          }
          <button mat-flat-button type="submit" [disabled]="saving()">
            <mat-icon>add_task</mat-icon> Assign selected tasks
          </button>
        </form>
      </section>

      <section class="panel">
        <div class="panel-title-row">
          <h2><mat-icon>calendar_month</mat-icon> Current assignments</h2>
          <mat-form-field appearance="outline" subscriptSizing="dynamic" class="compact">
            <mat-label>Worker</mat-label>
            <mat-select [value]="workerFilter()" (valueChange)="workerFilter.set($event)">
              <mat-option [value]="null">All workers</mat-option>
              @for (worker of workers(); track worker.id) {
                <mat-option [value]="worker.id">{{ worker.name }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
        </div>
        @if (loading()) { <mat-progress-bar mode="indeterminate" /> }
        <table mat-table [dataSource]="visibleAssignments()">
          <ng-container matColumnDef="task">
            <th mat-header-cell *matHeaderCellDef>Task</th>
            <td mat-cell *matCellDef="let a">
              <div class="strong" [class.done-text]="a.taskStatus === 'DONE'">{{ a.taskName }}</div>
              <div class="muted">{{ a.effortHours }} h{{ a.taskStatus === 'DONE' ? ' · done' : '' }}</div>
            </td>
          </ng-container>
          <ng-container matColumnDef="worker">
            <th mat-header-cell *matHeaderCellDef>Worker</th>
            <td mat-cell *matCellDef="let a">{{ a.workerName }}</td>
          </ng-container>
          <ng-container matColumnDef="dates">
            <th mat-header-cell *matHeaderCellDef>Dates</th>
            <td mat-cell *matCellDef="let a" [matTooltip]="allocationText(a)">
              {{ a.startDate | date: 'EEE d MMM' }} → {{ a.endDate | date: 'EEE d MMM' }}
            </td>
          </ng-container>
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let a" class="actions">
              <button mat-icon-button matTooltip="Move to another worker or date" (click)="reassign(a)"
                      [disabled]="a.taskStatus === 'DONE'"><mat-icon>swap_horiz</mat-icon></button>
              <button mat-icon-button matTooltip="Unassign" (click)="unassign(a)"><mat-icon>link_off</mat-icon></button>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columns"></tr>
          <tr mat-row *matRowDef="let row; columns: columns"></tr>
          <tr class="mat-row" *matNoDataRow>
            <td class="mat-cell empty" [attr.colspan]="columns.length">No assignments yet.</td>
          </tr>
        </table>
      </section>
    </div>
  `,
})
export class AssignmentsPage {
  private readonly assignmentsApi = inject(AssignmentsApi);
  private readonly tasksApi = inject(TasksApi);
  private readonly workersApi = inject(WorkersApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly columns = ['task', 'worker', 'dates', 'actions'];
  protected readonly today = startOfToday();
  protected readonly isWorkingDay = isWorkingDay;
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly workers = signal<Worker[]>([]);
  protected readonly tasks = signal<Task[]>([]);
  protected readonly assignments = signal<Assignment[]>([]);
  protected readonly workerFilter = signal<number | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    workerId: [null as unknown as number, Validators.required],
    startDate: [nextWorkingDay(), Validators.required],
    taskIds: [[] as number[], Validators.required],
  });

  private readonly formValue = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });
  protected readonly selectedWorker = computed(() => this.workers().find((w) => w.id === this.formValue().workerId) ?? null);
  protected readonly pendingTasks = computed(() => this.tasks().filter((t) => t.status === 'NOT_DONE' && !t.assignment));
  protected readonly selectedHours = computed(() => {
    const ids = new Set(this.formValue().taskIds ?? []);
    return this.tasks().filter((t) => ids.has(t.id)).reduce((sum, t) => sum + t.effortHours, 0);
  });
  protected readonly visibleAssignments = computed(() => {
    const workerId = this.workerFilter();
    return workerId ? this.assignments().filter((a) => a.workerId === workerId) : this.assignments();
  });

  constructor() {
    this.load();
    // Changing the worker clears tasks that the new worker cannot take.
    this.form.controls.workerId.valueChanges.subscribe(() => {
      const allowed = new Set(this.pendingTasks().filter((t) => this.missingFor(t).length === 0).map((t) => t.id));
      this.form.controls.taskIds.setValue(this.form.controls.taskIds.value.filter((id) => allowed.has(id)));
    });
  }

  protected missingFor(task: Task): string[] {
    const worker = this.selectedWorker();
    return worker ? missingSkills(task.skills, worker.skills) : [];
  }

  protected allocationText(a: Assignment): string {
    return a.allocations.map((al) => `${al.date}: ${al.hours} h`).join(' · ');
  }

  protected assign(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { workerId, startDate, taskIds } = this.form.getRawValue();
    // keep the order shown in the list, which is the order the worker will do them
    const ordered = this.pendingTasks().map((t) => t.id).filter((id) => taskIds.includes(id));
    this.saving.set(true);
    this.assignmentsApi.assign({ workerId, taskIds: ordered, startDate: toIsoDate(startDate) }).subscribe({
      next: (created) => {
        this.saving.set(false);
        const last = created[created.length - 1];
        this.snackBar.open(`${created.length} task(s) assigned to ${last.workerName}, finishing ${last.endDate}`, undefined, { duration: 4000 });
        this.form.controls.taskIds.reset([]);
        this.load();
      },
      error: () => this.saving.set(false),
    });
  }

  protected reassign(assignment: Assignment): void {
    const taskSkills = this.tasks().find((t) => t.id === assignment.taskId)?.skills ?? [];
    this.dialog.open<ReassignDialog, ReassignDialogData, Assignment>(ReassignDialog, {
      data: { assignment, taskSkills, workers: this.workers() }, width: '520px',
    }).afterClosed().pipe(filter(Boolean)).subscribe((moved) => {
      this.snackBar.open(`"${moved.taskName}" moved to ${moved.workerName}`, undefined, { duration: 3000 });
      this.load();
    });
  }

  protected unassign(assignment: Assignment): void {
    this.dialog.open(ConfirmDialog, {
      data: { title: 'Unassign task', message: `Remove "${assignment.taskName}" from ${assignment.workerName}?`, confirmLabel: 'Unassign' },
    }).afterClosed().pipe(filter(Boolean), switchMap(() => this.assignmentsApi.unassign(assignment.id)))
      .subscribe(() => {
        this.snackBar.open('Task unassigned', undefined, { duration: 2500 });
        this.load();
      });
  }

  protected openAutoAssign(): void {
    this.dialog.open<AutoAssignDialog, void, AutoAssignResult>(AutoAssignDialog, { width: '760px', maxWidth: '95vw' })
      .afterClosed().pipe(filter(Boolean)).subscribe((result) => {
        const skipped = result.unassigned.length ? `, ${result.unassigned.length} could not be assigned` : '';
        this.snackBar.open(`${result.assigned.length} task(s) auto-assigned${skipped}`, undefined, { duration: 4000 });
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
