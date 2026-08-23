import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, User as UserIcon } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { Input } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { extractErrorMessage } from '../../utils/errorExtractor';

export const RegisterForm: React.FC = () => {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!firstName.trim() || !lastName.trim() || !email.trim() || !password) {
      setError('Please fill in all required fields.');
      return;
    }

    if (password.length < 8) {
      setError('Password must be at least 8 characters long.');
      return;
    }

    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setIsLoading(true);
    try {
      await register({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim(),
        password,
      });
      navigate('/dashboard');
    } catch (err) {
      setError(extractErrorMessage(err, 'Registration failed.'));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.1rem' }}>
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

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
        <Input
          label="First Name"
          type="text"
          placeholder="Jane"
          value={firstName}
          onChange={(e) => setFirstName(e.target.value)}
          leftIcon={<UserIcon size={16} />}
          required
        />

        <Input
          label="Last Name"
          type="text"
          placeholder="Doe"
          value={lastName}
          onChange={(e) => setLastName(e.target.value)}
          leftIcon={<UserIcon size={16} />}
          required
        />
      </div>

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
        placeholder="At least 8 characters"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        leftIcon={<Lock size={16} />}
        required
      />

      <Input
        label="Confirm Password"
        type="password"
        placeholder="Re-enter password"
        value={confirmPassword}
        onChange={(e) => setConfirmPassword(e.target.value)}
        leftIcon={<Lock size={16} />}
        required
      />

      <Button type="submit" variant="primary" size="lg" isLoading={isLoading} style={{ marginTop: '0.5rem', width: '100%' }}>
        Create Account
      </Button>

      <div style={{ textAlign: 'center', fontSize: '0.85rem', color: '#94a3b8', marginTop: '0.75rem' }}>
        Already have an account?{' '}
        <Link to="/login" style={{ color: '#818cf8', fontWeight: 600 }}>
          Sign in
        </Link>
      </div>
    </form>
  );
};
