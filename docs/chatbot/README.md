# LegacyVault chatbot backend

The chatbot uses OpenAI's Responses API from the Spring Boot backend. The API key is never sent to the browser. Keep it in the ignored local config file; do not commit that file.

## Configure locally

For the easiest local setup, copy `application-local.example.yml` to `application-local.yml` in the backend project root and replace the placeholder with your API key. The local file is automatically loaded when Spring Boot starts and is ignored by Git.

Alternatively, set the key in the PowerShell window used to start the backend:

```powershell
$env:OPENAI_API_KEY = "your OpenAI API key"
.\mvnw.cmd spring-boot:run
```

If Maven is installed instead of using the wrapper, run `mvn spring-boot:run`. Without `OPENAI_API_KEY`, the app still starts; the message endpoint returns `503 AI_NOT_CONFIGURED` and makes no provider request. The local FAQ suggestions endpoint works without a key.

Optional settings are `OPENAI_BASE_URL`, `OPENAI_MODEL`, and `OPENAI_TIMEOUT`. Defaults are the OpenAI API, `gpt-6-luna`, and 20 seconds. The response is capped at 700 output tokens, and message requests are limited to 20 per authenticated user per minute.

## API behavior

- `POST /api/v1/chatbot/suggestions` searches a curated local English/Vietnamese FAQ list; it never calls the AI provider.
- `POST /api/v1/chatbot/message` uses the authenticated JWT role, the current message, and up to eight recent turns. The role is derived from the JWT, not accepted from request JSON.
- Chat messages are not written to the database. The request sets `store: false`; the question and selected history are still transmitted to OpenAI, so provider data-handling terms apply.
- No web search or tools are enabled. Answers are grounded by `src/main/resources/chatbot/product-guidance.md` and the system instructions.
- Do not send passwords, OTPs, identity numbers, payment-card information, private vault contents, or face data.

## Verification

Run `mvn -Dtest=OpenAiResponsesClientTest test` to verify request construction and safe no-key behavior without contacting OpenAI. A real provider call is intentionally not part of automated tests.
