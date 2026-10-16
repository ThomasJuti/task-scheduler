import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Same rule as the backend: letters/digits first, then letters, digits, spaces and + # . - / */
export const SKILL_PATTERN = /^\s*[\p{L}\p{N}][\p{L}\p{N} +#.\-/]*$/u;
export const SKILL_MAX_LENGTH = 50;

/** Rejects values made only of spaces (Validators.required accepts them). */
export const notBlank: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value;
  return typeof value === 'string' && value.length > 0 && value.trim().length === 0 ? { blank: true } : null;
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
    const invalid = skills.find((s) => !SKILL_PATTERN.test(s) || s.trim().length > SKILL_MAX_LENGTH);
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

/** Datepicker filter: Monday to Friday only. */
export const isWorkingDay = (date: Date | null): boolean => {
  if (!date) return false;
  const day = date.getDay();
  return day !== 0 && day !== 6;
};

/** First working day from today (today itself when it is a weekday). */
export function nextWorkingDay(from = new Date()): Date {
  const date = new Date(from.getFullYear(), from.getMonth(), from.getDate());
  while (!isWorkingDay(date)) date.setDate(date.getDate() + 1);
  return date;
}

export function startOfToday(): Date {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate());
}

/** Local date as YYYY-MM-DD (toISOString would shift the day in negative UTC offsets like Colombia). */
export function toIsoDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

/** Skills of the task that the worker lacks (case-insensitive), i.e. why Req3a would reject it. */
export function missingSkills(taskSkills: string[], workerSkills: string[]): string[] {
  const available = new Set(workerSkills.map((s) => s.toLowerCase()));
  return taskSkills.filter((s) => !available.has(s.toLowerCase()));
}
