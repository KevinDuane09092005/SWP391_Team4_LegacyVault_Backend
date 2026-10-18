-- LegacyVault Flow 5: Legal Verification
-- MySQL 8.0.16+. The application currently uses Hibernate ddl-auto=update;
-- this file documents the intended schema and may be used for manual setup.
-- Uploaded document bytes are stored encrypted outside MySQL and outside the web root.

CREATE TABLE IF NOT EXISTS legal_verification_cases (
    case_id CHAR(36) NOT NULL,
    handover_case_id VARCHAR(36) NOT NULL
        COMMENT 'Existing handover_cases.case_id created when the DMS is triggered',
    assignment_id CHAR(36) NOT NULL
        COMMENT 'Existing asset_assignments.assignment_id; one asset assignment per review case',
    beneficiary_contact_id CHAR(36) NOT NULL
        COMMENT 'Existing contacts.contact_id, verified against assignment_beneficiaries',
    executor_user_id CHAR(36) NOT NULL
        COMMENT 'Authenticated account matched to the assigned executor contact by email',
    reviewer_user_id CHAR(36) NULL
        COMMENT 'Authenticated user with the LEGAL_VERIFIER role',
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    decision_reason TEXT NULL
        COMMENT 'Required for final approval or rejection; platform review rationale only',
    decision_attestation_type VARCHAR(40) NULL
        COMMENT 'In-app acknowledgement only; not a notarial act or qualified signature',
    submitted_at DATETIME(6) NULL,
    decided_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    version INT UNSIGNED NOT NULL DEFAULT 0
        COMMENT 'Optimistic-lock version for concurrent review updates',
    PRIMARY KEY (case_id),
    UNIQUE KEY uq_legal_verification_target (handover_case_id, assignment_id, beneficiary_contact_id),
    KEY ix_legal_verification_queue (status, submitted_at),
    KEY ix_legal_verification_executor (executor_user_id, status),
    KEY ix_legal_verification_reviewer (reviewer_user_id, status),
    CONSTRAINT chk_legal_verification_case_status CHECK
        (status IN ('DRAFT', 'SUBMITTED', 'IN_REVIEW', 'NEEDS_INFORMATION', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_legal_verification_attestation CHECK
        (decision_attestation_type IS NULL OR decision_attestation_type = 'IN_APP_REVIEW_ACKNOWLEDGEMENT'),
    CONSTRAINT fk_legal_verification_handover_case
        FOREIGN KEY (handover_case_id) REFERENCES handover_cases (case_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_legal_verification_assignment
        FOREIGN KEY (assignment_id) REFERENCES asset_assignments (assignment_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_legal_verification_beneficiary
        FOREIGN KEY (beneficiary_contact_id) REFERENCES contacts (contact_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_legal_verification_executor
        FOREIGN KEY (executor_user_id) REFERENCES users (user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_legal_verification_reviewer
        FOREIGN KEY (reviewer_user_id) REFERENCES users (user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS legal_verification_documents (
    document_id CHAR(36) NOT NULL,
    case_id CHAR(36) NOT NULL,
    document_type VARCHAR(40) NOT NULL,
    storage_key VARCHAR(512) NOT NULL
        COMMENT 'Private encrypted local storage key; never expose to clients',
    display_filename VARCHAR(255) NOT NULL
        COMMENT 'Sanitized display name; never use as a filesystem path',
    media_type VARCHAR(127) NOT NULL,
    file_size_bytes BIGINT UNSIGNED NOT NULL,
    sha256 CHAR(64) NOT NULL
        COMMENT 'Integrity check only; does not establish legal authenticity',
    uploaded_by_user_id CHAR(36) NOT NULL,
    uploaded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (document_id),
    UNIQUE KEY uq_legal_verification_document_storage_key (storage_key),
    KEY ix_legal_verification_documents_case (case_id, uploaded_at),
    CONSTRAINT chk_legal_verification_document_type CHECK
        (document_type IN ('DEATH_CERTIFICATE', 'WILL', 'INHERITANCE_EVIDENCE', 'REPRESENTATIVE_AUTHORITY', 'IDENTITY_DOCUMENT', 'OTHER')),
    CONSTRAINT fk_legal_verification_documents_case
        FOREIGN KEY (case_id) REFERENCES legal_verification_cases (case_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_legal_verification_documents_uploader
        FOREIGN KEY (uploaded_by_user_id) REFERENCES users (user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS legal_verification_events (
    event_id CHAR(36) NOT NULL,
    case_id CHAR(36) NOT NULL,
    actor_user_id CHAR(36) NOT NULL,
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
        FOREIGN KEY (case_id) REFERENCES legal_verification_cases (case_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_legal_verification_events_actor
        FOREIGN KEY (actor_user_id) REFERENCES users (user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Flow rules enforced by the application:
-- 1. Require a triggered DMS handover and verify executor email against the executor contact on the assignment.
-- 2. Verify the beneficiary contact belongs to the same owner and is linked to the selected assignment.
-- 3. Save status transitions and their audit events in one transaction.
-- 4. Require a death-certificate upload before submission; other evidence is case-specific and optional.
-- 5. Never hard-delete a case, document record, or event through the API.
-- 6. Approval is only a Flow 5 review result; Flow 6 separately checks identity, assignment, scope, and release rules.
