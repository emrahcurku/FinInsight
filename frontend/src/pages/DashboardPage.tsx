import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  TrendingUp,
  TrendingDown,
  Wallet,
  ArrowUpRight,
  ArrowDownRight,
  Sparkles,
  ChevronRight,
  AlertTriangle,
} from 'lucide-react';
import { dashboardApi } from '../api/dashboardApi';
import { DashboardResponse } from '../types/dashboard.types';
import { Card } from '../components/common/Card';
import { Badge } from '../components/common/Badge';
import { DateRangeSelector } from '../components/common/DateRangeSelector';
import { LoadingSpinner } from '../components/feedback/LoadingSpinner';
import { ErrorState } from '../components/feedback/ErrorState';
import { CategoryDonutChart } from '../components/charts/CategoryDonutChart';
import { MonthlyBarChart } from '../components/charts/MonthlyBarChart';
import { formatCurrency, formatPercentage } from '../utils/currencyFormatter';
import { formatDate } from '../utils/dateFormatter';
import { useDateRange } from '../hooks/useDateRange';
import { extractErrorMessage } from '../utils/errorExtractor';

export const DashboardPage: React.FC = () => {
  const { from, to, setFrom, setTo } = useDateRange();
  const [data, setData] = useState<DashboardResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchDashboard = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await dashboardApi.getDashboard(from, to);
      setData(response);
    } catch (err) {
      setError(extractErrorMessage(err, 'Failed to load dashboard data.'));
    } finally {
      setIsLoading(false);
    }
  }, [from, to]);

  useEffect(() => {
    fetchDashboard();
  }, [fetchDashboard]);

  if (isLoading) {
    return <LoadingSpinner message="Aggregating composite dashboard analytics..." />;
  }

  if (error || !data) {
    return <ErrorState message={error || 'Dashboard unavailable'} onRetry={fetchDashboard} />;
  }

  const { financialSummary, previousMonthComparison, categorySpending, monthlyTrend, budgetOverview, recentTransactions, insights } = data;
  const isPositiveBalance = (financialSummary?.netBalance || 0) >= 0;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
      {/* Top Header */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '1rem',
        }}
      >
        <div>
          <h1 style={{ fontSize: '1.6rem', fontWeight: 800, color: '#f8fafc', letterSpacing: '-0.02em' }}>
            Financial Overview
          </h1>
          <p style={{ fontSize: '0.875rem', color: '#94a3b8' }}>
            Real-time multi-dimensional financial summary and behavioral metrics.
          </p>
        </div>
        <DateRangeSelector from={from} to={to} onFromChange={setFrom} onToChange={setTo} />
      </div>

      {/* Summary KPI Cards Grid */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
          gap: '1.25rem',
        }}
      >
        {/* Net Balance */}
        <Card>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <span style={{ fontSize: '0.85rem', color: '#94a3b8', fontWeight: 500 }}>Net Balance</span>
            <div
              style={{
                padding: '6px',
                borderRadius: '8px',
                backgroundColor: isPositiveBalance ? 'rgba(16, 185, 129, 0.12)' : 'rgba(239, 68, 68, 0.12)',
                color: isPositiveBalance ? '#10b981' : '#ef4444',
              }}
            >
              <Wallet size={18} />
            </div>
          </div>
          <div style={{ marginTop: '0.75rem' }}>
            <h2 style={{ fontSize: '1.75rem', fontWeight: 700, color: isPositiveBalance ? '#10b981' : '#ef4444' }}>
              {formatCurrency(financialSummary?.netBalance)}
            </h2>
            <span style={{ fontSize: '0.75rem', color: '#64748b' }}>Income minus expense for period</span>
          </div>
        </Card>

        {/* Total Income */}
        <Card>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <span style={{ fontSize: '0.85rem', color: '#94a3b8', fontWeight: 500 }}>Total Income</span>
            <div style={{ padding: '6px', borderRadius: '8px', backgroundColor: 'rgba(16, 185, 129, 0.12)', color: '#10b981' }}>
              <ArrowDownRight size={18} />
            </div>
          </div>
          <div style={{ marginTop: '0.75rem' }}>
            <h2 style={{ fontSize: '1.75rem', fontWeight: 700, color: '#f8fafc' }}>
              {formatCurrency(financialSummary?.totalIncome)}
            </h2>
            <span style={{ fontSize: '0.75rem', color: '#10b981', fontWeight: 600 }}>Active Cash Inflow</span>
          </div>
        </Card>

        {/* Total Expense */}
        <Card>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <span style={{ fontSize: '0.85rem', color: '#94a3b8', fontWeight: 500 }}>Total Expense</span>
            <div style={{ padding: '6px', borderRadius: '8px', backgroundColor: 'rgba(239, 68, 68, 0.12)', color: '#ef4444' }}>
              <ArrowUpRight size={18} />
            </div>
          </div>
          <div style={{ marginTop: '0.75rem' }}>
            <h2 style={{ fontSize: '1.75rem', fontWeight: 700, color: '#f8fafc' }}>
              {formatCurrency(financialSummary?.totalExpense)}
            </h2>
            {previousMonthComparison && (
              <span
                style={{
                  fontSize: '0.75rem',
                  color: previousMonthComparison.trend === 'INCREASED' ? '#ef4444' : '#10b981',
                  fontWeight: 600,
                  display: 'flex',
                  alignItems: 'center',
                  gap: '4px',
                }}
              >
                {previousMonthComparison.trend === 'INCREASED' ? <TrendingUp size={12} /> : <TrendingDown size={12} />}
                {formatPercentage(previousMonthComparison.expenseChangePercentage)} vs last month
              </span>
            )}
          </div>
        </Card>

        {/* Transaction Count */}
        <Card>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <span style={{ fontSize: '0.85rem', color: '#94a3b8', fontWeight: 500 }}>Logged Transactions</span>
            <Badge variant="info">{financialSummary?.transactionCount || 0} items</Badge>
          </div>
          <div style={{ marginTop: '0.75rem' }}>
            <h2 style={{ fontSize: '1.75rem', fontWeight: 700, color: '#f8fafc' }}>
              {financialSummary?.transactionCount || 0}
            </h2>
            <Link to="/transactions" style={{ fontSize: '0.75rem', color: '#818cf8', display: 'flex', alignItems: 'center', gap: '2px', fontWeight: 600 }}>
              View transaction history <ChevronRight size={12} />
            </Link>
          </div>
        </Card>
      </div>

      {/* AI Insights Highlight Card */}
      <Card
        variant="ai"
        title="AI Financial Intelligence"
        subtitle="Natural language financial synthesis derived exclusively from aggregated metrics."
        action={
          <Link
            to="/ai-insights"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: '0.8rem',
              fontWeight: 600,
              color: '#c084fc',
              backgroundColor: 'rgba(168, 85, 247, 0.15)',
              padding: '6px 12px',
              borderRadius: '8px',
              border: '1px solid rgba(168, 85, 247, 0.3)',
            }}
          >
            <Sparkles size={14} /> View AI Insights
          </Link>
        }
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {insights && insights.length > 0 ? (
            insights.slice(0, 2).map((ins, idx) => (
              <div
                key={idx}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                  padding: '10px 14px',
                  borderRadius: '10px',
                  backgroundColor: 'rgba(15, 23, 42, 0.6)',
                  border: '1px solid rgba(255, 255, 255, 0.06)',
                }}
              >
                <Badge variant={ins.severity === 'DANGER' ? 'danger' : ins.severity === 'WARNING' ? 'warning' : 'info'}>
                  {ins.severity}
                </Badge>
                <div style={{ flex: 1 }}>
                  <strong style={{ color: '#f8fafc', fontSize: '0.85rem' }}>{ins.title}: </strong>
                  <span style={{ color: '#94a3b8', fontSize: '0.85rem' }}>{ins.message}</span>
                </div>
              </div>
            ))
          ) : (
            <p style={{ fontSize: '0.85rem', color: '#94a3b8' }}>
              No critical financial warnings or alerts detected for the current period.
            </p>
          )}
        </div>
      </Card>

      {/* 2-Column Analytics Section */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(420px, 1fr))', gap: '1.5rem' }}>
        {/* 6-Month Trend */}
        <Card title="Monthly Spending & Income Trend" subtitle="6-month continuous aggregation with net savings">
          <MonthlyBarChart data={monthlyTrend} />
        </Card>

        {/* Category Breakdown */}
        <Card title="Top Category Distribution" subtitle="Top 5 expenditure categories with proportion">
          <CategoryDonutChart categories={categorySpending} />
        </Card>
      </div>

      {/* Budget Status & Recent Transactions */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(420px, 1fr))', gap: '1.5rem' }}>
        {/* Budget Health Card */}
        <Card
          title="Monthly Budget Health"
          subtitle="Real-time category spending limits and threshold alerts"
          action={<Link to="/budgets" style={{ fontSize: '0.8rem', color: '#818cf8', fontWeight: 600 }}>Manage Budgets</Link>}
        >
          {budgetOverview && budgetOverview.activeBudgetCount > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <span style={{ fontSize: '0.8rem', color: '#94a3b8' }}>Total Budget Utilization</span>
                  <div style={{ fontSize: '1.35rem', fontWeight: 700, color: '#f8fafc', marginTop: '2px' }}>
                    {formatCurrency(budgetOverview.totalSpent)}{' '}
                    <span style={{ fontSize: '0.85rem', color: '#64748b' }}>/ {formatCurrency(budgetOverview.totalBudgeted)}</span>
                  </div>
                </div>
                <Badge variant={budgetOverview.exceededBudgetCount > 0 ? 'danger' : budgetOverview.warningBudgetCount > 0 ? 'warning' : 'success'}>
                  {formatPercentage(budgetOverview.overallUsagePercentage)} Used
                </Badge>
              </div>

              <div style={{ display: 'flex', gap: '10px' }}>
                {budgetOverview.exceededBudgetCount > 0 && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.8rem', color: '#ef4444' }}>
                    <AlertTriangle size={14} /> {budgetOverview.exceededBudgetCount} Exceeded
                  </div>
                )}
                {budgetOverview.warningBudgetCount > 0 && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.8rem', color: '#f59e0b' }}>
                    <AlertTriangle size={14} /> {budgetOverview.warningBudgetCount} Above 80%
                  </div>
                )}
              </div>
            </div>
          ) : (
            <p style={{ fontSize: '0.85rem', color: '#64748b' }}>No active monthly budgets configured.</p>
          )}
        </Card>

        {/* Recent Transactions */}
        <Card
          title="Recent Transactions"
          subtitle="Latest logged activity"
          action={<Link to="/transactions" style={{ fontSize: '0.8rem', color: '#818cf8', fontWeight: 600 }}>View All</Link>}
        >
          {recentTransactions && recentTransactions.length > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {recentTransactions.map((tx) => (
                <div
                  key={tx.id}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    padding: '8px 0',
                    borderBottom: '1px solid rgba(255, 255, 255, 0.04)',
                    fontSize: '0.85rem',
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 600, color: '#f8fafc' }}>
                      {tx.description || tx.categoryName}
                    </div>
                    <div style={{ fontSize: '0.75rem', color: '#64748b' }}>
                      {tx.categoryName} • {formatDate(tx.transactionDate)}
                    </div>
                  </div>
                  <div
                    style={{
                      fontWeight: 700,
                      color: tx.type === 'INCOME' ? '#10b981' : '#ef4444',
                    }}
                  >
                    {tx.type === 'INCOME' ? '+' : '-'}{formatCurrency(tx.amount)}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p style={{ fontSize: '0.85rem', color: '#64748b' }}>No recent transactions recorded.</p>
          )}
        </Card>
      </div>
    </div>
  );
};
