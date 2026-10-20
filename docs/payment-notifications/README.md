# Billing and in-app notifications (demo)

This feature adds the backend contract for the pricing and billing screens in the project tracker. Payment processing is deliberately simulated. It does not connect to a bank or charge a real card. Payment events are written to the shared in-app notification service already used by the Flow 5-7 work; this feature does not create a second notification inbox.

## Flow

1. Anyone can load the Free, Essentials, and Plus plan catalog from `GET /api/v1/plans`. Prices and limits match the current frontend mock data.
2. A signed-in Vault Owner saves, edits, selects, lists, or removes a payment method. The backend receives only a mock payment-provider token and masked card metadata. It has no card-number or CVV fields.
3. The Vault Owner checks out a plan after seeing the billing cadence and next renewal date. Paid checkout requires explicit consent to recurring billing. The backend chooses the price from its own catalog; it never trusts a client-supplied amount.
4. In this demo, a mock token ending in `_declined` produces a failed transaction. Other `pm_mock_` tokens produce a successful transaction. A failed initial checkout leaves the current plan unchanged and creates an in-app notification.
5. A successful paid checkout starts an account-level monthly or yearly subscription with auto-renew enabled. The scheduled demo worker attempts renewal at the period end using the owner's default saved method.
6. A failed renewal moves the subscription to `PAST_DUE` for a seven-day grace period. The owner can replace the default method and manually retry. If payment is not recovered before grace ends, the subscription becomes `EXPIRED`; the demo does not delete assets or block the handover APIs.
7. Canceling turns off future auto-renewal while leaving the paid period active through its end date. The owner can view the current status and payment history.
8. Any signed-in user can read only their own notification inbox, open a notification, mark one read, or mark all read.

## Important payment boundary

The mock token is not a real payment token. Do not send a real card number or CVV to this API, even for a prototype. The payment-method response exposes only the brand, last four digits, expiry, billing details, and default status. The saved provider token is server-side only and is never returned to the client.

Before a real payment provider is selected, do not enable live charges. A future frontend should use the provider's hosted fields or SDK to tokenize card details, then replace `MockPaymentGateway` with a provider adapter. Production billing also needs provider-side idempotency, signed webhook handling, reconciliation, a robust retry policy, and secret management. The demo renewal worker is not production-grade.

PCI Security Standards Council guidance says card verification codes must not be retained after authorization, even encrypted: [FAQ 1280](https://www.pcisecuritystandards.org/faqs/1280/).

## API and database

- `legacyvault-payment-notifications-openapi.yaml` — API contract.
- `payment-notifications.sql` — intended MySQL schema. The application currently uses Hibernate schema update.
- `payment-notifications.puml` — sequence diagram.
- `GET /api/v1/plans` is public. Billing endpoints require an authenticated Vault Owner. Payment notifications use the shared authenticated notification endpoints from the Flow 5-7 work.
- `GET /api/v1/plans` matches the current frontend `mockPlans` response shape. The frontend can later replace `fetchPlans()` with this endpoint.
- Notification delivery is in-app only through the shared `in_app_notifications` table. Email, SMS, push, and real provider callbacks are not connected.
- The recurring lifecycle is simulated against `MockPaymentGateway`; no real money is charged. The demo worker scans every minute, makes one renewal attempt, and does not automatically retry a failure. Billing status is not used to gate asset access or legal handover.

## Local checks

From the repository root:

```powershell
.\mvnw.cmd test
```

The unit tests do not require MySQL. Live API checks require the existing MySQL database configuration. This branch does not include frontend changes or a payment-provider account.
