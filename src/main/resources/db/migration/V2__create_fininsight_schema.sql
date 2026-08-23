-- =============================================
-- FinInsight — Core Database Schema
-- Version: 2.0
-- =============================================

-- --- 1. USERS TABLE ---
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN'))
);

-- --- 2. CATEGORIES TABLE ---
CREATE TABLE categories (
    id UUID PRIMARY KEY,
    user_id UUID,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_categories_type CHECK (type IN ('INCOME', 'EXPENSE'))
);

CREATE UNIQUE INDEX uq_categories_user_name_type ON categories (user_id, name, type) WHERE user_id IS NOT NULL;
CREATE UNIQUE INDEX uq_categories_system_name_type ON categories (name, type) WHERE user_id IS NULL;
CREATE INDEX idx_categories_user_id ON categories (user_id);

-- --- 3. TRANSACTIONS TABLE ---
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    category_id UUID NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    type VARCHAR(20) NOT NULL,
    description VARCHAR(255),
    transaction_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT,
    CONSTRAINT chk_transactions_amount CHECK (amount > 0),
    CONSTRAINT chk_transactions_type CHECK (type IN ('INCOME', 'EXPENSE'))
);

CREATE INDEX idx_transactions_user_date ON transactions (user_id, transaction_date DESC);
CREATE INDEX idx_transactions_user_cat_date ON transactions (user_id, category_id, transaction_date);

-- --- 4. BUDGETS TABLE ---
CREATE TABLE budgets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    category_id UUID NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    month SMALLINT NOT NULL,
    year SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT,
    CONSTRAINT uq_budgets_user_cat_period UNIQUE (user_id, category_id, year, month),
    CONSTRAINT chk_budgets_amount CHECK (amount > 0),
    CONSTRAINT chk_budgets_month CHECK (month >= 1 AND month <= 12),
    CONSTRAINT chk_budgets_year CHECK (year >= 2000 AND year <= 2100)
);

CREATE INDEX idx_budgets_user_period ON budgets (user_id, year, month);

-- --- 5. SEED SYSTEM DEFAULT CATEGORIES ---
INSERT INTO categories (id, user_id, name, type, created_at, updated_at) VALUES
    (gen_random_uuid(), NULL, 'Salary', 'INCOME', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), NULL, 'Investment & Dividends', 'INCOME', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), NULL, 'Freelance & Side Business', 'INCOME', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), NULL, 'Other Income', 'INCOME', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), NULL, 'Groceries', 'EXPENSE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), NULL, 'Food & Dining', 'EXPENSE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), NULL, 'Rent & Housing', 'EXPENSE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), NULL, 'Transportation & Fuel', 'EXPENSE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), NULL, 'Utilities & Bills', 'EXPENSE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), NULL, 'Entertainment & Leisure', 'EXPENSE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), NULL, 'Health & Medical', 'EXPENSE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), NULL, 'Shopping & Personal', 'EXPENSE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), NULL, 'Education', 'EXPENSE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), NULL, 'Other Expense', 'EXPENSE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
