import React from 'react';
import { Outlet, Navigate } from 'react-router-dom';
import { TrendingUp } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { LoadingSpinner } from '../feedback/LoadingSpinner';

export const AuthLayout: React.FC = () => {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return <LoadingSpinner message="Checking authentication session..." />;
  }

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#090d16',
        backgroundImage: 'radial-gradient(circle at 50% 30%, rgba(99, 102, 241, 0.12) 0%, transparent 60%)',
        padding: '1.5rem',
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: '440px',
          backgroundColor: 'rgba(15, 23, 42, 0.85)',
          backdropFilter: 'blur(16px)',
          border: '1px solid rgba(255, 255, 255, 0.1)',
          borderRadius: '20px',
          boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)',
          padding: '2.5rem 2rem',
        }}
      >
        {/* Brand Header */}
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <div
            style={{
              width: '48px',
              height: '48px',
              borderRadius: '14px',
              background: 'linear-gradient(135deg, #6366f1 0%, #a855f7 100%)',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#ffffff',
              boxShadow: '0 0 20px rgba(99, 102, 241, 0.5)',
              marginBottom: '1rem',
            }}
          >
            <TrendingUp size={26} />
          </div>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 800, color: '#f8fafc', letterSpacing: '-0.02em' }}>
            Fin<span style={{ color: '#818cf8' }}>Insight</span>
          </h1>
          <p style={{ fontSize: '0.85rem', color: '#94a3b8', marginTop: '4px' }}>
            AI-Powered Personal Finance & Analytics
          </p>
        </div>

        <Outlet />
      </div>
    </div>
  );
};
