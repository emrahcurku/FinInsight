import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { Input } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { extractErrorMessage } from '../../utils/errorExtractor';

export const LoginForm: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!email || !password) {
      setError('Please enter both email and password.');
      return;
    }

    setIsLoading(true);
    try {
      await login({ email, password });
      navigate('/dashboard');
    } catch (err) {
      setError(extractErrorMessage(err, 'Invalid credentials or login failed.'));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
      {error && (
        <div
          style={{
            padding: '10px 14px',
            borderRadius: '8px',
            backgroundColor: 'rgba(239, 68, 68, 0.12)',
            border: '1px solid rgba(239, 68, 68, 0.3)',
            color: '#ef4444',
            fontSize: '0.85rem',
            fontWeight: 500,
          }}
        >
          {error}
        </div>
      )}

      <Input
        label="Email Address"
        type="email"
        placeholder="name@example.com"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        leftIcon={<Mail size={16} />}
        required
      />

      <Input
        label="Password"
        type="password"
        placeholder="••••••••"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        leftIcon={<Lock size={16} />}
        required
      />

      <Button type="submit" variant="primary" size="lg" isLoading={isLoading} style={{ marginTop: '0.5rem', width: '100%' }}>
        Sign In
      </Button>

      <div style={{ textAlign: 'center', fontSize: '0.85rem', color: '#94a3b8', marginTop: '1rem' }}>
        Don&apos;t have an account?{' '}
        <Link to="/register" style={{ color: '#818cf8', fontWeight: 600 }}>
          Create an account
        </Link>
      </div>
    </form>
  );
};
