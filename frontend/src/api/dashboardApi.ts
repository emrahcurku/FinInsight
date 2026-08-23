import { axiosClient } from './axiosClient';
import { ApiResponse } from '../types/api.types';
import { DashboardResponse } from '../types/dashboard.types';

export const dashboardApi = {
  getDashboard: async (from?: string, to?: string): Promise<DashboardResponse> => {
    const res = await axiosClient.get<ApiResponse<DashboardResponse>>('/dashboard', {
      params: { from, to },
    });
    return res.data.data;
  },
};
