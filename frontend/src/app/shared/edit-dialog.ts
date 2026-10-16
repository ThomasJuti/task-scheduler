import { ComponentType } from '@angular/cdk/portal';
import { MatDialog } from '@angular/material/dialog';
import { Observable, filter, switchMap } from 'rxjs';

import { SkillsApi } from '../core/api/skills-api';

/**
 * Opens a create/edit dialog once the known skills are loaded (they feed the skills autocomplete).
 * Emits the saved entity, or nothing when the dialog is cancelled.
 */
export function openEditDialog<D extends { skillOptions: string[] }, R>(
  dialog: MatDialog,
  skillsApi: SkillsApi,
  component: ComponentType<unknown>,
  data: Omit<D, 'skillOptions'>,
): Observable<R> {
  return skillsApi.list().pipe(
    switchMap((skillOptions) =>
      dialog
        .open<unknown, D, R>(component, { data: { ...data, skillOptions } as D, width: '520px' })
        .afterClosed(),
    ),
    filter((saved): saved is R => saved !== undefined),
  );
}
