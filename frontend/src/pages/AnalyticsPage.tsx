import React, { useEffect, useState, useCallback } from 'react';
import { analyticsApi } from '../api/analyticsApi';
import { FinancialSummaryDTO, MonthlyTrendDTO, CategorySpendingDTO } from '../types/dashboard.types';
import { Card } from '../components/common/Card';
import { DateRangeSelector } from '../components/common/DateRangeSelector';
import { LoadingSpinner } from '../components/feedback/LoadingSpinner';
import { ErrorState } from '../components/feedback/ErrorState';
import { CategoryDonutChart } from '../components/charts/CategoryDonutChart';
import { MonthlyBarChart } from '../components/charts/MonthlyBarChart';
import { formatCurrency } from '../utils/currencyFormatter';
import { useDateRange } from '../hooks/useDateRange';
import { extractErrorMessage } from '../utils/errorExtractor';

export const AnalyticsPage: React.FC = () => {
  const { from, to, setFrom, setTo } = useDateRange();
  const [summary, setSummary] = useState<FinancialSummaryDTO | null>(null);
  const [monthly, setMonthly] = useState<MonthlyTrendDTO[]>([]);
  const [categories, setCategories] = useState<CategorySpendingDTO[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchAnalytics = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [sumData, monthData, catData] = await Promise.all([
        analyticsApi.getFinancialSummary(from, to),
        analyticsApi.getMonthlySummary(from, to),
        analyticsApi.getCategoryBreakdown(from, to),
      ]);
      setSummary(sumData);
      setMonthly(monthData);
      setCategories(catData);
    } catch (err) {
      setError(extractErrorMessage(err, 'Failed to load financial analytics.'));
    } finally {
      setIsLoading(false);
    }
  }, [from, to]);

  useEffect(() => {
    fetchAnalytics();
  }, [fetchAnalytics]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.6rem', fontWeight: 800, color: '#f8fafc', letterSpacing: '-0.02em' }}>
            Financial Analytics
          </h1>
          <p style={{ fontSize: '0.875rem', color: '#94a3b8' }}>
            Multi-period financial breakdown, cash flow trends, and category distribution.
          </p>
        </div>
        <DateRangeSelector from={from} to={to} onFromChange={setFrom} onToChange={setTo} />
      </div>

      {isLoading ? (
        <LoadingSpinner message="Calculating analytics metrics..." />
      ) : error ? (
        <ErrorState message={error} onRetry={fetchAnalytics} />
      ) : (
        <>
          {/* Summary Metric Cards */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1.25rem' }}>
            <Card>
              <span style={{ fontSize: '0.85rem', color: '#94a3b8' }}>Total Income</span>
              <h3 style={{ fontSize: '1.6rem', fontWeight: 700, color: '#10b981', marginTop: '4px' }}>
                {formatCurrency(summary?.totalIncome)}
              </h3>
            </Card>

            <Card>
              <span style={{ fontSize: '0.85rem', color: '#94a3b8' }}>Total Expense</span>
              <h3 style={{ fontSize: '1.6rem', fontWeight: 700, color: '#ef4444', marginTop: '4px' }}>
                {formatCurrency(summary?.totalExpense)}
              </h3>
            </Card>

            <Card>
              <span style={{ fontSize: '0.85rem', color: '#94a3b8' }}>Net Surplus / Deficit</span>
              <h3
                style={{
                  fontSize: '1.6rem',
                  fontWeight: 700,
                  color: (summary?.netBalance || 0) >= 0 ? '#10b981' : '#ef4444',
                  marginTop: '4px',
                }}
              >
                {formatCurrency(summary?.netBalance)}
              </h3>
            </Card>
          </div>

          {/* Charts Grid */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(420px, 1fr))', gap: '1.5rem' }}>
            <Card title="Monthly Cash Flow" subtitle="Historical income vs expense comparison">
              <MonthlyBarChart data={monthly} />
            </Card>

            <Card title="Category Spending Proportions" subtitle="Distribution of expenses across categories">
              <CategoryDonutChart categories={categories} />
            </Card>
          </div>
        </>
      )}
    </div>
  );
};
