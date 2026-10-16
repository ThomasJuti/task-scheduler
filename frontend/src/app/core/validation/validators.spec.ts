import { FormControl } from '@angular/forms';

import { integer, isWorkingDay, missingSkills, nextWorkingDay, notBlank, skillsValidator, toIsoDate } from './validators';

describe('validators', () => {
  describe('skillsValidator', () => {
    const validate = (skills: string[]) => skillsValidator(3)(new FormControl(skills));

    it('accepts valid skills', () => {
      expect(validate(['Java', 'C#', 'Node.js'])).toBeNull();
      expect(validate(['CI/CD'])).toBeNull();
    });

    it('requires at least one skill', () => {
      expect(validate([])).toEqual({ skillsRequired: true });
    });

    it('limits the number of skills', () => {
      expect(validate(['a', 'b', 'c', 'd'])).toEqual({ skillsMax: { max: 3 } });
    });

    it('rejects unsafe characters', () => {
      expect(validate(['<script>'])).toEqual({ skillFormat: { skill: '<script>' } });
    });

    it('rejects case-insensitive duplicates', () => {
      expect(validate(['Java', 'java'])).toEqual({ skillDuplicate: { skill: 'java' } });
    });
  });

  it('notBlank rejects whitespace-only values', () => {
    expect(notBlank(new FormControl('   '))).toEqual({ blank: true });
    expect(notBlank(new FormControl('Task'))).toBeNull();
    expect(notBlank(new FormControl(''))).toBeNull(); // handled by required
  });

  it('integer rejects decimals', () => {
    expect(integer(new FormControl(2.5))).toEqual({ integer: true });
    expect(integer(new FormControl(8))).toBeNull();
  });

  it('only Monday to Friday are working days', () => {
    expect(isWorkingDay(new Date(2026, 9, 16))).toBe(true); // Friday
    expect(isWorkingDay(new Date(2026, 9, 17))).toBe(false); // Saturday
    expect(isWorkingDay(new Date(2026, 9, 18))).toBe(false); // Sunday
    expect(toIsoDate(nextWorkingDay(new Date(2026, 9, 17)))).toBe('2026-10-19');
  });

  it('formats dates in local time', () => {
    expect(toIsoDate(new Date(2026, 0, 5))).toBe('2026-01-05');
  });

  it('missingSkills compares case-insensitively', () => {
    expect(missingSkills(['Java', 'SQL', 'Docker'], ['java', 'sql'])).toEqual(['Docker']);
    expect(missingSkills(['Java'], ['JAVA', 'Angular'])).toEqual([]);
  });
});
