export type TrendDirection = 'INCREASED' | 'DECREASED' | 'UNCHANGED';
export type InsightSeverity = 'INFO' | 'WARNING' | 'DANGER';

export interface FinancialSummaryDTO {
  totalIncome: number;
  totalExpense: number;
  netBalance: number;
  transactionCount: number;
}

export interface MonthlyTrendDTO {
  yearMonth: string;
  totalIncome: number;
  totalExpense: number;
  netSavings: number;
}

export interface CategorySpendingDTO {
  categoryId: string;
  categoryName: string;
  totalAmount: number;
  percentage: number;
}

export interface BudgetOverviewDTO {
  totalBudgeted: number;
  totalSpent: number;
  overallUsagePercentage: number;
  activeBudgetCount: number;
  warningBudgetCount: number;
  exceededBudgetCount: number;
}

export interface RecentTransactionDTO {
  id: string;
  categoryId: string;
  categoryName: string;
  amount: number;
  type: 'INCOME' | 'EXPENSE';
  description: string | null;
  transactionDate: string;
}

export interface PreviousMonthComparisonDTO {
  currentMonthExpense: number;
  previousMonthExpense: number;
  expenseChangeAmount: number;
  expenseChangePercentage: number;
  trend: TrendDirection;
}

export interface FinancialInsightDTO {
  type: string;
  title: string;
  message: string;
  severity: InsightSeverity;
}

export interface DashboardResponse {
  financialSummary: FinancialSummaryDTO;
  monthlyTrend: MonthlyTrendDTO[];
  categorySpending: CategorySpendingDTO[];
  budgetOverview: BudgetOverviewDTO;
  recentTransactions: RecentTransactionDTO[];
  previousMonthComparison: PreviousMonthComparisonDTO;
  insights: FinancialInsightDTO[];
}
