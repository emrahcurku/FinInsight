import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from './Button';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  isLoading?: boolean;
}

export const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  onPageChange,
  isLoading = false,
}) => {
  if (totalPages <= 1) return null;

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '1rem 0 0 0',
        color: '#94a3b8',
        fontSize: '0.85rem',
      }}
    >
      <span>
        Page <strong style={{ color: '#f8fafc' }}>{currentPage + 1}</strong> of{' '}
        <strong style={{ color: '#f8fafc' }}>{totalPages}</strong>
      </span>
      <div style={{ display: 'flex', gap: '8px' }}>
        <Button
          variant="secondary"
          size="sm"
          disabled={currentPage === 0 || isLoading}
          onClick={() => onPageChange(currentPage - 1)}
          leftIcon={<ChevronLeft size={16} />}
        >
          Previous
        </Button>
        <Button
          variant="secondary"
          size="sm"
          disabled={currentPage >= totalPages - 1 || isLoading}
          onClick={() => onPageChange(currentPage + 1)}
          rightIcon={<ChevronRight size={16} />}
        >
          Next
        </Button>
      </div>
    </div>
  );
};
