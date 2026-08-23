import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { TransactionsPage } from '../pages/TransactionsPage';
import { transactionApi } from '../api/transactionApi';
import { categoryApi } from '../api/categoryApi';

vi.mock('../api/transactionApi', () => ({
  transactionApi: {
    getTransactions: vi.fn(),
    createTransaction: vi.fn(),
    updateTransaction: vi.fn(),
    deleteTransaction: vi.fn(),
  },
}));

vi.mock('../api/categoryApi', () => ({
  categoryApi: {
    getCategories: vi.fn(),
  },
}));

describe('TransactionsPage Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders transactions list with PagedResponse format', async () => {
    vi.mocked(categoryApi.getCategories).mockResolvedValue([
      { id: 'cat-1', userId: null, name: 'Groceries', type: 'EXPENSE', createdAt: '', updatedAt: '' },
    ]);

    vi.mocked(transactionApi.getTransactions).mockResolvedValue({
      content: [
        {
          id: 'tx-1',
          userId: 'user-1',
          categoryId: 'cat-1',
          categoryName: 'Groceries',
          amount: 85.5,
          type: 'EXPENSE',
          description: 'Market shopping',
          transactionDate: '2026-08-22',
          createdAt: '',
          updatedAt: '',
        },
      ],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
      last: true,
    });

    render(
      <BrowserRouter>
        <TransactionsPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Market shopping')).toBeInTheDocument();
      expect(screen.getByText('Groceries')).toBeInTheDocument();
      expect(screen.getByText('-$85.50')).toBeInTheDocument();
    });
  });

  it('submits a new transaction with valid fields', async () => {
    vi.mocked(categoryApi.getCategories).mockResolvedValue([
      { id: 'cat-1', userId: null, name: 'Groceries', type: 'EXPENSE', createdAt: '', updatedAt: '' },
    ]);

    vi.mocked(transactionApi.getTransactions).mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      last: true,
    });

    vi.mocked(transactionApi.createTransaction).mockResolvedValue({
      id: 'new-tx',
      userId: 'user-1',
      categoryId: 'cat-1',
      amount: 45.0,
      type: 'EXPENSE',
      description: 'Bakery',
      transactionDate: '2026-08-23',
      createdAt: '',
      updatedAt: '',
    });

    render(
      <BrowserRouter>
        <TransactionsPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Add Transaction/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Add Transaction/i }));

    expect(screen.getByText('New Transaction')).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/Amount \(\$\)/i), { target: { value: '45.00' } });
    fireEvent.change(screen.getByLabelText(/Description/i), { target: { value: 'Bakery' } });

    fireEvent.click(screen.getByRole('button', { name: /Save/i }));

    await waitFor(() => {
      expect(transactionApi.createTransaction).toHaveBeenCalledWith({
        categoryId: 'cat-1',
        amount: 45.0,
        type: 'EXPENSE',
        description: 'Bakery',
        transactionDate: expect.any(String),
      });
    });
  });
});
