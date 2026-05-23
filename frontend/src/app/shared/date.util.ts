/** Format Date or ISO string as yyyy-MM-dd for API query params. */
export function toApiDate(value: Date | string | null | undefined): string {
  if (value == null || value === '') {
    return '';
  }
  if (typeof value === 'string') {
    return value.slice(0, 10);
  }
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, '0');
  const day = String(value.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/** Default historical range: last 30 days including today. */
export function defaultHistoricalRange(): { from: Date; to: Date } {
  const to = new Date();
  to.setHours(0, 0, 0, 0);
  const from = new Date(to);
  from.setDate(from.getDate() - 29);
  return { from, to };
}
