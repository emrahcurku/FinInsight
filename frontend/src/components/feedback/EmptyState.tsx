import React from 'react';
import { Inbox } from 'lucide-react';
import { Button } from '../common/Button';

interface EmptyStateProps {
  title?: string;
  description?: string;
  actionText?: string;
  onAction?: () => void;
  icon?: React.ReactNode;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  title = 'No records found',
  description = 'There are no items to display at this moment.',
  actionText,
  onAction,
  icon,
}) => {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        padding: '3.5rem 1.5rem',
        borderRadius: '16px',
        backgroundColor: 'rgba(15, 23, 42, 0.4)',
        border: '1px dashed rgba(255, 255, 255, 0.1)',
        color: '#94a3b8',
      }}
    >
      <div
        style={{
          width: '56px',
          height: '56px',
          borderRadius: '14px',
          backgroundColor: 'rgba(99, 102, 241, 0.1)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: '#818cf8',
          marginBottom: '1rem',
        }}
      >
        {icon || <Inbox size={28} />}
      </div>
      <h4 style={{ fontSize: '1.05rem', fontWeight: 600, color: '#f8fafc', marginBottom: '0.35rem' }}>
        {title}
      </h4>
      <p style={{ fontSize: '0.85rem', color: '#64748b', maxWidth: '380px', marginBottom: actionText ? '1.25rem' : '0' }}>
        {description}
      </p>
      {actionText && onAction && (
        <Button variant="primary" size="sm" onClick={onAction}>
          {actionText}
        </Button>
      )}
    </div>
  );
};
