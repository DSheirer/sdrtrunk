/** The same unclipped maximum-per-pixel value used by the visible FFT trace. */
export function spectrumTraceValue(values, point, points) {
  if (!values?.length || !Number.isFinite(points) || points < 2 ||
      !Number.isInteger(point) || point < 0 || point >= points) return NaN;
  const first = Math.min(values.length - 1, Math.floor(point * values.length / points));
  const last = Math.min(values.length,
    Math.max(first + 1, Math.ceil((point + 1) * values.length / points)));
  let peak = -Infinity;
  for (let bin = first; bin < last; bin += 1) {
    if (Number.isFinite(values[bin])) peak = Math.max(peak, values[bin]);
  }
  return Number.isFinite(peak) ? peak : NaN;
}

/** Use the lowest visible trace reading, ignoring the transport's empty/invalid strength sentinel. */
export function estimateSpectrumDisplayFloor(values, ceilingDb, minimumDb = -200, minimumSpanDb = 5,
    points = values?.length) {
  if (!values || typeof values[Symbol.iterator] !== 'function' ||
      !Number.isFinite(ceilingDb) || !Number.isFinite(minimumDb) ||
      !(minimumSpanDb > 0) || ceilingDb - minimumDb < minimumSpanDb ||
      !Number.isInteger(points) || points < 2) return null;
  let lowest = Infinity;
  for (let point = 0; point < points; point += 1) {
    const value = spectrumTraceValue(values, point, points);
    if (Number.isFinite(value) && value !== -196 && value < lowest) lowest = value;
  }
  if (!Number.isFinite(lowest)) return null;
  return Math.max(minimumDb, Math.min(ceilingDb - minimumSpanDb, Math.floor(lowest)));
}

/** Capture one valid FFT after a confirmed tuner/center change, independently of producer restarts or zoom. */
export function createSpectrumAutoRange() {
  let selectedTarget = '';
  let retainedTarget = '';
  let centerHz = null;
  let generation = -1;
  let revision = -1;
  let live = false;
  let pending = false;
  return {
    selectTarget(targetId) {
      const next = String(targetId || '');
      if (next === selectedTarget) return false;
      selectedTarget = next;
      live = false;
      // Lease/mode transitions briefly unbind a tuner; that does not change its confirmed frequency.
      if (!next || next === retainedTarget) return false;
      retainedTarget = next;
      centerHz = null;
      generation = revision = -1;
      pending = false;
      return true;
    },
    confirm({ targetId, centerFrequencyHz, sampleRateHz, generation: nextGeneration,
      revision: nextRevision, live: nextLive }) {
      if (!selectedTarget || String(targetId || '') !== selectedTarget ||
          !Number.isSafeInteger(nextGeneration) || nextGeneration < 0 ||
          !Number.isSafeInteger(nextRevision) || nextRevision < 0 || nextGeneration < generation ||
          (nextGeneration === generation && nextRevision < revision)) return false;
      generation = nextGeneration;
      revision = nextRevision;
      live = nextLive === true && Number.isFinite(centerFrequencyHz) && centerFrequencyHz > 0 &&
        Number.isFinite(sampleRateHz) && sampleRateHz > 0;
      if (live && centerFrequencyHz !== centerHz) {
        centerHz = centerFrequencyHz;
        pending = true;
      }
      return true;
    },
    acceptsFrame(frameGeneration) { return live && frameGeneration === generation; },
    needsSample() { return pending && live; },
    requestSample() { pending = true; },
    sample(values, frameGeneration, ceilingDb, points = values?.length) {
      if (!pending || !this.acceptsFrame(frameGeneration)) return null;
      const floor = estimateSpectrumDisplayFloor(values, ceilingDb, -200, 5, points);
      if (floor !== null) pending = false;
      return floor;
    },
    discard() { pending = false; }
  };
}
