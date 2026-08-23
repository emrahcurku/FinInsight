export type ThresholdStatus = 'NORMAL' | 'WARNING' | 'EXCEEDED';

export interface Budget {
  id: string;
  userId: string;
  categoryId: string;
  categoryName?: string;
  amount: number;
  month: number;
  year: number;
  spentAmount?: number;
  remainingAmount?: number;
  usagePercentage?: number;
  status?: ThresholdStatus;
  createdAt: string;
  updatedAt: string;
}

export interface BudgetRequest {
  categoryId: string;
  amount: number;
  month: number;
  year: number;
}

export interface CreateBudgetRequest {
  categoryId: string;
  amount: number;
  month: number;
  year: number;
}

export interface UpdateBudgetRequest {
  amount: number;
}
