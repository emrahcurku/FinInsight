import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { DashboardPage } from '../pages/DashboardPage';
import { dashboardApi } from '../api/dashboardApi';
import { DashboardResponse } from '../types/dashboard.types';

vi.mock('../api/dashboardApi', () => ({
  dashboardApi: {
    getDashboard: vi.fn(),
  },
}));

const mockDashboardResponse: DashboardResponse = {
  financialSummary: {
    totalIncome: 7500.0,
    totalExpense: 3200.0,
    netBalance: 4300.0,
    transactionCount: 24,
  },
  monthlyTrend: [
    { yearMonth: '2026-03', totalIncome: 7000, totalExpense: 3000, netSavings: 4000 },
    { yearMonth: '2026-04', totalIncome: 7500, totalExpense: 3200, netSavings: 4300 },
  ],
  categorySpending: [
    { categoryId: 'cat-1', categoryName: 'Groceries', totalAmount: 850.0, percentage: 26.5 },
    { categoryId: 'cat-2', categoryName: 'Rent & Housing', totalAmount: 1500.0, percentage: 46.8 },
  ],
  budgetOverview: {
    totalBudgeted: 4000.0,
    totalSpent: 3200.0,
    overallUsagePercentage: 80.0,
    activeBudgetCount: 3,
    warningBudgetCount: 1,
    exceededBudgetCount: 0,
  },
  recentTransactions: [
    {
      id: 'tx-1',
      categoryId: 'cat-1',
      categoryName: 'Groceries',
      amount: 125.5,
      type: 'EXPENSE',
      description: 'Supermarket weekly run',
      transactionDate: '2026-08-20',
    },
  ],
  previousMonthComparison: {
    currentMonthExpense: 3200.0,
    previousMonthExpense: 3000.0,
    expenseChangeAmount: 200.0,
    expenseChangePercentage: 6.67,
    trend: 'INCREASED',
  },
  insights: [
    {
      type: 'SAVINGS_ALERT',
      title: 'Strong Savings Rate',
      message: 'You have saved over 50% of your total income this month.',
      severity: 'INFO',
    },
  ],
};

describe('DashboardPage Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders aggregated financial KPIs, trends, categories, and recent transactions', async () => {
    vi.mocked(dashboardApi.getDashboard).mockResolvedValue(mockDashboardResponse);

    render(
      <BrowserRouter>
        <DashboardPage />
      </BrowserRouter>
    );

    // Initial loading indicator
    expect(screen.getByText(/Aggregating composite dashboard analytics/i)).toBeInTheDocument();

    // After loading completes
    await waitFor(() => {
      expect(screen.getAllByText('$4,300.00').length).toBeGreaterThan(0); // Net balance
      expect(screen.getAllByText('$7,500.00').length).toBeGreaterThan(0); // Total income
      expect(screen.getAllByText('$3,200.00').length).toBeGreaterThan(0); // Total expense
      expect(screen.getByText('Supermarket weekly run')).toBeInTheDocument();
      expect(screen.getAllByText('Groceries').length).toBeGreaterThan(0);
      expect(screen.getByText(/Strong Savings Rate/i)).toBeInTheDocument();
    });
  });

  it('displays error state when dashboard API fails', async () => {
    vi.mocked(dashboardApi.getDashboard).mockRejectedValue(new Error('Network error'));

    render(
      <BrowserRouter>
        <DashboardPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/Network error|Failed to load dashboard/i)).toBeInTheDocument();
    });
  });
});
