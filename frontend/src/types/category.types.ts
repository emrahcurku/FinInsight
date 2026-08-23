export type CategoryType = 'INCOME' | 'EXPENSE';

export interface Category {
  id: string;
  userId: string | null;
  name: string;
  type: CategoryType;
  createdAt: string;
  updatedAt: string;
}

export interface CategoryRequest {
  name: string;
  type: CategoryType;
}
