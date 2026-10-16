/**
 * Skills of the task that the worker lacks (case-insensitive), i.e. why Req3a (skills rule: a task's skills
 * must be a subset of the worker's) would reject the assignment.
 */
export function missingSkills(taskSkills: string[], workerSkills: string[]): string[] {
  const available = new Set(workerSkills.map((s) => s.toLowerCase()));
  return taskSkills.filter((s) => !available.has(s.toLowerCase()));
}
