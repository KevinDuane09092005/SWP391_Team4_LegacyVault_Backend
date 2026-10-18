# LegacyVault Flow 5 — Legal Verification

Flow 5 is integrated into the backend application. It records an executor's evidence submission and a Legal Verifier's platform review. Approval is only a review result: it does not decrypt assets or grant access. Flow 6 must independently verify identity, assignment, scope, and release conditions.

## Integration with the existing backend

- A case references an existing `handover_cases` row and can be created only after its Dead Man's Switch is `triggered`.
- Each case is scoped to one existing asset assignment and one beneficiary contact linked to that assignment. The backend does not currently have a collection entity.
- The authenticated executor must have an account whose email matches the executor contact on that assignment. The request cannot supply an executor user ID.
- A reviewer must have the `LEGAL_VERIFIER` role in the authenticated JWT and cannot review their own case. The existing signup flow creates `VAULT_OWNER` users; the reviewer role must be assigned by an administrator in the database for the prototype.
- Uploaded bytes are encrypted with AES-GCM and kept under `var/private/legal-verification`, outside the public resource directory. Set `FLOW5_DOCUMENT_STORAGE_ENCRYPTION_KEY` to a persistent secret of at least 32 characters before uploading. Keep the same key to read existing files; never commit it.
- The API follows the existing `/api/v1` route convention and JWT filter.

## Run and verify

From the repository root:

```powershell
.\mvnw.cmd test
```

The tests do not need MySQL. Running the application uses the existing MySQL configuration in `src/main/resources/application.yml`; the database must be available. Before testing document upload, set the storage-encryption environment variable in the same PowerShell session used to run the app.

To enable the reviewer role for a prototype account, an administrator can set the existing user's role to `LEGAL_VERIFIER` in MySQL, then have that user sign in again to receive a JWT containing the updated role. Do not add a public role-selection endpoint.

## Files

- `legacyvault-flow5-openapi.yaml` — API contract at `/api/v1/legal-verification-cases`.
- `flow5-legal-verification.sql` — intended MySQL schema, including references to existing handover, assignment, contact, and user records. The running app currently uses Hibernate schema update.
- `flow5-legal-verification.puml` — sequence diagram for case creation, evidence upload, review, and outcomes.
- `legal-review.md` — legal-design guardrails and official sources; not legal advice.

## Remaining prototype limitations

- Notifications are not sent for Flow 5 updates; the executor sees changes by refreshing the case.
- The verifier role has no UI for provisioning yet.
- No production retention/deletion policy or external encrypted object-storage service has been connected. Local encrypted disk storage is for development/demo only.
- The 10 MB upload limit and accepted PDF/JPEG/PNG types are prototype settings, not legal requirements. File signatures are checked, but that does not establish legal authenticity.
