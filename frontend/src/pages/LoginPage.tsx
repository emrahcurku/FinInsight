import React from 'react';
import { LoginForm } from '../features/auth/LoginForm';

export const LoginPage: React.FC = () => {
  return (
    <div>
      <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#f8fafc', marginBottom: '1.5rem', textAlign: 'center' }}>
        Welcome Back
      </h2>
      <LoginForm />
    </div>
  );
};
