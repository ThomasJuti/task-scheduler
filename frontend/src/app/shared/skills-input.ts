import { Component, computed, input, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import {
  MatAutocompleteModule,
  MatAutocompleteSelectedEvent,
} from '@angular/material/autocomplete';
import { MatChipInputEvent, MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { COMMA, ENTER } from '@angular/cdk/keycodes';

import { SKILL_NAME_MAX } from '../core/validation/limits';
import { FieldErrorPipe } from './field-error.pipe';

/** Chip input for a list of skills, with autocomplete from the skills that already exist. */
@Component({
  selector: 'app-skills-input',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatChipsModule,
    MatIconModule,
    MatAutocompleteModule,
    FieldErrorPipe,
  ],
  templateUrl: './skills-input.html',
})
export class SkillsInput {
  readonly control = input.required<FormControl<string[]>>();
  readonly options = input<string[]>([]);
  readonly hint = input('Letters, digits, spaces and + # . - /');

  protected readonly separators = [ENTER, COMMA];
  protected readonly skillNameMax = SKILL_NAME_MAX;
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
