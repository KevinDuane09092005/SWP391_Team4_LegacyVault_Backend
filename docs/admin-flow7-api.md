# Flow 7 — Admin backend API

All `/api/v1/admin/**` routes require a valid JWT whose current database role is `ADMIN`. User role changes take effect immediately for existing JWTs; suspended accounts are rejected by the authentication filter and cannot sign in again until reactivated.

## Dashboard and access-request review

| Method | Route | Behavior |
| --- | --- | --- |
| `GET` | `/api/v1/admin/dashboard` | Returns database counts, up to five newest users, up to five oldest pending access requests, and user counts for each known role. No sample rows or synthetic metrics are returned. |
| `POST` | `/api/v1/admin/access-requests/{requestId}/approve` | Transitions a `PENDING` request to `APPROVED` and writes an audit event. |
| `POST` | `/api/v1/admin/access-requests/{requestId}/reject` | Transitions a `PENDING` request to `REJECTED` and writes an audit event. |

Reviewing a missing request returns `404`; reviewing anything other than `PENDING` returns `409`. Approval only records the access-request decision; it does not independently grant vault or asset access.

Dashboard `*_change` and urgency text fields return `—` because there is no historical snapshot table to calculate those values. The `notifications_sent` count is currently the number of notification rows stored in the database, not confirmed external email/SMS delivery. `system_settings_modules` is empty because the listed controls do not yet operate real key rotation, backup, or API configuration.

## User management

| Method | Route | Behavior |
| --- | --- | --- |
| `GET` | `/api/v1/admin/users?search=&role=ALL&status=ALL&page=0&size=20` | Lists users with optional name/email search and role/status filters. `size` is limited to 100. |
| `PATCH` | `/api/v1/admin/users/{userId}/role` | Body: `{"role":"LEGAL_VERIFIER"}`. Allowed roles: `ADMIN`, `VAULT_OWNER`, `EXECUTOR`, `BENEFICIARY`, `LEGAL_VERIFIER`. |
| `PATCH` | `/api/v1/admin/users/{userId}/status` | Body: `{"status":"SUSPENDED"}` or `{"status":"ACTIVE"}`. |

Administrators cannot change their own role/status, and the last administrator cannot be demoted. Updates are persisted, audited, and send an in-app notification to the affected user. Unknown users return `404`; invalid changes return `400` or `409`.

The `users.is_active` column is added by the current Hibernate schema-update setting. A null value on an existing row is treated as active. For a managed deployment, replace schema auto-update with an explicit migration before applying this code.

## Audit log

| Method | Route | Behavior |
| --- | --- | --- |
| `GET` | `/api/v1/admin/audit-logs?search=&role=ALL&action=ALL&from_date=YYYY-MM-DD&to_date=YYYY-MM-DD&page=0&size=6` | Searches persisted audit rows. Page size is limited to 200; invalid dates and ranges return `400`. |
| `GET` | `/api/v1/admin/audit-logs/export-csv?from_date=YYYY-MM-DD&to_date=YYYY-MM-DD` | Exports matching database rows as CSV with quoted/escaped cell values. |

An empty database returns zero rows and zero counts; the API no longer displays example people, events, or counts.

## Intentionally not exposed as live controls

The current FE mock implements role-permission editing, encryption/backup policy editing, integration toggles, and demo reset in browser-local storage. These do not map to real backend enforcement or connected services yet. The backend does not claim to change encryption algorithms, rotate keys, run backups, or enable third-party providers merely by saving a UI setting.
