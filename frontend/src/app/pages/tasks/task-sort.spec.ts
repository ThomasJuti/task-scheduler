import { Task } from '../../core/models';
import { sortTasks } from './task-sort';

function task(id: number, name: string, effortHours: number, workerName?: string): Task {
  return {
    id,
    name,
    effortHours,
    skills: [],
    status: 'NOT_DONE',
    assignment: workerName
      ? { id, workerId: id, workerName, startDate: '2026-10-19', endDate: '2026-10-19' }
      : null,
  };
}

describe('sortTasks', () => {
  const tasks = [task(1, 'Beta', 8, 'Zoe'), task(2, 'alpha', 16), task(3, 'Gamma', 2, 'Ana')];
  const ids = (sorted: Task[]) => sorted.map((t) => t.id);

  it('keeps the original order when there is no sort', () => {
    expect(sortTasks(tasks, { active: '', direction: '' })).toBe(tasks);
    expect(sortTasks(tasks, { active: 'name', direction: '' })).toBe(tasks);
  });

  it('sorts names alphabetically ignoring case', () => {
    expect(ids(sortTasks(tasks, { active: 'name', direction: 'asc' }))).toEqual([2, 1, 3]);
  });

  it('sorts numbers numerically in both directions', () => {
    expect(ids(sortTasks(tasks, { active: 'effortHours', direction: 'asc' }))).toEqual([3, 1, 2]);
    expect(ids(sortTasks(tasks, { active: 'effortHours', direction: 'desc' }))).toEqual([2, 1, 3]);
  });

  it('sorts by worker name with unassigned tasks first', () => {
    expect(ids(sortTasks(tasks, { active: 'assignment', direction: 'asc' }))).toEqual([2, 3, 1]);
  });

  it('does not modify the input array', () => {
    sortTasks(tasks, { active: 'effortHours', direction: 'asc' });
    expect(ids(tasks)).toEqual([1, 2, 3]);
  });
});
