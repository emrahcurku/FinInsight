import React, { useEffect, useState, useCallback } from 'react';
import { Plus, Trash2, Edit2, PieChart } from 'lucide-react';
import { budgetApi } from '../api/budgetApi';
import { categoryApi } from '../api/categoryApi';
import { Budget, CreateBudgetRequest, UpdateBudgetRequest } from '../types/budget.types';
import { Category } from '../types/category.types';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Select } from '../components/common/Select';
import { Modal } from '../components/common/Modal';
import { Badge } from '../components/common/Badge';
import { LoadingSpinner } from '../components/feedback/LoadingSpinner';
import { ErrorState } from '../components/feedback/ErrorState';
import { ConfirmDialog } from '../components/feedback/ConfirmDialog';
import { formatCurrency, formatPercentage } from '../utils/currencyFormatter';
import { extractErrorMessage } from '../utils/errorExtractor';

export const BudgetsPage: React.FC = () => {
  const now = new Date();
  const [month, setMonth] = useState<number>(now.getMonth() + 1);
  const [year, setYear] = useState<number>(now.getFullYear());
  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingBudget, setEditingBudget] = useState<Budget | null>(null);
  const [formCategoryId, setFormCategoryId] = useState('');
  const [formAmount, setFormAmount] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Delete State
  const [deleteTargetId, setDeleteTargetId] = useState<string | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const fetchCategories = async () => {
    try {
      const data = await categoryApi.getCategories();
      const expenseCats = data.filter((c) => c.type === 'EXPENSE');
      setCategories(expenseCats);
      if (expenseCats.length > 0 && !formCategoryId) {
        setFormCategoryId(expenseCats[0].id);
      }
    } catch {
      // Ignored
    }
  };

  const fetchBudgets = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await budgetApi.getBudgets({ month, year, size: 50 });
      setBudgets(data.content || []);
    } catch (err) {
      setError(extractErrorMessage(err, 'Failed to load budgets.'));
    } finally {
      setIsLoading(false);
    }
  }, [month, year]);

  useEffect(() => {
    fetchCategories();
  }, []);

  useEffect(() => {
    fetchBudgets();
  }, [fetchBudgets]);

  const handleOpenAdd = () => {
    setEditingBudget(null);
    setFormAmount('');
    if (categories.length > 0) setFormCategoryId(categories[0].id);
    setIsModalOpen(true);
  };

  const handleOpenEdit = (b: Budget) => {
    setEditingBudget(b);
    setFormCategoryId(b.categoryId);
    setFormAmount(String(b.amount));
    setIsModalOpen(true);
  };

  const handleSaveBudget = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formAmount || (!editingBudget && !formCategoryId)) return;

    setIsSubmitting(true);
    try {
      if (editingBudget) {
        const updateReq: UpdateBudgetRequest = {
          amount: parseFloat(formAmount),
        };
        await budgetApi.updateBudget(editingBudget.id, updateReq);
      } else {
        const createReq: CreateBudgetRequest = {
          categoryId: formCategoryId,
          amount: parseFloat(formAmount),
          month,
          year,
        };
        await budgetApi.createBudget(createReq);
      }

      setIsModalOpen(false);
      fetchBudgets();
    } catch (err) {
      alert(extractErrorMessage(err, 'Failed to save budget limit.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteBudget = async () => {
    if (!deleteTargetId) return;
    setIsDeleting(true);
    try {
      await budgetApi.deleteBudget(deleteTargetId);
      setDeleteTargetId(null);
      fetchBudgets();
    } catch (err) {
      alert(extractErrorMessage(err, 'Failed to delete budget limit.'));
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.6rem', fontWeight: 800, color: '#f8fafc', letterSpacing: '-0.02em' }}>
            Monthly Budgets
          </h1>
          <p style={{ fontSize: '0.875rem', color: '#94a3b8' }}>
            Set category limits and track real-time utilization thresholds.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
          <Select
            value={month}
            onChange={(e) => setMonth(Number(e.target.value))}
            options={[
              { value: 1, label: 'January' },
              { value: 2, label: 'February' },
              { value: 3, label: 'March' },
              { value: 4, label: 'April' },
              { value: 5, label: 'May' },
              { value: 6, label: 'June' },
              { value: 7, label: 'July' },
              { value: 8, label: 'August' },
              { value: 9, label: 'September' },
              { value: 10, label: 'October' },
              { value: 11, label: 'November' },
              { value: 12, label: 'December' },
            ]}
          />
          <Select
            value={year}
            onChange={(e) => setYear(Number(e.target.value))}
            options={[
              { value: 2025, label: '2025' },
              { value: 2026, label: '2026' },
              { value: 2027, label: '2027' },
            ]}
          />
          <Button variant="primary" size="md" onClick={handleOpenAdd} leftIcon={<Plus size={18} />}>
            Set Budget
          </Button>
        </div>
      </div>

      {isLoading ? (
        <LoadingSpinner message="Loading monthly budget thresholds..." />
      ) : error ? (
        <ErrorState message={error} onRetry={fetchBudgets} />
      ) : budgets.length === 0 ? (
        <Card style={{ textAlign: 'center', padding: '3rem 1.5rem' }}>
          <PieChart size={36} style={{ color: '#818cf8', margin: '0 auto 1rem auto' }} />
          <h3 style={{ fontSize: '1.1rem', fontWeight: 600, color: '#f8fafc', marginBottom: '0.5rem' }}>
            No Budgets Defined
          </h3>
          <p style={{ fontSize: '0.85rem', color: '#64748b', maxWidth: '400px', margin: '0 auto 1.5rem auto' }}>
            Set category limits for this month to monitor your spending and receive automated alerts.
          </p>
          <Button variant="primary" size="sm" onClick={handleOpenAdd}>
            Create First Budget
          </Button>
        </Card>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1.25rem' }}>
          {budgets.map((b) => {
            const spent = Number(b.spentAmount || 0);
            const total = Number(b.amount);
            const pct = total > 0 ? (spent / total) * 100 : 0;
            const isExceeded = pct > 100;
            const isWarning = pct >= 80 && pct <= 100;

            return (
              <Card key={b.id} style={{ padding: '1.25rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div>
                    <h4 style={{ fontSize: '1rem', fontWeight: 600, color: '#f8fafc' }}>
                      {b.categoryName || 'Category'}
                    </h4>
                    <span style={{ fontSize: '0.75rem', color: '#64748b' }}>
                      Target: {formatCurrency(b.amount)}
                    </span>
                  </div>
                  <Badge variant={isExceeded ? 'danger' : isWarning ? 'warning' : 'success'}>
                    {formatPercentage(pct)}
                  </Badge>
                </div>

                {/* Progress bar */}
                <div style={{ marginTop: '1rem', marginBottom: '0.75rem' }}>
                  <div
                    style={{
                      height: '8px',
                      backgroundColor: 'rgba(255, 255, 255, 0.06)',
                      borderRadius: '4px',
                      overflow: 'hidden',
                    }}
                  >
                    <div
                      style={{
                        height: '100%',
                        width: `${Math.min(pct, 100)}%`,
                        backgroundColor: isExceeded ? '#ef4444' : isWarning ? '#f59e0b' : '#10b981',
                        borderRadius: '4px',
                        transition: 'width 0.4s ease',
                      }}
                    />
                  </div>
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', color: '#94a3b8' }}>
                  <span>Spent: <strong style={{ color: '#f8fafc' }}>{formatCurrency(spent)}</strong></span>
                  <span>
                    {isExceeded ? (
                      <strong style={{ color: '#ef4444' }}>Exceeded by {formatCurrency(spent - total)}</strong>
                    ) : (
                      <span>Remaining: <strong style={{ color: '#10b981' }}>{formatCurrency(total - spent)}</strong></span>
                    )}
                  </span>
                </div>

                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'flex-end',
                    alignItems: 'center',
                    marginTop: '1rem',
                    gap: '8px',
                    borderTop: '1px solid rgba(255, 255, 255, 0.04)',
                    paddingTop: '0.75rem',
                  }}
                >
                  <button
                    onClick={() => handleOpenEdit(b)}
                    style={{ background: 'transparent', border: 'none', color: '#94a3b8', cursor: 'pointer', padding: '4px' }}
                    title="Edit"
                  >
                    <Edit2 size={15} />
                  </button>
                  <button
                    onClick={() => setDeleteTargetId(b.id)}
                    style={{ background: 'transparent', border: 'none', color: '#ef4444', cursor: 'pointer', padding: '4px' }}
                    title="Delete"
                  >
                    <Trash2 size={15} />
                  </button>
                </div>
              </Card>
            );
          })}
        </div>
      )}

      {/* Add/Edit Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingBudget ? 'Edit Budget Target' : 'New Monthly Budget'}
      >
        <form onSubmit={handleSaveBudget} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <Select
            label="Expense Category"
            value={formCategoryId}
            onChange={(e) => setFormCategoryId(e.target.value)}
            options={categories.map((c) => ({ value: c.id, label: c.name }))}
            disabled={!!editingBudget}
          />

          <Input
            label="Monthly Limit ($)"
            type="number"
            step="0.01"
            placeholder="500.00"
            value={formAmount}
            onChange={(e) => setFormAmount(e.target.value)}
            required
          />

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '0.5rem' }}>
            <Button variant="ghost" type="button" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" isLoading={isSubmitting}>
              {editingBudget ? 'Update' : 'Save'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation */}
      <ConfirmDialog
        isOpen={!!deleteTargetId}
        onClose={() => setDeleteTargetId(null)}
        onConfirm={handleDeleteBudget}
        title="Delete Budget Target"
        message="Are you sure you want to remove this category budget limit?"
        confirmText="Delete"
        isDangerous
        isLoading={isDeleting}
      />
    </div>
  );
};
