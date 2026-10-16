import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Observable, filter, switchMap } from 'rxjs';

export interface ConfirmData {
  title: string;
  message: string;
  confirmLabel?: string;
}

@Component({
  selector: 'app-confirm-dialog',
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>{{ data.message }}</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button class="danger" [mat-dialog-close]="true">
        {{ data.confirmLabel ?? 'Delete' }}
      </button>
    </mat-dialog-actions>
  `,
})
export class ConfirmDialog {
  protected readonly data = inject<ConfirmData>(MAT_DIALOG_DATA);
}

/** Asks for confirmation and only then runs {@code action}; emits its result, or nothing if cancelled. */
export function confirmThen<T>(
  dialog: MatDialog,
  data: ConfirmData,
  action: () => Observable<T>,
): Observable<T> {
  return dialog
    .open<ConfirmDialog, ConfirmData, boolean>(ConfirmDialog, { data })
    .afterClosed()
    .pipe(filter(Boolean), switchMap(action));
}
