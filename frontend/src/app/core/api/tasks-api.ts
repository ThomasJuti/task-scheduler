import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Task, TaskRequest, TaskStatus } from '../models';

@Injectable({ providedIn: 'root' })
export class TasksApi {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/tasks';

  list(filter: { status?: TaskStatus; unassigned?: boolean } = {}): Observable<Task[]> {
    let params = new HttpParams();
    if (filter.status) params = params.set('status', filter.status);
    if (filter.unassigned !== undefined) params = params.set('unassigned', filter.unassigned);
    return this.http.get<Task[]>(this.url, { params });
  }

  create(task: TaskRequest): Observable<Task> {
    return this.http.post<Task>(this.url, task);
  }

  update(id: number, task: TaskRequest): Observable<Task> {
    return this.http.put<Task>(`${this.url}/${id}`, task);
  }

  updateStatus(id: number, status: TaskStatus): Observable<Task> {
    return this.http.patch<Task>(`${this.url}/${id}/status`, { status });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
