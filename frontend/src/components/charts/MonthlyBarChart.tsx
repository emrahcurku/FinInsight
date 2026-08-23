import React from 'react';
import { MonthlyTrendDTO } from '../../types/dashboard.types';
import { formatCurrency } from '../../utils/currencyFormatter';
import { formatYearMonth } from '../../utils/dateFormatter';

interface MonthlyBarChartProps {
  data: MonthlyTrendDTO[];
}

export const MonthlyBarChart: React.FC<MonthlyBarChartProps> = ({ data }) => {
  if (!data || data.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: '2rem', color: '#64748b', fontSize: '0.85rem' }}>
        No monthly trends available.
      </div>
    );
  }

  // Calculate highest amount for scaling
  const maxVal = Math.max(
    ...data.flatMap((d) => [Number(d.totalIncome || 0), Number(d.totalExpense || 0)]),
    100
  );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', width: '100%' }}>
      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '16px', fontSize: '0.75rem', color: '#94a3b8' }}>
        <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <span style={{ width: '8px', height: '8px', borderRadius: '2px', backgroundColor: '#10b981' }} />
          Income
        </span>
        <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <span style={{ width: '8px', height: '8px', borderRadius: '2px', backgroundColor: '#ef4444' }} />
          Expense
        </span>
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: `repeat(${data.length}, 1fr)`,
          gap: '12px',
          alignItems: 'flex-end',
          height: '160px',
          paddingBottom: '24px',
          position: 'relative',
          borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
        }}
      >
        {data.map((item) => {
          const incHeight = Math.max((Number(item.totalIncome) / maxVal) * 120, 2);
          const expHeight = Math.max((Number(item.totalExpense) / maxVal) * 120, 2);

          return (
            <div
              key={item.yearMonth}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                height: '100%',
                justifyContent: 'flex-end',
                position: 'relative',
              }}
            >
              <div style={{ display: 'flex', gap: '4px', alignItems: 'flex-end', height: '100%' }}>
                {/* Income Bar */}
                <div
                  title={`Income: ${formatCurrency(item.totalIncome)}`}
                  style={{
                    width: '12px',
                    height: `${incHeight}px`,
                    backgroundColor: '#10b981',
                    borderRadius: '3px 3px 0 0',
                    transition: 'height 0.3s ease',
                  }}
                />
                {/* Expense Bar */}
                <div
                  title={`Expense: ${formatCurrency(item.totalExpense)}`}
                  style={{
                    width: '12px',
                    height: `${expHeight}px`,
                    backgroundColor: '#ef4444',
                    borderRadius: '3px 3px 0 0',
                    transition: 'height 0.3s ease',
                  }}
                />
              </div>
              <span
                style={{
                  position: 'absolute',
                  bottom: '-22px',
                  fontSize: '0.7rem',
                  color: '#64748b',
                  whiteSpace: 'nowrap',
                }}
              >
                {formatYearMonth(item.yearMonth)}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
};
