import React, { useEffect, useState } from 'react';
import { Plus, Trash2, Edit2, FolderTree, Lock } from 'lucide-react';
import { categoryApi } from '../api/categoryApi';
import { Category, CategoryRequest, CategoryType } from '../types/category.types';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Select } from '../components/common/Select';
import { Modal } from '../components/common/Modal';
import { Badge } from '../components/common/Badge';
import { LoadingSpinner } from '../components/feedback/LoadingSpinner';
import { ErrorState } from '../components/feedback/ErrorState';
import { ConfirmDialog } from '../components/feedback/ConfirmDialog';
import { extractErrorMessage } from '../utils/errorExtractor';

export const CategoriesPage: React.FC = () => {
  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState<Category | null>(null);
  const [name, setName] = useState('');
  const [type, setType] = useState<CategoryType>('EXPENSE');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Delete State
  const [deleteTargetId, setDeleteTargetId] = useState<string | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const fetchCategories = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await categoryApi.getCategories();
      setCategories(data);
    } catch (err) {
      setError(extractErrorMessage(err, 'Failed to load categories.'));
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchCategories();
  }, []);

  const handleOpenAdd = () => {
    setEditingCategory(null);
    setName('');
    setType('EXPENSE');
    setIsModalOpen(true);
  };

  const handleOpenEdit = (cat: Category) => {
    setEditingCategory(cat);
    setName(cat.name);
    setType(cat.type);
    setIsModalOpen(true);
  };

  const handleSaveCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    setIsSubmitting(true);
    try {
      const req: CategoryRequest = { name: name.trim(), type };
      if (editingCategory) {
        await categoryApi.updateCategory(editingCategory.id, req);
      } else {
        await categoryApi.createCategory(req);
      }
      setIsModalOpen(false);
      fetchCategories();
    } catch (err) {
      alert(extractErrorMessage(err, 'Failed to save category.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteCategory = async () => {
    if (!deleteTargetId) return;
    setIsDeleting(true);
    try {
      await categoryApi.deleteCategory(deleteTargetId);
      setDeleteTargetId(null);
      fetchCategories();
    } catch (err) {
      alert(extractErrorMessage(err, 'Cannot delete category (may be referenced by existing transactions or is a system category).'));
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
            Categories
          </h1>
          <p style={{ fontSize: '0.875rem', color: '#94a3b8' }}>
            Manage income and expense classification categories.
          </p>
        </div>
        <Button variant="primary" size="md" onClick={handleOpenAdd} leftIcon={<Plus size={18} />}>
          Add Custom Category
        </Button>
      </div>

      {isLoading ? (
        <LoadingSpinner message="Loading categories..." />
      ) : error ? (
        <ErrorState message={error} onRetry={fetchCategories} />
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1rem' }}>
          {categories.map((cat) => {
            const isSystem = cat.userId === null;

            return (
              <Card key={cat.id} style={{ padding: '1.25rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <div
                      style={{
                        padding: '8px',
                        borderRadius: '10px',
                        backgroundColor: cat.type === 'INCOME' ? 'rgba(16, 185, 129, 0.12)' : 'rgba(99, 102, 241, 0.12)',
                        color: cat.type === 'INCOME' ? '#10b981' : '#818cf8',
                      }}
                    >
                      <FolderTree size={18} />
                    </div>
                    <div>
                      <h4 style={{ fontSize: '0.95rem', fontWeight: 600, color: '#f8fafc' }}>{cat.name}</h4>
                      <span style={{ fontSize: '0.75rem', color: '#64748b' }}>
                        {isSystem ? 'System Default' : 'Custom Category'}
                      </span>
                    </div>
                  </div>

                  <Badge variant={cat.type === 'INCOME' ? 'success' : 'info'}>{cat.type}</Badge>
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
                  {isSystem ? (
                    <span style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.75rem', color: '#64748b' }}>
                      <Lock size={12} /> System Locked
                    </span>
                  ) : (
                    <>
                      <button
                        onClick={() => handleOpenEdit(cat)}
                        style={{ background: 'transparent', border: 'none', color: '#94a3b8', cursor: 'pointer', padding: '4px' }}
                        title="Edit"
                      >
                        <Edit2 size={15} />
                      </button>
                      <button
                        onClick={() => setDeleteTargetId(cat.id)}
                        style={{ background: 'transparent', border: 'none', color: '#ef4444', cursor: 'pointer', padding: '4px' }}
                        title="Delete"
                      >
                        <Trash2 size={15} />
                      </button>
                    </>
                  )}
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
        title={editingCategory ? 'Edit Category' : 'New Category'}
      >
        <form onSubmit={handleSaveCategory} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <Input
            label="Category Name"
            type="text"
            placeholder="e.g. Travel & Flight"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />

          <Select
            label="Category Type"
            value={type}
            onChange={(e) => setType(e.target.value as CategoryType)}
            options={[
              { value: 'EXPENSE', label: 'Expense' },
              { value: 'INCOME', label: 'Income' },
            ]}
          />

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '0.5rem' }}>
            <Button variant="ghost" type="button" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" isLoading={isSubmitting}>
              {editingCategory ? 'Update' : 'Save'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation */}
      <ConfirmDialog
        isOpen={!!deleteTargetId}
        onClose={() => setDeleteTargetId(null)}
        onConfirm={handleDeleteCategory}
        title="Delete Custom Category"
        message="Are you sure you want to delete this custom category? Categories linked to existing transactions cannot be deleted."
        confirmText="Delete"
        isDangerous
        isLoading={isDeleting}
      />
    </div>
  );
};
