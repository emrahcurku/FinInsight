import React from 'react';
import { RegisterForm } from '../features/auth/RegisterForm';

export const RegisterPage: React.FC = () => {
  return (
    <div>
      <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#f8fafc', marginBottom: '1.5rem', textAlign: 'center' }}>
        Create an Account
      </h2>
      <RegisterForm />
    </div>
  );
};
