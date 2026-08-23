/**
 * Formats ISO date string to human-friendly format (e.g. Aug 23, 2026)
 */
export function formatDate(dateString: string | null | undefined): string {
  if (!dateString) return '—';
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return dateString;
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  } catch {
    return dateString;
  }
}

/**
 * Formats YearMonth (e.g. 2026-08 -> August 2026)
 */
export function formatYearMonth(ym: string | null | undefined): string {
  if (!ym) return '—';
  const [year, month] = ym.split('-');
  if (!year || !month) return ym;
  const date = new Date(parseInt(year), parseInt(month) - 1, 1);
  return date.toLocaleDateString('en-US', { month: 'short', year: 'numeric' });
}

/**
 * Returns current month date range { from: 'YYYY-MM-01', to: 'YYYY-MM-DD' }
 */
export function getDefaultDateRange(): { from: string; to: string } {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const lastDay = new Date(year, now.getMonth() + 1, 0).getDate();

  return {
    from: `${year}-${month}-01`,
    to: `${year}-${month}-${String(lastDay).padStart(2, '0')}`,
  };
}
