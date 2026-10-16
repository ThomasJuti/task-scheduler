import { Component, computed, input, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatChipInputEvent, MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { COMMA, ENTER } from '@angular/cdk/keycodes';

import { FieldErrorPipe } from './field-error.pipe';

/** Chip input for a list of skills, with autocomplete from the skills that already exist. */
@Component({
  selector: 'app-skills-input',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatChipsModule, MatIconModule, MatAutocompleteModule, FieldErrorPipe],
  template: `
    <mat-form-field appearance="outline" class="full-width">
      <mat-label>Skills</mat-label>
      <mat-chip-grid #grid [formControl]="control()" aria-label="Skills">
        @for (skill of control().value; track skill) {
          <mat-chip-row (removed)="remove(skill)">
            {{ skill }}
            <button matChipRemove [attr.aria-label]="'Remove ' + skill"><mat-icon>cancel</mat-icon></button>
          </mat-chip-row>
        }
        <input
          placeholder="Type a skill and press Enter"
          [matChipInputFor]="grid"
          [matAutocomplete]="auto"
          [matChipInputSeparatorKeyCodes]="separators"
          (matChipInputTokenEnd)="add($event)"
          (input)="query.set($any($event.target).value)"
          maxlength="50"
        />
      </mat-chip-grid>
      <mat-autocomplete #auto="matAutocomplete" (optionSelected)="selected($event)">
        @for (option of suggestions(); track option) {
          <mat-option [value]="option">{{ option }}</mat-option>
        }
      </mat-autocomplete>
      <mat-hint>{{ hint() }}</mat-hint>
      <mat-error>{{ control().errors | fieldError }}</mat-error>
    </mat-form-field>
  `,
})
export class SkillsInput {
  readonly control = input.required<FormControl<string[]>>();
  readonly options = input<string[]>([]);
  readonly hint = input('Letters, digits, spaces and + # . - /');

  protected readonly separators = [ENTER, COMMA];
  protected readonly query = signal('');
  protected readonly suggestions = computed(() => {
    const q = this.query().trim().toLowerCase();
    const chosen = new Set(this.control().value.map((s) => s.toLowerCase()));
    return this.options()
      .filter((o) => !chosen.has(o.toLowerCase()) && o.toLowerCase().includes(q))
      .slice(0, 8);
  });

  protected add(event: MatChipInputEvent): void {
    this.push(event.value);
    event.chipInput.clear();
  }

  protected selected(event: MatAutocompleteSelectedEvent): void {
    this.push(event.option.viewValue);
    event.option.deselect();
  }

  protected remove(skill: string): void {
    const control = this.control();
    control.setValue(control.value.filter((s) => s !== skill));
    control.markAsTouched();
  }

  private push(raw: string): void {
    const value = raw.trim().replace(/\s+/g, ' ');
    const control = this.control();
    if (value) {
      control.setValue([...control.value, value]);
    }
    control.markAsTouched();
    this.query.set('');
  }
}
