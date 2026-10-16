import { openSetupGuide } from '../core/setup-guide.js?v=2';

const dismissedInMemory = new Set();
export const channelSetupGuideStorageKey = (account, dismissalScope) =>
  typeof dismissalScope === 'string' && dismissalScope.trim() ?
    `sdrtrunk-vce-channel-setup-guide-dismissed:v2:${encodeURIComponent(account || 'anonymous')}:${encodeURIComponent(dismissalScope)}` : null;

export function hasUsableSetupTuner(catalog) {
  return Array.isArray(catalog?.tuners) && catalog.tuners.some((tuner) =>
    tuner?.eligible === true && tuner.source_type === 'receiver');
}

export function createChannelSetupGuide({ ui, account, dismissalScope, channels, isCurrent, canPresent, loadTuners,
  newChannelButton, findSystemsButton, storage }) {
  const key = channelSetupGuideStorageKey(account, dismissalScope);
  const memoryKey = key ?? `session:${account || 'anonymous'}`;
  let guide = null;
  let checking = false;
  const dismissed = () => {
    if (dismissedInMemory.has(memoryKey)) return true;
    if (!key) return false;
    try { return (storage ?? globalThis.localStorage)?.getItem(key) === '1'; } catch (_) { return false; }
  };
  const remember = () => {
    dismissedInMemory.add(memoryKey);
    if (!key) return;
    try { (storage ?? globalThis.localStorage)?.setItem(key, '1'); }
    catch (_) { /* Keep this browser session usable without storage. */ }
  };
  const empty = () => { const rows = channels(); return Array.isArray(rows) && rows.length === 0; };
  const close = () => { guide?.close(); guide = null; };
  const refresh = async () => {
    if (guide && !guide.isOpen()) guide = null;
    if (!isCurrent() || !empty()) { close(); return; }
    if (checking || dismissed() || !findSystemsButton || (!guide && !canPresent())) return;
    checking = true;
    try {
      const tuners = await loadTuners();
      if (!isCurrent() || !empty() || !hasUsableSetupTuner(tuners)) { close(); return; }
      if (guide || dismissed() || !canPresent()) return;
      guide = openSetupGuide(ui, { title: 'Add your first channel', onDismiss: remember, choices: [
        { button: newChannelButton, description: 'Set up a channel manually using a known frequency.' },
        { button: findSystemsButton, description: 'Search for trunked radio systems your tuner can receive.' }
      ] });
    } catch (_) {
      // The ordinary channel actions remain available when tuner readiness is unknown.
      close();
    } finally { checking = false; }
  };
  return { refresh, close };
}
