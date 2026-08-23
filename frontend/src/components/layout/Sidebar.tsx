import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  ArrowLeftRight,
  FolderTree,
  PieChart,
  BarChart3,
  Sparkles,
  TrendingUp,
  X,
} from 'lucide-react';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ isOpen, onClose }) => {
  const navItems = [
    { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/transactions', label: 'Transactions', icon: ArrowLeftRight },
    { to: '/categories', label: 'Categories', icon: FolderTree },
    { to: '/budgets', label: 'Budgets', icon: PieChart },
    { to: '/analytics', label: 'Analytics', icon: BarChart3 },
    { to: '/ai-insights', label: 'AI Insights', icon: Sparkles, isAi: true },
  ];

  return (
    <>
      {/* Mobile Backdrop */}
      {isOpen && (
        <div
          onClick={onClose}
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(9, 13, 22, 0.7)',
            backdropFilter: 'blur(4px)',
            zIndex: 40,
            display: 'block',
          }}
        />
      )}

      <aside
        style={{
          width: '260px',
          backgroundColor: '#090d16',
          borderRight: '1px solid rgba(255, 255, 255, 0.08)',
          display: 'flex',
          flexDirection: 'column',
          height: '100vh',
          position: 'sticky',
          top: 0,
          zIndex: 45,
          transition: 'transform 0.3s ease',
          flexShrink: 0,
          ...(isOpen
            ? { position: 'fixed', left: 0, transform: 'translateX(0)' }
            : {}),
        }}
      >
        {/* Brand Header */}
        <div
          style={{
            padding: '1.5rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderBottom: '1px solid rgba(255, 255, 255, 0.06)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '10px',
                background: 'linear-gradient(135deg, #6366f1 0%, #a855f7 100%)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#ffffff',
                boxShadow: '0 0 15px rgba(99, 102, 241, 0.4)',
              }}
            >
              <TrendingUp size={20} />
            </div>
            <div>
              <span style={{ fontSize: '1.15rem', fontWeight: 800, letterSpacing: '-0.02em', color: '#f8fafc' }}>
                Fin<span style={{ color: '#818cf8' }}>Insight</span>
              </span>
              <span
                style={{
                  display: 'block',
                  fontSize: '0.65rem',
                  color: '#64748b',
                  fontWeight: 600,
                  textTransform: 'uppercase',
                  letterSpacing: '0.08em',
                }}
              >
                AI Financial Platform
              </span>
            </div>
          </div>
          {isOpen && (
            <button
              onClick={onClose}
              style={{
                background: 'transparent',
                border: 'none',
                color: '#94a3b8',
                cursor: 'pointer',
              }}
            >
              <X size={20} />
            </button>
          )}
        </div>

        {/* Navigation List */}
        <nav style={{ padding: '1.25rem 1rem', display: 'flex', flexDirection: 'column', gap: '6px', flex: 1 }}>
          {navItems.map((item) => {
            const Icon = item.icon;

            return (
              <NavLink
                key={item.to}
                to={item.to}
                onClick={onClose}
                style={({ isActive }) => ({
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                  padding: '10px 14px',
                  borderRadius: '10px',
                  fontSize: '0.875rem',
                  fontWeight: 500,
                  transition: 'all 0.15s ease',
                  textDecoration: 'none',
                  color: isActive ? '#ffffff' : '#94a3b8',
                  backgroundColor: isActive
                    ? item.isAi
                      ? 'rgba(168, 85, 247, 0.15)'
                      : 'rgba(99, 102, 241, 0.15)'
                    : 'transparent',
                  border: `1px solid ${
                    isActive
                      ? item.isAi
                        ? 'rgba(168, 85, 247, 0.3)'
                        : 'rgba(99, 102, 241, 0.3)'
                      : 'transparent'
                  }`,
                })}
              >
                <Icon
                  size={18}
                  style={{
                    color: item.isAi ? '#c084fc' : undefined,
                  }}
                />
                <span>{item.label}</span>
                {item.isAi && (
                  <span
                    style={{
                      marginLeft: 'auto',
                      fontSize: '0.65rem',
                      fontWeight: 700,
                      padding: '2px 6px',
                      borderRadius: '4px',
                      background: 'linear-gradient(135deg, #9333ea, #6366f1)',
                      color: '#ffffff',
                    }}
                  >
                    AI
                  </span>
                )}
              </NavLink>
            );
          })}
        </nav>

        {/* Footer info */}
        <div
          style={{
            padding: '1rem 1.5rem',
            borderTop: '1px solid rgba(255, 255, 255, 0.06)',
            fontSize: '0.75rem',
            color: '#64748b',
          }}
        >
          <span>FinInsight Modular Monolith</span>
          <span style={{ display: 'block', color: '#475569', fontSize: '0.7rem' }}>v0.1.0 (Phase 13)</span>
        </div>
      </aside>
    </>
  );
};
