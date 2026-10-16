import { Sort } from '@angular/material/sort';

import { TaskQuery, TaskSortField } from '../../core/models';

/** Columnas de la tabla que el backend sabe ordenar; "Assigned to" no se ordena. */
const SORTABLE: readonly TaskSortField[] = ['name', 'effortHours', 'status'];

/** Traduce el orden de MatSort al parámetro {@code sort} del API, o nada (orden por id) si no aplica. */
export function toTaskSort(sort: Sort): TaskQuery['sort'] {
  const field = SORTABLE.find((f) => f === sort.active);
  if (!field || !sort.direction) return undefined;
  return { field, direction: sort.direction };
}
