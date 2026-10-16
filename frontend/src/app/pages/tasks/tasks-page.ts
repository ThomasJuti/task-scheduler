import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';

import { EMPTY, Subject, catchError, debounceTime, distinctUntilChanged, finalize, switchMap } from 'rxjs';

import { SkillsApi } from '../../core/api/skills-api';
import { TasksApi } from '../../core/api/tasks-api';
import { RequiresRoleDirective } from '../../core/auth/requires-role.directive';
import { Task, TaskQuery, TaskStats } from '../../core/models';
import { PAGE_SIZE, PAGE_SIZE_MAX } from '../../core/validation/limits';
import { confirmThen } from '../../shared/confirm-dialog';
import { openEditDialog } from '../../shared/edit-dialog';
import { SkillChips } from '../../shared/skill-chips';
import { TaskDialog, TaskDialogData } from './task-dialog';
import { toTaskSort } from './task-sort';

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
    MatPaginatorModule,
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
  protected readonly pageSizeOptions = [10, 20, 50, PAGE_SIZE_MAX];
  protected readonly tasks = signal<Task[]>([]);
  protected readonly total = signal(0);
  protected readonly stats = signal<TaskStats | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal(false);
  protected readonly search = signal('');
  protected readonly statusFilter = signal<StatusFilter>('ALL');
  protected readonly sort = signal<Sort>({ active: '', direction: '' });
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(PAGE_SIZE);

  private readonly searchInput = new Subject<string>();
  private readonly reload = new Subject<void>();

  constructor() {
    // Busca en el servidor 300 ms después de dejar de escribir
    this.searchInput
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((text) => {
        this.search.set(text);
        this.goToFirstPage();
      });
    // switchMap descarta la respuesta de una búsqueda vieja si llega después de una nueva
    this.reload
      .pipe(
        switchMap(() => {
          this.loading.set(true);
          this.loadError.set(false);
          return this.tasksApi.list(this.query()).pipe(
            catchError(() => {
              this.loadError.set(true);
              this.tasks.set([]);
              return EMPTY;
            }),
            finalize(() => this.loading.set(false)),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((page) => {
        this.tasks.set(page.content);
        this.total.set(page.totalElements);
      });
    this.load();
  }

  protected onSearch(text: string): void {
    this.searchInput.next(text);
  }

  protected onStatusFilter(filter: StatusFilter): void {
    this.statusFilter.set(filter);
    this.goToFirstPage();
  }

  protected onSort(sort: Sort): void {
    this.sort.set(sort);
    this.goToFirstPage();
  }

  protected onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.reload.next();
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
      next: (updated) => {
        if (this.statusFilter() === 'ALL') {
          this.tasks.update((all) => all.map((t) => (t.id === updated.id ? updated : t)));
          this.loadStats();
        } else {
          this.load(); // la tarea ya no cumple el filtro de estado
        }
      },
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
    this.reload.next();
    this.loadStats();
  }

  private loadStats(): void {
    this.tasksApi.stats().subscribe({ next: (stats) => this.stats.set(stats), error: () => undefined });
  }

  private goToFirstPage(): void {
    this.pageIndex.set(0);
    this.reload.next();
  }

  private query(): TaskQuery {
    const status = this.statusFilter();
    return {
      status: status === 'ALL' ? undefined : status,
      q: this.search(),
      page: this.pageIndex(),
      size: this.pageSize(),
      sort: toTaskSort(this.sort()),
    };
  }
}
