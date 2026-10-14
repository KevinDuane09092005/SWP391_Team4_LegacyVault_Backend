-- LegacyVault Flow 5: Legal Verification
-- Draft for MySQL 8.0.16+. Align UUID storage and external keys with the real backend schema before applying.
-- Uploaded document bytes belong in private object storage, not these tables.
-- No foreign keys are guessed for users or Flow 4 activation tables because their schema was not available.

CREATE TABLE IF NOT EXISTS legal_verification_cases (
    case_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    activation_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Flow 4 activation identifier; add an FK after confirming its actual table and key',
    executor_user_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Authenticated executor resolved by the server',
    reviewer_user_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT 'Assigned Legal Verifier resolved by the server',
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    decision_reason TEXT NULL
        COMMENT 'Required for final approval or rejection; describes the platform review outcome',
    decision_attestation_type VARCHAR(40) NULL
        COMMENT 'Prototype record only; not a notarial act or qualified digital signature',
    submitted_at DATETIME(6) NULL,
    decided_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    version INT UNSIGNED NOT NULL DEFAULT 0
        COMMENT 'Optimistic-lock version for concurrent review updates',
    PRIMARY KEY (case_id),
    UNIQUE KEY uq_legal_verification_activation (activation_id),
    KEY ix_legal_verification_queue (status, submitted_at),
    KEY ix_legal_verification_executor (executor_user_id, status),
    KEY ix_legal_verification_reviewer (reviewer_user_id, status),
    CONSTRAINT chk_legal_verification_case_status CHECK
        (status IN ('DRAFT', 'SUBMITTED', 'IN_REVIEW', 'NEEDS_INFORMATION', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_legal_verification_attestation CHECK
        (decision_attestation_type IS NULL OR decision_attestation_type = 'IN_APP_REVIEW_ACKNOWLEDGEMENT')
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS legal_verification_documents (
    document_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    case_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    document_type VARCHAR(40) NOT NULL,
    storage_key VARCHAR(512) NOT NULL
        COMMENT 'Private object-storage key; never expose this value to clients',
    display_filename VARCHAR(255) NOT NULL
        COMMENT 'Sanitized display name; do not use as a filesystem path',
    media_type VARCHAR(127) NOT NULL,
    file_size_bytes BIGINT UNSIGNED NOT NULL,
    sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Integrity check only; does not establish document authenticity',
    uploaded_by_user_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    uploaded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (document_id),
    UNIQUE KEY uq_legal_verification_document_storage_key (storage_key),
    KEY ix_legal_verification_documents_case (case_id, uploaded_at),
    CONSTRAINT chk_legal_verification_document_type CHECK
        (document_type IN ('DEATH_CERTIFICATE', 'WILL', 'INHERITANCE_EVIDENCE', 'REPRESENTATIVE_AUTHORITY', 'IDENTITY_DOCUMENT', 'OTHER')),
    CONSTRAINT fk_legal_verification_documents_case
        FOREIGN KEY (case_id)
        REFERENCES legal_verification_cases (case_id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS legal_verification_events (
    event_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    case_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    actor_user_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    from_status VARCHAR(24) NULL,
    to_status VARCHAR(24) NULL,
    reason TEXT NULL,
    metadata JSON NULL
        COMMENT 'Non-sensitive event metadata only; never store document bytes or secrets',
    occurred_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (event_id),
    KEY ix_legal_verification_events_case (case_id, occurred_at),
    CONSTRAINT chk_legal_verification_event_type CHECK
        (event_type IN ('CASE_CREATED', 'DOCUMENT_UPLOADED', 'CASE_SUBMITTED', 'REVIEW_STARTED', 'INFORMATION_REQUESTED', 'CASE_RESUBMITTED', 'CASE_APPROVED', 'CASE_REJECTED')),
    CONSTRAINT chk_legal_verification_event_from_status CHECK
        (from_status IS NULL OR from_status IN ('DRAFT', 'SUBMITTED', 'IN_REVIEW', 'NEEDS_INFORMATION', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_legal_verification_event_to_status CHECK
        (to_status IS NULL OR to_status IN ('DRAFT', 'SUBMITTED', 'IN_REVIEW', 'NEEDS_INFORMATION', 'APPROVED', 'REJECTED')),
    CONSTRAINT fk_legal_verification_events_case
        FOREIGN KEY (case_id)
        REFERENCES legal_verification_cases (case_id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

-- Application transaction requirements:
-- 1. Validate every state transition in the service layer and update version atomically.
-- 2. Save the case status change and corresponding event in the same database transaction.
-- 3. Never hard-delete a case or event through the Flow 5 API.
-- 4. Upload/store the file privately, then persist metadata; clean up an orphaned object if metadata persistence fails.
-- 5. Add foreign keys for activation_id and user IDs only after matching the existing schema.
