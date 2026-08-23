import { axiosClient } from './axiosClient';
import { ApiResponse } from '../types/api.types';
import { AiInsightResponse } from '../types/ai.types';

export const aiApi = {
  getAiInsights: async (from?: string, to?: string): Promise<AiInsightResponse> => {
    const res = await axiosClient.get<ApiResponse<AiInsightResponse>>('/ai/insights', {
      params: { from, to },
    });
    return res.data.data;
  },
};
