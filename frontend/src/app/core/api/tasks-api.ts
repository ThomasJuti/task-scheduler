import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Page, Task, TaskQuery, TaskRequest, TaskStats, TaskStatus } from '../models';

@Injectable({ providedIn: 'root' })
export class TasksApi {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/tasks';

  /** Una página de tareas; el filtro, la búsqueda y el orden se resuelven en el servidor. */
  list(query: TaskQuery = {}): Observable<Page<Task>> {
    let params = new HttpParams();
    if (query.status) params = params.set('status', query.status);
    if (query.unassigned !== undefined) params = params.set('unassigned', query.unassigned);
    if (query.q?.trim()) params = params.set('q', query.q.trim());
    if (query.page !== undefined) params = params.set('page', query.page);
    if (query.size !== undefined) params = params.set('size', query.size);
    if (query.sort) params = params.set('sort', `${query.sort.field},${query.sort.direction}`);
    return this.http.get<Page<Task>>(this.url, { params });
  }

  stats(): Observable<TaskStats> {
    return this.http.get<TaskStats>(`${this.url}/stats`);
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
