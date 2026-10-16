import { missingSkills } from './skills';

describe('missingSkills', () => {
  it('compares case-insensitively', () => {
    expect(missingSkills(['Java', 'SQL', 'Docker'], ['java', 'sql'])).toEqual(['Docker']);
    expect(missingSkills(['Java'], ['JAVA', 'Angular'])).toEqual([]);
  });
});
