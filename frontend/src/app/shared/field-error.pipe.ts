import { Pipe, PipeTransform } from '@angular/core';
import { ValidationErrors } from '@angular/forms';

@Pipe({ name: 'fieldError' })
export class FieldErrorPipe implements PipeTransform {
  transform(errors: ValidationErrors | null | undefined): string {
    if (!errors) return '';
    if (errors['server']) return errors['server'];
    if (errors['required']) return 'This field is required';
    if (errors['blank']) return 'Cannot be only spaces';
    if (errors['maxlength']) return `Maximum ${errors['maxlength'].requiredLength} characters`;
    if (errors['min']) return `Minimum value is ${errors['min'].min}`;
    if (errors['max']) return `Maximum value is ${errors['max'].max}`;
    if (errors['integer']) return 'Use whole hours';
    if (errors['skillsRequired']) return 'Add at least one skill';
    if (errors['skillsMax']) return `Maximum ${errors['skillsMax'].max} skills`;
    if (errors['skillFormat']) return `"${errors['skillFormat'].skill}" is not a valid skill name`;
    if (errors['skillDuplicate']) return `"${errors['skillDuplicate'].skill}" is repeated`;
    if (errors['matDatepickerParse']) return 'Invalid date';
    if (errors['matDatepickerMin']) return 'The date cannot be in the past';
    if (errors['matDatepickerFilter']) return 'Choose a working day (Mon-Fri)';
    return 'Invalid value';
  }
}
