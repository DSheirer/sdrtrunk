/** Serialize receiver changes while coalescing pointer movement to its newest target. */
export function createSpectrumLiveTune({ apply, preview = () => {}, applied = () => {},
  failed = () => {}, idle = () => {}, intervalMs = 75,
  now = () => performance.now(), schedule = (callback, delay) => setTimeout(callback, delay),
  unschedule = (timer) => clearTimeout(timer) }) {
  let epoch = 0;
  let version = 0;
  let completions = 0;
  let pending = null;
  let active = null;
  let timer = null;
  let disposed = false;
  let lastStarted = -Infinity;
  let desiredFrequency = null;
  let issuedFrequency = null;
  let confirmedFrequency = null;

  const clearTimer = () => {
    if (timer !== null) unschedule(timer);
    timer = null;
  };
  const pump = () => {
    if (disposed || active || !pending || timer !== null) return;
    const delay = pending.final ? 0 : Math.max(0, intervalMs - (now() - lastStarted));
    if (delay > 0) {
      timer = schedule(() => { timer = null; pump(); }, delay);
      return;
    }
    const request = pending;
    pending = null;
    active = request;
    lastStarted = now();
    // The pointer callback never waits for the server. One transaction remains in flight across cancellation.
    Promise.resolve().then(() => {
      if (disposed || request.epoch !== epoch) return;
      issuedFrequency = request.frequency;
      request.issued = true;
      return apply(request.frequency);
    }).then((value) => {
      if (!disposed && request.issued) completions += 1;
      if (disposed || request.epoch !== epoch) return;
      confirmedFrequency = request.frequency;
      if (request.version === version) applied(value, request.frequency);
    }, (error) => {
      if (!disposed && request.issued) completions += 1;
      if (!disposed && request.epoch === epoch && request.version === version) failed(error, request.frequency);
    }).finally(() => {
      if (active === request) active = null;
      if (disposed) return;
      pump();
      if (!active && !pending && timer === null) idle();
    });
  };

  return {
    request(frequencyHz, { final = false } = {}) {
      if (disposed || !Number.isSafeInteger(frequencyHz) || frequencyHz <= 0) return;
      desiredFrequency = frequencyHz;
      if (!active && !pending && confirmedFrequency === frequencyHz) return;
      preview(frequencyHz);
      // Returning to the in-flight target cancels the intermediate queued target without repeating a write.
      if (active?.epoch === epoch && active.frequency === frequencyHz) {
        pending = null;
        clearTimer();
        active.version = ++version;
        return;
      }
      pending = { frequency: frequencyHz, epoch, version: ++version, final: final || pending?.final === true };
      if (final) clearTimer();
      pump();
    },
    cancel() {
      epoch += 1;
      version += 1;
      pending = null;
      clearTimer();
      desiredFrequency = issuedFrequency ?? confirmedFrequency;
      if (!active) idle();
    },
    reset() {
      this.cancel();
      desiredFrequency = issuedFrequency = confirmedFrequency = null;
    },
    close() {
      disposed = true;
      epoch += 1;
      pending = null;
      clearTimer();
    },
    get frequencyHz() { return desiredFrequency; },
    get revision() { return version + completions; },
    get busy() { return !!active || !!pending || timer !== null; }
  };
}
