/**
 * Formats numeric or string amount into readable currency ($X,XXX.XX)
 */
export function formatCurrency(amount: number | string | null | undefined, currency: string = '$'): string {
  if (amount === null || amount === undefined || isNaN(Number(amount))) {
    return `${currency}0.00`;
  }
  const numeric = typeof amount === 'string' ? parseFloat(amount) : amount;
  return `${numeric < 0 ? '-' : ''}${currency}${Math.abs(numeric).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

/**
 * Formats a percentage (e.g. 24.5%)
 */
export function formatPercentage(value: number | string | null | undefined): string {
  if (value === null || value === undefined || isNaN(Number(value))) {
    return '0.0%';
  }
  const numeric = typeof value === 'string' ? parseFloat(value) : value;
  return `${numeric.toFixed(1)}%`;
}
