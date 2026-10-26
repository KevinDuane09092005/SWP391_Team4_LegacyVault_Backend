# LegacyVault local face verification prototype

This small React page exercises a local browser-to-Spring-Boot flow:

1. The browser opens the camera and runs the Human face models locally.
2. After consent, it sends a face embedding to Spring Boot as the temporary reference.
3. The browser performs a randomized head-turn and blink challenge, then sends five-frame liveness/anti-spoof averages and the current embedding.
4. Spring Boot compares the embeddings, applies the demo thresholds, and returns the final pass/fail result.

The demo backend keeps templates in process memory for up to 15 minutes. It does not save camera images or videos, and does not use AWS or MySQL. Restarting the backend clears all demo sessions.

## Run both parts

From the repository root, start the backend in its isolated profile:

    .\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=face-demo'

Then, in a second terminal:

    cd face-verification-demo
    npm run dev

Open http://127.0.0.1:5173/, consent, start the camera, enroll your own face, and run the liveness check. To test a non-match, leave the same reference enrolled and let another consenting person complete the check. Use **Clear and stop** to delete the temporary backend session.

The first browser run needs an internet connection to fetch the face model files. The Vite dev server proxies only this demo's API route to http://127.0.0.1:8081.

## Demo API

- POST /api/v1/demo/face/sessions — create a short-lived in-memory test session.
- POST /api/v1/demo/face/sessions/{sessionId}/enrollment — save the consented face embedding in backend memory.
- POST /api/v1/demo/face/sessions/{sessionId}/verification — evaluate browser-reported challenge/liveness signals and compare the new embedding on the backend.
- DELETE /api/v1/demo/face/sessions/{sessionId} — clear the session and face reference.

## Important limitations

- The Spring face-demo profile uses an in-memory H2 database for the rest of the application and binds the backend to 127.0.0.1.
- This is an unauthenticated local demo API, not a deployment configuration.
- The browser sends liveness and anti-spoof scores to the backend. A modified client can forge them; the backend comparison is real, but this does not provide server-trusted liveness.
- Do not use real beneficiaries' biometric data. A face embedding is sensitive biometric information even though it is not a photograph.
- A passing result does not prove a legal identity, authorize a handover, or release assets.
- This demo does not call or modify the separate AWS-backed beneficiary verification endpoints.
