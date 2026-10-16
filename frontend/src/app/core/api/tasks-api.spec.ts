import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { TasksApi } from './tasks-api';

describe('TasksApi', () => {
  let api: TasksApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(TasksApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends paging, filters, search and sort to the server', () => {
    api
      .list({ status: 'NOT_DONE', unassigned: true, q: '  java ', page: 2, size: 50, sort: { field: 'name', direction: 'desc' } })
      .subscribe();

    const request = http.expectOne((r) => r.url === '/api/tasks');
    const params = request.request.params;
    expect(params.get('status')).toBe('NOT_DONE');
    expect(params.get('unassigned')).toBe('true');
    expect(params.get('q')).toBe('java');
    expect(params.get('page')).toBe('2');
    expect(params.get('size')).toBe('50');
    expect(params.get('sort')).toBe('name,desc');
    request.flush({ content: [], page: 2, size: 50, totalElements: 0, totalPages: 0 });
  });

  it('leaves out empty search and unset options', () => {
    api.list({ q: '   ' }).subscribe();

    const request = http.expectOne((r) => r.url === '/api/tasks');
    expect(request.request.params.keys()).toEqual([]);
    request.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  });
});
