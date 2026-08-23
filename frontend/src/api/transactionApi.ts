import { axiosClient } from './axiosClient';
import { ApiResponse, PagedResponse } from '../types/api.types';
import { Transaction, TransactionRequest, CategorySpendingAggregation } from '../types/transaction.types';

export const transactionApi = {
  getTransactions: async (params?: {
    from?: string;
    to?: string;
    type?: string;
    categoryId?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<Transaction>> => {
    const res = await axiosClient.get<ApiResponse<PagedResponse<Transaction>>>('/transactions', { params });
    return res.data.data;
  },

  getTransactionById: async (id: string): Promise<Transaction> => {
    const res = await axiosClient.get<ApiResponse<Transaction>>(`/transactions/${id}`);
    return res.data.data;
  },

  createTransaction: async (request: TransactionRequest): Promise<Transaction> => {
    const res = await axiosClient.post<ApiResponse<Transaction>>('/transactions', request);
    return res.data.data;
  },

  updateTransaction: async (id: string, request: TransactionRequest): Promise<Transaction> => {
    const res = await axiosClient.put<ApiResponse<Transaction>>(`/transactions/${id}`, request);
    return res.data.data;
  },

  deleteTransaction: async (id: string): Promise<void> => {
    await axiosClient.delete(`/transactions/${id}`);
  },

  getCategorySpending: async (from?: string, to?: string): Promise<CategorySpendingAggregation[]> => {
    const res = await axiosClient.get<ApiResponse<CategorySpendingAggregation[]>>('/transactions/spending/by-category', {
      params: { from, to },
    });
    return res.data.data;
  },
};
