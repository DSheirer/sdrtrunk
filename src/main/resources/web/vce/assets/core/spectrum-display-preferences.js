const STORAGE_PREFIX = 'sdrtrunk-vce-spectrum-display-v1:';
const PREFERENCE_KEYS = Object.freeze([
  'fft_auto_range_on_retune', 'waterfall_auto_range_on_retune'
]);
const defaults = () => ({ version: 1, fft_auto_range_on_retune: true, waterfall_auto_range_on_retune: true });

function browserStorage() {
  try { return globalThis.localStorage; } catch (_error) { return null; }
}

function decode(value) {
  if (typeof value !== 'string' || value.length > 512) return null;
  try {
    const record = JSON.parse(value);
    if (!record || Array.isArray(record) || record.version !== 1 ||
        Object.keys(record).length !== 3 ||
        !PREFERENCE_KEYS.every((key) => typeof record[key] === 'boolean')) return null;
    return record;
  } catch (_error) { return null; }
}

/** Browser-only display choices are isolated by stable account ID, with a separate anonymous record. */
export function createSpectrumDisplayPreferences({ identity = null, storage = browserStorage() } = {}) {
  const key = STORAGE_PREFIX + (identity === null ? 'anonymous' : `account:${encodeURIComponent(String(identity))}`);
  let current = defaults();
  let unsaved = false;
  const read = () => {
    try {
      const saved = decode(storage?.getItem(key));
      if (saved && !unsaved) current = saved;
    } catch (_error) { /* Storage may be disabled; retain this panel's in-memory choices. */ }
    return current;
  };
  const requireKey = (preference) => {
    if (!PREFERENCE_KEYS.includes(preference)) throw new TypeError('Unknown spectrum display preference.');
  };
  const save = (next) => {
    current = next;
    try {
      storage?.setItem(key, JSON.stringify(current));
      unsaved = false;
    } catch (_error) {
      unsaved = true;
      // A prior saved record must not overwrite this panel's choice after a failed write.
    }
  };
  return {
    autoRangeEnabled() {
      const saved = read();
      return saved.fft_auto_range_on_retune && saved.waterfall_auto_range_on_retune;
    },
    setAutoRangeEnabled(enabled) {
      if (typeof enabled !== 'boolean') throw new TypeError('Spectrum display preferences must be boolean.');
      // Keep the prior record readable, preserve an existing opt-out, and update both plots atomically.
      save({ ...read(), fft_auto_range_on_retune: enabled, waterfall_auto_range_on_retune: enabled });
    },
    get(preference) {
      requireKey(preference);
      return read()[preference];
    },
    set(preference, enabled) {
      requireKey(preference);
      if (typeof enabled !== 'boolean') throw new TypeError('Spectrum display preferences must be boolean.');
      save({ ...read(), [preference]: enabled });
    }
  };
}
