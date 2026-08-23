import React from 'react';
import { Menu, LogOut, User as UserIcon, Shield } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { Button } from '../common/Button';

interface TopbarProps {
  onToggleSidebar: () => void;
  title?: string;
}

export const Topbar: React.FC<TopbarProps> = ({ onToggleSidebar, title }) => {
  const { user, logout } = useAuth();

  return (
    <header
      style={{
        height: '64px',
        borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
        backgroundColor: 'rgba(15, 23, 42, 0.7)',
        backdropFilter: 'blur(12px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '0 1.5rem',
        position: 'sticky',
        top: 0,
        zIndex: 30,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
        <button
          onClick={onToggleSidebar}
          style={{
            background: 'transparent',
            border: 'none',
            color: '#94a3b8',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            padding: '6px',
            borderRadius: '6px',
          }}
          aria-label="Toggle Navigation"
        >
          <Menu size={22} />
        </button>
        {title && <h2 style={{ fontSize: '1.1rem', fontWeight: 600, color: '#f8fafc' }}>{title}</h2>}
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
        {user && (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              backgroundColor: 'rgba(30, 41, 59, 0.6)',
              border: '1px solid rgba(255, 255, 255, 0.08)',
              padding: '6px 12px',
              borderRadius: '9999px',
              fontSize: '0.8rem',
              color: '#f8fafc',
            }}
          >
            <div
              style={{
                width: '24px',
                height: '24px',
                borderRadius: '50%',
                backgroundColor: 'rgba(99, 102, 241, 0.2)',
                color: '#818cf8',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <UserIcon size={14} />
            </div>
            <span style={{ maxWidth: '160px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {user.email}
            </span>
            {user.role === 'ROLE_ADMIN' && (
              <span
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '2px',
                  fontSize: '0.65rem',
                  color: '#f59e0b',
                  fontWeight: 700,
                }}
              >
                <Shield size={12} />
                ADMIN
              </span>
            )}
          </div>
        )}

        <Button
          variant="ghost"
          size="sm"
          onClick={logout}
          leftIcon={<LogOut size={16} />}
          style={{ color: '#ef4444' }}
        >
          Logout
        </Button>
      </div>
    </header>
  );
};
