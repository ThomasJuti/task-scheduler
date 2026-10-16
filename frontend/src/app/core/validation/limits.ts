/** Replica backend common/Limits.java: cambia ambos a la vez. */
export const TASK_NAME_MAX = 120;
export const WORKER_NAME_MAX = 120;
export const EFFORT_MIN = 1;
export const EFFORT_MAX = 1000;
export const AVAILABILITY_MIN = 1;
export const AVAILABILITY_MAX = 24;
export const TASK_SKILLS_MAX = 20;
export const WORKER_SKILLS_MAX = 30;
export const SKILL_NAME_MAX = 50;
/** La fecha de inicio no puede pasar de hoy + estos años (igual que el backend). */
export const PLANNING_HORIZON_YEARS = 2;
