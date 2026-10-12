-- ============================================
-- Dhanova AI - Database Schema (Phase 1: Core 50%)
-- ============================================

CREATE DATABASE IF NOT EXISTS dhanova_ai;
USE dhanova_ai;

-- ============================================
-- USERS
-- ============================================
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    preferred_currency VARCHAR(10) DEFAULT 'INR',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- FAMILIES
-- ============================================
CREATE TABLE families (
    family_id INT AUTO_INCREMENT PRIMARY KEY,
    family_name VARCHAR(150) NOT NULL,
    base_currency VARCHAR(10) DEFAULT 'INR',
    created_by INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(user_id)
);

-- ============================================
-- FAMILY MEMBERS (link table with role)
-- ============================================
CREATE TABLE family_members (
    id INT AUTO_INCREMENT PRIMARY KEY,
    family_id INT NOT NULL,
    user_id INT NOT NULL,
    role ENUM('ADMIN', 'MEMBER') DEFAULT 'MEMBER',
    can_add_transactions BOOLEAN DEFAULT TRUE,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (family_id) REFERENCES families(family_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY unique_family_user (family_id, user_id)
);

-- ============================================
-- FAMILY INVITATIONS
-- ============================================
CREATE TABLE family_invitations (
    invitation_id INT AUTO_INCREMENT PRIMARY KEY,
    family_id INT NOT NULL,
    invited_email VARCHAR(150) NOT NULL,
    invited_by INT NOT NULL,
    status ENUM('PENDING', 'ACCEPTED', 'REJECTED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (family_id) REFERENCES families(family_id) ON DELETE CASCADE,
    FOREIGN KEY (invited_by) REFERENCES users(user_id)
);

-- ============================================
-- CATEGORIES
-- ============================================
CREATE TABLE categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type ENUM('INCOME', 'EXPENSE') NOT NULL,
    is_default BOOLEAN DEFAULT TRUE,
    family_id INT DEFAULT NULL,
    FOREIGN KEY (family_id) REFERENCES families(family_id) ON DELETE CASCADE
);

-- ============================================
-- CURRENCIES (simple static reference + rate to base)
-- ============================================
CREATE TABLE currencies (
    code VARCHAR(10) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    rate_to_inr DECIMAL(12,4) NOT NULL DEFAULT 1.0000
);

-- ============================================
-- TRANSACTIONS
-- ============================================
CREATE TABLE transactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    family_id INT NOT NULL,
    user_id INT NOT NULL,
    scope ENUM('FAMILY', 'PERSONAL') NOT NULL DEFAULT 'PERSONAL',
    type ENUM('INCOME', 'EXPENSE') NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    category_id INT,
    description VARCHAR(255),
    payment_method VARCHAR(50),
    transaction_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (family_id) REFERENCES families(family_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(category_id),
    FOREIGN KEY (currency) REFERENCES currencies(code)
);

-- ============================================
-- BUDGETS (FAMILY or INDIVIDUAL scope)
-- ============================================
CREATE TABLE budgets (
    budget_id INT AUTO_INCREMENT PRIMARY KEY,
    scope ENUM('FAMILY', 'INDIVIDUAL') NOT NULL,
    family_id INT NOT NULL,
    user_id INT DEFAULT NULL,
    category_id INT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (family_id) REFERENCES families(family_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(category_id)
);

-- ============================================
-- SAVINGS GOALS (FAMILY or INDIVIDUAL scope)
-- ============================================
CREATE TABLE savings_goals (
    goal_id INT AUTO_INCREMENT PRIMARY KEY,
    scope ENUM('FAMILY', 'INDIVIDUAL') NOT NULL,
    family_id INT NOT NULL,
    user_id INT DEFAULT NULL,
    goal_name VARCHAR(150) NOT NULL,
    target_amount DECIMAL(12,2) NOT NULL,
    current_amount DECIMAL(12,2) DEFAULT 0,
    target_date DATE,
    created_by INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (family_id) REFERENCES families(family_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(user_id)
);

-- ============================================
-- MEMBER CONTRIBUTIONS
-- ============================================
CREATE TABLE member_contributions (
    contribution_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    target_type ENUM('FAMILY_GOAL', 'FAMILY_BUDGET', 'INDIVIDUAL_GOAL', 'INDIVIDUAL_BUDGET') NOT NULL,
    goal_id INT DEFAULT NULL,
    budget_id INT DEFAULT NULL,
    committed_amount DECIMAL(12,2) NOT NULL,
    contributed_amount DECIMAL(12,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (goal_id) REFERENCES savings_goals(goal_id) ON DELETE CASCADE,
    FOREIGN KEY (budget_id) REFERENCES budgets(budget_id) ON DELETE CASCADE
);

-- ============================================
-- PLACEHOLDER TABLES FOR LATER PHASES (empty for now)
-- ============================================
CREATE TABLE receipts (
    receipt_id INT AUTO_INCREMENT PRIMARY KEY,
    transaction_id INT,
    image_path VARCHAR(255),
    raw_ocr_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE SET NULL
);

CREATE TABLE ai_insights (
    insight_id INT AUTO_INCREMENT PRIMARY KEY,
    family_id INT,
    insight_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (family_id) REFERENCES families(family_id) ON DELETE CASCADE
);

CREATE TABLE chat_history (
    chat_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    message TEXT,
    response TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ============================================
-- SEED DATA: default categories + currencies
-- ============================================
INSERT INTO currencies (code, name, rate_to_inr) VALUES
('INR', 'Indian Rupee', 1.0000),
('USD', 'US Dollar', 83.0000),
('EUR', 'Euro', 90.0000),
('GBP', 'British Pound', 105.0000);

INSERT INTO categories (name, type, is_default) VALUES
('Salary', 'INCOME', TRUE),
('Business', 'INCOME', TRUE),
('Other Income', 'INCOME', TRUE),
('Grocery', 'EXPENSE', TRUE),
('Transport', 'EXPENSE', TRUE),
('Bills', 'EXPENSE', TRUE),
('Shopping', 'EXPENSE', TRUE),
('Healthcare', 'EXPENSE', TRUE),
('Education', 'EXPENSE', TRUE),
('Entertainment', 'EXPENSE', TRUE),
('Other Expense', 'EXPENSE', TRUE);
