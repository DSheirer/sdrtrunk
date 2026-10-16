'use strict';

const P25_EVENT_SETTINGS = Object.freeze([
  Object.freeze({ id: 'emergency', label: 'Emergency', detail: 'Emergency activity involving a known radio or group.',
    highlight: true, autoZoom: true }),
  Object.freeze({ id: 'movement', label: 'Affiliation change',
    detail: 'A confirmed radio affiliation changed between talkgroups in comparable scope.',
    highlight: true, autoZoom: true }),
  Object.freeze({ id: 'denial', label: 'Denial', detail: 'A denied request associated with a known radio.',
    highlight: true, autoZoom: true }),
  Object.freeze({ id: 'patch', label: 'Patch change', detail: 'A stored patch observation, creation, or removal.',
    highlight: true, autoZoom: true }),
  Object.freeze({ id: 'busy', label: 'Busy response', detail: 'A stored busy response.',
    highlight: true, autoZoom: true }),
  Object.freeze({ id: 'queued', label: 'Queued response', detail: 'A stored queued response.',
    highlight: true, autoZoom: true }),
  Object.freeze({ id: 'check', label: 'Radio check', detail: 'A directed radio-check observation.',
    highlight: true, autoZoom: true }),
  Object.freeze({ id: 'page', label: 'Page', detail: 'A stored page or call-alert observation.',
    highlight: true, autoZoom: true }),
  Object.freeze({ id: 'logout', label: 'Logout', detail: 'A stored deregistration or logout observation.',
    highlight: true, autoZoom: true }),
  Object.freeze({ id: 'call', label: 'Calls and grants',
    detail: 'Routine stored call and control-channel grant activity. This does not prove affiliation.',
    highlight: true, autoZoom: false })
]);

function defaultP25EventSettings() {
  return Object.fromEntries(P25_EVENT_SETTINGS.map((entry) => [entry.id, {
    highlight: entry.highlight,
    autoZoom: entry.autoZoom
  }]));
}

function normalizeP25EventSettings(value) {
  const source = value && typeof value === 'object' && !Array.isArray(value) ? value : {};
  return Object.fromEntries(P25_EVENT_SETTINGS.map((entry) => {
    const saved = source[entry.id];
    return [entry.id, {
      highlight: typeof saved?.highlight === 'boolean' ? saved.highlight : entry.highlight,
      autoZoom: typeof saved?.autoZoom === 'boolean' ? saved.autoZoom : entry.autoZoom
    }];
  }));
}

function enabledP25EventCategories(settings, property) {
  const normalized = normalizeP25EventSettings(settings);
  return new Set(P25_EVENT_SETTINGS.filter((entry) => normalized[entry.id]?.[property]).map((entry) => entry.id));
}

function routineP25ActivityEnabled(settings) {
  const call = normalizeP25EventSettings(settings).call;
  return call.highlight || call.autoZoom;
}

export {
  P25_EVENT_SETTINGS,
  defaultP25EventSettings,
  normalizeP25EventSettings,
  enabledP25EventCategories,
  routineP25ActivityEnabled
};
