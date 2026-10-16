export function missingSkills(taskSkills: string[], workerSkills: string[]): string[] {
  const available = new Set(workerSkills.map((s) => s.toLowerCase()));
  return taskSkills.filter((s) => !available.has(s.toLowerCase()));
}
