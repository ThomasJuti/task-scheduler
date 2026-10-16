import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

import { SKILL_NAME_MAX } from './limits';

/** Same rule as the backend: letters/digits first, then letters, digits, spaces and + # . - / */
export const SKILL_PATTERN = /^\s*[\p{L}\p{N}][\p{L}\p{N} +#.\-/]*$/u;

/** Rejects values made only of spaces (Validators.required accepts them). */
export const notBlank: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value;
  return typeof value === 'string' && value.length > 0 && value.trim().length === 0
    ? { blank: true }
    : null;
};

/** Whole numbers only (the API works with integer hours). */
export const integer: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value;
  if (value === null || value === undefined || value === '') return null;
  return Number.isInteger(Number(value)) ? null : { integer: true };
};

/** Validates a string[] of skills: count limits, format, length and no case-insensitive duplicates. */
export function skillsValidator(max: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const skills = (control.value ?? []) as string[];
    if (skills.length === 0) return { skillsRequired: true };
    if (skills.length > max) return { skillsMax: { max } };
    const invalid = skills.find((s) => !SKILL_PATTERN.test(s) || s.trim().length > SKILL_NAME_MAX);
    if (invalid !== undefined) return { skillFormat: { skill: invalid } };
    const seen = new Set<string>();
    for (const skill of skills) {
      const key = skill.trim().toLowerCase();
      if (seen.has(key)) return { skillDuplicate: { skill } };
      seen.add(key);
    }
    return null;
  };
}
