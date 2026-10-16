import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { filter, switchMap } from 'rxjs';

import { SkillsApi } from '../../core/api/skills-api';
import { TasksApi } from '../../core/api/tasks-api';
import { HasRoleDirective } from '../../core/auth/has-role.directive';
import { Task } from '../../core/models';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { SkillChips } from '../../shared/skill-chips';
import { TaskDialog, TaskDialogData } from './task-dialog';

type StatusFilter = 'ALL' | 'NOT_DONE' | 'DONE';

@Component({
  selector: 'app-tasks-page',
  imports: [DatePipe, FormsModule, MatTableModule, MatSortModule, MatButtonModule, MatButtonToggleModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSlideToggleModule, MatTooltipModule, MatProgressBarModule, HasRoleDirective, SkillChips],
  template: `
    <header class="page-header">
      <div>
        <h1>Tasks</h1>
        <p class="subtitle">{{ pendingCount() }} pending · {{ doneCount() }} done</p>
      </div>
      <button mat-flat-button *appHasRole="'ADMIN'" (click)="openDialog()"><mat-icon>add</mat-icon> New task</button>
    </header>

    <div class="toolbar-row">
      <mat-form-field appearance="outline" subscriptSizing="dynamic" class="search">
        <mat-icon matPrefix>search</mat-icon>
        <mat-label>Search by name, skill or worker</mat-label>
        <input matInput [ngModel]="search()" (ngModelChange)="search.set($event)" maxlength="100" />
      </mat-form-field>
      <mat-button-toggle-group [value]="statusFilter()" (change)="statusFilter.set($event.value)" aria-label="Status filter">
        <mat-button-toggle value="ALL">All</mat-button-toggle>
        <mat-button-toggle value="NOT_DONE">Not done</mat-button-toggle>
        <mat-button-toggle value="DONE">Done</mat-button-toggle>
      </mat-button-toggle-group>
    </div>

    <div class="table-card">
      @if (loading()) { <mat-progress-bar mode="indeterminate" /> }
      <div class="table-scroll">
      <table mat-table [dataSource]="rows()" matSort (matSortChange)="sort.set($event)">
        <ng-container matColumnDef="name">
          <th mat-header-cell *matHeaderCellDef mat-sort-header>Name</th>
          <td mat-cell *matCellDef="let task" class="strong">{{ task.name }}</td>
        </ng-container>
        <ng-container matColumnDef="effortHours">
          <th mat-header-cell *matHeaderCellDef mat-sort-header>Effort</th>
          <td mat-cell *matCellDef="let task">{{ task.effortHours }} h</td>
        </ng-container>
        <ng-container matColumnDef="skills">
          <th mat-header-cell *matHeaderCellDef>Skills</th>
          <td mat-cell *matCellDef="let task"><app-skill-chips [skills]="task.skills" /></td>
        </ng-container>
        <ng-container matColumnDef="assignment">
          <th mat-header-cell *matHeaderCellDef mat-sort-header>Assigned to</th>
          <td mat-cell *matCellDef="let task">
            @if (task.assignment; as a) {
              <div class="strong">{{ a.workerName }}</div>
              <div class="muted">{{ a.startDate | date: 'EEE d MMM' }} → {{ a.endDate | date: 'EEE d MMM' }}</div>
            } @else {
              <span class="muted">Unassigned</span>
            }
          </td>
        </ng-container>
        <ng-container matColumnDef="status">
          <th mat-header-cell *matHeaderCellDef mat-sort-header>Done</th>
          <td mat-cell *matCellDef="let task">
            <mat-slide-toggle [checked]="task.status === 'DONE'" (change)="toggleStatus(task, $event.checked)"
                              [attr.aria-label]="'Mark ' + task.name + ' as done'" />
          </td>
        </ng-container>
        <ng-container matColumnDef="actions">
          <th mat-header-cell *matHeaderCellDef></th>
          <td mat-cell *matCellDef="let task" class="actions">
            <ng-container *appHasRole="'ADMIN'">
              <button mat-icon-button matTooltip="Edit" [attr.aria-label]="'Edit ' + task.name" (click)="openDialog(task)"><mat-icon>edit</mat-icon></button>
              <button mat-icon-button matTooltip="Delete" [attr.aria-label]="'Delete ' + task.name" (click)="remove(task)"><mat-icon>delete</mat-icon></button>
            </ng-container>
          </td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="columns"></tr>
        <tr mat-row *matRowDef="let row; columns: columns" [class.row-done]="row.status === 'DONE'"></tr>
        <tr class="mat-row" *matNoDataRow>
          <td class="mat-cell empty" [attr.colspan]="columns.length">No tasks match the current filters.</td>
        </tr>
      </table>
      </div>
    </div>
  `,
})
export class TasksPage {
  private readonly tasksApi = inject(TasksApi);
  private readonly skillsApi = inject(SkillsApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly columns = ['name', 'effortHours', 'skills', 'assignment', 'status', 'actions'];
  protected readonly tasks = signal<Task[]>([]);
  protected readonly loading = signal(true);
  protected readonly search = signal('');
  protected readonly statusFilter = signal<StatusFilter>('ALL');
  protected readonly sort = signal<Sort>({ active: '', direction: '' });

  protected readonly pendingCount = computed(() => this.tasks().filter((t) => t.status === 'NOT_DONE').length);
  protected readonly doneCount = computed(() => this.tasks().filter((t) => t.status === 'DONE').length);

  protected readonly rows = computed(() => {
    const q = this.search().trim().toLowerCase();
    const status = this.statusFilter();
    const { active, direction } = this.sort();
    const rows = this.tasks().filter((t) =>
      (status === 'ALL' || t.status === status) &&
      (!q || t.name.toLowerCase().includes(q) || t.skills.some((s) => s.toLowerCase().includes(q)) ||
        (t.assignment?.workerName.toLowerCase().includes(q) ?? false)));
    if (!active || !direction) return rows;
    const key = (t: Task): string | number =>
      active === 'assignment' ? t.assignment?.workerName ?? '' : (t[active as keyof Task] as string | number);
    const sign = direction === 'asc' ? 1 : -1;
    return [...rows].sort((a, b) => (key(a) > key(b) ? 1 : key(a) < key(b) ? -1 : 0) * sign);
  });

  constructor() {
    this.load();
  }

  protected openDialog(task?: Task): void {
    this.skillsApi.list().pipe(
      switchMap((skillOptions) => this.dialog.open<TaskDialog, TaskDialogData, Task>(TaskDialog, {
        data: { task, skillOptions }, width: '520px',
      }).afterClosed()),
      filter(Boolean),
    ).subscribe((saved) => {
      this.snackBar.open(`Task "${saved.name}" saved`, undefined, { duration: 2500 });
      this.load();
    });
  }

  protected toggleStatus(task: Task, done: boolean): void {
    this.tasksApi.updateStatus(task.id, done ? 'DONE' : 'NOT_DONE').subscribe({
      next: (updated) => this.tasks.update((all) => all.map((t) => (t.id === updated.id ? updated : t))),
      error: () => this.load(),
    });
  }

  protected remove(task: Task): void {
    const message = task.assignment
      ? `"${task.name}" is assigned to ${task.assignment.workerName}. Deleting it also removes the assignment.`
      : `Delete "${task.name}"?`;
    this.dialog.open(ConfirmDialog, { data: { title: 'Delete task', message } }).afterClosed()
      .pipe(filter(Boolean), switchMap(() => this.tasksApi.delete(task.id)))
      .subscribe(() => {
        this.snackBar.open('Task deleted', undefined, { duration: 2500 });
        this.load();
      });
  }

  private load(): void {
    this.loading.set(true);
    this.tasksApi.list().subscribe({
      next: (tasks) => {
        this.tasks.set(tasks);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
