import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AssignRequest, Assignment, AutoAssignResult, ReassignRequest } from '../models';

@Injectable({ providedIn: 'root' })
export class AssignmentsApi {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/assignments';

  list(workerId?: number): Observable<Assignment[]> {
    const params = workerId ? new HttpParams().set('workerId', workerId) : undefined;
    return this.http.get<Assignment[]>(this.url, { params });
  }

  assign(request: AssignRequest): Observable<Assignment[]> {
    return this.http.post<Assignment[]>(this.url, request);
  }

  reassign(id: number, request: ReassignRequest): Observable<Assignment> {
    return this.http.patch<Assignment>(`${this.url}/${id}`, request);
  }

  unassign(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  autoAssign(startDate: string, dryRun: boolean): Observable<AutoAssignResult> {
    return this.http.post<AutoAssignResult>(`${this.url}/auto`, { startDate, dryRun });
  }
}
