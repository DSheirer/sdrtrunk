'use strict';

const CAMERA_PRIORITY = Object.freeze({
  emergency: 4,
  movement: 3,
  denial: 3,
  patch: 2,
  busy: 2,
  queued: 2,
  check: 1,
  page: 1,
  logout: 1,
  call: 0.5
});

function finite(value, fallback) {
  const number = Number(value);
  return Number.isFinite(number) ? number : fallback;
}

function createP25CameraCoordinator(options = {}) {
  const timing = Object.freeze({
    transitionMs: Math.max(0, finite(options.transitionMs, 900)),
    focusMs: Math.max(0, finite(options.focusMs, 5_000)),
    returnMs: Math.max(0, finite(options.returnMs, 900)),
    cooldownMs: Math.max(0, finite(options.cooldownMs, 8_000))
  });
  let mode = options.mode === 'manual' ? 'manual' : 'auto';
  let phase = mode === 'auto' ? 'roam' : 'disabled';
  let current = null;
  let phaseAtMs = 0;
  let returnAtMs = 0;
  let roamAtMs = 0;
  let cooldownUntilMs = 0;
  let cooldownPriority = 0;
  const seen = new Map();

  function snapshot(changed = false) {
    return Object.freeze({ mode, phase, changed, current, phaseAtMs, returnAtMs, roamAtMs,
      cooldownUntilMs, cooldownPriority, timing });
  }

  function setMode(value) {
    mode = value === 'manual' ? 'manual' : 'auto';
    phase = mode === 'auto' ? 'roam' : 'disabled';
    current = null;
    phaseAtMs = returnAtMs = roamAtMs = 0;
    return snapshot(true);
  }

  function consider(event, atMs = Date.now()) {
    const now = finite(atMs, Date.now());
    const key = String(event?.key || '');
    const observedAtMs = finite(event?.observedAtMs, now);
    const priority = Math.max(0, finite(event?.priority, CAMERA_PRIORITY[event?.category] || 0));
    const focusKeys = Array.isArray(event?.focusKeys) ? event.focusKeys.filter(Boolean).map(String) : [];
    if (mode !== 'auto' || !key || !priority || !focusKeys.length ||
        (seen.get(key) || 0) >= observedAtMs) return Object.freeze({ accepted: false, interrupted: false,
      state: snapshot() });
    seen.set(key, observedAtMs);
    if (seen.size > 500) seen.delete(seen.keys().next().value);
    const sequenceActive = phase === 'focus' || phase === 'hold' || phase === 'return';
    const protectedPriority = sequenceActive ? current?.priority || cooldownPriority : cooldownPriority;
    const cooling = now < cooldownUntilMs;
    if ((sequenceActive || cooling) && priority <= protectedPriority) {
      return Object.freeze({ accepted: false, interrupted: false, state: snapshot() });
    }
    const interrupted = sequenceActive || cooling;
    current = Object.freeze({ ...event, key, observedAtMs, priority, focusKeys });
    phase = 'focus';
    phaseAtMs = now + timing.transitionMs;
    returnAtMs = phaseAtMs + timing.focusMs;
    roamAtMs = returnAtMs + timing.returnMs;
    cooldownUntilMs = now + timing.cooldownMs;
    cooldownPriority = priority;
    return Object.freeze({ accepted: true, interrupted, state: snapshot(true) });
  }

  function update(atMs = Date.now()) {
    if (mode !== 'auto') return snapshot();
    const now = finite(atMs, Date.now());
    let changed = false;
    if (phase === 'focus' && now >= phaseAtMs) {
      phase = 'hold';
      changed = true;
    } else if (phase === 'hold' && now >= returnAtMs) {
      phase = 'return';
      changed = true;
    } else if (phase === 'return' && now >= roamAtMs) {
      phase = 'roam';
      current = null;
      changed = true;
    }
    if (now >= cooldownUntilMs && phase === 'roam') cooldownPriority = 0;
    return snapshot(changed);
  }

  function cancel(atMs = Date.now()) {
    if (mode !== 'auto') return snapshot();
    phase = 'roam';
    current = null;
    phaseAtMs = returnAtMs = roamAtMs = finite(atMs, Date.now());
    return snapshot(true);
  }

  function reset() {
    phase = mode === 'auto' ? 'roam' : 'disabled';
    current = null;
    phaseAtMs = returnAtMs = roamAtMs = cooldownUntilMs = cooldownPriority = 0;
    seen.clear();
    return snapshot(true);
  }

  return Object.freeze({ consider, update, setMode, cancel, reset, snapshot });
}

export { CAMERA_PRIORITY, createP25CameraCoordinator };
