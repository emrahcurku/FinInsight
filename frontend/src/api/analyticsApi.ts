import { axiosClient } from './axiosClient';
import { ApiResponse } from '../types/api.types';
import { FinancialSummaryDTO, MonthlyTrendDTO, CategorySpendingDTO, BudgetOverviewDTO } from '../types/dashboard.types';

export interface TopCategoryResponse {
  categoryId: string;
  categoryName: string;
  totalSpent: number;
  transactionCount: number;
  percentageOfTotal: number;
}

export const analyticsApi = {
  getFinancialSummary: async (from?: string, to?: string): Promise<FinancialSummaryDTO> => {
    const res = await axiosClient.get<ApiResponse<FinancialSummaryDTO>>('/analytics/summary', {
      params: { from, to },
    });
    return res.data.data;
  },

  getMonthlySummary: async (from?: string, to?: string): Promise<MonthlyTrendDTO[]> => {
    const res = await axiosClient.get<ApiResponse<MonthlyTrendDTO[]>>('/analytics/monthly-summary', {
      params: { from, to },
    });
    return res.data.data;
  },

  getCategoryBreakdown: async (from?: string, to?: string): Promise<CategorySpendingDTO[]> => {
    const res = await axiosClient.get<ApiResponse<CategorySpendingDTO[]>>('/analytics/spending-by-category', {
      params: { from, to },
    });
    return res.data.data;
  },

  getBudgetOverview: async (year?: number, month?: number): Promise<BudgetOverviewDTO> => {
    const res = await axiosClient.get<ApiResponse<BudgetOverviewDTO>>('/analytics/budget-overview', {
      params: { year, month },
    });
    return res.data.data;
  },

  getTopCategory: async (from?: string, to?: string): Promise<TopCategoryResponse> => {
    const res = await axiosClient.get<ApiResponse<TopCategoryResponse>>('/analytics/top-category', {
      params: { from, to },
    });
    return res.data.data;
  },
};
