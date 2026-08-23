import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { BudgetsPage } from '../pages/BudgetsPage';
import { budgetApi } from '../api/budgetApi';
import { categoryApi } from '../api/categoryApi';

vi.mock('../api/budgetApi', () => ({
  budgetApi: {
    getBudgets: vi.fn(),
    createBudget: vi.fn(),
    updateBudget: vi.fn(),
    deleteBudget: vi.fn(),
  },
}));

vi.mock('../api/categoryApi', () => ({
  categoryApi: {
    getCategories: vi.fn(),
  },
}));

describe('BudgetsPage Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders budget cards with thresholds and utilization percentages', async () => {
    vi.mocked(categoryApi.getCategories).mockResolvedValue([
      { id: 'cat-1', userId: null, name: 'Dining', type: 'EXPENSE', createdAt: '', updatedAt: '' },
    ]);

    vi.mocked(budgetApi.getBudgets).mockResolvedValue({
      content: [
        {
          id: 'b-1',
          userId: 'u-1',
          categoryId: 'cat-1',
          categoryName: 'Dining',
          amount: 500.0,
          month: 8,
          year: 2026,
          spentAmount: 450.0,
          remainingAmount: 50.0,
          usagePercentage: 90.0,
          status: 'WARNING',
          createdAt: '',
          updatedAt: '',
        },
      ],
      page: 0,
      size: 50,
      totalElements: 1,
      totalPages: 1,
      last: true,
    });

    render(
      <BrowserRouter>
        <BudgetsPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Dining')).toBeInTheDocument();
      expect(screen.getByText('90.0%')).toBeInTheDocument();
      expect(screen.getByText('$450.00')).toBeInTheDocument(); // Spent
      expect(screen.getByText('$50.00')).toBeInTheDocument(); // Remaining
    });
  });
});
