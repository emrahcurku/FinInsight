import React from 'react';
import { CategorySpendingDTO } from '../../types/dashboard.types';
import { formatCurrency, formatPercentage } from '../../utils/currencyFormatter';

interface CategoryDonutChartProps {
  categories: CategorySpendingDTO[];
}

const PALETTE = ['#6366f1', '#a855f7', '#ec4899', '#f59e0b', '#10b981', '#06b6d4'];

export const CategoryDonutChart: React.FC<CategoryDonutChartProps> = ({ categories }) => {
  if (!categories || categories.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: '2rem', color: '#64748b', fontSize: '0.85rem' }}>
        No category spending recorded for this period.
      </div>
    );
  }

  const total = categories.reduce((sum, c) => sum + Number(c.totalAmount), 0);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
      {/* Progress Bars Breakdown */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        {categories.map((cat, idx) => {
          const color = PALETTE[idx % PALETTE.length];
          const pct = total > 0 ? (Number(cat.totalAmount) / total) * 100 : 0;

          return (
            <div key={cat.categoryId || idx} style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem' }}>
                <span style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#f8fafc' }}>
                  <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: color }} />
                  {cat.categoryName}
                </span>
                <span style={{ color: '#94a3b8', fontWeight: 500 }}>
                  {formatCurrency(cat.totalAmount)}{' '}
                  <span style={{ color: '#64748b', fontSize: '0.75rem' }}>({formatPercentage(pct)})</span>
                </span>
              </div>
              <div
                style={{
                  height: '6px',
                  width: '100%',
                  backgroundColor: 'rgba(255, 255, 255, 0.05)',
                  borderRadius: '3px',
                  overflow: 'hidden',
                }}
              >
                <div
                  style={{
                    height: '100%',
                    width: `${Math.min(pct, 100)}%`,
                    backgroundColor: color,
                    borderRadius: '3px',
                    transition: 'width 0.4s ease',
                  }}
                />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
