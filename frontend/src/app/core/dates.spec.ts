import { isWorkingDay, maxPlanningDate, nextWorkingDay, toIsoDate } from './dates';

describe('dates', () => {
  it('only Monday to Friday are working days', () => {
    expect(isWorkingDay(new Date(2026, 9, 16))).toBe(true); // viernes
    expect(isWorkingDay(new Date(2026, 9, 17))).toBe(false); // sábado
    expect(isWorkingDay(new Date(2026, 9, 18))).toBe(false); // domingo
    expect(toIsoDate(nextWorkingDay(new Date(2026, 9, 17)))).toBe('2026-10-19');
  });

  it('formats dates in local time', () => {
    expect(toIsoDate(new Date(2026, 0, 5))).toBe('2026-01-05');
  });

  it('limits start dates to the same planning horizon as the backend', () => {
    expect(toIsoDate(maxPlanningDate(new Date(2026, 9, 16)))).toBe('2028-10-16');
  });
});
