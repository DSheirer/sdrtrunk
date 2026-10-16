'use strict';

import {
  P25_HISTORY_ACTIONS,
  P25_ROUTINE_ACTIONS,
  createP25HistoryState,
  applyP25ActivityRows,
  buildP25Graph,
  nextP25HighlightExpiry,
  groupedP25Events,
  mostActiveP25System
} from './history.js?v=10';
import { createP25CameraCoordinator } from './camera.js?v=4';
import { createP25Renderer } from './renderer.js?v=7';
import {
  P25_EVENT_SETTINGS,
  normalizeP25EventSettings,
  enabledP25EventCategories,
  routineP25ActivityEnabled
} from './settings.js?v=2';

const HOUR_MS = 60 * 60 * 1_000;
const POLL_MS = 5_000;
const PAGE_LIMIT = 5_000;
const MAX_SEED_ROWS = 250_000;
const MAX_POLL_ROWS = 25_000;
const MAX_BUFFERED_ACTIVITY_BATCHES = 64;
const CLOCK_TOLERANCE_MS = 2_000;

function textButton(node, label, className = 'ui-button ui-button-secondary') {
  const button = node('button', className, label);
  button.type = 'button';
  return button;
}

function formatTime(value) {
  const timestamp = Number(value);
  return Number.isFinite(timestamp) && timestamp > 0 ? new Intl.DateTimeFormat(undefined, {
    hour: 'numeric', minute: '2-digit', second: '2-digit'
  }).format(timestamp) : 'Unknown time';
}

function validPage(value) {
  if (!value || typeof value !== 'object' || !Array.isArray(value.rows) || typeof value.has_more !== 'boolean') {
    throw new Error('The receiver returned invalid saved P25 activity.');
  }
  return value;
}

function reseedError(message) {
  const error = new Error(message);
  error.reseed = true;
  return error;
}

function createP25Visualizer(dependencies = {}) {
  const required = ['node', 'iconGlyph', 'iconButton', 'requestActivity', 'openReadOnlyModal',
    'preferenceCheckbox', 'settingsCard', 'settingsCardGrid', 'modalFooter',
    'loadEventSettings', 'saveEventSettings'];
  required.forEach((name) => {
    if (typeof dependencies[name] !== 'function') throw new TypeError(`P25 Visualizer dependency ${name} is required.`);
  });
  const { node, iconGlyph, iconButton, requestActivity, openReadOnlyModal, preferenceCheckbox,
    settingsCard, settingsCardGrid, modalFooter, loadEventSettings, saveEventSettings } = dependencies;
  const layout = node('section', 'network-visualizer-layout');
  layout.setAttribute('aria-label', 'P25 Visualizer');
  const toolbar = node('div', 'network-visualizer-toolbar ui-surface');
  toolbar.setAttribute('role', 'toolbar');
  toolbar.setAttribute('aria-label', 'P25 Visualizer controls');
  const brand = node('div', 'network-visualizer-brand');
  brand.append(iconGlyph('icon-network-visualizer'), node('strong', '', 'P25 Visualizer'));
  const status = node('span', 'badge ui-pill state-stale network-visualizer-status', 'Loading saved activity');
  status.setAttribute('role', 'status');
  status.setAttribute('aria-live', 'polite');

  const actions = node('div', 'network-visualizer-actions');
  const scopeControls = node('div', 'network-visualizer-choice-group');
  scopeControls.setAttribute('role', 'group');
  scopeControls.setAttribute('aria-label', 'History scope');
  const oneHour = textButton(node, '1h');
  const day = textButton(node, '24h');
  oneHour.setAttribute('aria-pressed', 'true');
  day.setAttribute('aria-pressed', 'false');
  scopeControls.append(oneHour, day);
  const cameraControls = node('div', 'network-visualizer-choice-group');
  cameraControls.setAttribute('role', 'group');
  cameraControls.setAttribute('aria-label', 'Camera mode');
  const auto = textButton(node, 'Auto');
  const manual = textButton(node, 'Manual');
  auto.setAttribute('aria-pressed', 'true');
  manual.setAttribute('aria-pressed', 'false');
  cameraControls.append(auto, manual);
  const eventsToggle = textButton(node, 'Events');
  eventsToggle.setAttribute('aria-pressed', 'false');
  const settingsButton = textButton(node, 'Settings');
  const fullscreen = iconButton('icon-fullscreen', 'Enter fullscreen');
  fullscreen.classList.add('network-visualizer-fullscreen');
  actions.append(scopeControls, cameraControls, eventsToggle, settingsButton, fullscreen);
  toolbar.append(brand, status, actions);

  const stage = node('div', 'network-visualizer-stage');
  stage.tabIndex = 0;
  stage.setAttribute('aria-label', 'Interactive P25 history visualization');
  const canvas = node('div', 'network-visualizer-canvas');
  const back = textButton(node, '← All systems',
    'ui-button ui-button-secondary network-visualizer-back');
  back.hidden = true;
  const scopeTitle = node('div', 'network-visualizer-scope-title', 'P25 radio systems');
  const autoFocus = node('aside', 'network-visualizer-auto-focus');
  autoFocus.hidden = true;
  autoFocus.setAttribute('role', 'status');
  autoFocus.setAttribute('aria-live', 'polite');
  autoFocus.setAttribute('aria-atomic', 'true');
  const autoFocusKicker = node('span', 'network-visualizer-auto-focus-kicker', 'AUTO FOCUS');
  const autoFocusTitle = node('strong', 'network-visualizer-auto-focus-title');
  const autoFocusDetail = node('span', 'network-visualizer-auto-focus-detail');
  autoFocus.append(autoFocusKicker, autoFocusTitle, autoFocusDetail);
  const manualGuide = node('aside', 'network-visualizer-manual-guide');
  manualGuide.hidden = true;
  manualGuide.append(node('strong', '', 'Manual camera'), node('span', '', 'W/S move · A/D strafe · Q/E rise'),
    node('span', '', 'Arrow keys look · Drag orbits · Wheel zooms at cursor'));
  const empty = node('div', 'network-visualizer-empty');
  empty.append(iconGlyph('icon-network-visualizer'), node('strong', '', 'Loading saved P25 activity…'),
    node('p', '', 'This view uses only persisted Activity history.'));
  const legend = node('div', 'network-visualizer-legend');
  legend.append(node('span', '', 'Sphere · system'), node('span', '', 'Cube · talkgroup'),
    node('span', '', 'Triangle · radio'), node('span', '', 'Faint line · earlier affiliation'),
    node('span', '', 'Dashed line · call activity'));
  const events = node('aside', 'network-visualizer-events');
  events.hidden = true;
  const eventsHeader = node('header', 'network-visualizer-panel-header');
  const eventsHeading = node('div', 'network-visualizer-panel-heading');
  eventsHeading.append(node('h2', '', 'Noteworthy P25 activity'),
    node('p', '', 'Grouped from saved Activity history'));
  const closeEvents = iconButton('icon-close', 'Close noteworthy activity');
  eventsHeader.append(eventsHeading, closeEvents);
  const eventList = node('ol', 'network-visualizer-event-list');
  eventList.tabIndex = 0;
  eventList.setAttribute('aria-label', 'Saved noteworthy P25 activity');
  events.append(eventsHeader, eventList);
  stage.append(canvas, back, scopeTitle, autoFocus, manualGuide, empty, legend, events);
  layout.append(toolbar, stage);

  let state = createP25HistoryState();
  let eventSettings = normalizeP25EventSettings(loadEventSettings());
  let highlightedCategories = enabledP25EventCategories(eventSettings, 'highlight');
  let autoZoomCategories = enabledP25EventCategories(eventSettings, 'autoZoom');
  let renderer = null;
  let selectedSystemKey = '';
  let selectedGroupKey = '';
  let visibleNodeKeys = new Set();
  let historyHours = 1;
  let mode = 'auto';
  let cursor = 0;
  let generation = 0;
  let requestController = null;
  let pollTimer = 0;
  let highlightTimer = 0;
  let grantActivityTimeoutMs = 1_000;
  let serverTimeOffsetMs = 0;
  let liveSource = null;
  let liveBound = false;
  let liveReady = false;
  let collectionEnabled = true;
  let catchupRequested = false;
  let bufferedActivity = [];
  let bufferedRowCount = 0;
  let introTimer = 0;
  let animationFrame = 0;
  let lastAnimationAt = performance.now();
  let closed = false;
  let polling = false;
  let lastCameraPhase = 'roam';
  let navigationRevision = 0;
  let eventsModal = null;
  const shortViewport = window.matchMedia('(max-height: 640px)');
  const pressedKeys = new Set();
  const camera = createP25CameraCoordinator({ focusMs: 5_000, cooldownMs: 8_000, transitionMs: 900,
    returnMs: 900 });

  function setStatus(label, tone = 'current') {
    status.textContent = label;
    status.className = `badge ui-pill network-visualizer-status state-${tone}`;
  }

  function setEmpty(title, detail = '', kind = '') {
    empty.hidden = false;
    empty.dataset.kind = kind;
    empty.replaceChildren(iconGlyph('icon-network-visualizer'), node('strong', '', title));
    if (detail) empty.append(node('p', '', detail));
  }

  function clearAutoFocus() {
    autoFocus.hidden = true;
    autoFocus.dataset.phase = '';
    autoFocus.dataset.category = '';
  }

  function showAutoFocus(event) {
    autoFocusKicker.textContent = `AUTO FOCUS · ${formatTime(event.observedAtMs)}`;
    autoFocusTitle.textContent = event.title || 'Noteworthy P25 activity';
    autoFocusDetail.textContent = event.detail || 'P25 system activity';
    autoFocus.dataset.category = event.category || '';
    autoFocus.dataset.phase = 'focus';
    autoFocus.hidden = false;
  }

  function renderEvents() {
    const rows = groupedP25Events(state, selectedSystemKey).filter((event) => !selectedGroupKey ||
      event.focusKeys.some((key) => visibleNodeKeys.has(key)));
    if (!rows.length) {
      eventList.replaceChildren(node('li', 'network-visualizer-event network-visualizer-event-empty',
        'No noteworthy saved activity in this scope.'));
      ensureEventsSpace();
      return;
    }
    eventList.replaceChildren(...rows.map((event) => {
      const item = node('li', 'network-visualizer-event');
      item.dataset.category = event.category;
      item.append(node('strong', 'network-visualizer-event-kind', event.title),
        node('span', 'network-visualizer-event-detail', event.detail),
        node('time', '', formatTime(event.observedAtMs)));
      return item;
    }));
    ensureEventsSpace();
  }

  function openEventsSheet() {
    if (eventsModal || closed || document.body.classList.contains('modal-open')) return;
    const body = node('div', 'network-visualizer-events-sheet');
    body.append(node('p', 'modal-introduction', 'Grouped from saved Activity history'), events);
    eventsModal = openReadOnlyModal('Noteworthy P25 activity', body, {
      id: 'p25-visualizer-events', className: 'network-visualizer-events-modal',
      onClose: () => {
        eventsModal = null;
        events.hidden = true;
        stage.append(events);
        eventsToggle.setAttribute('aria-pressed', 'false');
        if (!closed) eventsToggle.focus({ preventScroll: true });
      }
    });
    if (!eventsModal) stage.append(events);
    else if (document.fullscreenElement === layout) layout.append(eventsModal.state.backdrop);
  }

  function ensureEventsSpace() {
    if (events.hidden || eventsModal || closed) return;
    const entryHeight = eventList.firstElementChild?.getBoundingClientRect().height || 0;
    if (shortViewport.matches || eventList.clientHeight < entryHeight) openEventsSheet();
  }
  shortViewport.addEventListener('change', ensureEventsSpace);
  const eventsResizeObserver = new ResizeObserver(ensureEventsSpace);
  eventsResizeObserver.observe(stage);

  function openSettings() {
    const form = node('form', 'admin-form network-visualizer-settings-form');
    const message = node('div', 'admin-form-message');
    message.setAttribute('role', 'status');
    const controls = new Map();
    const choices = (property) => P25_EVENT_SETTINGS.map((entry) => {
      const choice = preferenceCheckbox(`p25-visualizer-${property}-${entry.id}`, entry.label,
        eventSettings[entry.id][property], entry.detail);
      controls.set(`${property}:${entry.id}`, choice.input);
      return choice.control;
    });
    const highlightCard = settingsCard('Scene highlights',
      'Briefly emphasize new stored observations in the 3D scene.', ...choices('highlight'));
    const zoomCard = settingsCard('Automatic camera',
      'Allow these newly stored observations to interrupt Auto mode. Manual mode never moves itself.',
      ...choices('autoZoom'));
    const cancel = textButton(node, 'Cancel');
    const save = textButton(node, 'Save choices', 'ui-button ui-button-primary');
    save.type = 'submit';
    form.append(node('p', 'modal-introduction',
      'These choices are saved in this browser for the current web profile.'),
      settingsCardGrid(highlightCard, zoomCard), message,
      modalFooter(cancel, save));
    const modal = openReadOnlyModal('P25 Visualizer settings', form, {
      id: 'p25-visualizer-settings', className: 'network-visualizer-settings-modal'
    });
    if (!modal) return;
    cancel.addEventListener('click', modal.close);
    form.addEventListener('input', () => modal.setDirty(true));
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      if (save.disabled) return;
      const next = Object.fromEntries(P25_EVENT_SETTINGS.map((entry) => [entry.id, {
        highlight: controls.get(`highlight:${entry.id}`).checked,
        autoZoom: controls.get(`autoZoom:${entry.id}`).checked
      }]));
      save.disabled = true;
      cancel.disabled = true;
      modal.setBusy(true);
      message.textContent = 'Saving visualizer choices…';
      try {
        await saveEventSettings(next);
        eventSettings = normalizeP25EventSettings(next);
        highlightedCategories = enabledP25EventCategories(eventSettings, 'highlight');
        autoZoomCategories = enabledP25EventCategories(eventSettings, 'autoZoom');
        modal.setDirty(false);
        modal.setBusy(false);
        if (modal.close()) void loadSeed();
      } catch (error) {
        message.textContent = error?.message || 'Visualizer choices could not be saved in this browser.';
      } finally {
        modal.setBusy(false);
        if (save.isConnected) save.disabled = false;
        if (cancel.isConnected) cancel.disabled = false;
      }
    });
  }

  function renderGraph(animate = true, affiliationChanges = []) {
    if (!renderer) return;
    const graph = buildP25Graph(state, selectedSystemKey, Date.now() + serverTimeOffsetMs, selectedGroupKey,
      { highlightCategories: highlightedCategories });
    visibleNodeKeys = new Set(graph.nodes.map((value) => value.id));
    renderer.setData(graph, { animate, affiliationChanges });
    renderEvents();
    if (highlightTimer) window.clearTimeout(highlightTimer);
    const now = Date.now() + serverTimeOffsetMs;
    const expiresAt = nextP25HighlightExpiry(state, now, { highlightCategories: highlightedCategories });
    highlightTimer = expiresAt ? window.setTimeout(() => {
      highlightTimer = 0;
      if (!closed) renderGraph(false);
    }, Math.max(1, expiresAt - now)) : 0;
  }

  function renderScopeTitle(system, group = null) {
    const label = system.name || system.identity || system.label;
    const context = node('div', 'muted');
    const destination = dependencies.systemHref?.(system);
    const systemText = destination ? node('a', 'ui-link-text', label) : node('span', '', label);
    if (destination) systemText.href = destination;
    if (group) {
      context.append('System: ', systemText);
      if (system.identity) context.append(` · ${system.identity}`);
      scopeTitle.replaceChildren(node('strong', '', group.label), context);
    } else {
      scopeTitle.replaceChildren(systemText);
      if (system.identity && system.identity !== label) scopeTitle.append(node('div', 'muted', system.identity));
    }
  }

  function enterSystem(systemKey, optionsValue = {}) {
    const system = state.systems.get(String(systemKey || ''));
    if (!system || !renderer) return false;
    if (introTimer) window.clearTimeout(introTimer);
    introTimer = 0;
    if (optionsValue.automatic !== true) navigationRevision += 1;
    selectedSystemKey = system.key;
    selectedGroupKey = '';
    back.hidden = false;
    back.textContent = '← All systems';
    renderScopeTitle(system);
    clearAutoFocus();
    camera.reset();
    lastCameraPhase = 'roam';
    renderer.setCameraPhase('roam');
    renderGraph(optionsValue.animate !== false);
    renderer.enterSystem(system.key, { immediate: optionsValue.immediate === true });
    return true;
  }

  function enterTalkgroup(groupKey) {
    const group = state.groups.get(String(groupKey || ''));
    if (!group || group.systemKey !== selectedSystemKey || !renderer) return false;
    navigationRevision += 1;
    selectedGroupKey = group.key;
    back.hidden = false;
    back.textContent = '← System';
    renderScopeTitle(state.systems.get(group.systemKey), group);
    clearAutoFocus();
    camera.reset();
    lastCameraPhase = 'roam';
    renderer.setCameraPhase('roam');
    renderGraph(true);
    renderer.enterTalkgroup(selectedSystemKey, group.key);
    return true;
  }

  function showOverview() {
    if (!renderer) return;
    if (introTimer) window.clearTimeout(introTimer);
    introTimer = 0;
    navigationRevision += 1;
    selectedSystemKey = '';
    selectedGroupKey = '';
    back.hidden = true;
    scopeTitle.textContent = 'P25 radio systems';
    clearAutoFocus();
    camera.reset();
    lastCameraPhase = 'roam';
    renderer.setCameraPhase('roam');
    renderGraph(false);
    renderer.showOverview();
  }

  function applyAttention(candidates) {
    if (mode !== 'auto' || !selectedSystemKey || document.hidden) return;
    for (const event of candidates.filter((candidate) => candidate.systemKey === selectedSystemKey &&
      autoZoomCategories.has(candidate.category))) {
      const focusKeys = event.focusKeys.filter((key) => visibleNodeKeys.has(key));
      if (!focusKeys.length) continue;
      const visibleEvent = { ...event, focusKeys };
      const decision = camera.consider(visibleEvent, Date.now());
      if (!decision.accepted) continue;
      lastCameraPhase = 'focus';
      renderer?.setCameraPhase('focus');
      const focused = event.affiliationRadioKey && renderer?.focusAffiliation?.(
        event.affiliationRadioKey, decision.state.timing.transitionMs) ||
        renderer?.focus(focusKeys, decision.state.timing.transitionMs);
      if (!focused) {
        camera.cancel();
        lastCameraPhase = 'roam';
        renderer?.setCameraPhase('roam');
        clearAutoFocus();
        break;
      }
      showAutoFocus(event);
      break;
    }
  }

  function historyBounds() {
    const toMs = Date.now() + serverTimeOffsetMs + CLOCK_TOLERANCE_MS;
    return { fromMs: toMs - historyHours * HOUR_MS, toMs };
  }

  async function requestForwardPage(afterId, watermarkId, signal, bounds) {
    const { fromMs, toMs } = bounds;
    const actions = routineP25ActivityEnabled(eventSettings) ?
      [...P25_HISTORY_ACTIONS, ...P25_ROUTINE_ACTIONS] : P25_HISTORY_ACTIONS;
    const page = validPage(await requestActivity({ from_ms: fromMs, to_ms: toMs,
      actions: actions.join(','), after_id: afterId, watermark_id: watermarkId,
      limit: PAGE_LIMIT }, { signal }));
    const timing = page.traffic_grant_age_out_milliseconds;
    if (Number.isInteger(timing) && timing >= 100 && timing <= 15_000) grantActivityTimeoutMs = timing;
    return page;
  }

  async function loadSeed() {
    const localGeneration = ++generation;
    navigationRevision += 1;
    if (highlightTimer) window.clearTimeout(highlightTimer);
    highlightTimer = 0;
    if (introTimer) window.clearTimeout(introTimer);
    introTimer = 0;
    requestController?.abort();
    liveSource?.close();
    liveSource = null;
    liveBound = liveReady = catchupRequested = false;
    bufferedActivity = [];
    bufferedRowCount = 0;
    const controller = new AbortController();
    requestController = controller;
    const abort = () => controller.abort();
    dependencies.signal?.addEventListener?.('abort', abort, { once: true });
    state = createP25HistoryState();
    selectedSystemKey = '';
    selectedGroupKey = '';
    visibleNodeKeys = new Set();
    cursor = 0;
    camera.reset();
    clearAutoFocus();
    renderer?.setData({ nodes: [], links: [] }, { animate: false });
    back.hidden = true;
    scopeTitle.textContent = 'P25 radio systems';
    setEmpty(`Loading ${historyHours}h of saved P25 activity…`,
      routineP25ActivityEnabled(eventSettings) ? 'Including saved calls and grants.' :
        'Routine calls and grants are off in Visualizer settings.');
    setStatus('Loading saved activity', 'stale');
    const history = dependencies.historyStatus || {};
    if (history.available && !history.historyActive && !history.historyRetained) {
      setEmpty('Saved P25 activity is unavailable',
        'Turn on Save individual activity events in Administration > Activity settings. The visualizer uses ' +
          'saved activity rather than live decoder activity.',
        'error');
      setStatus('History unavailable', 'error');
      dependencies.signal?.removeEventListener?.('abort', abort);
      return;
    }
    try {
      const bounds = historyBounds();
      let afterId = 0;
      let watermarkId = null;
      let processed = 0;
      const seedRows = [];
      do {
        const page = await requestForwardPage(afterId, watermarkId, controller.signal, bounds);
        if (closed || localGeneration !== generation) return;
        if (page.reset_required === true || page.reseed_required === true ||
            Number(page.watermark_id) < afterId) {
          throw reseedError('Saved Activity history restarted. Reloading the selected scope.');
        }
        if (watermarkId === null) watermarkId = Number(page.watermark_id) || 0;
        const capacity = Math.max(0, MAX_SEED_ROWS - processed);
        if (page.rows.length > capacity || page.has_more && processed + page.rows.length >= MAX_SEED_ROWS) {
          throw new Error('This history scope contains too much noteworthy activity to load safely. Choose 1h.');
        }
        const rows = page.rows.slice(0, capacity);
        seedRows.push(...rows);
        processed += rows.length;
        const next = Number(page.next_after_id);
        if (!Number.isSafeInteger(next) || next < afterId || page.has_more && next === afterId) {
          throw new Error('Saved activity paging did not advance.');
        }
        afterId = next;
        if (!page.has_more) break;
      } while (!controller.signal.aborted);
      if (closed || localGeneration !== generation) return;
      applyP25ActivityRows(state, seedRows, { initial: true, highlightCategories: highlightedCategories });
      cursor = Math.max(afterId, watermarkId || 0, state.latestId);
      const mostActive = mostActiveP25System(state);
      if (!mostActive) {
        renderGraph(false);
        setEmpty('No noteworthy P25 activity was saved in this scope',
          routineP25ActivityEnabled(eventSettings) ?
            'No selected noteworthy, call, or grant activity was found.' :
            'Enable calls and grants in Visualizer settings to include routine activity.');
      } else {
        empty.hidden = true;
        selectedSystemKey = '';
        renderGraph(false);
        renderer?.showOverview({ immediate: true });
        const expectedNavigation = navigationRevision;
        introTimer = window.setTimeout(() => {
          introTimer = 0;
          if (!closed && localGeneration === generation && navigationRevision === expectedNavigation &&
              !selectedSystemKey) {
            enterSystem(mostActive.key, { automatic: true, animate: false });
          }
        }, 2_500);
      }
      const paused = history.available && !history.historyActive;
      setStatus(paused ? 'Saved history · updates paused' : 'Saved history', paused ? 'stale' : 'current');
      connectLiveActivity(localGeneration);
      schedulePoll();
    } catch (error) {
      if (error?.name === 'AbortError' || closed || localGeneration !== generation) return;
      if (error?.reseed) {
        setStatus('Reloading saved history', 'stale');
        window.setTimeout(() => void loadSeed(), 0);
        return;
      }
      setEmpty('Saved P25 activity could not be loaded',
        error?.message || 'The existing Activity history remains unchanged. Try this view again.', 'error');
      setStatus('History error', 'error');
    } finally {
      dependencies.signal?.removeEventListener?.('abort', abort);
    }
  }

  function schedulePoll(delay = POLL_MS) {
    if (pollTimer) window.clearTimeout(pollTimer);
    if (!closed) pollTimer = window.setTimeout(() => void poll(), delay);
  }

  function applyActivityUpdate(rows) {
    const selected = routineP25ActivityEnabled(eventSettings) ? rows :
      rows.filter((row) => !P25_ROUTINE_ACTIONS.includes(String(row?.action || '').toUpperCase()));
    const result = applyP25ActivityRows(state, selected, { initial: false,
      atMs: Date.now() + serverTimeOffsetMs, highlightAtObservationTime: true,
      highlightCategories: highlightedCategories, grantActivityTimeoutMs });
    if (result.changed) {
      if (state.systems.size) empty.hidden = true;
      renderGraph(true, result.affiliationChanges);
    }
    applyAttention(result.focusCandidates);
  }

  function requestCatchup() {
    liveReady = false;
    catchupRequested = true;
    if (!polling) schedulePoll(0);
  }

  function ingestLiveActivity(update) {
    if (!Array.isArray(update?.rows) || update.rows.length > MAX_POLL_ROWS ||
        !Number.isSafeInteger(update.next_after_id) || update.next_after_id < 0) return requestCatchup();
    if (!liveReady || polling || catchupRequested) {
      if (bufferedRowCount + update.rows.length > MAX_POLL_ROWS ||
          bufferedActivity.length >= MAX_BUFFERED_ACTIVITY_BATCHES) {
        bufferedActivity = [];
        bufferedRowCount = 0;
        return requestCatchup();
      }
      bufferedActivity.push(update);
      bufferedRowCount += update.rows.length;
      return;
    }
    applyActivityUpdate(update.rows);
    cursor = Math.max(cursor, update.next_after_id, ...update.rows.map((row) =>
      Number.isSafeInteger(row?.id) && row.id > 0 ? row.id : 0));
  }

  function connectLiveActivity(localGeneration) {
    if (typeof dependencies.subscribeActivity !== 'function') return;
    const current = () => !closed && localGeneration === generation;
    liveSource = dependencies.subscribeActivity({
      sourceChange: (value) => {
        if (!current()) return;
        const timing = value?.traffic_grant_age_out_milliseconds;
        if (Number.isInteger(timing) && timing >= 100 && timing <= 15_000) grantActivityTimeoutMs = timing;
        const serverTime = Number(value?.server_time_ms);
        if (Number.isFinite(serverTime) && serverTime > 0) serverTimeOffsetMs = serverTime - Date.now();
        if (typeof value?.collection_enabled === 'boolean') collectionEnabled = value.collection_enabled;
        liveBound = true;
        requestCatchup();
      },
      activityAppend: (value) => current() && ingestLiveActivity(value),
      gap: () => {
        if (!current()) return;
        bufferedActivity = [];
        bufferedRowCount = 0;
        requestCatchup();
      },
      error: () => {
        if (!current()) return;
        liveBound = liveReady = false;
        setStatus('History update delayed', 'error');
        schedulePoll();
      }
    });
  }

  async function poll() {
    if (closed || polling || document.hidden) return schedulePoll();
    polling = true;
    catchupRequested = false;
    const localGeneration = generation;
    const controller = requestController;
    let reseed = false;
    let recovered = false;
    try {
      const bounds = historyBounds();
      let afterId = cursor;
      let watermarkId = null;
      let processed = 0;
      const pollRows = [];
      do {
        const page = await requestForwardPage(afterId, watermarkId, controller.signal, bounds);
        if (closed || localGeneration !== generation) return;
        if (page.reset_required === true || page.reseed_required === true ||
            Number(page.watermark_id) < afterId) {
          throw reseedError('Saved Activity history restarted. Reloading the selected scope.');
        }
        if (watermarkId === null) watermarkId = Number(page.watermark_id) || afterId;
        const capacity = Math.max(0, MAX_POLL_ROWS - processed);
        const rows = page.rows.slice(0, capacity);
        pollRows.push(...rows);
        processed += rows.length;
        const next = Number(page.next_after_id);
        if (!Number.isSafeInteger(next) || next < afterId || page.has_more && next === afterId) {
          throw new Error('Saved activity paging did not advance.');
        }
        afterId = next;
        if (page.rows.length > capacity || page.has_more && processed >= MAX_POLL_ROWS) {
          throw reseedError('More saved activity arrived than one update can safely process. Reloading the selected scope.');
        }
        if (!page.has_more) break;
      } while (!controller.signal.aborted);
      cursor = Math.max(cursor, afterId, watermarkId || 0);
      applyActivityUpdate(pollRows);
      recovered = true;
      setStatus(collectionEnabled ? 'Saved history' : 'Saved history · updates paused',
        collectionEnabled ? 'current' : 'stale');
    } catch (error) {
      if (error?.reseed && !closed && localGeneration === generation) {
        reseed = true;
        setStatus('Reloading saved history', 'stale');
        window.setTimeout(() => void loadSeed(), 0);
      } else if (error?.name !== 'AbortError' && !closed && localGeneration === generation) {
        setStatus('History update delayed', 'error');
      }
    } finally {
      polling = false;
      if (!closed && localGeneration === generation && !reseed) {
        if (!recovered) schedulePoll();
        else if (catchupRequested) schedulePoll(0);
        else if (liveBound) {
          liveReady = true;
          const updates = bufferedActivity;
          bufferedActivity = [];
          bufferedRowCount = 0;
          updates.forEach(ingestLiveActivity);
          if (pollTimer) window.clearTimeout(pollTimer);
          pollTimer = 0;
        } else schedulePoll();
      }
    }
  }

  function setHistoryHours(value) {
    historyHours = value === 24 ? 24 : 1;
    oneHour.setAttribute('aria-pressed', String(historyHours === 1));
    day.setAttribute('aria-pressed', String(historyHours === 24));
    void loadSeed();
  }

  function setMode(value) {
    mode = value === 'manual' ? 'manual' : 'auto';
    auto.setAttribute('aria-pressed', String(mode === 'auto'));
    manual.setAttribute('aria-pressed', String(mode === 'manual'));
    manualGuide.hidden = mode !== 'manual';
    camera.setMode(mode);
    clearAutoFocus();
    lastCameraPhase = mode === 'auto' ? 'roam' : 'disabled';
    renderer?.setMode(mode);
    renderer?.setCameraPhase(mode === 'auto' ? 'roam' : 'disabled');
    if (mode === 'manual') stage.focus({ preventScroll: true });
    else if (selectedSystemKey) renderer?.home(900);
  }

  function keyName(event) {
    return event.key.length === 1 ? event.key.toLowerCase() : event.key;
  }

  function onKeyDown(event) {
    if (mode !== 'manual' || !['w', 'a', 's', 'd', 'q', 'e', 'ArrowLeft', 'ArrowRight', 'ArrowUp',
      'ArrowDown'].includes(keyName(event))) return;
    pressedKeys.add(keyName(event));
    event.preventDefault();
  }

  function onKeyUp(event) {
    pressedKeys.delete(keyName(event));
  }

  function animate(at) {
    animationFrame = 0;
    if (closed) return;
    const delta = Math.min(0.05, Math.max(0, (at - lastAnimationAt) / 1_000));
    lastAnimationAt = at;
    if (mode === 'manual') renderer?.moveManualCamera({
      forward: Number(pressedKeys.has('w')) - Number(pressedKeys.has('s')),
      right: Number(pressedKeys.has('d')) - Number(pressedKeys.has('a')),
      up: Number(pressedKeys.has('e')) - Number(pressedKeys.has('q')),
      yaw: Number(pressedKeys.has('ArrowLeft')) - Number(pressedKeys.has('ArrowRight')),
      pitch: Number(pressedKeys.has('ArrowUp')) - Number(pressedKeys.has('ArrowDown'))
    }, delta);
    const cameraState = camera.update(Date.now());
    if (mode === 'auto' && cameraState.changed && cameraState.phase !== lastCameraPhase) {
      lastCameraPhase = cameraState.phase;
      renderer?.setCameraPhase(cameraState.phase);
      if (cameraState.phase === 'return') {
        autoFocus.dataset.phase = 'return';
        renderer?.home(cameraState.timing.returnMs);
      } else if (cameraState.phase === 'roam') clearAutoFocus();
    }
    animationFrame = requestAnimationFrame(animate);
  }

  oneHour.addEventListener('click', () => historyHours !== 1 && setHistoryHours(1));
  day.addEventListener('click', () => historyHours !== 24 && setHistoryHours(24));
  auto.addEventListener('click', () => mode !== 'auto' && setMode('auto'));
  manual.addEventListener('click', () => mode !== 'manual' && setMode('manual'));
  eventsToggle.addEventListener('click', () => {
    events.hidden = !events.hidden;
    eventsToggle.setAttribute('aria-pressed', String(!events.hidden));
    if (!events.hidden) renderEvents();
  });
  settingsButton.addEventListener('click', openSettings);
  closeEvents.addEventListener('click', () => {
    events.hidden = true;
    eventsToggle.setAttribute('aria-pressed', 'false');
    eventsToggle.focus();
  });
  back.addEventListener('click', () => selectedGroupKey ? enterSystem(selectedSystemKey) : showOverview());
  fullscreen.addEventListener('click', async () => {
    if (!renderer) return;
    if (document.fullscreenElement === layout) await renderer.exitFullscreen();
    else await renderer.enterFullscreen();
  });
  stage.addEventListener('keydown', onKeyDown);
  window.addEventListener('keyup', onKeyUp);
  const onVisibilityChange = () => {
    if (document.hidden) liveReady = false;
    else requestCatchup();
  };
  document.addEventListener('visibilitychange', onVisibilityChange);

  async function initialize() {
    renderer = await createP25Renderer({ host: canvas, mode, signal: dependencies.signal,
      onNodeClick: (selected) => {
        if (selected?.type === 'system' && selectedSystemKey !== selected.systemKey) enterSystem(selected.systemKey);
        else if (selected?.type === 'talkgroup' && selectedSystemKey && selected.id !== selectedGroupKey) {
          enterTalkgroup(selected.id);
        }
      }, onInteraction: () => {
        if (mode === 'auto') {
          camera.cancel(Date.now());
          clearAutoFocus();
          lastCameraPhase = 'roam';
          renderer?.setCameraPhase('roam');
        }
      } });
    if (closed) return renderer.dispose();
    void loadSeed();
  }

  const close = () => {
    if (closed) return;
    closed = true;
    generation += 1;
    requestController?.abort();
    liveSource?.close();
    liveSource = null;
    bufferedActivity = [];
    bufferedRowCount = 0;
    if (pollTimer) window.clearTimeout(pollTimer);
    if (highlightTimer) window.clearTimeout(highlightTimer);
    if (introTimer) window.clearTimeout(introTimer);
    if (animationFrame) cancelAnimationFrame(animationFrame);
    window.removeEventListener('keyup', onKeyUp);
    shortViewport.removeEventListener('change', ensureEventsSpace);
    eventsResizeObserver.disconnect();
    document.removeEventListener('visibilitychange', onVisibilityChange);
    eventsModal?.close();
    renderer?.dispose();
  };
  dependencies.signal?.addEventListener?.('abort', close, { once: true });
  animationFrame = requestAnimationFrame(animate);
  void initialize();

  return Object.freeze({ element: layout, close });
}

export { createP25Visualizer };
