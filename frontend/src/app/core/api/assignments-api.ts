import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AssignRequest, Assignment, AutoAssignResult, Page, ReassignRequest } from '../models';

@Injectable({ providedIn: 'root' })
export class AssignmentsApi {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/assignments';

  list(query: { workerId?: number | null; page?: number; size?: number } = {}): Observable<Page<Assignment>> {
    let params = new HttpParams();
    if (query.workerId) params = params.set('workerId', query.workerId);
    if (query.page !== undefined) params = params.set('page', query.page);
    if (query.size !== undefined) params = params.set('size', query.size);
    return this.http.get<Page<Assignment>>(this.url, { params });
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
