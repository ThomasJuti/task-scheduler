import { Sort } from '@angular/material/sort';

import { Task } from '../../core/models';

/** Value each sortable column of the tasks table is ordered by. */
const SORT_VALUES: Record<string, (task: Task) => string | number> = {
  name: (task) => task.name,
  effortHours: (task) => task.effortHours,
  assignment: (task) => task.assignment?.workerName ?? '',
  status: (task) => task.status,
};

function compareValues(a: string | number, b: string | number): number {
  if (typeof a === 'number' && typeof b === 'number') return a - b;
  return String(a).localeCompare(String(b));
}

/** Comparator for the active column and direction, or null when the table is not sorted. */
export function taskComparator(sort: Sort): ((a: Task, b: Task) => number) | null {
  const value = SORT_VALUES[sort.active];
  if (!value || !sort.direction) return null;
  const sign = sort.direction === 'asc' ? 1 : -1;
  return (a, b) => compareValues(value(a), value(b)) * sign;
}

/** Sorted copy of the tasks (the same array when there is no sort). */
export function sortTasks(tasks: Task[], sort: Sort): Task[] {
  const comparator = taskComparator(sort);
  return comparator ? [...tasks].sort(comparator) : tasks;
}
