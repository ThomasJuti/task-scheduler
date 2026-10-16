import { Component, input } from '@angular/core';

/** Read-only list of skills; skills in {@code missing} are highlighted. */
@Component({
  selector: 'app-skill-chips',
  template: `
    <span class="skill-list">
      @for (skill of skills(); track skill) {
        <span class="skill" [class.skill-missing]="missing().includes(skill)">{{ skill }}</span>
      }
    </span>
  `,
})
export class SkillChips {
  readonly skills = input.required<string[]>();
  readonly missing = input<string[]>([]);
}
