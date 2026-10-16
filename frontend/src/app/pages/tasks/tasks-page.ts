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

import { SkillsApi } from '../../core/api/skills-api';
import { TasksApi } from '../../core/api/tasks-api';
import { RequiresRoleDirective } from '../../core/auth/requires-role.directive';
import { Task } from '../../core/models';
import { confirmThen } from '../../shared/confirm-dialog';
import { openEditDialog } from '../../shared/edit-dialog';
import { SkillChips } from '../../shared/skill-chips';
import { TaskDialog, TaskDialogData } from './task-dialog';
import { sortTasks } from './task-sort';

type StatusFilter = 'ALL' | 'NOT_DONE' | 'DONE';

@Component({
  selector: 'app-tasks-page',
  imports: [
    DatePipe,
    FormsModule,
    MatTableModule,
    MatSortModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatTooltipModule,
    MatProgressBarModule,
    RequiresRoleDirective,
    SkillChips,
  ],
  templateUrl: './tasks-page.html',
})
export class TasksPage {
  private readonly tasksApi = inject(TasksApi);
  private readonly skillsApi = inject(SkillsApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly columns = ['name', 'effortHours', 'skills', 'assignment', 'status', 'actions'];
  protected readonly tasks = signal<Task[]>([]);
  protected readonly loading = signal(true);
  protected readonly loadError = signal(false);
  protected readonly search = signal('');
  protected readonly statusFilter = signal<StatusFilter>('ALL');
  protected readonly sort = signal<Sort>({ active: '', direction: '' });

  protected readonly pendingCount = computed(
    () => this.tasks().filter((t) => t.status === 'NOT_DONE').length,
  );
  protected readonly doneCount = computed(
    () => this.tasks().filter((t) => t.status === 'DONE').length,
  );

  protected readonly rows = computed(() => {
    const query = this.search().trim().toLowerCase();
    const status = this.statusFilter();
    const visible = this.tasks().filter(
      (t) => (status === 'ALL' || t.status === status) && matchesSearch(t, query),
    );
    return sortTasks(visible, this.sort());
  });

  constructor() {
    this.load();
  }

  protected openDialog(task?: Task): void {
    openEditDialog<TaskDialogData, Task>(this.dialog, this.skillsApi, TaskDialog, {
      task,
    }).subscribe((saved) => {
      this.snackBar.open(`Task "${saved.name}" saved`, undefined, { duration: 2500 });
      this.load();
    });
  }

  protected toggleStatus(task: Task, done: boolean): void {
    this.tasksApi.updateStatus(task.id, done ? 'DONE' : 'NOT_DONE').subscribe({
      next: (updated) =>
        this.tasks.update((all) => all.map((t) => (t.id === updated.id ? updated : t))),
      error: () => this.load(),
    });
  }

  protected remove(task: Task): void {
    const message = task.assignment
      ? `"${task.name}" is assigned to ${task.assignment.workerName}. Deleting it also removes the assignment.`
      : `Delete "${task.name}"?`;
    confirmThen(this.dialog, { title: 'Delete task', message }, () =>
      this.tasksApi.delete(task.id),
    ).subscribe(() => {
      this.snackBar.open('Task deleted', undefined, { duration: 2500 });
      this.load();
    });
  }

  protected load(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.tasksApi.list().subscribe({
      next: (tasks) => {
        this.tasks.set(tasks);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.loadError.set(true);
      },
    });
  }
}

/** Matches the task name, one of its skills or the assigned worker's name (query already lower-cased). */
function matchesSearch(task: Task, query: string): boolean {
  return (
    !query ||
    task.name.toLowerCase().includes(query) ||
    task.skills.some((s) => s.toLowerCase().includes(query)) ||
    (task.assignment?.workerName.toLowerCase().includes(query) ?? false)
  );
}
