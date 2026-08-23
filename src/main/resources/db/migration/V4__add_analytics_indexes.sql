-- =============================================
-- FinInsight — Analytics Database Indexes
-- Version: 4.0
-- =============================================

-- Optimize analytics filtering by user, transaction type and transaction date
CREATE INDEX IF NOT EXISTS idx_transactions_user_type_date ON transactions (user_id, type, transaction_date);
