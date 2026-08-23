import React from 'react';
import { AlertCircle, RefreshCw } from 'lucide-react';
import { Button } from '../common/Button';

interface ErrorStateProps {
  title?: string;
  message?: string;
  correlationId?: string;
  onRetry?: () => void;
}

export const ErrorState: React.FC<ErrorStateProps> = ({
  title = 'Something went wrong',
  message = 'We encountered an error while loading your data. Please try again.',
  correlationId,
  onRetry,
}) => {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        padding: '3rem 1.5rem',
        borderRadius: '16px',
        backgroundColor: 'rgba(239, 68, 68, 0.05)',
        border: '1px solid rgba(239, 68, 68, 0.2)',
      }}
    >
      <div
        style={{
          width: '50px',
          height: '50px',
          borderRadius: '12px',
          backgroundColor: 'rgba(239, 68, 68, 0.12)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: '#ef4444',
          marginBottom: '1rem',
        }}
      >
        <AlertCircle size={26} />
      </div>
      <h4 style={{ fontSize: '1rem', fontWeight: 600, color: '#f8fafc', marginBottom: '0.35rem' }}>
        {title}
      </h4>
      <p style={{ fontSize: '0.85rem', color: '#94a3b8', maxWidth: '420px', marginBottom: '1rem' }}>
        {message}
      </p>
      {correlationId && (
        <span
          style={{
            fontSize: '0.7rem',
            fontFamily: 'monospace',
            color: '#64748b',
            backgroundColor: 'rgba(0, 0, 0, 0.3)',
            padding: '2px 8px',
            borderRadius: '4px',
            marginBottom: '1.25rem',
          }}
        >
          Ref: {correlationId}
        </span>
      )}
      {onRetry && (
        <Button variant="secondary" size="sm" onClick={onRetry} leftIcon={<RefreshCw size={14} />}>
          Try Again
        </Button>
      )}
    </div>
  );
};
