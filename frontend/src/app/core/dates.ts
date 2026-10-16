/** Datepicker filter: Monday to Friday only. */
export const isWorkingDay = (date: Date | null): boolean => {
  if (!date) return false;
  const day = date.getDay();
  return day !== 0 && day !== 6;
};

/** First working day from today (today itself when it is a weekday). */
export function nextWorkingDay(from = new Date()): Date {
  const date = new Date(from.getFullYear(), from.getMonth(), from.getDate());
  while (!isWorkingDay(date)) date.setDate(date.getDate() + 1);
  return date;
}

export function startOfToday(): Date {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate());
}

/** Local date as YYYY-MM-DD (toISOString would shift the day in negative UTC offsets like Colombia). */
export function toIsoDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}
