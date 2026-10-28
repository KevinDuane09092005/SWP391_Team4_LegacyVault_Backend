-- LegacyVault demo billing tables. Payment events use the shared
-- in_app_notifications table managed by the Flow 5-7 notification service.
-- The payment gateway is simulated. No PAN or CVV columns are present.

CREATE TABLE IF NOT EXISTS payment_methods (
    payment_method_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    provider VARCHAR(32) NOT NULL DEFAULT 'MOCK',
    provider_payment_method_token VARCHAR(128) NOT NULL,
    cardholder_name VARCHAR(100) NOT NULL,
    card_brand VARCHAR(24) NOT NULL,
    card_last_four CHAR(4) NOT NULL,
    expiry_month TINYINT NOT NULL,
    expiry_year SMALLINT NOT NULL,
    billing_address_line1 VARCHAR(200) NULL,
    billing_city VARCHAR(100) NULL,
    billing_postal_code VARCHAR(20) NULL,
    billing_country CHAR(2) NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (payment_method_id),
    UNIQUE KEY uq_payment_method_provider_token (provider_payment_method_token),
    KEY ix_payment_method_user_default (user_id, is_default, created_at),
    CONSTRAINT chk_payment_method_expiry_month CHECK (expiry_month BETWEEN 1 AND 12),
    CONSTRAINT chk_payment_method_last_four CHECK (card_last_four REGEXP '^[0-9]{4}$'),
    CONSTRAINT fk_payment_method_user FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS payment_transactions (
    transaction_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    plan_id VARCHAR(24) NOT NULL,
    billing_cycle VARCHAR(12) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(12) NOT NULL,
    provider_transaction_reference VARCHAR(128) NULL,
    failure_code VARCHAR(48) NULL,
    failure_message VARCHAR(255) NULL,
    payment_method_brand VARCHAR(24) NULL,
    payment_method_last_four CHAR(4) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (transaction_id),
    KEY ix_payment_transactions_user_created (user_id, created_at),
    CONSTRAINT chk_payment_transaction_cycle CHECK (billing_cycle IN ('MONTHLY', 'YEARLY')),
    CONSTRAINT chk_payment_transaction_status CHECK (status IN ('SUCCEEDED', 'FAILED')),
    CONSTRAINT fk_payment_transaction_user FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS user_subscriptions (
    subscription_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    plan_id VARCHAR(24) NOT NULL,
    billing_cycle VARCHAR(12) NOT NULL,
    status VARCHAR(12) NOT NULL,
    started_at DATETIME(6) NOT NULL,
    current_period_ends_at DATETIME(6) NULL,
    grace_period_ends_at DATETIME(6) NULL,
    auto_renew BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (subscription_id),
    UNIQUE KEY uq_user_subscription_user (user_id),
    KEY ix_user_subscription_renewal (status, current_period_ends_at),
    KEY ix_user_subscription_grace (status, grace_period_ends_at),
    CONSTRAINT chk_user_subscription_plan CHECK (plan_id IN ('free', 'essentials', 'plus')),
    CONSTRAINT chk_user_subscription_cycle CHECK (billing_cycle IN ('MONTHLY', 'YEARLY')),
    CONSTRAINT chk_user_subscription_status CHECK (status IN ('ACTIVE', 'PAST_DUE', 'EXPIRED', 'CANCELED')),
    CONSTRAINT fk_user_subscription_user FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Prices and plan limits are currently held in PlanCatalogService and intentionally
-- match the frontend mock catalog. Replace with admin-managed data only if required.
-- A real provider must replace MockPaymentGateway before any live payment use.
