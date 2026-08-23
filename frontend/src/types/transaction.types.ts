export type TransactionType = 'INCOME' | 'EXPENSE';

export interface Transaction {
  id: string;
  userId: string;
  categoryId: string;
  categoryName?: string;
  amount: number;
  type: TransactionType;
  description: string | null;
  transactionDate: string;
  createdAt: string;
  updatedAt: string;
}

export interface TransactionRequest {
  categoryId: string;
  amount: number;
  type: TransactionType;
  description?: string;
  transactionDate: string;
}

export interface CategorySpendingAggregation {
  categoryId: string;
  categoryName: string;
  totalAmount: number;
  percentage: number;
}
