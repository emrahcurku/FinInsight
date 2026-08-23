import React from 'react';
import { Calendar } from 'lucide-react';

interface DateRangeSelectorProps {
  from: string;
  to: string;
  onFromChange: (val: string) => void;
  onToChange: (val: string) => void;
}

export const DateRangeSelector: React.FC<DateRangeSelectorProps> = ({
  from,
  to,
  onFromChange,
  onToChange,
}) => {
  return (
    <div
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '8px',
        backgroundColor: 'rgba(15, 23, 42, 0.8)',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        padding: '6px 12px',
        borderRadius: '10px',
        fontSize: '0.85rem',
      }}
    >
      <Calendar size={16} style={{ color: '#818cf8' }} />
      <input
        type="date"
        value={from}
        onChange={(e) => onFromChange(e.target.value)}
        style={{
          background: 'transparent',
          border: 'none',
          color: '#f8fafc',
          fontSize: '0.85rem',
          outline: 'none',
          cursor: 'pointer',
        }}
      />
      <span style={{ color: '#64748b' }}>to</span>
      <input
        type="date"
        value={to}
        onChange={(e) => onToChange(e.target.value)}
        style={{
          background: 'transparent',
          border: 'none',
          color: '#f8fafc',
          fontSize: '0.85rem',
          outline: 'none',
          cursor: 'pointer',
        }}
      />
    </div>
  );
};
