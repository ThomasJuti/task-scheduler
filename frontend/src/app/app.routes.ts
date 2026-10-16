import { Routes } from '@angular/router';

import { authGuard, guestGuard } from './core/auth/auth.guards';
import { Shell } from './layout/shell';

export const routes: Routes = [
  { path: 'login', canActivate: [guestGuard], loadComponent: () => import('./pages/login/login').then((m) => m.Login), title: 'Sign in' },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: 'tasks', loadComponent: () => import('./pages/tasks/tasks-page').then((m) => m.TasksPage), title: 'Tasks' },
      { path: 'workers', loadComponent: () => import('./pages/workers/workers-page').then((m) => m.WorkersPage), title: 'Workers' },
      { path: 'assignments', loadComponent: () => import('./pages/assignments/assignments-page').then((m) => m.AssignmentsPage), title: 'Assignments' },
      { path: '', pathMatch: 'full', redirectTo: 'tasks' },
    ],
  },
  { path: '**', redirectTo: '' },
];
