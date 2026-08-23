import React, { forwardRef } from 'react';

interface Option {
  value: string | number;
  label: string;
}

interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
  options: Option[];
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, error, options, className = '', id, ...props }, ref) => {
    const selectId = id || props.name || Math.random().toString(36).substring(7);

    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', width: '100%' }}>
        {label && (
          <label
            htmlFor={selectId}
            style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary, #94a3b8)' }}
          >
            {label}
          </label>
        )}
        <select
          id={selectId}
          ref={ref}
          style={{
            width: '100%',
            padding: '9px 12px',
            backgroundColor: '#0f172a',
            border: `1px solid ${error ? 'var(--danger, #ef4444)' : 'rgba(255, 255, 255, 0.1)'}`,
            borderRadius: '8px',
            color: 'var(--text-primary, #f8fafc)',
            fontSize: '0.875rem',
            outline: 'none',
            cursor: 'pointer',
          }}
          {...props}
        >
          {options.map((opt) => (
            <option key={opt.value} value={opt.value} style={{ background: '#0f172a', color: '#f8fafc' }}>
              {opt.label}
            </option>
          ))}
        </select>
        {error && (
          <span style={{ fontSize: '0.75rem', color: 'var(--danger, #ef4444)', fontWeight: 500 }}>
            {error}
          </span>
        )}
      </div>
    );
  }
);

Select.displayName = 'Select';
