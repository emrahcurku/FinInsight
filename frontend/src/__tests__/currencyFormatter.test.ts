import { describe, it, expect } from 'vitest';
import { formatCurrency, formatPercentage } from '../utils/currencyFormatter';

describe('currencyFormatter', () => {
  it('formats positive and negative amounts properly', () => {
    expect(formatCurrency(1250.5)).toBe('$1,250.50');
    expect(formatCurrency(-320.75)).toBe('-$320.75');
    expect(formatCurrency(0)).toBe('$0.00');
    expect(formatCurrency(null)).toBe('$0.00');
    expect(formatCurrency(undefined)).toBe('$0.00');
  });

  it('formats percentage properly', () => {
    expect(formatPercentage(45.67)).toBe('45.7%');
    expect(formatPercentage(0)).toBe('0.0%');
    expect(formatPercentage(null)).toBe('0.0%');
  });
});
