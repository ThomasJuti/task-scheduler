import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { filter, switchMap } from 'rxjs';

import { SkillsApi } from '../../core/api/skills-api';
import { WorkersApi } from '../../core/api/workers-api';
import { HasRoleDirective } from '../../core/auth/has-role.directive';
import { Worker } from '../../core/models';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { SkillChips } from '../../shared/skill-chips';
import { WorkerDialog, WorkerDialogData } from './worker-dialog';

@Component({
  selector: 'app-workers-page',
  imports: [FormsModule, MatTableModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, MatTooltipModule,
    MatProgressBarModule, HasRoleDirective, SkillChips],
  template: `
    <header class="page-header">
      <div>
        <h1>Workers</h1>
        <p class="subtitle">{{ workers().length }} workers · {{ totalCapacity() }} h/day of capacity</p>
      </div>
      <button mat-flat-button *appHasRole="'ADMIN'" (click)="openDialog()"><mat-icon>person_add</mat-icon> New worker</button>
    </header>

    <div class="toolbar-row">
      <mat-form-field appearance="outline" subscriptSizing="dynamic" class="search">
        <mat-icon matPrefix>search</mat-icon>
        <mat-label>Search by name or skill</mat-label>
        <input matInput [ngModel]="search()" (ngModelChange)="search.set($event)" maxlength="100" />
      </mat-form-field>
    </div>

    <div class="table-card">
      @if (loading()) { <mat-progress-bar mode="indeterminate" /> }
      <table mat-table [dataSource]="rows()">
        <ng-container matColumnDef="name">
          <th mat-header-cell *matHeaderCellDef>Name</th>
          <td mat-cell *matCellDef="let w" class="strong">{{ w.name }}</td>
        </ng-container>
        <ng-container matColumnDef="availability">
          <th mat-header-cell *matHeaderCellDef>Availability</th>
          <td mat-cell *matCellDef="let w">{{ w.availabilityHoursPerDay }} h/day</td>
        </ng-container>
        <ng-container matColumnDef="skills">
          <th mat-header-cell *matHeaderCellDef>Skills</th>
          <td mat-cell *matCellDef="let w"><app-skill-chips [skills]="w.skills" /></td>
        </ng-container>
        <ng-container matColumnDef="load">
          <th mat-header-cell *matHeaderCellDef>Assigned</th>
          <td mat-cell *matCellDef="let w">
            <div class="strong">{{ w.assignedTasks }} task{{ w.assignedTasks === 1 ? '' : 's' }}</div>
            <div class="muted">{{ w.assignedHours }} h ≈ {{ days(w) }} working day{{ days(w) === 1 ? '' : 's' }}</div>
          </td>
        </ng-container>
        <ng-container matColumnDef="actions">
          <th mat-header-cell *matHeaderCellDef></th>
          <td mat-cell *matCellDef="let w" class="actions">
            <ng-container *appHasRole="'ADMIN'">
              <button mat-icon-button matTooltip="Edit" (click)="openDialog(w)"><mat-icon>edit</mat-icon></button>
              <button mat-icon-button matTooltip="Delete" (click)="remove(w)"><mat-icon>delete</mat-icon></button>
            </ng-container>
          </td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="columns"></tr>
        <tr mat-row *matRowDef="let row; columns: columns"></tr>
        <tr class="mat-row" *matNoDataRow>
          <td class="mat-cell empty" [attr.colspan]="columns.length">No workers yet.</td>
        </tr>
      </table>
    </div>
  `,
})
export class WorkersPage {
  private readonly workersApi = inject(WorkersApi);
  private readonly skillsApi = inject(SkillsApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly columns = ['name', 'availability', 'skills', 'load', 'actions'];
  protected readonly workers = signal<Worker[]>([]);
  protected readonly loading = signal(true);
  protected readonly search = signal('');
  protected readonly totalCapacity = computed(() => this.workers().reduce((sum, w) => sum + w.availabilityHoursPerDay, 0));
  protected readonly rows = computed(() => {
    const q = this.search().trim().toLowerCase();
    return this.workers().filter((w) =>
      !q || w.name.toLowerCase().includes(q) || w.skills.some((s) => s.toLowerCase().includes(q)));
  });

  constructor() {
    this.load();
  }

  protected days(worker: Worker): number {
    return Math.ceil(worker.assignedHours / worker.availabilityHoursPerDay);
  }

  protected openDialog(worker?: Worker): void {
    this.skillsApi.list().pipe(
      switchMap((skillOptions) => this.dialog.open<WorkerDialog, WorkerDialogData, Worker>(WorkerDialog, {
        data: { worker, skillOptions }, width: '520px',
      }).afterClosed()),
      filter(Boolean),
    ).subscribe((saved) => {
      this.snackBar.open(`Worker "${saved.name}" saved`, undefined, { duration: 2500 });
      this.load();
    });
  }

  protected remove(worker: Worker): void {
    const message = worker.assignedTasks > 0
      ? `${worker.name} has ${worker.assignedTasks} assigned task(s). Deleting the worker leaves them unassigned.`
      : `Delete ${worker.name}?`;
    this.dialog.open(ConfirmDialog, { data: { title: 'Delete worker', message } }).afterClosed()
      .pipe(filter(Boolean), switchMap(() => this.workersApi.delete(worker.id)))
      .subscribe(() => {
        this.snackBar.open('Worker deleted', undefined, { duration: 2500 });
        this.load();
      });
  }

  private load(): void {
    this.loading.set(true);
    this.workersApi.list().subscribe({
      next: (workers) => {
        this.workers.set(workers);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
