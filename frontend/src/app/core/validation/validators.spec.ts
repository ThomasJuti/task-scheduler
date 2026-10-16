import { FormControl } from '@angular/forms';

import { integer, notBlank, skillsValidator } from './validators';

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
    expect(notBlank(new FormControl(''))).toBeNull(); // lo maneja required
  });

  it('integer rejects decimals', () => {
    expect(integer(new FormControl(2.5))).toEqual({ integer: true });
    expect(integer(new FormControl(8))).toBeNull();
  });
});
