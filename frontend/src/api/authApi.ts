import { axiosClient } from './axiosClient';
import { ApiResponse } from '../types/api.types';
import { AuthResponse, LoginRequest, RegisterRequest, UserSummaryResponse } from '../types/auth.types';

export const authApi = {
  login: async (request: LoginRequest): Promise<AuthResponse> => {
    const res = await axiosClient.post<ApiResponse<AuthResponse>>('/auth/login', request);
    return res.data.data;
  },

  register: async (request: RegisterRequest): Promise<UserSummaryResponse> => {
    const res = await axiosClient.post<ApiResponse<UserSummaryResponse>>('/auth/register', request);
    return res.data.data;
  },

  getCurrentUser: async (): Promise<UserSummaryResponse> => {
    const res = await axiosClient.get<ApiResponse<UserSummaryResponse>>('/auth/me');
    return res.data.data;
  },

  refresh: async (): Promise<AuthResponse> => {
    const res = await axiosClient.post<ApiResponse<AuthResponse>>('/auth/refresh');
    return res.data.data;
  },

  logout: async (): Promise<void> => {
    await axiosClient.post<ApiResponse<void>>('/auth/logout');
  },
};
