# LegacyVault Backend — Flow 5 Legal Verification

Spring Boot MVC starting point for Flow 5. This local implementation is a scaffold awaiting integration with the team's authentication, Flow 4 activation, and private document-storage services.

## Check the code

Requirements: Java 25 and Maven 3.9+.

From this folder, run:

```powershell
mvn test
```

The unit tests do not need MySQL. A successful run ends with `BUILD SUCCESS` and reports the tests run.

## Run the API locally

Install/start MySQL 8.0.16+ and create a database named `legacyvault`, then set credentials in PowerShell:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/legacyvault'
$env:DB_USERNAME = 'your-mysql-user'
$env:DB_PASSWORD = 'your-mysql-password'
mvn spring-boot:run
```

Flyway applies the Flow 5 migration at startup; Hibernate validates the resulting schema. The API listens at `http://localhost:8080`. The OpenAPI contract is in `docs/flow5-legal-verification/legacyvault-flow5-openapi.yaml`.

The API is not end-to-end usable yet: connect the team's JWT/security configuration so the servlet request has a trusted principal and the `DIGITAL_EXECUTOR` / `LEGAL_VERIFIER` roles; implement `ActivationGateway` against Flow 4; and implement `DocumentStorage` with private encrypted storage. The placeholder integrations deliberately return HTTP 503 instead of pretending an activation check or secure file storage succeeded. Do not expose the document endpoints until those integrations and authorization are in place.

## Flow 5 files

- `docs/flow5-legal-verification/flow5-legal-verification.puml` — workflow sequence diagram; integrations not implemented in this scaffold are marked as pending.
- `docs/flow5-legal-verification/flow5-legal-verification.sql` — proposed MySQL schema; review against the team's existing schema before merge.
- `docs/flow5-legal-verification/legacyvault-flow5-openapi.yaml` — REST API contract.
- `docs/flow5-legal-verification/legal-review.md` — legal guardrails and cited sources; not legal advice.
- `src/main/resources/db/migration/V1__create_flow5_legal_verification.sql` — Flyway migration copied from the proposed schema.

## Decisions to confirm before merging

- The schema currently allows one verification case per activation; confirm whether a rejected case can be retried.
- Confirm actual ID formats, Flow 4 activation assignment rules, JWT role names, storage/retention policy, and MySQL version against the team's backend.
- Flow 5 records a platform review decision only. Approval does not unlock assets; Flow 6 must independently check release rules.
