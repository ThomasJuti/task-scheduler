import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Worker, WorkerRequest } from '../models';

@Injectable({ providedIn: 'root' })
export class WorkersApi {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/workers';

  list(): Observable<Worker[]> {
    return this.http.get<Worker[]>(this.url);
  }

  create(worker: WorkerRequest): Observable<Worker> {
    return this.http.post<Worker>(this.url, worker);
  }

  update(id: number, worker: WorkerRequest): Observable<Worker> {
    return this.http.put<Worker>(`${this.url}/${id}`, worker);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
