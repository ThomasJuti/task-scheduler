import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup } from '@angular/forms';

import { applyServerErrors, problemMessage } from './problem';

describe('problem helpers', () => {
  it('maps backend field errors onto the form controls', () => {
    const form = new FormGroup({ name: new FormControl(''), skills: new FormControl<string[]>([]) });
    const error = new HttpErrorResponse({
      status: 400,
      error: { status: 400, detail: 'Validation failed', errors: { name: 'must not be blank', 'skills[0]': 'invalid' } },
    });

    expect(applyServerErrors(form, error)).toBe(true);
    expect(form.controls.name.errors).toEqual({ server: 'must not be blank' });
    expect(form.controls.skills.errors).toEqual({ server: 'invalid' });
  });

  it('explains network errors', () => {
    expect(problemMessage(new HttpErrorResponse({ status: 0 }))).toContain('Cannot reach the server');
  });

  it('uses the problem detail', () => {
    const error = new HttpErrorResponse({ status: 422, error: { status: 422, detail: 'missing skills [Java]' } });
    expect(problemMessage(error)).toBe('missing skills [Java]');
  });
});
