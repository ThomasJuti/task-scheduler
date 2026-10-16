export type Role = 'ADMIN' | 'USER';
export type TaskStatus = 'DONE' | 'NOT_DONE';

export interface AssignmentSummary {
  id: number;
  workerId: number;
  workerName: string;
  startDate: string;
  endDate: string;
}

export interface Task {
  id: number;
  name: string;
  effortHours: number;
  skills: string[];
  status: TaskStatus;
  assignment: AssignmentSummary | null;
}

export interface TaskRequest {
  name: string;
  effortHours: number;
  skills: string[];
  status: TaskStatus;
}

export interface Worker {
  id: number;
  name: string;
  availabilityHoursPerDay: number;
  skills: string[];
  assignedTasks: number;
  assignedHours: number;
}

export interface WorkerRequest {
  name: string;
  availabilityHoursPerDay: number;
  skills: string[];
}

export interface Allocation {
  date: string;
  hours: number;
}

export interface Assignment {
  id: number;
  workerId: number;
  workerName: string;
  taskId: number;
  taskName: string;
  effortHours: number;
  taskStatus: TaskStatus;
  taskSkills: string[];
  startDate: string;
  endDate: string;
  allocations: Allocation[];
}

export interface AssignRequest {
  workerId: number;
  taskIds: number[];
  startDate: string;
}

export interface ReassignRequest {
  workerId: number;
  startDate: string;
}

export interface AutoAssignedItem {
  taskId: number;
  taskName: string;
  effortHours: number;
  workerId: number;
  workerName: string;
  startDate: string;
  endDate: string;
}

export interface UnassignedItem {
  taskId: number;
  taskName: string;
  skills: string[];
  reason: string;
}

export interface AutoAssignResult {
  dryRun: boolean;
  startDate: string;
  assigned: AutoAssignedItem[];
  unassigned: UnassignedItem[];
}

export interface LoginResponse {
  token: string;
  username: string;
  role: Role;
  expiresAt: string;
}

export interface Problem {
  status: number;
  title?: string;
  detail?: string;
  errors?: Record<string, string>;
}

/** Una página de resultados del API (tareas, asignaciones). */
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface TaskStats {
  total: number;
  pending: number;
  done: number;
  pendingUnassigned: number;
}

export type TaskSortField = 'id' | 'name' | 'effortHours' | 'status';

export interface TaskQuery {
  status?: TaskStatus;
  unassigned?: boolean;
  q?: string;
  page?: number;
  size?: number;
  sort?: { field: TaskSortField; direction: 'asc' | 'desc' };
}
