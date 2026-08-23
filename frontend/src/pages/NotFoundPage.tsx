import React from 'react';
import { Link } from 'react-router-dom';
import { HelpCircle } from 'lucide-react';
import { Button } from '../components/common/Button';

export const NotFoundPage: React.FC = () => {
  return (
    <div
      style={{
        minHeight: '80vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        padding: '2rem',
      }}
    >
      <HelpCircle size={48} style={{ color: '#818cf8', marginBottom: '1rem' }} />
      <h1 style={{ fontSize: '2rem', fontWeight: 800, color: '#f8fafc', marginBottom: '0.5rem' }}>
        404 — Page Not Found
      </h1>
      <p style={{ fontSize: '0.9rem', color: '#94a3b8', maxWidth: '400px', marginBottom: '1.5rem' }}>
        The page you are looking for does not exist or has been moved.
      </p>
      <Link to="/dashboard">
        <Button variant="primary" size="md">
          Back to Dashboard
        </Button>
      </Link>
    </div>
  );
};
