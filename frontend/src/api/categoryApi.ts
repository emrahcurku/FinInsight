import { axiosClient } from './axiosClient';
import { ApiResponse } from '../types/api.types';
import { Category, CategoryRequest } from '../types/category.types';

export const categoryApi = {
  getCategories: async (): Promise<Category[]> => {
    const res = await axiosClient.get<ApiResponse<Category[]>>('/categories');
    return res.data.data;
  },

  getCategoryById: async (id: string): Promise<Category> => {
    const res = await axiosClient.get<ApiResponse<Category>>(`/categories/${id}`);
    return res.data.data;
  },

  createCategory: async (request: CategoryRequest): Promise<Category> => {
    const res = await axiosClient.post<ApiResponse<Category>>('/categories', request);
    return res.data.data;
  },

  updateCategory: async (id: string, request: CategoryRequest): Promise<Category> => {
    const res = await axiosClient.put<ApiResponse<Category>>(`/categories/${id}`, request);
    return res.data.data;
  },

  deleteCategory: async (id: string): Promise<void> => {
    await axiosClient.delete(`/categories/${id}`);
  },
};
