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

import { SkillsApi } from '../../core/api/skills-api';
import { WorkersApi } from '../../core/api/workers-api';
import { RequiresRoleDirective } from '../../core/auth/requires-role.directive';
import { Worker } from '../../core/models';
import { confirmThen } from '../../shared/confirm-dialog';
import { openEditDialog } from '../../shared/edit-dialog';
import { SkillChips } from '../../shared/skill-chips';
import { WorkerDialog, WorkerDialogData } from './worker-dialog';

@Component({
  selector: 'app-workers-page',
  imports: [
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule,
    MatProgressBarModule,
    RequiresRoleDirective,
    SkillChips,
  ],
  templateUrl: './workers-page.html',
})
export class WorkersPage {
  private readonly workersApi = inject(WorkersApi);
  private readonly skillsApi = inject(SkillsApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly columns = ['name', 'availability', 'skills', 'load', 'actions'];
  protected readonly workers = signal<Worker[]>([]);
  protected readonly loading = signal(true);
  protected readonly loadError = signal(false);
  protected readonly search = signal('');
  protected readonly totalCapacity = computed(() =>
    this.workers().reduce((sum, w) => sum + w.availabilityHoursPerDay, 0),
  );
  protected readonly rows = computed(() => {
    const q = this.search().trim().toLowerCase();
    return this.workers().filter(
      (w) =>
        !q || w.name.toLowerCase().includes(q) || w.skills.some((s) => s.toLowerCase().includes(q)),
    );
  });

  constructor() {
    this.load();
  }

  protected days(worker: Worker): number {
    return Math.ceil(worker.assignedHours / worker.availabilityHoursPerDay);
  }

  protected openDialog(worker?: Worker): void {
    openEditDialog<WorkerDialogData, Worker>(this.dialog, this.skillsApi, WorkerDialog, {
      worker,
    }).subscribe((saved) => {
      this.snackBar.open(`Worker "${saved.name}" saved`, undefined, { duration: 2500 });
      this.load();
    });
  }

  protected remove(worker: Worker): void {
    const message =
      worker.assignedTasks > 0
        ? `${worker.name} has ${worker.assignedTasks} assigned task(s). Deleting the worker leaves them unassigned.`
        : `Delete ${worker.name}?`;
    confirmThen(this.dialog, { title: 'Delete worker', message }, () =>
      this.workersApi.delete(worker.id),
    ).subscribe(() => {
      this.snackBar.open('Worker deleted', undefined, { duration: 2500 });
      this.load();
    });
  }

  protected load(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.workersApi.list().subscribe({
      next: (workers) => {
        this.workers.set(workers);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.loadError.set(true);
      },
    });
  }
}
