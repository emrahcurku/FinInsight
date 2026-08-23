import { axiosClient } from './axiosClient';
import { ApiResponse, PagedResponse } from '../types/api.types';
import { Budget, CreateBudgetRequest, UpdateBudgetRequest } from '../types/budget.types';

export const budgetApi = {
  getBudgets: async (params?: {
    month?: number;
    year?: number;
    categoryId?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<Budget>> => {
    const res = await axiosClient.get<ApiResponse<PagedResponse<Budget>>>('/budgets', { params });
    return res.data.data;
  },

  getBudgetById: async (id: string): Promise<Budget> => {
    const res = await axiosClient.get<ApiResponse<Budget>>(`/budgets/${id}`);
    return res.data.data;
  },

  createBudget: async (request: CreateBudgetRequest): Promise<Budget> => {
    const res = await axiosClient.post<ApiResponse<Budget>>('/budgets', request);
    return res.data.data;
  },

  updateBudget: async (id: string, request: UpdateBudgetRequest): Promise<Budget> => {
    const res = await axiosClient.put<ApiResponse<Budget>>(`/budgets/${id}`, request);
    return res.data.data;
  },

  deleteBudget: async (id: string): Promise<void> => {
    await axiosClient.delete(`/budgets/${id}`);
  },
};
