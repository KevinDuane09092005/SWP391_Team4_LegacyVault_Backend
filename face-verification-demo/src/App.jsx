import { useEffect, useRef, useState } from 'react';

const API = '/api/v1/demo/face';
const MODEL_BASE_PATH = 'https://vladmandic.github.io/human-models/models/';
const SCORE_SAMPLES = 5;
const SCAN_TIMEOUT_MS = 45_000;

const humanConfig = {
  backend: 'webgl',
  modelBasePath: MODEL_BASE_PATH,
  filter: { enabled: true, flip: false },
  face: {
    enabled: true,
    detector: { rotation: true, return: true, mask: false, minConfidence: 0.6 },
    description: { enabled: true },
    iris: { enabled: true },
    emotion: { enabled: false },
    antispoof: { enabled: true },
    liveness: { enabled: true },
  },
  body: { enabled: false },
  hand: { enabled: false },
  object: { enabled: false },
  segmentation: { enabled: false },
  gesture: { enabled: true },
};

const percent = (value) => typeof value === 'number' && Number.isFinite(value)
  ? Math.round(value * 100) + '%'
  : '—';
const average = (values) => values.reduce((sum, value) => sum + value, 0) / values.length;

async function api(path, options = {}) {
  const response = await fetch(API + path, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options.headers },
  });
  if (!response.ok) {
    let message = 'Backend request failed (' + response.status + ')';
    try {
      const body = await response.json();
      message = body.detail || body.message || message;
    } catch {
      // Use the HTTP status if the backend did not return JSON.
    }
    throw new Error(message);
  }
  return response.status === 204 ? null : response.json();
}

function cameraError(error) {
  if (error?.name === 'NotAllowedError' || error?.name === 'PermissionDeniedError') {
    return 'Camera access was blocked. Allow camera access for this local page, then try again.';
  }
  if (error?.name === 'NotFoundError' || error?.name === 'DevicesNotFoundError') {
    return 'No camera was found. Connect a webcam and try again.';
  }
  if (error?.name === 'NotReadableError') {
    return 'The camera is already being used by another app. Close that app and retry.';
  }
  return error?.message || 'Could not start. Check the local backend and your internet connection.';
}

export default function App() {
  const videoRef = useRef(null);
  const streamRef = useRef(null);
  const humanRef = useRef(null);
  const sessionRef = useRef(null);
  const latestFaceRef = useRef(null);
  const challengeRef = useRef(null);

  const [consent, setConsent] = useState(false);
  const [sessionId, setSessionId] = useState(null);
  const [cameraActive, setCameraActive] = useState(false);
  const [starting, setStarting] = useState(false);
  const [busy, setBusy] = useState(false);
  const [referenceReady, setReferenceReady] = useState(false);
  const [challenge, setChallenge] = useState(null);
  const [message, setMessage] = useState('');
  const [modelStatus, setModelStatus] = useState('Face models load when you start the camera.');
  const [metrics, setMetrics] = useState({ faceCount: 0, confidence: null });
  const [result, setResult] = useState(null);

  function updateSession(id) {
    sessionRef.current = id;
    setSessionId(id);
  }

  function stopCamera() {
    challengeRef.current = null;
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
    if (videoRef.current) videoRef.current.srcObject = null;
    latestFaceRef.current = null;
    setCameraActive(false);
    setMetrics({ faceCount: 0, confidence: null });
  }

  useEffect(() => () => {
    streamRef.current?.getTracks().forEach((track) => track.stop());
    const id = sessionRef.current;
    if (id) void fetch(API + '/sessions/' + id, { method: 'DELETE', keepalive: true }).catch(() => {});
  }, []);

  async function loadModels() {
    if (humanRef.current) return humanRef.current;
    setModelStatus('Loading face match and liveness models…');
    const { default: Human } = await import('@vladmandic/human');
    const human = new Human(humanConfig);
    await human.load();
    humanRef.current = human;
    setModelStatus('Browser face models are ready.');
    return human;
  }

  async function startCamera() {
    if (!consent) {
      setMessage('Please give consent before starting this local test.');
      return;
    }
    setStarting(true);
    setMessage('Connecting to the local Spring Boot demo and preparing the camera…');
    try {
      const session = await api('/sessions', { method: 'POST', body: '{}' });
      updateSession(session.sessionId);
      await loadModels();
      if (!navigator.mediaDevices?.getUserMedia) {
        throw new Error('Camera access is unavailable. Open this page on localhost in a recent browser.');
      }
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: false,
        video: { facingMode: 'user', width: { ideal: 960 }, height: { ideal: 720 } },
      });
      streamRef.current = stream;
      videoRef.current.srcObject = stream;
      await videoRef.current.play();
      setCameraActive(true);
      setResult(null);
      setMessage('Camera is on. Center one face in the preview.');
    } catch (error) {
      stopCamera();
      const id = sessionRef.current;
      if (id) void api('/sessions/' + id, { method: 'DELETE' }).catch(() => {});
      updateSession(null);
      setMessage(cameraError(error));
      setModelStatus('Camera and backend are ready when you are.');
    } finally {
      setStarting(false);
    }
  }

  async function enrollFace() {
    const face = latestFaceRef.current;
    if (!face?.embedding?.length || metrics.faceCount !== 1) {
      setMessage('Wait until exactly one clear face is detected, then try again.');
      return;
    }
    setBusy(true);
    setMessage('Sending the face template to the local backend…');
    try {
      const response = await api('/sessions/' + sessionId + '/enrollment', {
        method: 'POST',
        body: JSON.stringify({ consent: true, embedding: face.embedding }),
      });
      setReferenceReady(response.enrolled);
      setResult(null);
      setMessage(response.message);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function sendVerification(active, face, challengeCompleted) {
    const livenessScore = active.liveSamples.length
      ? average(active.liveSamples) : (face.live || 0);
    const antiSpoofScore = active.realSamples.length
      ? average(active.realSamples) : (face.real || 0);
    try {
      const response = await api('/sessions/' + sessionId + '/verification', {
        method: 'POST',
        body: JSON.stringify({
          challengeCompleted,
          livenessScore,
          antiSpoofScore,
          embedding: face.embedding,
        }),
      });
      setResult(response);
      setMessage(response.message);
    } catch (error) {
      setResult({ status: 'ERROR', passed: false, message: error.message });
      setMessage(error.message);
    } finally {
      challengeRef.current = null;
      setChallenge(null);
      setBusy(false);
    }
  }

  function startVerification() {
    if (!sessionId || !referenceReady || !cameraActive) return;
    setResult(null);
    setBusy(true);
    const direction = Math.random() < 0.5 ? 'left' : 'right';
    const active = {
      active: true,
      step: 'turn',
      direction,
      startedAt: Date.now(),
      liveSamples: [],
      realSamples: [],
      lastFace: null,
    };
    challengeRef.current = active;
    setChallenge({ step: 'turn', direction });
    setMessage('Turn your face toward the ' + direction + ' side of the preview.');
  }

  function cancelVerification() {
    challengeRef.current = null;
    setChallenge(null);
    setBusy(false);
    setMessage('Check cancelled. The camera is still on.');
  }

  async function resetDemo() {
    const id = sessionRef.current;
    stopCamera();
    if (id) {
      try {
        await api('/sessions/' + id, { method: 'DELETE' });
      } catch {
        // Sessions expire automatically if the page is disconnected.
      }
    }
    updateSession(null);
    setReferenceReady(false);
    setChallenge(null);
    setBusy(false);
    setResult(null);
    setMessage('The demo reference was cleared. Start the camera to try again.');
  }

  useEffect(() => {
    if (!cameraActive || !humanRef.current) return undefined;
    let running = true;
    let timerId;

    async function detectLoop() {
      if (!running) return;
      const video = videoRef.current;
      if (video?.readyState >= 2) {
        try {
          const detected = await humanRef.current.detect(video);
          if (!running) return;
          const faces = detected.face || [];
          const face = faces.length === 1 ? faces[0] : null;
          const gestures = (detected.gesture || []).map((item) => item.gesture);
          latestFaceRef.current = face;
          setMetrics({ faceCount: faces.length, confidence: face?.faceScore ?? face?.boxScore ?? null });

          const active = challengeRef.current;
          if (active?.active) {
            if (Date.now() - active.startedAt > SCAN_TIMEOUT_MS) {
              active.active = false;
              if (face?.embedding?.length) {
                setMessage('Challenge timed out. Sending the incomplete check to the backend…');
                await sendVerification(active, face, false);
              } else {
                challengeRef.current = null;
                setChallenge(null);
                setBusy(false);
                setResult({ status: 'FAILED', passed: false, message: 'Challenge timed out without a clear face.' });
                setMessage('Challenge timed out without a clear face. Try again in better light.');
              }
            } else if (face && active.step === 'turn' && gestures.includes('facing ' + active.direction)) {
              active.step = 'blink';
              setChallenge({ step: 'blink', direction: active.direction });
              setMessage('Good. Now blink once.');
            } else if (face && active.step === 'blink' && gestures.some((gesture) => gesture.startsWith('blink '))) {
              active.step = 'sampling';
              setChallenge({ step: 'sampling', direction: active.direction });
              setMessage('Challenge complete. Hold still while Spring Boot checks the result.');
            } else if (face && active.step === 'sampling') {
              if (Number.isFinite(face.live) && Number.isFinite(face.real) && face.embedding?.length) {
                active.liveSamples.push(face.live);
                active.realSamples.push(face.real);
                active.lastFace = face;
              }
              if (active.liveSamples.length >= SCORE_SAMPLES) {
                active.active = false;
                await sendVerification(active, active.lastFace, true);
              }
            }
          }
        } catch (error) {
          if (running) {
            challengeRef.current = null;
            setChallenge(null);
            setBusy(false);
            setMessage('Face processing stopped: ' + (error?.message || 'unknown model error'));
          }
        }
      }
      if (running) timerId = window.setTimeout(detectLoop, 160);
    }

    void detectLoop();
    return () => {
      running = false;
      window.clearTimeout(timerId);
    };
  }, [cameraActive, sessionId]);

  const prompt = challenge?.step === 'turn'
    ? 'Turn your face ' + challenge.direction
    : challenge?.step === 'blink'
      ? 'Blink once'
      : challenge?.step === 'sampling'
        ? 'Hold still…'
        : null;

  return (
    <main className="page">
      <header className="topbar">
        <div className="brand-mark">LV</div>
        <div className="brand-copy"><strong>LegacyVault</strong><span>Face verification test</span></div>
        <span className="demo-label">LOCAL DEMO</span>
      </header>

      <section className="heading">
        <p className="eyebrow">FUNCTIONAL PROTOTYPE</p>
        <h1>Face liveness + match</h1>
        <p>Test the camera flow, then see the result returned by the local Spring backend.</p>
      </section>

      <section className="camera-card" aria-label="Face verification camera">
        <div className={'camera-frame ' + (cameraActive ? 'active' : '')}>
          <video ref={videoRef} muted playsInline autoPlay aria-label="Live camera preview" className={cameraActive ? 'visible' : ''} />
          {!cameraActive && <div className="placeholder"><div className="camera-icon">◉</div><strong>Live camera preview</strong><span>Your camera starts only after you give consent and press the button.</span></div>}
          {cameraActive && <div className="face-guide" aria-hidden="true" />}
          {cameraActive && <div className="camera-status"><i className={metrics.faceCount === 1 ? 'found' : ''} />
            {metrics.faceCount === 1 ? 'One face detected' : metrics.faceCount > 1 ? 'Show only one face' : 'Looking for one face…'}
          </div>}
          {prompt && <div className="challenge-prompt">{prompt}</div>}
        </div>

        <div className="camera-controls">
          <label className="consent">
            <input type="checkbox" checked={consent} onChange={(event) => setConsent(event.target.checked)} disabled={cameraActive || starting} />
            <span>I consent to use my face for this local demo. The face template stays in backend memory for up to 15 minutes; liveness scores are sent by the browser for each check.</span>
          </label>
          <div className="actions">
            {!cameraActive ? (
              <button className="primary" onClick={startCamera} disabled={starting || !consent}>
                {starting ? 'Preparing camera…' : 'Start camera'}
              </button>
            ) : busy && challenge ? (
              <button className="primary" onClick={cancelVerification}>Cancel check</button>
            ) : !referenceReady ? (
              <button className="primary" onClick={enrollFace} disabled={busy || metrics.faceCount !== 1}>
                {busy ? 'Saving reference…' : 'Enroll this face'}
              </button>
            ) : (
              <button className="primary" onClick={startVerification} disabled={busy || metrics.faceCount !== 1}>
                {busy ? 'Checking…' : 'Start liveness check'}
              </button>
            )}
            {cameraActive && <button className="secondary" onClick={resetDemo} disabled={starting || busy}>Clear and stop</button>}
          </div>
          <div className="status-line" role="status">{message || modelStatus}</div>
          {cameraActive && <div className="model-line">{modelStatus} {metrics.confidence != null && <>Face quality {percent(metrics.confidence)}.</>}</div>}
        </div>
      </section>

      {result && <section className={'result-card ' + (result.passed ? 'passed' : 'failed')} aria-live="polite">
        <div className="result-icon">{result.passed ? '✓' : '!'}</div>
        <div className="result-body">
          <strong>{result.status === 'ERROR' ? 'Could not finish check' : result.passed ? 'Prototype checks passed' : 'Prototype checks did not pass'}</strong>
          {result.faceMatchScore != null && <div className="scores">
            <span>Face match <b>{percent(result.faceMatchScore)}</b></span>
            <span>Liveness <b>{percent(result.livenessScore)}</b></span>
            <span>Anti-spoof <b>{percent(result.antiSpoofScore)}</b></span>
          </div>}
          <small>{result.message}</small>
        </div>
      </section>}

      <p className="disclaimer"><strong>Demo only.</strong> Face analysis and liveness signals run in the browser; Spring Boot holds the temporary template and calculates the match/final result. Browser-reported liveness signals can be forged. No photo/video is uploaded, saved, or used to unlock assets.</p>
    </main>
  );
}
