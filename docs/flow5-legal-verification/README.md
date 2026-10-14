# LegacyVault Flow 5: Legal Verification

Status: implementation scaffold added to the local backend repository; authentication, Flow 4 activation lookup, and private document storage remain unconnected.

## Flow boundary

The Digital Executor / Trustee submits a legal-verification case for a Dead Man's Switch activation. A Legal Verifier reviews the submitted evidence and either approves it, rejects it, or requests more information. An approval is an input to Flow 6; Flow 5 itself does not decrypt assets or grant a beneficiary access.

The course materials identify the executor's death-certificate request and the verifier's approval/rejection as the core behavior. Signature/document validation and case history are described as optional features. This draft includes the history needed to explain decisions, but treats any in-app confirmation as an internal review record, not a legally qualified electronic signature or notarial act.

## Files

- legacyvault-flow5-openapi.yaml — OpenAPI 3.0.3 contract, using the /api base path and bearer authentication convention in the Flow 2 contract.
- flow5-legal-verification.sql — proposed MySQL 8.0.16+ tables. Uploaded file bytes stay in private object storage; SQL stores metadata and a storage key.
- flow5-legal-verification.puml — PlantUML sequence diagram.
- legal-review.md — checked legal claims, corrections, implementation guardrails, and official sources.

## Source alignment

- The LegacyVault actor and feature list is in FA26_SWP391.xlsx, Project!A7:G7. The Must Have and Nice to Have cells for that row are blank, so this package follows the Flow 5 assignment confirmed by the team rather than claiming the workbook itself labels Flow 5 as required.
- The supplied legal note, lưu ý luật pháp flow 5-7.docx, describes Flow 5 as evidence review before any handover and explicitly says a death-certificate upload must not automatically unlock assets.
- LegacyVault_System_References.docx maps the project flow to executor submission, verifier review, approval/rejection, and communication. It also frames that mapping as a design interpretation.
- The existing Flow 2 OpenAPI YAML supplies the /api server base path and bearer-JWT convention. No Flow 5 SQL schema or Flow 4/5 API contract was found in the checked project folders.
- The local Spring MVC service and Flyway migration now implement the contract shape and core state/authorization checks. Placeholder activation and storage adapters return 503 until replaced with real integrations. No JWT filter or user-role mapping has been invented.

## Proposed MVC layout

- Controller: LegalVerificationCaseController
- Service: LegalVerificationService
- Repositories: LegalVerificationCaseRepository, LegalVerificationDocumentRepository, LegalVerificationEventRepository
- Entities: LegalVerificationCase, LegalVerificationDocument, LegalVerificationEvent
- DTOs: create case, upload document, request information, decision, case response, and error response

Keep authorization and state transitions in the service layer. The controller must derive the acting user from the authenticated principal; clients must not choose their own executor or reviewer ID.

## Integration assumptions to confirm with the backend owner

1. Flow 4 exposes an activation identifier. This draft calls it activationId; replace it with the actual Flow 4 name and validate that the authenticated executor is assigned to that activation.
2. Public IDs are UUIDs, matching the Flow 2 OpenAPI examples. The SQL uses CHAR(36) provisionally. Match the existing entity/database ID representation before merging.
3. One legal-verification case is allowed per activation. The SQL has a unique constraint on activation_id. Confirm how a rejected case can be retried before keeping that constraint.
4. The actual users, roles, activation, audit-log, and file-storage schemas were not present in the checked project folder. External foreign keys are therefore intentionally not guessed; add them after comparing the backend schema.
5. Confirm whether “Legal Verifier” is an internal reviewer role or a verified notary integration. A product role alone must not be presented as proof of notary status.
6. Confirm upload size/type rules, document retention/deletion policy, and the signature mechanism. The 10 MB per-file limit in the API is a prototype default, not a legal requirement.

## Suggested review order

Have the backend owner confirm the Flow 4 activation ID, user ID format, and whether one case per activation is correct. Review the code and status transitions alongside the legal guardrails, then align the SQL and adapters to the real backend schema before merging. The local unit tests cover required evidence and case-access rules; they do not replace an integration test against the team's MySQL, authentication, Flow 4, and storage services.
