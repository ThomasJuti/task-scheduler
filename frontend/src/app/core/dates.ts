export const isWorkingDay = (date: Date | null): boolean => {
  if (!date) return false;
  const day = date.getDay();
  return day !== 0 && day !== 6;
};

export function nextWorkingDay(from = new Date()): Date {
  const date = new Date(from.getFullYear(), from.getMonth(), from.getDate());
  while (!isWorkingDay(date)) date.setDate(date.getDate() + 1);
  return date;
}

export function startOfToday(): Date {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate());
}

/** Fecha local como YYYY-MM-DD (toISOString correría el día en offsets UTC negativos como el de Colombia). */
export function toIsoDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}
