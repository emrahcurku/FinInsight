import { Component, ErrorInfo, ReactNode } from 'react';
import { AlertOctagon, RefreshCw, Home } from 'lucide-react';
import { Button } from '../common/Button';

interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
  errorId: string | null;
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  public state: ErrorBoundaryState = {
    hasError: false,
    error: null,
    errorId: null,
  };

  public static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return {
      hasError: true,
      error,
      errorId: crypto.randomUUID().substring(0, 8),
    };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    // Log unexpected rendering exceptions to console/observability without crashing whole app
    console.error('Uncaught React component error caught by ErrorBoundary:', error, errorInfo);
  }

  private handleReset = (): void => {
    this.setState({ hasError: false, error: null, errorId: null });
    window.location.reload();
  };

  private handleGoHome = (): void => {
    this.setState({ hasError: false, error: null, errorId: null });
    window.location.href = '/dashboard';
  };

  public render(): ReactNode {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div
          style={{
            minHeight: '80vh',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '2rem',
            backgroundColor: 'var(--bg-base, #090d16)',
          }}
        >
          <div
            className="glass-card"
            style={{
              maxWidth: '520px',
              width: '100%',
              padding: '2.5rem 2rem',
              textAlign: 'center',
              backgroundColor: 'rgba(15, 23, 42, 0.85)',
              border: '1px solid rgba(239, 68, 68, 0.25)',
              borderRadius: '20px',
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.5)',
            }}
          >
            <div
              style={{
                width: '60px',
                height: '60px',
                borderRadius: '16px',
                backgroundColor: 'rgba(239, 68, 68, 0.12)',
                color: '#ef4444',
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                marginBottom: '1.25rem',
              }}
            >
              <AlertOctagon size={32} />
            </div>

            <h2 style={{ fontSize: '1.35rem', fontWeight: 700, color: '#f8fafc', marginBottom: '0.5rem' }}>
              Unexpected Application Error
            </h2>

            <p style={{ fontSize: '0.875rem', color: '#94a3b8', lineHeight: 1.5, marginBottom: '1.5rem' }}>
              A client-side rendering exception occurred. Your data and authentication state remain safe on the server.
            </p>

            {this.state.errorId && (
              <div
                style={{
                  display: 'inline-block',
                  fontSize: '0.75rem',
                  fontFamily: 'monospace',
                  color: '#64748b',
                  backgroundColor: 'rgba(0, 0, 0, 0.4)',
                  padding: '4px 10px',
                  borderRadius: '6px',
                  marginBottom: '1.75rem',
                }}
              >
                Diagnostic Ref: {this.state.errorId}
              </div>
            )}

            <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
              <Button variant="secondary" size="md" onClick={this.handleReset} leftIcon={<RefreshCw size={16} />}>
                Reload Application
              </Button>
              <Button variant="primary" size="md" onClick={this.handleGoHome} leftIcon={<Home size={16} />}>
                Back to Dashboard
              </Button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
