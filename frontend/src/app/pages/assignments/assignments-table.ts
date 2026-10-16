import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';

import { Assignment } from '../../core/models';

@Component({
  selector: 'app-assignments-table',
  imports: [DatePipe, MatTableModule, MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './assignments-table.html',
})
export class AssignmentsTable {
  readonly assignments = input.required<Assignment[]>();
  readonly reassign = output<Assignment>();
  readonly unassign = output<Assignment>();

  protected readonly columns = ['task', 'worker', 'dates', 'actions'];

  protected allocationText(assignment: Assignment): string {
    return assignment.allocations.map((al) => `${al.date}: ${al.hours} h`).join(' · ');
  }
}
