import React, { forwardRef } from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
  leftIcon?: React.ReactNode;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, helperText, leftIcon, className = '', id, ...props }, ref) => {
    const inputId = id || props.name || Math.random().toString(36).substring(7);

    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', width: '100%' }}>
        {label && (
          <label
            htmlFor={inputId}
            style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-secondary, #94a3b8)' }}
          >
            {label}
          </label>
        )}
        <div style={{ position: 'relative', display: 'flex', alignItems: 'center', width: '100%' }}>
          {leftIcon && (
            <div
              style={{
                position: 'absolute',
                left: '12px',
                display: 'flex',
                alignItems: 'center',
                color: 'var(--text-muted, #64748b)',
                pointerEvents: 'none',
              }}
            >
              {leftIcon}
            </div>
          )}
          <input
            id={inputId}
            ref={ref}
            style={{
              width: '100%',
              padding: leftIcon ? '9px 12px 9px 38px' : '9px 12px',
              backgroundColor: 'rgba(15, 23, 42, 0.6)',
              border: `1px solid ${error ? 'var(--danger, #ef4444)' : 'rgba(255, 255, 255, 0.1)'}`,
              borderRadius: '8px',
              color: 'var(--text-primary, #f8fafc)',
              fontSize: '0.875rem',
              outline: 'none',
              transition: 'border-color 0.15s ease',
            }}
            {...props}
          />
        </div>
        {error && (
          <span style={{ fontSize: '0.75rem', color: 'var(--danger, #ef4444)', fontWeight: 500 }}>
            {error}
          </span>
        )}
        {helperText && !error && (
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted, #64748b)' }}>
            {helperText}
          </span>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';
