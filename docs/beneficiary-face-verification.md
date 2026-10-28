# Beneficiary face enrollment and Flow 6 check

This is a local university-project prototype using the same `@vladmandic/human` browser library as the camera demo. It does not call AWS or another paid face API.

## Intended flow

1. The beneficiary signs into a handover case and verifies its one-time code. The resulting JWT must have the `BENEFICIARY` role.
2. The frontend checks `GET /api/v1/handover/beneficiary/face/enrollment`. If no reference exists, it asks for consent, captures a live camera frame locally, extracts the Human face embedding, and sends only that embedding to `PUT /api/v1/handover/beneficiary/face/enrollment`.
3. Before access to a case's inherited assets, the frontend starts a fresh scan with `POST /api/v1/handover/beneficiary/face/cases/{caseId}/sessions`. The response supplies a one-time session ID and a randomized `left` or `right` head-turn prompt.
4. The browser performs its head-turn/blink and liveness checks, then posts `challengeCompleted`, browser liveness/anti-spoof scores, and the current embedding to `POST /api/v1/handover/beneficiary/face/cases/{caseId}/sessions/{sessionId}/complete`.
5. Spring compares the new embedding against the stored reference and updates only that beneficiary's case claim status to `VERIFIED` or `FAILED`. Existing Flow 6 asset/detail/acknowledgement methods reject access unless the selected active, OTP-verified claim has `kycFacialStatus=VERIFIED`. Run a fresh scan for another case; sessions are short-lived, single-use, and tied to the case and beneficiary.

The `{caseId}` route accepts either the handover case UUID or its case number, such as `LV-1042`. Flow 6 asset and receipt endpoints also accept `?case_ref=...`; pass it whenever a beneficiary can have multiple active cases. Omitting it is supported only when there is exactly one OTP-verified active case.

## API examples

All face endpoints require `Authorization: Bearer <beneficiary JWT>`.

Enrollment status:

```http
GET /api/v1/handover/beneficiary/face/enrollment
```

Enroll or replace the reference after explicit consent (schematic body; replace the placeholder with the full numeric array from Human):

```text
PUT /api/v1/handover/beneficiary/face/enrollment
{
  "consent": true,
  "embedding": HUMAN_EMBEDDING_ARRAY
}
```

Start a case-bound verification session:

```http
POST /api/v1/handover/beneficiary/face/cases/LV-1042/sessions
```

Complete it using the same case and session ID:

```text
POST /api/v1/handover/beneficiary/face/cases/LV-1042/sessions/{sessionId}/complete
{
  "challengeCompleted": true,
  "livenessScore": 0.96,
  "antiSpoofScore": 0.71,
  "embedding": CURRENT_HUMAN_EMBEDDING_ARRAY
}
```

`DELETE /api/v1/handover/beneficiary/face/enrollment` removes the saved reference and resets active claims to `PENDING_SCAN`.

The same case reference must accompany Flow 6 status/asset/receipt calls, for example `GET /api/v1/handover/beneficiary/kyc-status?case_ref=LV-1042`, `GET /api/v1/handover/beneficiary/authorized-assets?case_ref=LV-1042`, and `GET /api/v1/handover/beneficiary/assets/ASSET-1042-A/detail?case_ref=LV-1042`.

The `embedding` arrays above are abbreviated examples: send the full numeric descriptor from Human, not strings or the camera image itself. Current validation accepts 64–4096 finite values.

## Local setup

The backend requires a random 32-byte AES key, Base64-encoded, to save or read a face reference. In PowerShell, generate one for the current terminal session and start Spring Boot from that same terminal:

```powershell
$faceKeyBytes = New-Object byte[] 32
$faceKeyGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$faceKeyGenerator.GetBytes($faceKeyBytes)
$env:FACE_ENCRYPTION_KEY = [Convert]::ToBase64String($faceKeyBytes)
$faceKeyGenerator.Dispose()
.\mvnw.cmd spring-boot:run
```

Keep this key out of Git and do not share it. If the key is lost or changed, existing embeddings cannot be decrypted and beneficiaries must enroll again. The `beneficiary_face_enrollments` table is created by the project's current Hibernate schema-update configuration.

## Data and prototype limits

- The database stores one AES-GCM-encrypted embedding per beneficiary, its model version, and consent/creation timestamps. It does not store a photo, video, camera frame, or per-attempt score/history.
- The claim stores only the existing facial verification status (`PENDING_SCAN`, `VERIFIED`, or `FAILED`) needed by the access gate. A verification session's prompt and expiry live in backend memory for at most 10 minutes and disappear on restart.
- Liveness and anti-spoof scores and challenge completion are calculated in the browser and sent to the API. A modified client can forge these values. Therefore this prototype demonstrates the camera flow and embedding comparison, but it is not a trusted identity-proofing or production security control. The API response flags this as demo-only.
- The face check is an extra gate, not the final legal approval or release decision. Existing case authorization and Flow 6 rules still apply.
