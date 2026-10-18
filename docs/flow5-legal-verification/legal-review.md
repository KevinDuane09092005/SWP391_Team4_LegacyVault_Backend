# Flow 5 legal review notes

Reviewed against official Vietnamese legal sources on 13 October 2026. This is a software-design review, not legal advice. The team should obtain qualified Vietnamese legal review before treating the workflow as a real-world legal verification service.

## What the supplied legal note gets right

- A beneficiary assignment inside LegacyVault should not be treated by the application as proof that the named person has an enforceable inheritance right.
- Uploading a death certificate must not automatically unlock a vault.
- The verifier should record a decision and its reason, and the system should retain an attributable history of case actions.
- Documents and review actions need server-side access control and careful protection because they can contain personal data.

These are strong product-safety requirements. The exact workflow, evidence checklist, and audit fields are design decisions; the cited laws do not prescribe this particular LegacyVault database or API.

## Important refinement: notarization is not required for every will

The Civil Code recognizes several written-will forms, including wills without witnesses and wills with or without notarization/certification. Article 630(4) expressly says a written will without notarization or certification may still be valid if it meets the requirements in Article 630(1). Therefore:

- Do not make a notarized will a universal upload requirement.
- Let a reviewer record that the submitted documents were reviewed, without claiming that the app has conclusively determined inheritance rights or will validity.
- If evidence is unclear, contested, or outside the reviewer's authority, allow “request more information” or “reject/escalate”; do not auto-approve.

The Law on Notarization 46/2024/QH15 is in force from 1 July 2025, but that does not make notarization mandatory for every will. The applicable form depends on the Civil Code and the facts of the case.

## Electronic approval and signature wording

The Law on Electronic Transactions 20/2023/QH15 has been in force since 1 July 2024. It recognizes data messages and electronic signatures, but Article 1(2) says the law does not itself determine the content, conditions, or form of a transaction. Article 9(2) also says an electronic data message does not replace a legally required notarized/certified document unless it is notarized/certified under the applicable law.

For the student prototype, call the action an “in-app review acknowledgment” or “review decision,” and retain the authenticated reviewer, timestamp, outcome, and reason. Do not label an ordinary button click a “digital signature,” “notarization,” or legally conclusive certification. If the project later adds a real signature provider, define and validate that integration separately.

## Proposed Flow 5 guardrails

1. The executor may submit only for an activation that exists and is assigned to that authenticated executor. Resolve the owner and activation from trusted server-side records, not request-supplied owner IDs.
2. A reviewer must have the Legal Verifier role and must not be the executor on the same case. The role is an application permission, not proof of statutory authority.
3. Require a death-certificate document before submission. Will, inheritance, identity, or representative-authority documents may be added as applicable; do not require a will in every case.
4. The reviewer manually records a platform review outcome. File presence, a checksum, OCR, or an in-app button does not prove a document is authentic or establish inheritance rights.
5. “Approved” means the platform's Flow 5 review step passed. It only emits an approval outcome to Flow 6. Flow 6 must separately check beneficiary identity, asset-level assignment, and release conditions before any decryption or access.
6. Record who acted, when, the prior/new status, and the reason. Keep uploaded bytes out of the audit event. A SHA-256 digest can detect a changed stored file; it does not prove legal authenticity.
7. Store documents in private encrypted storage, return content only through an authorization-checked endpoint, and avoid public or permanent download URLs. Minimize returned personal data.
8. Define a retention/deletion policy and data-subject request process before production. The project files do not specify one, and this draft does not invent a retention period.

## Current-law references checked

| Source | What was checked | Design relevance |
| --- | --- | --- |
| [Civil Code 91/2015/QH13, National Legal Database](https://vbpl.vn/bothongtin/Pages/vbpq-toanvan.aspx?ItemID=95942&Keyword=) | The database lists the Code as in force. Articles 627–630 describe will forms and validity; Article 630(4) allows a written will without notarization or certification to be valid if Article 630(1) is satisfied. | Do not require notarization for every will or make the app the final judge of will validity. |
| [Law on Notarization 46/2024/QH15, Government legal documents portal](https://vanban.chinhphu.vn/?classid=1&docid=212474&orggroupid=1&pageid=27160) | Effective 1 July 2025. | Keep platform review distinct from an act of notarization. |
| [Law on Electronic Transactions 20/2023/QH15, National Legal Database](https://vbpl.vn/botuphap/Pages/vbpq-toanvan.aspx?ItemID=165913) | Effective 1 July 2024. Articles 1(2), 8, 9, and 22 are relevant to the limits of electronic records and signatures. | Treat the prototype's click-to-approve as an internal acknowledgment unless an appropriate signature workflow is implemented. |
| [Personal Data Protection Law 91/2025/QH15, Government legal documents portal](https://vanban.chinhphu.vn/?classid=1&docid=214590&pageid=27160&typegroup=) | Effective 1 January 2026. | Minimize personal data and keep access, storage, and retention decisions explicit. |
| [Decree 356/2025/NĐ-CP, Government legal documents portal](https://vanban.chinhphu.vn/?docid=216387&pageid=27160&typegroupid=4) | Effective 1 January 2026; implements parts of the Personal Data Protection Law. | Apply limited access and security controls to personal-data processing; confirm detailed obligations for any production deployment. |
| [Cybersecurity Law 116/2025/QH15, Government legal documents portal](https://vanban.chinhphu.vn/?classid=1&docid=216499&orggroupid=1&pageid=27160) | Effective 1 July 2026; the official legal database indicates it is partially in force. | The supplied Flow 7 note cites a current law, but a full cybersecurity compliance assessment is outside this Flow 5 review. |

## Scope limitation

This review assumes the team is designing a Vietnam-focused coursework prototype. Cross-border estates, foreign assets, disputes, statutory representative authority, and production data-retention requirements need separate legal analysis. The system should not claim that a platform reviewer replaces a court, notary, competent authority, or legal counsel.
