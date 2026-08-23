import React from 'react';
import { Loader2 } from 'lucide-react';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost' | 'ai';
  size?: 'sm' | 'md' | 'lg';
  isLoading?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

export const Button: React.FC<ButtonProps> = ({
  children,
  variant = 'primary',
  size = 'md',
  isLoading = false,
  leftIcon,
  rightIcon,
  className = '',
  disabled,
  ...props
}) => {
  const baseStyles = 'inline-flex items-center justify-center font-medium rounded-lg transition-all focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer';

  const sizeStyles = {
    sm: 'px-3 py-1.5 text-xs gap-1.5',
    md: 'px-4 py-2 text-sm gap-2',
    lg: 'px-5 py-2.5 text-base gap-2.5',
  };

  const variantStyles = {
    primary: 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-md hover:shadow-indigo-500/20 focus:ring-indigo-500 border border-indigo-500/30',
    secondary: 'bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 hover:border-slate-600 focus:ring-slate-500',
    danger: 'bg-rose-600 hover:bg-rose-500 text-white shadow-md hover:shadow-rose-500/20 focus:ring-rose-500 border border-rose-500/30',
    ghost: 'bg-transparent hover:bg-slate-800/60 text-slate-300 hover:text-white focus:ring-slate-500',
    ai: 'bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white shadow-md hover:shadow-purple-500/20 border border-purple-500/30 focus:ring-purple-500',
  };

  return (
    <button
      className={`${baseStyles} ${sizeStyles[size]} ${variantStyles[variant]} ${className}`}
      disabled={disabled || isLoading}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        borderRadius: '8px',
        fontWeight: 500,
        transition: 'all 0.15s ease',
        cursor: disabled || isLoading ? 'not-allowed' : 'pointer',
        opacity: disabled || isLoading ? 0.6 : 1,
        padding: size === 'sm' ? '6px 12px' : size === 'lg' ? '12px 24px' : '9px 18px',
        fontSize: size === 'sm' ? '0.8rem' : size === 'lg' ? '1rem' : '0.875rem',
        border: '1px solid transparent',
        ...(variant === 'primary' ? { background: '#4f46e5', color: '#ffffff', borderColor: 'rgba(99, 102, 241, 0.4)' } : {}),
        ...(variant === 'secondary' ? { background: '#1e293b', color: '#f8fafc', borderColor: 'rgba(255, 255, 255, 0.1)' } : {}),
        ...(variant === 'danger' ? { background: '#dc2626', color: '#ffffff', borderColor: 'rgba(239, 68, 68, 0.4)' } : {}),
        ...(variant === 'ghost' ? { background: 'transparent', color: '#94a3b8' } : {}),
        ...(variant === 'ai' ? { background: 'linear-gradient(135deg, #9333ea 0%, #4f46e5 100%)', color: '#ffffff', borderColor: 'rgba(168, 85, 247, 0.4)' } : {}),
      }}
      {...props}
    >
      {isLoading ? <Loader2 size={16} className="animate-spin" style={{ animation: 'spin 1s linear infinite' }} /> : leftIcon}
      <span>{children}</span>
      {!isLoading && rightIcon}
    </button>
  );
};
