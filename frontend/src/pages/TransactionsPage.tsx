import React, { useEffect, useState, useCallback } from 'react';
import { Plus, Trash2, Edit2, ArrowDownRight, ArrowUpRight } from 'lucide-react';
import { transactionApi } from '../api/transactionApi';
import { categoryApi } from '../api/categoryApi';
import { Transaction, TransactionRequest } from '../types/transaction.types';
import { Category } from '../types/category.types';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Select } from '../components/common/Select';
import { Modal } from '../components/common/Modal';
import { Table } from '../components/common/Table';
import { Pagination } from '../components/common/Pagination';
import { Badge } from '../components/common/Badge';
import { LoadingSpinner } from '../components/feedback/LoadingSpinner';
import { ErrorState } from '../components/feedback/ErrorState';
import { ConfirmDialog } from '../components/feedback/ConfirmDialog';
import { formatCurrency } from '../utils/currencyFormatter';
import { formatDate } from '../utils/dateFormatter';
import { extractErrorMessage } from '../utils/errorExtractor';

export const TransactionsPage: React.FC = () => {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingTx, setEditingTx] = useState<Transaction | null>(null);
  const [formCategoryId, setFormCategoryId] = useState('');
  const [formAmount, setFormAmount] = useState('');
  const [formType, setFormType] = useState<'INCOME' | 'EXPENSE'>('EXPENSE');
  const [formDescription, setFormDescription] = useState('');
  const [formDate, setFormDate] = useState(new Date().toISOString().split('T')[0]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Delete State
  const [deleteTargetId, setDeleteTargetId] = useState<string | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const fetchCategories = async () => {
    try {
      const data = await categoryApi.getCategories();
      setCategories(data);
      if (data.length > 0 && !formCategoryId) {
        setFormCategoryId(data[0].id);
      }
    } catch {
      // Handled silently
    }
  };

  const fetchTransactions = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await transactionApi.getTransactions({ page, size: 10 });
      setTransactions(data?.content || []);
      setTotalPages(data?.totalPages || 0);
    } catch (err) {
      setError(extractErrorMessage(err, 'Failed to load transactions.'));
    } finally {
      setIsLoading(false);
    }
  }, [page]);

  useEffect(() => {
    fetchCategories();
  }, []);

  useEffect(() => {
    fetchTransactions();
  }, [fetchTransactions]);

  const handleOpenAddModal = () => {
    setEditingTx(null);
    setFormAmount('');
    setFormDescription('');
    setFormType('EXPENSE');
    setFormDate(new Date().toISOString().split('T')[0]);
    if (categories.length > 0) setFormCategoryId(categories[0].id);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (tx: Transaction) => {
    setEditingTx(tx);
    setFormCategoryId(tx.categoryId);
    setFormAmount(String(tx.amount));
    setFormType(tx.type);
    setFormDescription(tx.description || '');
    setFormDate(tx.transactionDate);
    setIsModalOpen(true);
  };

  const handleSaveTransaction = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formAmount || !formCategoryId) return;

    setIsSubmitting(true);
    try {
      const request: TransactionRequest = {
        categoryId: formCategoryId,
        amount: parseFloat(formAmount),
        type: formType,
        description: formDescription.trim() || undefined,
        transactionDate: formDate,
      };

      if (editingTx) {
        await transactionApi.updateTransaction(editingTx.id, request);
      } else {
        await transactionApi.createTransaction(request);
      }

      setIsModalOpen(false);
      fetchTransactions();
    } catch (err) {
      alert(extractErrorMessage(err, 'Failed to save transaction.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteTransaction = async () => {
    if (!deleteTargetId) return;
    setIsDeleting(true);
    try {
      await transactionApi.deleteTransaction(deleteTargetId);
      setDeleteTargetId(null);
      fetchTransactions();
    } catch (err) {
      alert(extractErrorMessage(err, 'Failed to delete transaction.'));
    } finally {
      setIsDeleting(false);
    }
  };

  const columns = [
    {
      header: 'Date',
      render: (tx: Transaction) => (
        <span style={{ color: '#94a3b8', fontSize: '0.85rem' }}>{formatDate(tx.transactionDate)}</span>
      ),
    },
    {
      header: 'Description & Category',
      render: (tx: Transaction) => (
        <div>
          <div style={{ fontWeight: 600, color: '#f8fafc' }}>{tx.description || '—'}</div>
          <div style={{ fontSize: '0.75rem', color: '#64748b' }}>{tx.categoryName || 'General'}</div>
        </div>
      ),
    },
    {
      header: 'Type',
      render: (tx: Transaction) => (
        <Badge variant={tx.type === 'INCOME' ? 'success' : 'danger'}>
          {tx.type === 'INCOME' ? (
            <>
              <ArrowDownRight size={12} /> Income
            </>
          ) : (
            <>
              <ArrowUpRight size={12} /> Expense
            </>
          )}
        </Badge>
      ),
    },
    {
      header: 'Amount',
      align: 'right' as const,
      render: (tx: Transaction) => (
        <span style={{ fontWeight: 700, color: tx.type === 'INCOME' ? '#10b981' : '#ef4444' }}>
          {tx.type === 'INCOME' ? '+' : '-'}{formatCurrency(tx.amount)}
        </span>
      ),
    },
    {
      header: 'Actions',
      align: 'right' as const,
      render: (tx: Transaction) => (
        <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
          <button
            onClick={() => handleOpenEditModal(tx)}
            style={{ background: 'transparent', border: 'none', color: '#94a3b8', cursor: 'pointer', padding: '4px' }}
            title="Edit"
          >
            <Edit2 size={16} />
          </button>
          <button
            onClick={() => setDeleteTargetId(tx.id)}
            style={{ background: 'transparent', border: 'none', color: '#ef4444', cursor: 'pointer', padding: '4px' }}
            title="Delete"
          >
            <Trash2 size={16} />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Top Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.6rem', fontWeight: 800, color: '#f8fafc', letterSpacing: '-0.02em' }}>
            Transactions
          </h1>
          <p style={{ fontSize: '0.875rem', color: '#94a3b8' }}>
            Manage and track your income and expense transactions.
          </p>
        </div>
        <Button variant="primary" size="md" onClick={handleOpenAddModal} leftIcon={<Plus size={18} />}>
          Add Transaction
        </Button>
      </div>

      {/* Main Table Card */}
      <Card>
        {isLoading ? (
          <LoadingSpinner message="Loading transactions..." />
        ) : error ? (
          <ErrorState message={error} onRetry={fetchTransactions} />
        ) : (
          <>
            <Table
              columns={columns}
              data={transactions}
              keyExtractor={(item) => item.id}
              emptyText="No transactions recorded yet. Click 'Add Transaction' to log your first entry."
            />
            <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} isLoading={isLoading} />
          </>
        )}
      </Card>

      {/* Add/Edit Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingTx ? 'Edit Transaction' : 'New Transaction'}
      >
        <form onSubmit={handleSaveTransaction} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <Select
            label="Transaction Type"
            value={formType}
            onChange={(e) => setFormType(e.target.value as 'INCOME' | 'EXPENSE')}
            options={[
              { value: 'EXPENSE', label: 'Expense' },
              { value: 'INCOME', label: 'Income' },
            ]}
          />

          <Select
            label="Category"
            value={formCategoryId}
            onChange={(e) => setFormCategoryId(e.target.value)}
            options={categories.map((c) => ({ value: c.id, label: c.name }))}
          />

          <Input
            label="Amount ($)"
            type="number"
            step="0.01"
            placeholder="0.00"
            value={formAmount}
            onChange={(e) => setFormAmount(e.target.value)}
            required
          />

          <Input
            label="Description (Optional)"
            type="text"
            placeholder="e.g. Grocery shopping"
            value={formDescription}
            onChange={(e) => setFormDescription(e.target.value)}
          />

          <Input
            label="Transaction Date"
            type="date"
            value={formDate}
            onChange={(e) => setFormDate(e.target.value)}
            required
          />

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '0.5rem' }}>
            <Button variant="ghost" type="button" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" isLoading={isSubmitting}>
              {editingTx ? 'Update' : 'Save'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation */}
      <ConfirmDialog
        isOpen={!!deleteTargetId}
        onClose={() => setDeleteTargetId(null)}
        onConfirm={handleDeleteTransaction}
        title="Delete Transaction"
        message="Are you sure you want to permanently delete this transaction? This action cannot be undone."
        confirmText="Delete"
        isDangerous
        isLoading={isDeleting}
      />
    </div>
  );
};
