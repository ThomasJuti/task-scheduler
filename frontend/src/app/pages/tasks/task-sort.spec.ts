import { toTaskSort } from './task-sort';

describe('toTaskSort', () => {
  it('maps a sortable column and its direction', () => {
    expect(toTaskSort({ active: 'effortHours', direction: 'desc' })).toEqual({ field: 'effortHours', direction: 'desc' });
    expect(toTaskSort({ active: 'name', direction: 'asc' })).toEqual({ field: 'name', direction: 'asc' });
  });

  it('returns nothing when the sort is cleared', () => {
    expect(toTaskSort({ active: 'name', direction: '' })).toBeUndefined();
  });

  it('ignores columns the API cannot sort', () => {
    expect(toTaskSort({ active: 'assignment', direction: 'asc' })).toBeUndefined();
    expect(toTaskSort({ active: 'skills', direction: 'asc' })).toBeUndefined();
  });
});
