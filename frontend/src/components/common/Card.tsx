import React from 'react';

interface CardProps {
  children: React.ReactNode;
  title?: string;
  subtitle?: string;
  action?: React.ReactNode;
  variant?: 'default' | 'elevated' | 'ai';
  className?: string;
  style?: React.CSSProperties;
}

export const Card: React.FC<CardProps> = ({
  children,
  title,
  subtitle,
  action,
  variant = 'default',
  style,
}) => {
  return (
    <div
      className="glass-card"
      style={{
        padding: '1.5rem',
        borderRadius: '16px',
        backgroundColor: variant === 'ai' ? 'rgba(30, 27, 75, 0.4)' : 'rgba(15, 23, 42, 0.75)',
        border: `1px solid ${variant === 'ai' ? 'rgba(168, 85, 247, 0.3)' : 'rgba(255, 255, 255, 0.08)'}`,
        boxShadow: variant === 'ai' ? '0 0 25px rgba(168, 85, 247, 0.1)' : '0 4px 6px -1px rgba(0, 0, 0, 0.3)',
        ...style,
      }}
    >
      {(title || action) && (
        <div
          style={{
            display: 'flex',
            alignItems: 'flex-start',
            justifyContent: 'space-between',
            marginBottom: '1rem',
            gap: '1rem',
          }}
        >
          <div>
            {title && (
              <h3 style={{ fontSize: '1.05rem', fontWeight: 600, color: 'var(--text-primary, #f8fafc)' }}>
                {title}
              </h3>
            )}
            {subtitle && (
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted, #64748b)', marginTop: '2px' }}>
                {subtitle}
              </p>
            )}
          </div>
          {action && <div>{action}</div>}
        </div>
      )}
      {children}
    </div>
  );
};
