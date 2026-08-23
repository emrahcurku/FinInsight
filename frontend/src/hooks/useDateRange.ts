import { useState } from 'react';
import { getDefaultDateRange } from '../utils/dateFormatter';

export function useDateRange(initialFrom?: string, initialTo?: string) {
  const defaultRange = getDefaultDateRange();
  const [from, setFrom] = useState<string>(initialFrom || defaultRange.from);
  const [to, setTo] = useState<string>(initialTo || defaultRange.to);

  const resetToCurrentMonth = () => {
    const range = getDefaultDateRange();
    setFrom(range.from);
    setTo(range.to);
  };

  return {
    from,
    to,
    setFrom,
    setTo,
    resetToCurrentMonth,
  };
}
