# Local backend configuration

Set secrets through environment variables; do not put credentials in `application.yml`, source code, or Git.

Required for normal MySQL use:

- `SPRING_DATASOURCE_URL` (defaults to local `legacy_vault_db`)
- `SPRING_DATASOURCE_USERNAME` (defaults to `root`)
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET` (at least 32 bytes)
- `ENCRYPTION_SECRET_KEY`

To create the first admin account once, set `ADMIN_BOOTSTRAP_ENABLED=true`, `ADMIN_BOOTSTRAP_EMAIL`, and `ADMIN_BOOTSTRAP_PASSWORD` (at least 16 characters). Optionally set `ADMIN_BOOTSTRAP_FULL_NAME`. After the first successful start, turn the bootstrap flag off and remove the bootstrap password from the environment. If the email already belongs to an account, the bootstrap does not elevate or overwrite it.

Required if you want real OTP/notification email delivery (otherwise email is not delivered):

- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`

Optional for AWS face verification:

- `FACE_VERIFICATION_AWS_ENABLED=true`
- `FACE_VERIFICATION_AWS_REGION=ap-southeast-7` or `ap-southeast-5`
- `FACE_ENCRYPTION_KEY` (Base64-encoded random 32-byte AES key)

If `JWT_SECRET` is not configured, token signing fails instead of using a known default. If `ENCRYPTION_SECRET_KEY` is not configured, asset encryption/decryption fails instead of using a repository-bundled key. AWS face verification is disabled by default.

The mail credential previously present in tracked configuration must be rotated at the mail provider. Removing it from the current file does not erase it from existing Git history; treat it as compromised if that history was shared or pushed.
