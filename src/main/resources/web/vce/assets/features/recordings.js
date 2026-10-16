import { createDualRange } from '../core/dual-range.js?v=1';
import { systemLabel, systemName } from '../core/system-labels.js?v=1';
import { formatP25RadioIdentifier } from '../core/radio-labels.js?v=4';
import { formatSourceName } from '../core/source-names.js?v=1';

const CALLS = '/api/v1/recordings/calls';
const SUGGESTIONS = '/api/v1/recordings/suggestions';
const STATUS = '/api/v1/recordings/status';
const ADMIN = '/api/v1/admin/recordings';
const PAGE_SIZE = 25;
const SUGGESTION_DELAY_MS = 240;

const SEARCH_FIELDS = Object.freeze([
  ['system_key', 'Radio system', 'system', 'System name or key'],
  ['site', 'Site', 'site', 'Channel name, site name, or ID'],
  ['talkgroup_id', 'Talkgroup', 'talkgroup', 'Talkgroup alias or ID'],
  ['radio_id', 'Radio ID', 'radio', 'Radio alias, ID, or OTA name'],
  ['channel_id', 'Analog channel', 'channel', 'Channel name']
]);

function queryPath(root, values = {}) {
  const query = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) query.set(key, String(value));
  });
  return query.size ? `${root}?${query}` : root;
}

export function recordingSuggestionFilters(filters, suggestion) {
  const next = { ...filters };
  if (!suggestion?.name_query) return next;
  for (const key of ['radio_id', 'radio_identity_key', 'radio_min', 'radio_max', 'talkgroup_id',
    'talkgroup_min', 'talkgroup_max', 'alias_list_id']) next[key] = '';
  next.q = String(suggestion.name_query);
  if (suggestion.system_key) next.system_key = String(suggestion.system_key);
  return next;
}

function value(row, ...names) {
  for (const name of names) {
    const candidate = row?.[name];
    if (candidate !== null && candidate !== undefined && candidate !== '') return candidate;
  }
  return null;
}

// Convert recorded identities into search constraints, never display names or detail-page routes.
export function recordingResultFilter(row, reference, field = '') {
  const system = value(row, 'system_key', 'radio_system_key') || row?.radio_system_entity_ref?.key;
  const channel = value(row, 'channel_id') || row?.channel_entity_ref?.key;
  const scope = system ? { system_key: String(system) } : channel ? { channel_id: String(channel) } : null;
  if (/home.system/i.test(field)) return null;
  if (field === 'site' || field === 'site_entity_ref') {
    const site = row?.audio_from || row;
    const coordinates = {
      wacn: value(site, 'wacn'), sysid: value(site, 'system_id', 'sysid'),
      rfss: value(site, 'rfss', 'rfss_id'), site_id: value(site, 'site_id')
    };
    if (Object.values(coordinates).some(item => item === null || !Number.isInteger(Number(item)) || Number(item) < 0)) return null;
    return { key: 'site', filters: { ...scope, site: '', ...Object.fromEntries(
      Object.entries(coordinates).map(([key, item]) => [key, String(item)])) } };
  }
  if (field === 'alias_list_entity_ref') {
    const id = value(row, 'alias_list_id');
    return id !== null && Number(id) > 0 ? { key: 'alias_list_id', filters: { alias_list_id: String(id) } } : null;
  }
  if (reference?.kind === 'radio_system' || field === 'system' || field === 'radio_system_entity_ref') {
    const key = reference?.key || system;
    return key && (!system || key === system) ? { key: 'system_key', filters: { system_key: String(key) } } : null;
  }
  if (reference?.kind === 'channel' || field === 'channel' || field === 'channel_entity_ref') {
    const key = reference?.key || channel;
    return key ? { key: 'channel_id', filters: { channel_id: String(key) } } : null;
  }
  const kind = reference?.kind || (field === 'source' || field === 'source_entity_ref' ? 'radio' :
    field === 'target' || field === 'target_entity_ref' ?
      (value(row, 'destination_radio_id') !== null ? 'radio' : 'talkgroup') : '');
  if (!scope || !['radio', 'talkgroup', 'patch_group'].includes(kind)) return null;
  const tuple = /^v1-([rgp])-(x|[0-9a-f]{5})-(x|[0-9a-f]{3})-([1-9][0-9]*)$/.exec(reference?.identity_key || '');
  if (reference?.identity_key && (!tuple || (tuple[2] === 'x') !== (tuple[3] === 'x') ||
      Number(tuple[4]) > 0xFFFFFF || (kind === 'radio' ? tuple[1] !== 'r' : !['g', 'p'].includes(tuple[1])))) return null;
  if (reference?.radio_system_key && system && reference.radio_system_key !== system) return null;
  if (kind !== 'radio') {
    const target = field === 'target' || field === 'target_entity_ref';
    if (/^p25:/.test(system || '') && tuple?.[2] !== 'x' && tuple &&
        `p25:${tuple[2]}:${tuple[3]}` !== system) return null;
    const home = ['target_home_wacn', 'target_home_system_id', 'target_home_id'].map(key => value(row, key));
    if (target && /^p25:/.test(system || '') && home.some(item => item !== null) &&
        (home.some(item => item === null) || Number(home[0]) !== Number.parseInt(system.slice(4, 9), 16) ||
          Number(home[1]) !== Number.parseInt(system.slice(10), 16) ||
          Number(home[2]) !== Number(value(row, 'talkgroup_id', 'group_id')))) return null;
    const id = target ? value(row, 'talkgroup_id', 'group_id') ?? tuple?.[4] :
      tuple?.[4] ?? value(row, 'talkgroup_id', 'group_id');
    return id !== null && id !== undefined && Number(id) > 0 ? { key: 'talkgroup_id', filters: {
      ...scope, talkgroup_id: String(id), talkgroup_min: '', talkgroup_max: ''
    } } : null;
  }
  const target = field === 'target' || field === 'target_entity_ref';
  const source = field === 'source' || field === 'source_entity_ref';
  const prefix = target ? 'target' : 'source';
  const id = target ? value(row, 'destination_radio_id', 'target_id') : source ?
    value(row, 'source_id', 'radio_id') : tuple?.[4] ?? value(row, 'source_id', 'radio_id');
  const p25 = /^p25:[0-9a-f]{5}:[0-9a-f]{3}$/.test(system || '');
  let exact = tuple && tuple[2] !== 'x' && tuple[3] !== 'x' ? reference.identity_key : null;
  if (p25 && !exact) {
    const homeWacn = source || target ? value(row, `${prefix}_home_wacn`) : null;
    const homeSystem = source || target ? value(row, `${prefix}_home_system_id`) : null;
    const homeId = source || target ? value(row, `${prefix}_home_id`) : null;
    if ([homeWacn, homeSystem, homeId].every(item => item !== null)) {
      exact = `v1-r-${Number(homeWacn).toString(16).padStart(5, '0')}-${Number(homeSystem).toString(16).padStart(3, '0')}-${homeId}`;
    } else if ([homeWacn, homeSystem, homeId].some(item => item !== null)) return null;
    else if (id !== null && Number(id) > 0) exact = `v1-r-${system.slice(4, 9)}-${system.slice(10)}-${id}`;
  }
  if (p25 && exact) return { key: 'radio_id', filters: {
    ...scope, radio_identity_key: exact, radio_id: '', radio_min: '', radio_max: ''
  } };
  const nativeId = id ?? tuple?.[4];
  return nativeId !== null && nativeId !== undefined && Number(nativeId) > 0 ? { key: 'radio_id', filters: {
    ...scope, radio_id: String(nativeId), radio_identity_key: '', radio_min: '', radio_max: ''
  } } : null;
}

function recordingRadioId(row, prefix, id) {
  if (!/^(?:APCO25|P25)(?:_|$)/i.test(String(row?.protocol || ''))) return id;
  const identity = {
    wacn: row?.[`${prefix}_home_wacn`],
    system_id: row?.[`${prefix}_home_system_id`],
    subscriber_id: row?.[`${prefix}_home_id`]
  };
  const home = row?.[`${prefix}_home_system`];
  const homeKey = home?.radio_system_key || home?.key ||
    (identity.wacn != null && identity.system_id != null ?
      `p25:${Number(identity.wacn).toString(16).padStart(5, '0')}:${
        Number(identity.system_id).toString(16).padStart(3, '0')}` : '');
  return formatP25RadioIdentifier(identity, {
    servingSystemKey: row?.system_key || row?.radio_system_key,
    homeSystemName: row?.[`${prefix}_home_system_name`] || systemName(home) || systemName(homeKey),
    workingId: id
  }) || id;
}

function label(row) {
  const group = value(row, 'talkgroup_alias', 'group_alias', 'talkgroup_name');
  const groupId = value(row, 'talkgroup_id', 'group_id');
  const destination = value(row, 'destination_radio_id');
  const destinationAlias = value(row, 'destination_radio_alias', 'destination_radio_ota_alias');
  const channel = value(row, 'channel_name', 'analog_channel_name');
  if (['NBFM', 'AM'].includes(value(row, 'protocol'))) return channel || 'Recorded channel';
  return group || (groupId !== null ? `Talkgroup ${groupId}` :
    (destination !== null ? `Direct to ${destinationAlias || `Radio ${recordingRadioId(row, 'target', destination)}`}` : channel)) ||
    systemName(row) || 'Recorded call';
}

function sourceLabel(row, mode) {
  const source = value(row, 'source_alias', 'radio_alias');
  const id = recordingRadioId(row, 'source', value(row, 'source_id', 'radio_id'));
  const ota = value(row, 'source_ota_alias', 'source_ota_ta', 'ota_alias', 'talker_alias');
  return [formatSourceName(source, ota, mode), id !== null ? `Radio ${id}` : null].filter(Boolean).join(' · ');
}

function sourceBrief(row, mode) {
  const id = recordingRadioId(row, 'source', value(row, 'source_id', 'radio_id'));
  return formatSourceName(value(row, 'source_alias', 'radio_alias'),
    value(row, 'source_ota_alias', 'source_ota_ta', 'ota_alias', 'talker_alias'), mode) ||
    (id !== null ? `Radio ${id}` : '');
}

function millis(row) {
  const numeric = Number(value(row, 'start_ms', 'started_at_ms', 'call_start_ms'));
  return Number.isFinite(numeric) ? numeric : 0;
}

function duration(row) {
  const number = Number(value(row, 'duration_ms', 'call_duration_ms'));
  if (!Number.isFinite(number) || number < 0) return '';
  const seconds = Math.round(number / 1000);
  return `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
}

function dateTime(timestamp, dateOnly = false) {
  if (!timestamp) return '';
  return new Intl.DateTimeFormat(undefined, dateOnly ?
    { year: 'numeric', month: 'short', day: 'numeric' } :
    { year: 'numeric', month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit', second: '2-digit' })
    .format(new Date(timestamp));
}

function timeOnly(timestamp) {
  if (!timestamp) return '';
  return new Intl.DateTimeFormat(undefined, { hour: 'numeric', minute: '2-digit', second: '2-digit' })
    .format(new Date(timestamp));
}

function localDateTime(milliseconds) {
  if (!milliseconds) return '';
  const date = new Date(Number(milliseconds));
  if (!Number.isFinite(date.getTime())) return '';
  const pad = (number) => String(number).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function numberOrNull(input) {
  const text = String(input || '').trim();
  if (!text) return null;
  const number = Number(text);
  return Number.isFinite(number) ? number : null;
}

function frequency(row) {
  const hertz = Number(value(row, 'frequency_hz', 'winner_frequency_hz'));
  return Number.isFinite(hertz) && hertz > 0 ? `${(hertz / 1_000_000).toFixed(4)} MHz` : '';
}

function siteText(site) {
  if (!site) return '';
  if (typeof site === 'string') return site;
  const name = value(site, 'site_name', 'name', 'site_key');
  if (name) return String(name);
  const rfss = value(site, 'rfss_id', 'rfss');
  const id = value(site, 'site_id');
  return [rfss !== null ? `RFSS ${rfss}` : '', id !== null ? `Site ${id}` : '']
    .filter(Boolean).join(' · ');
}

function winningSite(row) {
  const source = value(row, 'audio_from');
  return value(row, 'site_name', 'winner_site_name') || siteText(source);
}

function prettify(valueToFormat) {
  return String(valueToFormat || '').replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (match) => match.toUpperCase());
}

function protocolLabel(protocol) {
  return ({ APCO25: 'P25 Phase 1', APCO25_PHASE2: 'P25 Phase 2',
    NBFM: 'Analog FM', AM: 'Analog AM' })[protocol] || prettify(protocol);
}

function callKey(row) {
  return `${millis(row)}:${String(row?.id ?? '')}`;
}

function afterCall(row, selected) {
  if (millis(row) !== millis(selected)) return millis(row) > millis(selected);
  const id = Number(row?.id);
  const selectedId = Number(selected?.id);
  return Number.isFinite(id) && Number.isFinite(selectedId) ? id > selectedId :
    String(row?.id ?? '') > String(selected?.id ?? '');
}

function appendFact(node, host, name, detail) {
  if (detail === null || detail === undefined || detail === '') return;
  const term = node('dt', '', name);
  const description = node('dd', '');
  description.append(detail);
  host.append(term, description);
}

function button(node, text, action, className = 'ui-button ui-button-secondary') {
  const control = node('button', className, text);
  control.type = 'button';
  if (action) control.addEventListener('click', action);
  return control;
}

function select(node, options, labelText) {
  const control = node('select', 'ui-select');
  control.setAttribute('aria-label', labelText);
  options.forEach(([key, name]) => {
    const choice = node('option', '', name);
    choice.value = key;
    control.append(choice);
  });
  return control;
}

// Reuse map: the shared page header and section shells frame a compact data
// workspace with desktop filter/results columns; ui-input, ui-select, ui-dual-range,
// ui-button, ui-choice-card, ui-feedback, and
// the application's modal foundation own controls and feedback. This module
// owns only recordings geometry and the historical audio lifecycle. Administration
// reuses section shells, ui-field/ui-toggle, ui-action-row, ui-admin-facts, and
// ui-section-disclosure in desktop/mobile and light/dark layouts.
export function createRecordingsFeature(deps) {
  const { node, requestJson, openReadOnlyModal, section, pageHeader, beginPage,
    captureRenderContext, renderIsCurrent, content, isPrimaryAdmin, canViewRadio,
    entityRefHref, stopLiveAudio, href, anchor, uiToggleField, metrics, browsingWorkflows, modalFooter, closeReadOnlyModal,
    getSourceNameDisplay = () => 'talker_alias' } = deps;
  const search = {
    q: '', transcript: '', from_ms: '', to_ms: '', system_key: '', site: '', talkgroup_id: '', radio_id: '',
    channel_id: '', alias_list_id: '', radio_identity_key: '', min_duration_ms: '', max_duration_ms: '', frequency_hz: '',
    talkgroup_min: '', talkgroup_max: '', radio_min: '', radio_max: '',
    call_type: '', voice_type: '', protocol: '', wacn: '', sysid: '', rfss: '', site_id: ''
  };
  const selectedSuggestions = new Map();
  const selection = new Set();
  let currentResults = [];
  let currentFilters = {};
  let appliedSuggestions = new Map();
  let appliedRange = '24h';
  let sharedDetails = new Map();
  let sharedDate = '';
  let currentPage = 0;
  let cursors = [null];
  let nextCursor = null;
  let resultTotal = null;
  let pageHost = null;
  let resultHost = null;
  let resultStatus = null;
  let countHost = null;
  let countText = null;
  let pageSelect = null;
  let pagerHost = null;
  let sharedHost = null;
  let selectedFiltersHost = null;
  let searchHint = null;
  let searchInputs = new Map();
  let filterCountHost = null;
  let selectedHost = null;
  let sortControl = null;
  let searchRequest = 0;
  let searchSignal = null;
  let player = null;

  const makeNotice = (text, type = '') => {
    const notice = node('div', `ui-feedback${type ? ` ui-feedback-${type}` : ''}`, text);
    notice.setAttribute('role', type === 'error' ? 'alert' : 'status');
    return notice;
  };

  const entityLink = (text, reference, row, field) => {
    if (pageHost?.isConnected && row) {
      const match = recordingResultFilter(row, reference, field);
      if (!match) return String(text);
      const values = Object.fromEntries(Object.entries({ ...currentFilters, ...match.filters,
        sort: sortControl?.value || 'desc', recording_filters: '1' }).filter(([, item]) => item !== ''));
      const link = anchor(String(text), href('recordings', values));
      link.title = `Filter recordings by ${text}`;
      link.addEventListener('click', (event) => {
        if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
        event.preventDefault();
        const apply = () => {
          Object.keys(search).forEach(key => { search[key] = currentFilters[key] ?? ''; });
          search.range = appliedRange;
          selectedSuggestions.clear();
          appliedSuggestions.forEach((suggestion, key) => selectedSuggestions.set(key, suggestion));
          Object.assign(search, match.filters);
          selectedSuggestions.set(match.key, { label: String(text),
            appliedKeys: match.key === 'site' ? ['wacn', 'sysid', 'rfss', 'site_id'] :
              match.filters.radio_identity_key ? ['radio_identity_key'] : [] });
          if (match.filters.system_key && match.key !== 'system_key') selectedSuggestions.set('system_key', {
            label: systemLabel(row), id: match.filters.system_key
          });
          currentFilters = effectiveFilters();
          appliedSuggestions = new Map(selectedSuggestions);
          pageHost.querySelector('.recordings-search')?.replaceWith(makeSearchForm());
          cursors = [null];
          nextCursor = null;
          currentPage = 0;
          selection.clear();
          drawSelection();
          drawSelectedFilters();
          void loadPage(0);
        };
        if (closeReadOnlyModal && !closeReadOnlyModal(false, false, closed => { if (closed) apply(); })) return;
        apply();
      });
      return link;
    }
    const target = canViewRadio?.() ? entityRefHref?.(reference) : null;
    return target ? anchor(String(text), target) : String(text);
  };

  function query(filters, extra = {}) {
    return queryPath(CALLS, { ...filters, ...extra, limit: PAGE_SIZE });
  }

  function effectiveFilters() {
    return Object.fromEntries(Object.entries(search).filter(([key, item]) =>
      key !== 'range' && item !== '' && !(key === 'site' && selectedSuggestions.get('site')?.appliedKeys?.length)));
  }

  function applySuggestion(key, suggestion) {
    selectedSuggestions.get(key)?.appliedKeys?.forEach((applied) => { search[applied] = ''; });
    if (suggestion.name_query) {
      const sameSystem = search.system_key === String(suggestion.system_key);
      const existingSystem = selectedSuggestions.get('system_key');
      Object.assign(search, recordingSuggestionFilters(search, suggestion));
      for (const identityKey of ['radio_id', 'talkgroup_id']) {
        selectedSuggestions.delete(identityKey);
        const input = searchInputs.get(identityKey);
        if (input && identityKey !== key) input.value = '';
      }
      selectedSuggestions.set(key, { ...suggestion, appliedKeys: ['q'] });
      const queryInput = searchInputs.get('q');
      if (queryInput) queryInput.value = search.q;
      if (suggestion.system_key && (!sameSystem || !existingSystem || suggestion.system_name)) {
        selectedSuggestions.set('system_key', {
        kind: 'system', id: suggestion.system_key, label: systemLabel(suggestion)
        });
      }
      return;
    }
    if (key === 'site') {
      const hasNumericSite = value(suggestion, 'rfss', 'rfss_id') !== null &&
        value(suggestion, 'site_id') !== null;
      const appliedKeys = [];
      for (const [target, names] of [
        ['wacn', ['wacn']], ['sysid', ['sysid', 'system_id']],
        ['rfss', ['rfss', 'rfss_id']], ['site_id', ['site_id']],
        ['channel_id', ['channel_id']]
      ]) {
        if (target === 'channel_id' && hasNumericSite) continue;
        const found = value(suggestion, ...names);
        if (found === null) continue;
        search[target] = String(found);
        appliedKeys.push(target);
      }
      search.site = String(suggestion.label || suggestion.id || '');
      if (!appliedKeys.length) search.site = String(suggestion.id || suggestion.label || '');
      selectedSuggestions.set(key, { ...suggestion, appliedKeys });
      return;
    }
    const range = key === 'talkgroup_id' && suggestion.kind === 'talkgroup_range' ?
      ['talkgroup_min', 'talkgroup_max'] :
      (key === 'radio_id' && suggestion.kind === 'radio_range' ? ['radio_min', 'radio_max'] : null);
    if (range) {
      search[key] = '';
      search[range[0]] = String(suggestion.min_id);
      search[range[1]] = String(suggestion.max_id);
      selectedSuggestions.set(key, { ...suggestion, appliedKeys: range });
    } else {
      search[key] = String(key === 'system_key' ?
        (suggestion.system_key ?? suggestion.id ?? suggestion.label ?? '') :
        (suggestion.id ?? suggestion.label ?? ''));
      selectedSuggestions.set(key, suggestion);
    }
    if (['radio_id', 'talkgroup_id'].includes(key) && suggestion.system_key) {
      const sameSystem = search.system_key === String(suggestion.system_key);
      const existingSystem = selectedSuggestions.get('system_key');
      search.system_key = String(suggestion.system_key);
      if (!sameSystem || !existingSystem || suggestion.system_name) {
        selectedSuggestions.set('system_key', {
          kind: 'system', id: suggestion.system_key, label: systemLabel(suggestion)
        });
      }
    }
  }

  function audioUrl(row) {
    return `${CALLS}/${encodeURIComponent(String(row.id))}/audio`;
  }

  const playbackObservers = new Set();
  const maximumSharedCalls = 100;
  let sharedQueueRequest = 0;
  let clickMode = 'queue';
  let queueEntrySequence = 0;

  function queueEntry(row) {
    return { ...row, _queueEntryKey: `recording-entry-${++queueEntrySequence}` };
  }

  function queuedIndex(state, reference) {
    const key = String(reference);
    return state.queue.findIndex((row) => row._queueEntryKey === key || String(row.id) === key);
  }

  function recordedPlayer() {
    if (player) return player;
    const audio = new Audio();
    audio.preload = 'metadata';
    player = { audio, current: null, queue: [], history: [], mode: 'idle', continuation: null,
      loading: false, pendingStart: false, stopped: true, token: 0, detailsToken: 0, detailsLoading: false, detailsError: '', status: '' };
    ['timeupdate', 'loadedmetadata', 'durationchange', 'play', 'pause', 'volumechange']
      .forEach((event) => audio.addEventListener(event, drawQueue));
    audio.addEventListener('ended', () => void advance());
    audio.addEventListener('error', () => {
      if (!player?.current) return;
      player.status = 'This recording could not be played. Moving to the next call.';
      void advance();
    });
    return player;
  }

  function playbackState() {
    const state = recordedPlayer();
    const measuredDuration = Number(state.audio.duration);
    const callDuration = Number(value(state.current, 'duration_ms', 'call_duration_ms')) / 1000;
    const total = Number.isFinite(measuredDuration) && measuredDuration > 0 ? measuredDuration :
      (Number.isFinite(callDuration) && callDuration > 0 ? callDuration : 0);
    return { current: state.current, queue: [...state.queue], history: [...state.history],
      playing: Boolean(state.current && !state.stopped && !state.audio.paused),
      paused: Boolean(state.current && !state.stopped && state.audio.paused), stopped: state.stopped, loading: state.loading,
      status: state.status, duration: total, currentTime: Number(state.audio.currentTime) || 0,
      volume: state.audio.volume, mode: state.mode, clickMode,
      continuation: Boolean(state.continuation && !state.continuation.done),
      canNext: Boolean(state.queue.length || (state.continuation && !state.continuation.done)),
      canPrevious: Boolean(state.history.length), detailsLoading: state.detailsLoading,
      detailsError: state.detailsError };
  }

  function drawQueue() {
    if (!playbackObservers.size) return;
    const snapshot = playbackState();
    playbackObservers.forEach((observer) => {
      try { observer(snapshot); } catch (_error) { /* Playback must outlive its presentation. */ }
    });
  }

  function subscribeState(observer) {
    if (typeof observer !== 'function') return () => {};
    playbackObservers.add(observer);
    observer(playbackState());
    return () => playbackObservers.delete(observer);
  }

  async function toggleAudio() {
    const state = recordedPlayer();
    if (state.current) {
      if (state.audio.paused || state.stopped) {
        const token = state.token;
        const current = state.current;
        await stopLiveAudio();
        if (token !== state.token || String(current.id) !== String(state.current?.id)) return;
        if (state.stopped) {
          state.audio.src = audioUrl(current);
          state.audio.currentTime = 0;
          state.stopped = false;
        }
        state.status = state.mode === 'continue' && state.continuation ? 'Continuing through matching calls' : '';
        try { await state.audio.play(); }
        catch (_error) {
          if (state.token === token) state.status = 'Playback could not start. Try again.';
        }
      } else state.audio.pause();
      drawQueue();
      return;
    }
    await advance();
  }

  function stopAudio() {
    sharedQueueRequest++;
    if (!player) return;
    const state = player;
    state.token++;
    state.audio.pause();
    state.audio.removeAttribute('src');
    state.audio.load();
    state.continuation = null;
    state.loading = false;
    state.pendingStart = false;
    state.stopped = true;
    state.status = 'Stopped.';
    drawQueue();
  }

  function resetAudio() {
    stopAudio();
    if (!player) return;
    player.detailsToken++;
    player.current = null;
    player.queue = [];
    player.history = [];
    player.mode = 'idle';
    player.detailsLoading = false;
    player.detailsError = '';
    player.status = '';
    drawQueue();
  }

  async function loadCurrentDetails(state, row) {
    const token = ++state.detailsToken;
    state.detailsLoading = true;
    state.detailsError = '';
    drawQueue();
    try {
      const result = await requestJson(`${CALLS}/${encodeURIComponent(String(row.id))}`, { page: false });
      if (token !== state.detailsToken || String(state.current?.id) !== String(row.id)) return;
      state.current = { ...row, ...(result?.call || result || {}),
        ...(result?.transcription ? { transcription: result.transcription } : {}) };
    } catch (_error) {
      if (token === state.detailsToken) state.detailsError = 'Call details are unavailable. Try this call again.';
    } finally {
      if (token === state.detailsToken) {
        state.detailsLoading = false;
        drawQueue();
      }
    }
  }

  function loadContinuation(state) {
    const continuation = state.continuation;
    if (!continuation || continuation.done) return Promise.resolve();
    if (continuation.loading) return continuation.request;
    continuation.loading = true;
    continuation.request = (async () => {
      try {
        const result = await requestJson(query(continuation.filters, {
          from_ms: continuation.fromMs, sort: 'asc', cursor: continuation.cursor
        }), { page: false });
        if (state.continuation !== continuation) return;
        for (const row of Array.isArray(result?.calls) ? result.calls : []) {
          if (!afterCall(row, continuation.anchor)) continue;
          if (continuation.seen.has(callKey(row))) continue;
          continuation.seen.add(callKey(row));
          state.queue.push(queueEntry(row));
        }
        continuation.cursor = result?.next_cursor || null;
        continuation.done = !continuation.cursor;
        if (continuation.done && !state.queue.length) state.continuation = null;
        drawQueue();
      } catch (_error) {
        if (state.continuation === continuation) {
          continuation.done = true;
          state.status = 'Could not load the next matching calls.';
          drawQueue();
        }
      } finally {
        continuation.loading = false;
      }
    })();
    return continuation.request;
  }

  async function advance({ remember = true } = {}) {
    const state = recordedPlayer();
    if (state.loading) return;
    const token = state.token;
    state.loading = true;
    state.pendingStart = true;
    drawQueue();
    try {
      state.audio.pause();
      while (!state.queue.length && state.continuation) {
        await loadContinuation(state);
        if (token !== state.token) return;
        if (state.continuation?.loading || state.continuation?.done) break;
      }
      const next = state.queue[0];
      if (!next) {
        state.stopped = true;
        state.continuation = null;
        state.audio.removeAttribute('src');
        state.audio.load();
        if (state.status !== 'Could not load the next matching calls.') state.status = 'Playback finished.';
        drawQueue();
        return;
      }
      await stopLiveAudio();
      if (token !== state.token) return;
      state.queue.shift();
      if (remember && state.current) {
        state.history.push(state.current);
        if (state.history.length > maximumSharedCalls) state.history.shift();
      }
      state.current = next;
      state.pendingStart = false;
      state.stopped = false;
      state.status = state.mode === 'continue' ? 'Continuing through matching calls' : '';
      state.audio.src = audioUrl(next);
      state.audio.currentTime = 0;
      void loadCurrentDetails(state, next);
      drawQueue();
      try { await state.audio.play(); }
      catch (_error) {
        if (token === state.token) state.status = 'Playback could not start. Press Play to retry.';
      }
      if (token === state.token && state.continuation && state.queue.length < 8) void loadContinuation(state);
    } finally {
      if (token === state.token) {
        state.loading = false;
        state.pendingStart = false;
        drawQueue();
      }
    }
  }

  function playbackAction(row, action = 'once') {
    if (!row || row.id === null || row.id === undefined) return;
    const state = recordedPlayer();
    sharedQueueRequest++;
    if (action === 'queue') {
      state.queue.push(queueEntry(row));
      state.mode = state.mode === 'idle' ? 'queue' : state.mode;
      state.status = 'Call added to the recording queue.';
      drawQueue();
      return;
    }
    state.token++;
    state.detailsToken++;
    state.audio.pause();
    state.current = null;
    state.queue = [queueEntry(row)];
    state.history = [];
    state.loading = false;
    state.pendingStart = false;
    state.detailsLoading = false;
    state.detailsError = '';
    state.continuation = action === 'continue' ? {
      filters: { ...currentFilters }, anchor: row, fromMs: millis(row), cursor: null,
      seen: new Set([callKey(row)]), loading: false, done: false
    } : null;
    state.mode = action;
    drawQueue();
    void advance();
  }

  function seekAudio(seconds) {
    const state = recordedPlayer();
    const target = Number(seconds);
    const total = playbackState().duration;
    if (!state.current || !Number.isFinite(target) || !total) return;
    state.audio.currentTime = Math.max(0, Math.min(total, target));
    drawQueue();
  }

  function previousAudio() {
    const state = recordedPlayer();
    const previous = state.history.pop();
    if (!previous) return;
    state.token++;
    state.loading = false;
    state.pendingStart = false;
    if (state.current) state.queue.unshift(state.current);
    state.queue.unshift(previous);
    void advance({ remember: false });
  }

  function clearQueue() {
    const state = recordedPlayer();
    sharedQueueRequest++;
    state.token++;
    state.loading = false;
    state.pendingStart = false;
    state.queue = [];
    state.continuation = null;
    state.status = 'Recording queue cleared.';
    drawQueue();
  }

  function queueHref() {
    const state = recordedPlayer();
    const ids = [state.current, ...state.queue].filter(Boolean).map((row) => String(row.id));
    if (!ids.length || ids.length > maximumSharedCalls || ids.some((id) => !/^[1-9]\d{0,18}$/.test(id))) return null;
    return href('recordings', { recording_queue: ids.join(',') });
  }

  async function loadSharedQueue(params) {
    const raw = typeof params === 'string' ? new URLSearchParams(params).get('recording_queue') :
      params?.get ? params.get('recording_queue') : params?.recording_queue;
    if (!raw) return false;
    const ids = String(raw).split(',');
    if (ids.length > maximumSharedCalls || ids.some((id) => !/^[1-9]\d{0,18}$/.test(id))) return false;
    const token = ++sharedQueueRequest;
    const state = recordedPlayer();
    state.status = 'Loading shared recording queue…';
    drawQueue();
    const rows = [];
    let unavailable = 0;
    // Bound concurrent detail reads as well as the URL's call count.
    for (let offset = 0; offset < ids.length; offset += 5) {
      const results = await Promise.allSettled(ids.slice(offset, offset + 5).map(async (id) => {
        const detail = await requestJson(`${CALLS}/${encodeURIComponent(id)}`, { page: false });
        const call = detail?.call || detail;
        if (!call || String(call.id) !== id) throw new Error('Recording unavailable');
        return { ...call, ...(detail?.transcription ? { transcription: detail.transcription } : {}) };
      }));
      if (token !== sharedQueueRequest) return false;
      results.forEach((result) => {
        if (result.status === 'fulfilled') rows.push(result.value);
        else unavailable++;
      });
    }
    state.queue.push(...rows.map(queueEntry));
    state.mode = state.mode === 'idle' ? 'queue' : state.mode;
    state.status = unavailable ? `${rows.length} calls queued; ${unavailable} calls are unavailable.` :
      `${rows.length} calls added to the recording queue. Press Play to start.`;
    drawQueue();
    return rows.length > 0;
  }

  const playback = {
    subscribeState, viewState: playbackState, toggle: toggleAudio, next: advance, previous: previousAudio,
    stop: stopAudio, reset: resetAudio, seek: seekAudio, skipSeconds: (seconds) => seekAudio(playbackState().currentTime + Number(seconds)),
    setVolume: (volume) => {
      if (!Number.isFinite(Number(volume))) return;
      recordedPlayer().audio.volume = Math.max(0, Math.min(1, Number(volume)));
      drawQueue();
    },
    clearQueue,
    setClickMode: (mode) => {
      if (['queue', 'once', 'continue'].includes(mode)) { clickMode = mode; drawQueue(); }
    },
    queueHref, loadSharedQueue, renderDetails, renderTranscript, play: playbackAction,
    removeQueued: (reference) => {
      const state = recordedPlayer();
      const index = queuedIndex(state, reference);
      if (index < 0) return;
      const restart = state.loading && state.pendingStart;
      if (restart) {
        state.token++;
        state.loading = false;
        state.pendingStart = false;
      }
      state.queue.splice(index, 1);
      drawQueue();
      if (restart) void advance();
    },
    playQueued: (reference) => {
      const state = recordedPlayer();
      const index = queuedIndex(state, reference);
      if (index < 0) return;
      // Invalidate an in-flight start before reordering. It must neither consume
      // the selected row nor clear the loading state of the replacement start.
      state.token++;
      state.loading = false;
      state.pendingStart = false;
      const [row] = state.queue.splice(index, 1);
      state.queue.unshift(row);
      void advance();
    }
  };

  function choosePlayback(row) {
    const body = node('div', 'recordings-playback-choices');
    body.append(node('p', '', `${dateTime(millis(row))} · ${label(row)}`));
    const choices = node('div', 'recordings-choice-options');
    let modal;
    for (const [mode, title, description] of [
      ['once', 'Play once', 'Play this call, then stop.'],
      ['continue', 'Continue from here', 'Play this call, then newer matching calls in time order.'],
      ['queue', 'Add to queue', 'Add this call to the recording queue without starting it.']
    ]) {
      const choice = button(node, '', () => {
        modal.close();
        playbackAction(row, mode);
      }, 'ui-choice-card recordings-choice-option');
      choice.append(node('strong', '', title), node('small', 'ui-field-detail', description));
      choice.title = description;
      choices.append(choice);
    }
    body.append(choices);
    modal = openReadOnlyModal('Play recording', body, {
      id: `play-recording-${row.id}`, className: 'recordings-playback-modal'
    });
    const preferredIndex = ['once', 'continue', 'queue'].indexOf(clickMode);
    const focusPreferred = () => (choices.children?.[preferredIndex] || choices.firstElementChild)?.focus();
    if (modal?.ready) void modal.ready.then((ready) => { if (ready) focusPreferred(); });
    else if (modal) focusPreferred();
  }

  function hexIdentity(identity, width) {
    if (identity === null || identity === undefined || identity === '') return '';
    const numeric = Number(identity);
    return Number.isInteger(numeric) && numeric >= 0 ? numeric.toString(16).toUpperCase().padStart(width, '0') :
      String(identity);
  }

  function fileSize(bytes) {
    if (bytes === null || bytes === undefined || bytes === '') return '';
    const count = Number(bytes);
    if (!Number.isFinite(count) || count < 0) return '';
    return count < 1024 ? `${count} bytes` : count < 1_048_576 ? `${(count / 1024).toFixed(1)} KB` :
      `${(count / 1_048_576).toFixed(1)} MB`;
  }

  function receivedSites(row) {
    const sites = value(row, 'also_received_on', 'also_received_sites');
    if (!Array.isArray(sites) || !sites.length) return '';
    const list = node('span', '');
    sites.forEach((site) => {
      const text = siteText(site);
      if (!text) return;
      if (list.childNodes.length) list.append(', ');
      list.append(entityLink(text, site?.entity_ref, { ...row, audio_from: site }, 'site'));
    });
    return list.childNodes.length ? list : '';
  }

  function patchMembers(row) {
    const members = value(row, 'patch_members', 'patch_talkgroups');
    if (!Array.isArray(members) || !members.length) return '';
    const list = node('span', '');
    members.forEach((member) => {
      const id = typeof member === 'object' ? value(member, 'id', 'talkgroup_id') : member;
      const name = typeof member === 'object' ? value(member, 'alias', 'name', 'ota_alias') : null;
      if (id === null && !name) return;
      if (list.childNodes.length) list.append(', ');
      const text = name ? `${name}${id !== null ? ` · ${id}` : ''}` : String(id);
      list.append(entityLink(text, member?.entity_ref, { ...row, talkgroup_id: member?.id ?? member?.talkgroup_id }, 'member'));
    });
    return list.childNodes.length ? list : '';
  }

  function factGroups(row, { includeEmpty = false, empty = false } = {}) {
    const group = value(row, 'call_type') === 'PATCH' ? 'Patch' : 'Talkgroup';
    const target = label(row);
    const source = sourceLabel(row, getSourceNameDisplay());
    const savedChannel = value(row, 'channel_name', 'analog_channel_name') ||
      (row?.channel_entity_ref ? 'Open channel' : null);
    const groups = [
      ['', [
        ['Target', target, 'target_entity_ref'], ['Source', source, 'source_entity_ref'],
        ['Latest OTA name', value(row, 'source_ota_alias', 'source_ota_ta', 'ota_alias', 'talker_alias'), 'source_entity_ref'],
        ['Radio system', systemLabel(row), 'radio_system_entity_ref'],
        ['Saved channel', savedChannel, 'channel_entity_ref'],
        ['Winning site', winningSite(row), 'site_entity_ref'], ['Also received on', receivedSites(row)],
        ['Started', dateTime(millis(row))], ['Duration', duration(row)],
        ['Call type', prettify(value(row, 'call_type'))], ['Voice type', prettify(value(row, 'voice_type'))],
        ['Decoder', protocolLabel(value(row, 'protocol'))], ['Frequency', frequency(row)],
        ['Timeslot', value(row, 'timeslot')],
        ['Encrypted', typeof row?.encrypted === 'boolean' ? (row.encrypted ? 'Yes' : 'No') : null],
        ['Alias List', value(row, 'alias_list_name', 'alias_list'), 'alias_list_entity_ref'],
        ['Matched Scan Lists', Array.isArray(row?.matched_scan_lists) ? row.matched_scan_lists.map((item) =>
          typeof item === 'string' ? item : value(item, 'name', 'id')).filter(Boolean).join(', ') : null]
      ]],
      ['Source & target identities', [
        ['Source Radio ID', value(row, 'source_id', 'radio_id'), 'source_entity_ref'],
        ['Source alias', value(row, 'source_alias', 'radio_alias'), 'source_entity_ref'],
        ['Source group', value(row, 'source_group', 'radio_group')],
        ['Source description', value(row, 'source_description', 'radio_description')],
        [`${group} ID`, value(row, 'talkgroup_id', 'group_id'), 'target_entity_ref'],
        [`${group} alias`, value(row, 'talkgroup_alias', 'group_alias', 'talkgroup_name'), 'target_entity_ref'],
        [`${group} group`, value(row, 'talkgroup_group', 'group_group')],
        [`${group} description`, value(row, 'talkgroup_description', 'group_description')],
        ['Destination Radio ID', value(row, 'destination_radio_id'), 'target_entity_ref'],
        ['Destination alias', value(row, 'destination_radio_alias', 'destination_radio_ota_alias'), 'target_entity_ref'],
        ['Destination group', value(row, 'destination_radio_group')],
        ['Destination description', value(row, 'destination_radio_description')], ['Patch members', patchMembers(row)]
      ]],
      ['Protocol & signaling', [
        ['WACN', hexIdentity(value(row, 'wacn'), 5)],
        ['SysID', hexIdentity(value(row, 'system_id', 'sysid'), 3)],
        ['Network ID', value(row, 'network_id', 'network')],
        ['Model', value(row, 'model', 'network_model')],
        ['Location category', value(row, 'location_category')],
        ['RFSS', value(row, 'rfss_id', 'rfss')], ['Site ID', value(row, 'site_id')],
        ['NAC', hexIdentity(value(row, 'nac'), 3)], ['Tone kind', value(row, 'tone_kind')],
        ['Tone', value(row, 'tone')], ['PL', value(row, 'pl')], ['DPL', value(row, 'dpl')]
      ]],
      ['Recording identifiers', [
        ['Recording ID', value(row, 'id')], ['Call ID', value(row, 'call_id')],
        ['Ended', dateTime(Number(value(row, 'end_ms', 'ended_at_ms')))],
        ['File size', fileSize(value(row, 'size_bytes', 'file_size_bytes'))],
        ['System key', value(row, 'system_key')], ['Channel ID', value(row, 'channel_id'), 'channel_entity_ref'],
        ['Alias List ID', value(row, 'alias_list_id'), 'alias_list_entity_ref'],
        ['Raw target ID', value(row, 'target_id'), 'target_entity_ref'],
        ['Source home system name', value(row, 'source_home_system_name') || systemName(row?.source_home_system),
          row?.source_home_system?.entity_ref],
        ['Source home WACN', hexIdentity(value(row, 'source_home_wacn'), 5)],
        ['Source home system', hexIdentity(value(row, 'source_home_system_id'), 3)],
        ['Source home identity', value(row, 'source_home_id'), 'source_entity_ref'],
        ['Target home system name', value(row, 'target_home_system_name') || systemName(row?.target_home_system),
          row?.target_home_system?.entity_ref],
        ['Target home WACN', hexIdentity(value(row, 'target_home_wacn'), 5)],
        ['Target home system', hexIdentity(value(row, 'target_home_system_id'), 3)],
        ['Target home identity', value(row, 'target_home_id'), 'target_entity_ref']
      ]]
    ];
    const sites = [value(row, 'audio_from'), ...(Array.isArray(value(row, 'also_received_on', 'also_received_sites')) ?
      value(row, 'also_received_on', 'also_received_sites') : [])].filter((site) => site && typeof site === 'object');
    if (sites.length) groups.push(['Received site identities', sites.flatMap((site, index) => {
      const prefix = index === 0 && row.audio_from ? 'Winning site' : `Received site ${index + (row.audio_from ? 0 : 1)}`;
      return [[`${prefix} name`, siteText(site), 'site_entity_ref', { ...row, audio_from: site }],
        [`${prefix} system name`, systemName(site) || systemName(site.system_identity),
          site.radio_system_entity_ref || site.system_identity?.entity_ref],
        [`${prefix} WACN`, hexIdentity(value(site, 'wacn'), 5)],
        [`${prefix} SysID`, hexIdentity(value(site, 'system_id', 'sysid'), 3)],
        [`${prefix} RFSS`, value(site, 'rfss_id', 'rfss')], [`${prefix} ID`, value(site, 'site_id')]];
    })]);
    const members = value(row, 'patch_members', 'patch_talkgroups');
    if (Array.isArray(members) && members.length) groups.push(['Patch member identities', members.flatMap((member, index) => {
      if (!member || typeof member !== 'object') return [];
      const prefix = `Member ${index + 1}`;
      return [[`${prefix} kind`, prettify(value(member, 'kind'))],
        [`${prefix} ID`, value(member, 'id', 'talkgroup_id'), member.entity_ref],
        [`${prefix} alias`, value(member, 'alias', 'name', 'ota_alias'), member.entity_ref],
        [`${prefix} home system name`, value(member, 'home_system_name') || systemName(member.home_system),
          member.home_system?.entity_ref],
        [`${prefix} home WACN`, hexIdentity(value(member, 'home_wacn'), 5)],
        [`${prefix} home system`, hexIdentity(value(member, 'home_system_id'), 3)],
        [`${prefix} home identity`, value(member, 'home_identity_id'), member.entity_ref]];
    })]);
    return groups.map(([title, facts]) => [title, facts.filter(([, detail]) => includeEmpty ||
      detail !== null && detail !== undefined && detail !== '').map(([name, detail, reference, context]) => [name,
      empty || detail === null || detail === undefined || detail === '' ? '' :
        reference ? entityLink(detail, typeof reference === 'string' ? row?.[reference] : reference, context || row,
          typeof reference === 'string' ? reference : name) : detail])])
      .filter(([, facts]) => facts.length);
  }

  function renderDetails(host, call = player?.current, { shell = false, empty = false } = {}) {
    host.replaceChildren();
    if (!call && !shell) {
      host.append(makeNotice('Choose a recording to see its call details.'));
      return;
    }
    factGroups(call || {}, { includeEmpty: shell, empty }).forEach(([title, facts]) => {
      const list = node('dl', 'ui-fact-list audio-dock-facts');
      facts.forEach(([name, detail]) => {
        if (shell && (detail === '' || detail === null || detail === undefined)) {
          list.append(node('dt', '', name), node('dd', '', ''));
        } else appendFact(node, list, name, detail);
      });
      if (!title) host.append(list);
      else {
        list.classList.add('ui-form-section');
        const group = node('details', 'ui-section-disclosure');
        group.append(node('summary', 'ui-section-summary', title), list);
        host.append(group);
      }
    });
    if (empty || !call) return;
    if (String(player?.current?.id) === String(call.id)) {
      if (player.detailsLoading) host.append(makeNotice('Loading complete call details…', 'loading'));
      else if (player.detailsError) host.append(makeNotice(player.detailsError, 'error'));
    }
    const actions = node('div', 'ui-action-row');
    const download = anchor('Download recording', audioUrl(call), 'ui-button ui-button-secondary');
    download.setAttribute('download', '');
    actions.append(download);
    host.append(actions);
  }

  function renderTranscript(host, call = player?.current) {
    host.replaceChildren();
    if (!call) { host.append(makeNotice('Choose a recording to see its transcript.')); return; }
    const transcript = call.transcription;
    if (!transcript && player?.detailsLoading && String(player.current?.id) === String(call.id)) {
      host.append(makeNotice('Loading transcript…', 'loading'));
      return;
    }
    if (!transcript && player?.detailsError && String(player.current?.id) === String(call.id)) {
      host.append(makeNotice(player.detailsError, 'error'));
      return;
    }
    const state = String(transcript?.status || 'PENDING').toUpperCase();
    const message = state === 'COMPLETE' ?
      (transcript?.text ? `Transcribed ${dateTime(transcript.stored_at_ms)}` : 'No speech was returned.') :
      state === 'FAILED' ? 'Transcription failed.' : state === 'DISABLED' ? 'Transcription is off.' :
      state === 'TOO_SHORT' || transcript?.too_short ? 'Call is shorter than the minimum length.' :
      state === 'PENDING' ? 'Pending transcription.' : prettify(state);
    host.append(node('p', 'recordings-transcript-status', message));
    if (state === 'COMPLETE' && transcript?.text) {
      host.append(node('p', 'recordings-transcript-text', String(transcript.text)));
    }
    if (state !== 'FAILED' || !isPrimaryAdmin()) return;
    const retryStatus = node('div', 'recordings-form-status');
    retryStatus.setAttribute('role', 'status');
    const retry = button(node, 'Retry transcription', async () => {
      retry.disabled = true;
      try {
        const result = await requestJson(`${ADMIN}/calls/${encodeURIComponent(String(call.id))}/transcription/retry`,
          { method: 'POST', page: false });
        if (String(player?.current?.id) === String(call.id)) {
          player.current = { ...player.current, transcription: result };
          drawQueue();
        }
        if (host.isConnected) renderTranscript(host, { ...call, transcription: result });
      } catch (error) {
        if (host.isConnected) {
          retryStatus.replaceChildren(makeNotice(error.message || 'Transcription could not be retried. Try again.', 'error'));
          retry.disabled = false;
        }
      }
    });
    host.append(retry, retryStatus);
  }

  async function openDetails(row) {
    const body = node('div', 'recordings-detail');
    body.append(makeNotice('Loading call details…', 'loading'));
    const modal = openReadOnlyModal('Call details', body, { id: `recording-${row.id}`, className: 'recordings-detail-modal' });
    if (!modal || (modal.ready && !await modal.ready)) return;
    try {
      const detail = await requestJson(`${CALLS}/${encodeURIComponent(String(row.id))}`, { page: false });
      if (!modal.dialog.isConnected) return;
      const call = { ...row, ...(detail?.call || detail || {}),
        ...(detail?.transcription ? { transcription: detail.transcription } : {}) };
      const heading = node('div', 'recordings-detail-heading');
      heading.append(node('h3', '', label(call)), node('span', '', dateTime(millis(call))));
      const facts = node('div', 'ui-editor-sections recordings-detail-facts');
      renderDetails(facts, call);
      const transcript = node('section', 'recordings-transcript');
      transcript.append(node('h4', '', 'Transcript'));
      const transcriptBody = node('div', '');
      renderTranscript(transcriptBody, call);
      transcript.append(transcriptBody);
      body.replaceChildren(heading, facts, transcript);
    } catch (error) {
      if (modal.dialog.isConnected) body.replaceChildren(makeNotice(error.message ||
        'Call details are unavailable. Refresh the results and try again.', 'error'));
    }
  }

  function resultDetails(row) {
    const analog = ['NBFM', 'AM'].includes(value(row, 'protocol'));
    const groupId = value(row, 'talkgroup_id', 'group_id');
    const sourceId = value(row, 'source_id', 'radio_id');
    const system = value(row, 'system_key') || row.radio_system_entity_ref?.key;
    const siteScope = system || value(row, 'channel_id');
    return [
      ['system', analog ? 'Channel collection' : 'Radio system',
        systemLabel(row), row.radio_system_entity_ref,
        system],
      ['target', value(row, 'call_type') === 'PATCH' ? 'Patch' : 'Talkgroup',
        !analog && groupId !== null ? `${label(row)} · ${groupId}` : null, row.target_entity_ref,
        !analog && groupId !== null && system && `${system}|${value(row, 'call_type')}|${groupId}|` +
          ['target_home_wacn', 'target_home_system_id', 'target_home_id'].map((key) => value(row, key)).join('|')],
      ['description', analog ? 'Description' : 'Talkgroup description',
        value(row, 'talkgroup_description', 'group_description')],
      ['group', analog ? 'Category' : 'Group', value(row, 'talkgroup_group', 'group_group')],
      ['site', 'Site', winningSite(row), null,
        winningSite(row) && siteScope && `${siteScope}|${JSON.stringify(row.audio_from) || winningSite(row)}`],
      ['channel', 'Channel', value(row, 'channel_name', 'analog_channel_name'), row.channel_entity_ref,
        value(row, 'channel_id') || row.channel_entity_ref?.key],
      ['source', 'Source radio', sourceLabel(row, getSourceNameDisplay()), row.source_entity_ref,
        sourceId !== null && system && `${system}|${sourceId}|` +
          ['source_home_wacn', 'source_home_system_id', 'source_home_id'].map((key) => value(row, key)).join('|')]
    ].filter(([, , text, , identity]) => Boolean(text || identity));
  }

  function callCard(row) {
    const card = node('article', 'recordings-call');
    const play = button(node, '', () => choosePlayback(row), 'ui-card-play recordings-card-play');
    play.setAttribute('aria-label', `Choose playback for ${label(row)} at ${dateTime(millis(row))}`);
    play.title = 'Playback options';
    const glyph = node('span', 'ui-card-play-glyph recordings-play-glyph', '▶');
    glyph.setAttribute('aria-hidden', 'true');
    play.append(glyph);
    const time = node('div', 'recordings-call-time');
    time.title = dateTime(millis(row));
    time.append(node('strong', '', timeOnly(millis(row))));
    if (!sharedDate) time.append(node('span', 'recordings-call-date', dateTime(millis(row), true)));
    time.append(node('span', '', duration(row)));
    const main = node('div', 'recordings-call-main');
    const analog = ['NBFM', 'AM'].includes(value(row, 'protocol'));
    const sharedGroup = sharedDetails.has('target');
    const source = sourceBrief(row, getSourceNameDisplay());
    const sourceIsTitle = sharedGroup && Boolean(source);
    const title = sourceIsTitle ? source :
      (analog && sharedDetails.has('channel') ? 'Analog call' : sharedGroup ? 'Unidentified source' : label(row));
    const heading = node('div', 'recordings-call-heading');
    const titleHost = node('strong', 'recordings-call-title');
    titleHost.title = sourceIsTitle ? sourceLabel(row, getSourceNameDisplay()) : title;
    titleHost.append(entityLink(title, sourceIsTitle ? row.source_entity_ref :
      analog ? row.channel_entity_ref : row.target_entity_ref, row, sourceIsTitle ? 'source' : analog ? 'channel' : 'target'));
    heading.append(titleHost);
    const id = value(row, 'talkgroup_id', 'group_id');
    const hasAlias = value(row, 'talkgroup_alias', 'group_alias', 'talkgroup_name');
    if (!analog && id !== null && !sharedGroup && hasAlias) {
      const identity = node('span', 'recordings-call-id');
      identity.append(entityLink(`${value(row, 'call_type') === 'PATCH' ? 'Patch' : 'TG'} ${id}`,
        row.target_entity_ref, row, 'target'));
      heading.append(identity);
    }
    main.append(heading);
    const meta = node('div', 'recordings-call-meta');
    const addContext = (name, text, ref, key) => {
      if (!text) return;
      const item = node('span', `recordings-call-context recordings-context-${key}`);
      item.title = `${name}: ${key === 'source' ? sourceLabel(row, getSourceNameDisplay()) : text}`;
      item.append(entityLink(text, ref, row, key));
      meta.append(item);
    };
    if (source && !sourceIsTitle && !sharedDetails.has('source')) {
      addContext('Source radio', source, row.source_entity_ref, 'source');
    }
    resultDetails(row).filter(([key]) => !sharedDetails.has(key) &&
      ['system', 'site'].includes(key)).forEach(([key, name, text, ref]) => addContext(name, text, ref, key));
    if (meta.childNodes.length) main.append(meta);
    const excerpt = value(row, 'transcript_excerpt');
    if (excerpt) main.append(node('p', 'recordings-transcript-preview', String(excerpt)));
    const info = button(node, 'ⓘ', () => void openDetails(row),
      'ui-disclosure-button ui-icon-button recordings-call-info');
    info.setAttribute('aria-label', `Call details for ${label(row)} at ${dateTime(millis(row))}`);
    info.title = 'Call details';
    card.append(play);
    if (isPrimaryAdmin()) {
      const checkbox = node('input', 'recordings-call-select ui-selection-check');
      checkbox.type = 'checkbox';
      checkbox.checked = selection.has(String(row.id));
      checkbox.setAttribute('aria-label', `Select ${label(row)} at ${dateTime(millis(row))}`);
      checkbox.addEventListener('change', () => {
        if (checkbox.checked) selection.add(String(row.id));
        else selection.delete(String(row.id));
        drawSelection();
      });
      card.append(checkbox);
    }
    card.append(time, main, info);
    return card;
  }

  function drawSharedContext() {
    sharedDetails = new Map();
    if (!sharedHost) return;
    sharedHost.replaceChildren();
    // Summarize the calls actually shown, without claiming a page represents every matching call.
    if (currentResults.length < 2) return;
    const rows = currentResults.map(resultDetails);
    for (const detail of rows[0]) {
      const [key, , text, ref, identity] = detail;
      const matches = rows.map((details) => details.find(([otherKey]) => otherKey === key));
      if (!matches.every(Boolean)) continue;
      const byIdentity = ['system', 'target', 'channel', 'source', 'site'].includes(key);
      if (byIdentity && (!identity || !matches.every((match) => match[4] === identity))) continue;
      if (!byIdentity && !matches.every((match) => match[2] === text)) continue;
      const knownRefs = matches.map((match) => match[3]).filter(Boolean);
      if (knownRefs.some((candidate) => JSON.stringify(candidate) !== JSON.stringify(knownRefs[0]))) continue;
      // Navigation is optional; a missing link does not change a proven system or channel identity.
      const named = matches.find((match, index) => match[2] && (key === 'system' ?
        Boolean(systemName(currentResults[index])) : match[2] !== match[4])) ||
        matches.find((match) => match[2]);
      if (!named) continue;
      sharedDetails.set(key, [key, detail[1], named[2], ref || knownRefs[0], identity]);
    }
    if (!sharedDetails.has('target')) {
      sharedDetails.delete('description');
      sharedDetails.delete('group');
    }
    if (!sharedDetails.size) return;
    const heading = node('div', 'recordings-shared-heading');
    heading.append(node('strong', '', 'Shared on this page'));
    const info = button(node, 'ⓘ', () => {
      const fullFacts = node('dl', 'ui-fact-list recordings-detail-facts');
      for (const [key, name, text, ref] of sharedDetails.values()) {
        appendFact(node, fullFacts, name, entityLink(text, ref, currentResults[0], key));
      }
      openReadOnlyModal('Shared call details', fullFacts, {
        id: 'shared-recording-details', className: 'recordings-detail-modal'
      });
    }, 'ui-disclosure-button ui-icon-button recordings-shared-info');
    info.setAttribute('aria-label', 'Show all shared call details');
    info.title = 'Shared call details';
    heading.append(info);
    const facts = node('dl', 'recordings-shared-facts');
    const order = ['target', 'system', 'site', 'channel', 'source'];
    for (const [key, name, text, ref] of [...sharedDetails.values()].filter(([key]) => order.includes(key)).sort((a, b) =>
      order.indexOf(a[0]) - order.indexOf(b[0]))) {
      const item = node('div', 'recordings-shared-fact');
      const content = node('dd', '');
      content.title = String(text);
      content.append(entityLink(text, ref, currentResults[0], key));
      item.append(node('dt', '', name), content);
      facts.append(item);
    }
    sharedHost.append(heading, facts);
  }

  function drawSelectedFilters() {
    if (!selectedFiltersHost) return;
    selectedFiltersHost.replaceChildren();
    if (searchHint) searchHint.hidden = Boolean(search.system_key);
    if (filterCountHost) {
      const groups = [['system_key'], ['site', 'rfss', 'site_id'],
        ['talkgroup_id', 'talkgroup_min', 'talkgroup_max'], ['radio_id', 'radio_identity_key', 'radio_min', 'radio_max'],
        ['channel_id'], ['alias_list_id'], ['transcript'], ['min_duration_ms', 'max_duration_ms'], ['frequency_hz'],
        ['call_type'], ['voice_type'], ['protocol']];
      const count = groups.filter((keys) => keys.some((key) => currentFilters[key])).length +
        (search.range && search.range !== '24h' ? 1 : 0);
      filterCountHost.textContent = String(count);
      filterCountHost.hidden = count === 0;
    }
    for (const [key, suggestion] of selectedSuggestions) {
      if (!search[key] && !suggestion.appliedKeys?.some((applied) => search[applied])) continue;
      const remove = button(node, `${suggestion.label || suggestion.id} ×`, () => {
        suggestion.appliedKeys?.forEach((applied) => {
          search[applied] = '';
          const appliedInput = searchInputs.get(applied);
          if (appliedInput) appliedInput.value = '';
        });
        search[key] = '';
        selectedSuggestions.delete(key);
        const input = searchInputs.get(key);
        if (input) input.value = '';
        drawSelectedFilters();
      }, 'ui-button ui-button-secondary recordings-filter-chip');
      remove.setAttribute('aria-label', `Remove ${suggestion.label || suggestion.id} filter`);
      selectedFiltersHost.append(remove);
    }
  }

  function drawSelection() {
    if (pageSelect) {
      const selected = currentResults.filter((row) => selection.has(String(row.id))).length;
      pageSelect.checked = currentResults.length > 0 && selected === currentResults.length;
      pageSelect.indeterminate = selected > 0 && selected < currentResults.length;
      pageSelect.disabled = currentResults.length === 0;
    }
    if (!selectedHost) return;
    selectedHost.replaceChildren();
    selectedHost.hidden = !selection.size || !isPrimaryAdmin();
    if (selectedHost.hidden) return;
    selectedHost.append(node('strong', '', `${selection.size} selected`),
      button(node, 'Review delete', () => confirmDelete(), 'ui-button ui-button-danger-quiet'),
      button(node, 'Clear selection', () => {
        selection.clear();
        resultHost.querySelectorAll('input.recordings-call-select').forEach((input) => {
          input.checked = false;
        });
        drawSelection();
        resultHost.querySelector('input.recordings-call-select')?.focus();
      }, 'ui-button ui-button-secondary'));
  }

  function confirmDelete() {
    if (!isPrimaryAdmin() || !selection.size) return;
    const ids = [...selection];
    const body = node('div', 'recordings-delete-confirm');
    body.append(node('p', '', `Delete ${ids.length} selected managed ${ids.length === 1 ? 'recording' : 'recordings'}? ` +
      'Their audio files and call entries will be removed.'));
    const status = node('div', 'recordings-form-status');
    status.setAttribute('role', 'status');
    const cancel = button(node, 'Cancel', () => modal.close());
    const remove = button(node, 'Delete recordings', async () => {
      remove.disabled = true;
      modal.setBusy(true);
      status.replaceChildren(makeNotice('Deleting selected calls…', 'loading'));
      try {
        await requestJson(`${ADMIN}/calls`, { method: 'DELETE',
          body: { ids: ids.map((id) => Number(id)) }, page: false });
        if (player && ids.includes(String(player.current?.id))) resetAudio();
        else if (player) {
          player.queue = player.queue.filter((row) => !ids.includes(String(row.id)));
          drawQueue();
        }
        ids.forEach((id) => selection.delete(id));
        modal.setBusy(false);
        modal.close();
        void renderSearchPage();
      } catch (error) {
        modal.setBusy(false);
        remove.disabled = false;
        status.replaceChildren(makeNotice(error.message || 'Calls could not be deleted.', 'error'));
      }
    }, 'ui-button ui-button-danger');
    body.append(status, modalFooter(cancel, remove));
    const modal = openReadOnlyModal('Delete recordings', body, { id: 'delete-recordings',
      className: 'recordings-detail-modal' });
  }

  function drawResults() {
    if (!resultHost) return;
    drawSharedContext();
    const dates = currentResults.map((row) => dateTime(millis(row), true));
    sharedDate = dates.length > 1 && dates[0] && dates.every((date) => date === dates[0]) ? dates[0] : '';
    resultHost.replaceChildren();
    if (!currentResults.length) {
      resultHost.append(makeNotice(nextCursor ?
        'No matches in this batch. More calls may match; select Next to continue.' :
        'No matching calls. Try a wider time range or fewer filters.', 'empty'));
    } else currentResults.forEach((row) => resultHost.append(callCard(row)));
    const showCount = Boolean(currentResults.length || nextCursor);
    const showPager = currentPage > 0 || Boolean(nextCursor);
    if (countHost) {
      countHost.hidden = !showCount;
      const count = resultTotal === null ? currentResults.length : Number(resultTotal);
      countText.textContent = `${count.toLocaleString()} ${count === 1 ? 'call' : 'calls'}` +
        (resultTotal === null ? ' shown' : '') +
        (sharedDate ? ` · ${sharedDate}` : '') +
        (showPager && nextCursor ? ' · more available' : '');
    }
    if (pagerHost) {
      pagerHost.hidden = !showPager;
      pagerHost.update({ countText: `Page ${currentPage + 1}`,
        previous: { enabled: currentPage > 0, onClick: () => void loadPage(currentPage - 1) },
        next: { enabled: Boolean(nextCursor), onClick: () => void loadPage(currentPage + 1) } });
    }
    drawSelection();
    drawSelectedFilters();
  }

  async function loadPage(page = 0) {
    if (!pageHost) return;
    if (page > currentPage && !cursors[page]) cursors[page] = nextCursor;
    if (page < 0 || (page > 0 && !cursors[page])) return;
    const request = ++searchRequest;
    searchSignal?.abort();
    searchSignal = new AbortController();
    resultStatus.replaceChildren(makeNotice('Finding calls…', 'loading'));
    try {
      const result = await requestJson(query(currentFilters, {
        cursor: cursors[page], sort: sortControl.value
      }), { page: false, signal: searchSignal.signal });
      if (request !== searchRequest || !pageHost.isConnected) return;
      currentResults = Array.isArray(result?.calls) ? result.calls : [];
      currentPage = page;
      nextCursor = result?.next_cursor || null;
      resultTotal = result?.total !== null && result?.total !== undefined &&
        Number.isFinite(Number(result.total)) ? Number(result.total) : null;
      resultStatus.replaceChildren();
      drawResults();
    } catch (error) {
      if (request !== searchRequest || error?.name === 'AbortError') return;
      resultStatus.replaceChildren(makeNotice(error.message || 'Calls could not be loaded. Try again.', 'error'));
    }
  }

  function field(title, control) {
    const wrapper = node('label', 'ui-field recordings-field');
    wrapper.append(node('span', 'ui-field-label', title), control);
    return wrapper;
  }

  function textInput(placeholder, type = 'search') {
    const input = node('input', 'ui-input');
    input.type = type;
    input.placeholder = placeholder;
    input.autocomplete = 'off';
    return input;
  }

  function autocompleteFilter(key, title, kind, placeholder) {
    const input = textInput(placeholder);
    input.id = `recordings-input-${key}`;
    searchInputs.set(key, input);
    input.value = selectedSuggestions.get(key)?.label || search[key] || '';
    const control = node('div', 'recordings-autocomplete');
    const options = node('div', 'ui-popover recordings-suggestions');
    options.id = `recordings-options-${key}`;
    options.setAttribute('role', 'listbox');
    options.hidden = true;
    input.setAttribute('role', 'combobox');
    input.setAttribute('aria-autocomplete', 'list');
    input.setAttribute('aria-controls', options.id);
    input.setAttribute('aria-expanded', 'false');
    let timer = null;
    let blurTimer = null;
    let generation = 0;
    let active = -1;
    const open = () => {
      options.hidden = false;
      input.setAttribute('aria-expanded', 'true');
    };
    const close = () => {
      window.clearTimeout(timer);
      generation++;
      options.hidden = true;
      input.setAttribute('aria-expanded', 'false');
      input.removeAttribute('aria-activedescendant');
      active = -1;
    };
    const showMessage = (message, type = '') => {
      const notice = makeNotice(message, type);
      notice.classList.add('recordings-suggestion-feedback');
      options.replaceChildren(notice);
      open();
      active = -1;
      input.removeAttribute('aria-activedescendant');
    };
    const choose = (suggestion) => {
      if (key === 'q') {
        const matching = { system: 'system_key', site: 'site', talkgroup: 'talkgroup_id',
          talkgroup_range: 'talkgroup_id', radio: 'radio_id', radio_range: 'radio_id',
          channel: 'channel_id' }[suggestion.kind];
        if (matching) {
          applySuggestion(matching, suggestion);
          input.value = suggestion.name_query || '';
          search.q = suggestion.name_query || '';
          const matchingInput = searchInputs.get(matching);
          if (matchingInput) matchingInput.value = String(suggestion.label || suggestion.id || '');
          drawSelectedFilters();
        } else {
          input.value = String(suggestion.label || suggestion.id || '');
          search.q = String(suggestion.id ?? suggestion.label ?? '');
        }
      } else {
        input.value = String(suggestion.label || suggestion.id || '');
        applySuggestion(key, suggestion);
        drawSelectedFilters();
      }
      close();
      input.focus();
    };
    input.addEventListener('input', () => {
      if (key === 'q') {
        for (const [selectedKey, suggestion] of selectedSuggestions) {
          if (suggestion.name_query) {
            selectedSuggestions.delete(selectedKey);
            const selectedInput = searchInputs.get(selectedKey);
            if (selectedInput && selectedKey !== key) selectedInput.value = '';
          }
        }
      }
      search[key] = input.value.trim();
      selectedSuggestions.get(key)?.appliedKeys?.forEach((applied) => {
        search[applied] = '';
        const appliedInput = searchInputs.get(applied);
        if (appliedInput) appliedInput.value = '';
      });
      selectedSuggestions.delete(key);
      window.clearTimeout(timer);
      const request = ++generation;
      const q = input.value.trim();
      if (!q || (q.length < 2 && !/^\d+$/.test(q))) { close(); return; }
      showMessage('Finding suggestions…', 'loading');
      timer = window.setTimeout(async () => {
        try {
          const result = await requestJson(queryPath(SUGGESTIONS, {
            q, kind, system_key: key === 'system_key' ? '' : search.system_key, limit: 20
          }), { page: false });
          if (request !== generation || input.value.trim() !== q || !input.isConnected) return;
          options.replaceChildren();
          const rows = Array.isArray(result) ? result : (result?.rows || result?.suggestions || []);
          if (!rows.length) {
            showMessage('No matching names or IDs. Try another term.');
            return;
          }
          const heading = node('div', 'recordings-suggestion-heading');
          heading.append(node('strong', '', 'Suggestions'),
            node('span', '', `${rows.length} shown`));
          options.append(heading);
          rows.forEach((suggestion, index) => {
            const option = button(node, '', () => choose(suggestion),
              'ui-suggestion-option recordings-suggestion');
            option.id = `${options.id}-${index}`;
            option.setAttribute('role', 'option');
            const description = node('span', 'recordings-suggestion-copy');
            description.append(node('strong', '', String(suggestion.label || suggestion.id || 'Unnamed')));
            if (suggestion.detail) description.append(node('small', '', String(suggestion.detail)));
            option.append(node('span', 'recordings-suggestion-kind', prettify(suggestion.kind || kind || 'Match')),
              description);
            option.addEventListener('mousedown', (event) => event.preventDefault());
            options.append(option);
          });
          open();
        } catch (_error) {
          if (request === generation && input.isConnected) {
            showMessage('Suggestions could not load. Keep typing to retry.', 'error');
          }
        }
      }, SUGGESTION_DELAY_MS);
    });
    input.addEventListener('keydown', (event) => {
      const rows = [...options.querySelectorAll('[role="option"]')];
      if (event.key === 'Escape') {
        if (!options.hidden) event.preventDefault();
        close(); return;
      }
      if (options.hidden || !rows.length) return;
      if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
        event.preventDefault();
        active = event.key === 'ArrowDown' ? (active + 1) % rows.length :
          (active < 0 ? rows.length - 1 : (active + rows.length - 1) % rows.length);
      } else if (event.key === 'Home' || event.key === 'End') {
        event.preventDefault();
        active = event.key === 'Home' ? 0 : rows.length - 1;
      }
      if (['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) {
        rows.forEach((row, index) => row.setAttribute('aria-selected', String(index === active)));
        input.setAttribute('aria-activedescendant', rows[active].id);
      } else if (event.key === 'Enter' && active >= 0) {
        event.preventDefault();
        rows[active].click();
      }
    });
    input.addEventListener('focus', () => window.clearTimeout(blurTimer));
    input.addEventListener('blur', () => {
      generation++;
      window.clearTimeout(timer);
      blurTimer = window.setTimeout(close, 120);
    });
    control.append(input, options);
    // Keep the popup outside the label so its active option cannot become
    // part of the combobox's accessible name.
    const wrapper = node('div', 'ui-field recordings-field');
    const inputLabel = node('label', 'ui-field-label', title);
    inputLabel.setAttribute('for', input.id);
    wrapper.append(inputLabel, control);
    if (key === 'system_key') {
      searchHint = node('small', 'ui-field-detail',
        'Choose a radio system to search over-the-air radio names.');
      searchHint.id = 'recordings-system-search-hint';
      input.setAttribute('aria-describedby', searchHint.id);
      searchHint.hidden = Boolean(search.system_key);
      wrapper.append(searchHint);
    }
    if (key === 'site') {
      const siteHint = node('small', 'ui-field-detail', 'Choose a site from the suggestions.');
      siteHint.id = 'recordings-site-search-hint';
      input.setAttribute('aria-describedby', siteHint.id);
      wrapper.append(siteHint);
    }
    return wrapper;
  }

  function makeSearchForm() {
    const form = node('form', 'recordings-search');
    const primary = node('div', 'recordings-search-primary');
    searchInputs = new Map();
    const queryField = autocompleteFilter('q', 'Find a call', '',
      'System, site, talkgroup, radio, or channel');
    queryField.classList.add('recordings-query-field');
    const range = select(node, [['24h', 'Past 24h'], ['7d', 'Past 7d'], ['30d', 'Past 30d'],
      ['10y', 'Past 10y'], ['custom', 'Custom dates']], 'Date & time');
    range.value = search.range || '24h';
    const from = textInput('', 'datetime-local');
    const to = textInput('', 'datetime-local');
    from.value = localDateTime(search.from_ms);
    to.value = localDateTime(search.to_ms);
    const customDates = node('div', 'recordings-custom-dates');
    customDates.hidden = range.value !== 'custom';
    customDates.append(field('From', from), field('To', to));
    range.addEventListener('change', () => { customDates.hidden = range.value !== 'custom'; });
    const rangeField = field('Date & time', range);
    primary.append(rangeField, queryField);
    const advanced = node('div', 'recordings-search-advanced');
    advanced.hidden = true;
    const advancedFields = node('div', 'recordings-search-fields');
    SEARCH_FIELDS.forEach(([key, title, kind, placeholder]) =>
      advancedFields.append(autocompleteFilter(key, title, kind, placeholder)));
    const transcript = textInput('Words or phrase in a transcript');
    transcript.value = search.transcript;
    transcript.maxLength = 240;
    advancedFields.append(field('Transcript text', transcript));
    const durationCeiling = 30;
    const minimumSeconds = Math.min(durationCeiling, Number(search.min_duration_ms || 0) / 1000);
    const maximumSeconds = search.max_duration_ms ?
      Math.min(durationCeiling, Math.floor(Number(search.max_duration_ms) / 1000)) : durationCeiling;
    // Endpoint positions include sub-second calls and calls longer than 30 seconds.
    const callDuration = createDualRange({ node, label: 'Call length',
      min: 0, max: durationCeiling, step: 1,
      lower: minimumSeconds, upper: maximumSeconds,
      format: (seconds, endpoint) => endpoint === 'upper' && seconds === durationCeiling ?
        '30s+' : seconds === 0 ? '<1s' : `${seconds}s` });
    callDuration.field.classList.add('recordings-field');
    const frequencyInput = textInput('MHz', 'number');
    frequencyInput.step = '0.0001';
    frequencyInput.min = '0';
    frequencyInput.value = search.frequency_hz ? String(Number(search.frequency_hz) / 1_000_000) : '';
    const callType = select(node, [['', 'Any call type'], ['CONVENTIONAL', 'Conventional'],
      ['GROUP', 'Group'], ['DIRECT', 'Direct'], ['PATCH', 'Patch']], 'Call type');
    callType.value = search.call_type;
    const voiceType = select(node, [['', 'Any voice type'], ['CLEAR', 'Clear'],
      ['ENCRYPTED', 'Encrypted'], ['UNKNOWN', 'Unknown']], 'Voice type');
    voiceType.value = search.voice_type;
    const protocol = select(node, [['', 'Any protocol'], ['APCO25', 'P25 Phase 1'],
      ['APCO25_PHASE2', 'P25 Phase 2'], ['DMR', 'DMR'], ['NXDN', 'NXDN'],
      ['NBFM', 'Analog FM'], ['AM', 'Analog AM'], ['DCS', 'DCS']], 'Protocol');
    protocol.value = search.protocol;
    advancedFields.append(callDuration.field, field('Frequency', frequencyInput),
      field('Call type', callType), field('Voice type', voiceType), field('Protocol', protocol));
    advanced.append(advancedFields);
    const actions = node('div', 'recordings-search-actions');
    const submit = button(node, 'Search', null, 'ui-button ui-button-primary');
    submit.type = 'submit';
    const filters = button(node, 'Filters', null);
    filters.id = 'recordings-filters-toggle';
    filterCountHost = node('span', 'ui-pill ui-pill-compact recordings-filter-count');
    filterCountHost.hidden = true;
    filters.append(filterCountHost);
    advanced.id = 'recordings-advanced-filters';
    const clear = button(node, 'Clear filters', () => {
      Object.keys(search).forEach((key) => { search[key] = ''; });
      selectedSuggestions.clear();
      selection.clear();
      pageHost.replaceChildren();
      renderSearchPage();
    }, 'ui-button ui-button-secondary recordings-clear-filters');
    actions.append(submit, filters);
    primary.append(actions);
    advanced.append(clear);
    selectedFiltersHost = node('div', 'recordings-selected-filters');
    form.append(primary, selectedFiltersHost, customDates, advanced);
    browsingWorkflows.createFilterDisclosure({ node, openReadOnlyModal, form, panel: advanced,
      fields: [rangeField, customDates, advancedFields], button: filters, clearAction: clear,
      returnFocusSelector: '#recordings-filters-toggle', id: 'recordings-filters' });
    drawSelectedFilters();
    form.addEventListener('submit', (event) => {
      event.preventDefault();
      const now = Date.now();
      if (search.site && !selectedSuggestions.has('site')) {
        resultStatus.replaceChildren(makeNotice('Choose a site from the suggestions before searching.', 'error'));
        return;
      }
      for (const [key, name] of [['talkgroup_id', 'talkgroup'], ['radio_id', 'radio ID']]) {
        if (search[key] && !/^[0-9]+$/.test(String(search[key])) && !selectedSuggestions.has(key)) {
          resultStatus.replaceChildren(makeNotice(`Choose a ${name} suggestion or enter its numeric ID.`, 'error'));
          return;
        }
      }
      if (range.value === 'custom') {
        const fromMs = new Date(from.value).getTime();
        const toMs = new Date(to.value).getTime() + 59_999;
        if (!Number.isFinite(fromMs) || !Number.isFinite(toMs) || toMs <= fromMs ||
            toMs - fromMs > 3650 * 86_400_000) {
          resultStatus.replaceChildren(makeNotice('Choose a date range of up to 10 years, with the end after the start.',
            'error'));
          return;
        }
      }
      search.range = range.value;
      const rangeMs = { '24h': 86_400_000, '7d': 7 * 86_400_000, '30d': 30 * 86_400_000,
        '10y': 3650 * 86_400_000 }[range.value];
      search.from_ms = rangeMs ? String(now - rangeMs) : range.value === 'custom' && from.value ?
        String(new Date(from.value).getTime()) : '';
      search.to_ms = range.value === 'custom' && to.value ?
        String(new Date(to.value).getTime() + 59_999) : (rangeMs ? String(now) : '');
      const durationRange = callDuration.values();
      search.min_duration_ms = durationRange.lower > 0 ? String(Math.round(durationRange.lower * 1000)) : '';
      search.max_duration_ms = durationRange.upper < durationCeiling ?
        String(durationRange.upper === 0 ? 999 : Math.round(durationRange.upper * 1000)) : '';
      search.frequency_hz = frequencyInput.value ? String(Math.round(Number(frequencyInput.value) * 1_000_000)) : '';
      search.transcript = transcript.value.trim();
      search.call_type = callType.value;
      search.voice_type = voiceType.value;
      search.protocol = protocol.value;
      currentFilters = effectiveFilters();
      appliedSuggestions = new Map(selectedSuggestions);
      appliedRange = search.range;
      cursors = [null];
      currentPage = 0;
      selection.clear();
      void loadPage(0);
    });
    return form;
  }

  async function renderSearchPage() {
    const parameters = new URLSearchParams(globalThis.location?.search || '');
    if (parameters.get('recording_filters') === '1') {
      Object.keys(search).forEach(key => { search[key] = parameters.get(key) || ''; });
      search.range = 'custom';
      appliedRange = search.range;
      selectedSuggestions.clear();
      for (const [key, names] of [['system_key', ['system_key']], ['talkgroup_id', ['talkgroup_id']],
        ['radio_id', ['radio_identity_key', 'radio_id']], ['site', ['rfss', 'site_id']],
        ['channel_id', ['channel_id']], ['alias_list_id', ['alias_list_id']]]) {
        const values = names.map(name => search[name]).filter(Boolean);
        if (values.length) selectedSuggestions.set(key, { label: values.join(' · '),
          appliedKeys: key === 'radio_id' && search.radio_identity_key ? ['radio_identity_key'] :
            key === 'site' ? ['wacn', 'sysid', 'rfss', 'site_id'] : [] });
      }
    }
    const context = captureRenderContext();
    searchRequest++;
    searchSignal?.abort();
    if (!beginPage(context, pageHeader('Recordings',
      'Find and play saved calls across radio systems and conventional channels.'))) return;
    pageHost = node('div', 'recordings-library data-workspace');
    const host = pageHost;
    host.append(makeNotice('Loading recordings…', 'loading'));
    content.append(host);
    let status;
    try {
      status = await requestJson(STATUS, { signal: context.signal });
    } catch (error) {
      if (renderIsCurrent(context) && host.isConnected) host.replaceChildren(makeNotice(
        error.message || 'Recording availability could not be checked. Try again.', 'error'));
      return;
    }
    if (!renderIsCurrent(context) || !host.isConnected) return;
    host.replaceChildren();
    if (status?.available !== true) {
      host.append(makeNotice('The managed recordings library is unavailable. Check receiver status and try again.',
        'error'));
      return;
    }
    if (!status?.has_calls) {
      const empty = node('section', 'ui-empty-state recordings-library-empty');
      if (status?.mode === 'MANAGED') {
        empty.append(node('h2', '', 'Ready for the first call'), node('p', '',
          'Managed Recordings is enabled. New saved calls will appear here after they finish.'));
        empty.append(button(node, 'Refresh calls', () => void renderSearchPage(),
          'ui-button ui-button-primary'));
      } else if (isPrimaryAdmin()) {
        empty.append(node('h2', '', 'Make every call easy to find'), node('p', '',
          'Enable Managed Recordings to organize calls into folders, search by radio or talkgroup, ' +
          'play them in your browser, and set an optional age limit.'));
        empty.append(anchor('Enable Managed Recordings', href('admin', { tab: 'recordings' }),
          'ui-button ui-button-primary'));
        empty.append(node('p', 'recordings-empty-footnote',
          'Using Trunking Recorder or another tool that watches the flat recordings folder? Keep Classic mode.'));
      } else {
        empty.append(node('h2', '', 'No managed recordings yet'), node('p', '',
          'This receiver saves new calls to the Classic recordings folder. Managed calls will appear here if ' +
          'the primary administrator enables Managed Recordings.'));
      }
      host.append(empty);
      return;
    }
    if (status?.mode === 'CLASSIC') host.append(node('p', 'ui-notice recordings-mode-notice',
      'Classic mode is saving new calls in the flat recordings folder. Earlier Managed calls remain searchable here.'));
    const form = makeSearchForm();
    const searchSection = node('section', 'section ui-section recordings-search-panel');
    searchSection.setAttribute('aria-label', 'Search calls');
    const filterTitle = node('div', 'section-title ui-section-title', 'Filters');
    searchSection.append(filterTitle, form);
    const browser = node('div', 'recordings-browser');
    const resultSection = node('section', 'section ui-section recordings-results-panel');
    const titleBar = node('div', 'section-title ui-section-title');
    titleBar.append(node('span', '', 'Calls'));
    const titleActions = node('div', 'ui-section-actions');
    sortControl = select(node, [['desc', 'Newest first'], ['asc', 'Oldest first']], 'Sort calls');
    sortControl.value = parameters.get('recording_filters') === '1' && parameters.get('sort') === 'asc' ? 'asc' : 'desc';
    sortControl.addEventListener('change', () => {
      cursors = [null];
      currentPage = 0;
      void loadPage(0);
    });
    titleActions.append(sortControl);
    titleBar.append(titleActions);
    const body = node('div', 'recordings-results-body');
    sharedHost = node('div', 'recordings-shared');
    selectedHost = node('div', 'recordings-selected ui-selection-bar');
    selectedHost.hidden = true;
    countHost = node('div', 'recordings-result-count');
    countText = node('span');
    countHost.append(countText);
    pageSelect = null;
    if (isPrimaryAdmin()) {
      const pageSelection = node('label', 'recordings-page-selection');
      pageSelect = node('input', 'recordings-page-select ui-selection-check');
      pageSelect.type = 'checkbox';
      pageSelect.disabled = true;
      pageSelect.addEventListener('change', () => {
        currentResults.forEach((row) => pageSelect.checked ? selection.add(String(row.id)) :
          selection.delete(String(row.id)));
        resultHost.querySelectorAll('input.recordings-call-select').forEach((input) => {
          input.checked = pageSelect.checked;
        });
        drawSelection();
      });
      pageSelection.append(pageSelect, node('span', '', 'Select all calls on this page'));
      countHost.append(pageSelection);
    }
    resultStatus = node('div', 'recordings-result-status');
    resultStatus.setAttribute('role', 'status');
    resultHost = node('div', 'recordings-results');
    pagerHost = browsingWorkflows.createBrowsingPager({ node, className: 'recordings-pager',
      ariaLabel: 'Call result pages' });
    body.append(sharedHost, selectedHost, countHost, resultStatus, resultHost, pagerHost);
    resultSection.append(titleBar, body);
    browser.append(searchSection, resultSection);
    host.append(browser);
    if (Object.values(search).every((item) => !item)) search.from_ms = String(Date.now() - 86_400_000);
    currentFilters = effectiveFilters();
    appliedSuggestions = new Map(selectedSuggestions);
    appliedRange = search.range || '24h';
    cursors = [null];
    currentPage = 0;
    void loadPage(0);
  }

  function adminLink(title, tab) {
    return anchor(title, href('admin', { tab }), 'ui-button ui-button-secondary');
  }

  function adminActions(...controls) {
    const actions = node('div', 'ui-action-row');
    actions.append(...controls);
    return actions;
  }

  function confirmAdminAction({ title, description, confirmLabel, perform, onError, danger = false,
    details = [] }) {
    const body = node('div', 'recordings-admin-dialog-body');
    const status = node('div', 'recordings-form-status');
    status.setAttribute('role', 'status');
    let modal;
    const cancel = button(node, 'Cancel', () => modal?.close());
    const confirm = button(node, confirmLabel, async () => {
      confirm.disabled = true;
      cancel.disabled = true;
      modal.setBusy(true);
      try {
        await perform();
        modal.setBusy(false);
        modal.close();
      } catch (error) {
        onError?.(error);
        status.replaceChildren(makeNotice(error.message || `${title} could not be completed. Try again.`, 'error'));
      } finally {
        modal.setBusy(false);
        confirm.disabled = false;
        cancel.disabled = false;
      }
    }, danger ? 'ui-button ui-button-danger' : 'ui-button ui-button-primary');
    body.append(node('p', '', description), ...details, status, modalFooter(cancel, confirm));
    modal = openReadOnlyModal(title, body, { id: 'recordings-administration-action' });
  }

  async function renderAdminRecordings(options = {}) {
    const wrapper = node('div', 'recordings-admin editor-workspace');
    const status = node('div', 'recordings-form-status');
    status.setAttribute('role', 'status');
    wrapper.append(status);
    content.append(wrapper);
    try {
      const [settings, catalog] = await Promise.all([
        requestJson(`${ADMIN}/settings`), requestJson(`${ADMIN}/status`)
      ]);
      if (!wrapper.isConnected) return;
      const modeBody = node('div', 'recordings-admin-body');
      const modes = node('div', 'recordings-mode-choices');
      const radios = {};
      for (const [mode, title, detail] of [
        ['CLASSIC', 'Classic', 'Keep a flat recordings folder for tools such as Trunking Recorder.'],
        ['MANAGED', 'Managed · Recommended', 'Organize calls automatically, search by radio or talkgroup, ' +
          'listen in your browser, and clear old calls by age.']
      ]) {
        const choice = node('label', 'ui-choice-card recordings-mode-choice');
        const radio = node('input', 'ui-choice-radio');
        radio.type = 'radio';
        radio.name = 'recording-mode';
        radio.value = mode;
        radio.checked = settings?.mode === mode;
        radios[mode] = radio;
        choice.append(radio, node('strong', '', title), node('span', '', detail));
        modes.append(choice);
      }
      const folder = node('dl', 'ui-fact-list recordings-managed-folder');
      appendFact(node, folder, 'Managed folder', settings?.managed_directory);
      const modeStatus = node('div', 'recordings-form-status');
      modeStatus.setAttribute('role', 'status');
      const saveMode = button(node, 'Save mode', async () => {
        const mode = radios.MANAGED.checked ? 'MANAGED' : 'CLASSIC';
        saveMode.disabled = true;
        try {
          await requestJson(`${ADMIN}/settings`, { method: 'PUT', body: { mode }, page: false });
          settings.mode = mode;
          modeStatus.replaceChildren(makeNotice('Recording mode saved.'));
        } catch (error) {
          modeStatus.replaceChildren(makeNotice(error.message || 'Mode could not be saved.', 'error'));
        } finally { saveMode.disabled = false; }
      }, 'ui-button ui-button-primary');
      modeBody.append(modes, folder, node('p', 'recordings-admin-note',
        'Managed calls are saved as MP3 for browser playback. The recording format setting applies to Classic mode. ' +
        'Existing managed calls stay searchable when you switch to Classic, and their age limit still applies.'),
      adminActions(saveMode, adminLink('Audio quality settings', 'audio-quality')), modeStatus);
      const ageBody = node('div', 'recordings-admin-body');
      const age = textInput('No age limit', 'number');
      age.min = '1';
      age.step = '1';
      age.value = settings?.retention_days == null ? '' : String(settings.retention_days);
      const ageStatus = node('div', 'recordings-form-status');
      ageStatus.setAttribute('role', 'status');
      const saveAge = button(node, 'Save age limit', async () => {
        const days = numberOrNull(age.value);
        if (age.value && (!Number.isInteger(days) || days < 1)) {
          ageStatus.replaceChildren(makeNotice('Enter a whole number of days, or leave the field blank.', 'error'));
          return;
        }
        saveAge.disabled = true;
        try {
          await requestJson(`${ADMIN}/settings`, { method: 'PUT', body: { retention_days: days }, page: false });
          ageStatus.replaceChildren(makeNotice('Age limit saved.'));
        } catch (error) {
          ageStatus.replaceChildren(makeNotice(error.message || 'Age limit could not be saved.', 'error'));
        } finally { saveAge.disabled = false; }
      }, 'ui-button ui-button-primary');
      const ageField = field('Remove managed calls older than (days)', age);
      ageField.append(node('small', 'ui-field-detail', 'Leave blank for no age limit. The age limit continues to ' +
        'apply to existing managed calls in Classic mode.'));
      ageBody.append(ageField, adminActions(saveAge, adminLink('Transcription settings', 'transcription')),
        ageStatus);
      const catalogBody = node('div', 'recordings-admin-body recordings-catalog-body');
      const catalogSummary = node('div', 'recordings-catalog-summary');
      const catalogCount = node('div', 'recordings-catalog-count');
      const catalogFacts = node('dl', 'ui-facts ui-record-facts recordings-catalog-facts');
      catalogSummary.append(catalogCount, catalogFacts);
      const stat = (name, detail) => {
        if (detail === null || detail === undefined || detail === '') return;
        const fact = node('div', 'ui-fact');
        const description = node('dd');
        description.append(detail);
        fact.append(node('dt', '', name), description);
        catalogFacts.append(fact);
      };
      const operationStatus = node('div', 'recordings-form-status');
      operationStatus.setAttribute('role', 'status');
      let currentCatalog = catalog;
      const operation = (snapshot) => snapshot?.maintenance || snapshot?.operation || snapshot || {};
      const operationState = (snapshot) => String(value(operation(snapshot), 'state', 'status') || 'IDLE')
        .toUpperCase();
      const activeOperation = (snapshot) => ['RUNNING', 'QUEUED', 'IN_PROGRESS'].includes(operationState(snapshot));
      const drawCatalog = (snapshot) => {
        currentCatalog = snapshot;
        catalogFacts.replaceChildren();
        const counts = snapshot?.catalog || snapshot;
        catalogCount.replaceChildren(metrics([
          ['Listed calls', Number(value(counts, 'call_count', 'total_calls') || 0)]
        ], true));
        stat('Oldest call', dateTime(value(counts, 'oldest_call_ms', 'oldest_start_ms'), true));
        stat('Last recount', dateTime(value(snapshot, 'last_recount_ms')));
        const state = operationState(snapshot);
        const statePill = node('span', `ui-pill${state === 'FAILED' ? ' ui-pill-danger' : ''}`,
          prettify(state));
        stat('Maintenance', statePill);
        const current = operation(snapshot);
        if (activeOperation(snapshot)) operationStatus.replaceChildren(makeNotice(
          `${prettify(value(current, 'kind') || 'Maintenance')} running` +
            `${value(current, 'inspected') !== null ? ` · ${Number(current.inspected).toLocaleString()} checked` : ''}`,
          'loading'));
        else if (state === 'FAILED') operationStatus.replaceChildren(makeNotice(
          'Maintenance could not finish. Review the receiver log before trying again.', 'error'));
        else if (state === 'COMPLETED') operationStatus.replaceChildren(makeNotice(
          'Maintenance finished. Call totals are current.'));
        else operationStatus.replaceChildren();
      };
      const refreshCatalog = async () => {
        const latest = await requestJson(`${ADMIN}/status`, { page: false });
        if (wrapper.isConnected) {
          drawCatalog(latest);
        }
        return latest;
      };
      const refreshStatus = button(node, 'Refresh status', async () => {
        refreshStatus.disabled = true;
        try { await refreshCatalog(); }
        catch (error) {
          operationStatus.replaceChildren(makeNotice(error.message ||
            'Call catalog status could not be loaded. Try again.', 'error'));
        } finally { refreshStatus.disabled = false; }
      });
      const watchMaintenance = async (initial) => {
        let latest = initial;
        while (wrapper.isConnected && activeOperation(latest)) {
          await new Promise((resolve) => window.setTimeout(resolve, 1_500));
          if (!wrapper.isConnected) return;
          try { latest = await refreshCatalog(); }
          catch (error) {
            operationStatus.replaceChildren(makeNotice(error.message || 'Maintenance status could not be read.',
              'error'));
            return;
          }
        }
        if (wrapper.isConnected && initial !== latest) {
          const failed = operationState(latest) === 'FAILED';
          operationStatus.replaceChildren(makeNotice(failed ?
            'Maintenance could not finish. Review the receiver log before trying again.' :
            'Maintenance finished. Call totals are current.', failed ? 'error' : ''));
        }
      };
      drawCatalog(catalog);
      const reindexWarning = 'On a very large library, reindexing can delay new managed calls and may cause some ' +
        'to be dropped. Run during quiet reception.';
      const reindexLimitation = 'Reindex checks listed calls. It does not recover unlisted audio files.';
      const maintenance = node('div', 'recordings-maintenance');
      const run = (name, description, action) => {
        const row = node('div', 'recordings-maintenance-row');
        const copy = node('div', 'ui-record-card-copy');
        copy.append(node('strong', 'ui-record-title', name), node('p', 'recordings-admin-note', description));
        const control = button(node, `Run ${name.toLowerCase()}`, () => {
          const details = [];
          if (action === 'recount') {
            const counts = currentCatalog?.catalog || currentCatalog;
            details.push(metrics([
              ['Listed calls', Number(value(counts, 'call_count', 'total_calls') || 0)]
            ], true));
            const lastRecount = dateTime(value(currentCatalog, 'last_recount_ms'));
            if (lastRecount) {
              const facts = node('dl', 'ui-facts ui-admin-facts recordings-admin-facts');
              const fact = node('div', 'ui-fact');
              fact.append(node('dt', '', 'Last recount'), node('dd', '', lastRecount));
              facts.append(fact);
              details.push(facts);
            }
          } else {
            details.push(node('div', 'ui-notice ui-notice-warning', reindexWarning),
              node('p', 'recordings-admin-note', reindexLimitation));
          }
          confirmAdminAction({
            title: name, confirmLabel: `Run ${name.toLowerCase()}`, details,
            description: action === 'recount' ? 'Refresh the displayed call total for the managed recording catalog.' :
              'Check listed calls and remove broken entries from the managed call catalog.',
            perform: async () => {
              control.disabled = true;
              operationStatus.replaceChildren(makeNotice(`${name} started…`, 'loading'));
              try {
                await requestJson(`${ADMIN}/${action}`, { method: 'POST', page: false,
                  timeoutMs: 60_000 });
                const latest = await refreshCatalog();
                if (activeOperation(latest)) void watchMaintenance(latest).finally(() => {
                  control.disabled = false;
                });
                else {
                  operationStatus.replaceChildren(makeNotice(`${name} complete.`));
                  control.disabled = false;
                }
              } catch (error) {
                control.disabled = false;
                throw error;
              }
            },
            onError: (error) => operationStatus.replaceChildren(makeNotice(error.message ||
              `${name} could not be started.`, 'error'))
          });
        });
        row.append(copy, control);
        maintenance.append(row);
      };
      run('Recount calls', 'Refresh the displayed call total.', 'recount');
      run('Reindex calls', 'Check listed calls and remove broken entries.', 'reindex');
      const maintenanceDisclosure = node('details', 'ui-section-disclosure ui-section-disclosure-flat');
      maintenanceDisclosure.append(node('summary', 'ui-section-summary', 'Maintenance'), maintenance,
        node('div', 'ui-notice ui-notice-warning recordings-maintenance-warning', reindexWarning),
        node('p', 'recordings-admin-note recordings-maintenance-note', reindexLimitation));
      catalogBody.append(catalogSummary, operationStatus, maintenanceDisclosure);
      wrapper.append(section('Recording mode', modeBody));
      if (options.classicFormatPanel) wrapper.append(options.classicFormatPanel);
      wrapper.append(section('Managed call retention', ageBody),
        section('Call catalog', catalogBody, refreshStatus));
      if (activeOperation(catalog)) void watchMaintenance(catalog);
    } catch (error) {
      if (wrapper.isConnected) status.replaceChildren(makeNotice(error.message ||
        'Recording settings could not be loaded. Try again.', 'error'));
    }
  }

  async function renderAdminTranscription() {
    const wrapper = node('div', 'recordings-admin editor-workspace');
    const status = node('div', 'recordings-form-status');
    status.setAttribute('role', 'status');
    wrapper.append(status);
    content.append(wrapper);
    try {
      const [settings, catalog] = await Promise.all([
        requestJson(`${ADMIN}/settings`), requestJson(`${ADMIN}/status`)
      ]);
      if (!wrapper.isConnected) return;
      const transcriptionBody = node('div', 'recordings-admin-body');
      const enabledField = uiToggleField('Transcribe managed calls',
        settings?.transcription_enabled === true, 'Transcribe managed calls',
        'Includes retained calls that have not been transcribed.');
      const enabled = enabledField.querySelector('input');
      const endpoint = textInput('http://127.0.0.1:8000/v1/audio/transcriptions', 'url');
      endpoint.value = settings?.transcription_url || '';
      const model = textInput('Model ID', 'text');
      model.value = settings?.transcription_model || '';
      const minimum = textInput('500', 'number');
      minimum.min = '500';
      minimum.max = '600000';
      minimum.step = '1';
      minimum.value = String(settings?.transcription_min_duration_ms ?? 500);
      const key = textInput(settings?.transcription_key_configured ? 'Leave blank to keep saved key' :
        'Optional API key', 'password');
      key.autocomplete = 'new-password';
      const keyField = field('API key', key);
      const keyState = node('small', 'ui-field-detail', settings?.transcription_key_configured ?
        'An API key is saved on this receiver.' : 'No API key is saved.');
      keyField.append(keyState);
      const clearKey = button(node, 'Clear saved key', () => {
        confirmAdminAction({
          title: 'Clear saved transcription key', confirmLabel: 'Clear saved key', danger: true,
          description: 'Clear the saved transcription API key from this receiver.',
          details: [node('p', 'recordings-admin-note', 'The endpoint and model remain configured. A service that ' +
            'requires a key may stop accepting transcription requests.')],
          perform: async () => {
            clearKey.disabled = true;
            try {
              await requestJson(`${ADMIN}/settings`, { method: 'PUT',
                body: { transcription_clear_api_key: true }, page: false });
              key.value = '';
              settings.transcription_key_configured = false;
              keyState.textContent = 'No API key is saved.';
              key.placeholder = 'Optional API key';
              clearKey.hidden = true;
              transcriptionStatus.replaceChildren(makeNotice('Saved API key cleared.'));
            } finally { clearKey.disabled = false; }
          },
          onError: (error) => transcriptionStatus.replaceChildren(makeNotice(error.message ||
            'API key could not be cleared.', 'error'))
        });
      });
      clearKey.hidden = settings?.transcription_key_configured !== true;
      const transcriptionFields = node('div', 'recordings-transcription-fields');
      const endpointField = field('Transcription endpoint URL', endpoint);
      endpointField.classList.add('recordings-transcription-wide');
      keyField.classList.add('recordings-transcription-wide');
      const minimumField = field('Minimum call length (ms)', minimum);
      minimumField.append(node('small', 'ui-field-detail', 'Only calls at or above this length are eligible.'));
      transcriptionFields.append(endpointField, field('Model ID', model), minimumField, keyField);
      const transcriptionStatus = node('div', 'recordings-form-status');
      transcriptionStatus.setAttribute('role', 'status');
      const saveTranscription = button(node, 'Save transcription settings', async () => {
        const milliseconds = numberOrNull(minimum.value);
        if (!Number.isInteger(milliseconds) || milliseconds < 500 || milliseconds > 600_000) {
          transcriptionStatus.replaceChildren(makeNotice('Enter a whole number from 500 to 600,000 milliseconds.',
            'error'));
          return;
        }
        if (enabled.checked && (!endpoint.value.trim() || !model.value.trim())) {
          transcriptionStatus.replaceChildren(makeNotice('Enter a transcription endpoint URL and model ID.', 'error'));
          return;
        }
        const body = { transcription_enabled: enabled.checked, transcription_url: endpoint.value.trim(),
          transcription_model: model.value.trim(), transcription_min_duration_ms: milliseconds };
        if (key.value) body.transcription_api_key = key.value;
        saveTranscription.disabled = true;
        try {
          await requestJson(`${ADMIN}/settings`, { method: 'PUT', body, page: false });
          if (key.value) {
            settings.transcription_key_configured = true;
            key.value = '';
            key.placeholder = 'Leave blank to keep saved key';
            keyState.textContent = 'An API key is saved on this receiver.';
            clearKey.hidden = false;
          }
          transcriptionStatus.replaceChildren(makeNotice('Transcription settings saved.'));
        } catch (error) {
          transcriptionStatus.replaceChildren(makeNotice(error.message ||
            'Transcription settings could not be saved.', 'error'));
        } finally { saveTranscription.disabled = false; }
      }, 'ui-button ui-button-primary');
      const transcriptionProgress = node('div', 'recordings-transcription-progress');
      const drawTranscription = (snapshot) => {
        transcriptionProgress.replaceChildren();
        const progress = snapshot?.transcription || {};
        const counts = [
          ['Pending', progress.pending], ['Completed', progress.completed], ['Failed', progress.failed]
        ].filter(([, count]) => count !== null && count !== undefined);
        if (counts.length) {
          const countTiles = metrics(counts, true);
          countTiles.classList.add('ui-metric-grid-fit', 'ui-metric-grid-inline', 'recordings-transcription-counts');
          transcriptionProgress.append(countTiles);
        }
        if (progress.active) {
          const worker = node('dl', 'ui-facts ui-admin-facts recordings-transcription-worker');
          const fact = node('div', 'ui-fact recordings-transcription-worker-row');
          const status = node('dd');
          status.append(node('span', 'ui-pill state-current', 'Transcribing a call'));
          fact.append(node('dt', '', 'Worker'), status);
          worker.append(fact);
          transcriptionProgress.append(worker);
        }
        if (progress.last_error) {
          const error = node('div', 'ui-notice ui-notice-danger recordings-transcription-error');
          error.append(node('strong', '', 'Last error'), node('p', '', String(progress.last_error)));
          transcriptionProgress.append(error);
        }
      };
      drawTranscription(catalog);
      const keyActions = node('div', 'ui-toolbar');
      keyActions.append(saveTranscription, clearKey);
      transcriptionBody.append(enabledField, transcriptionFields,
        keyActions, transcriptionStatus);
      const progressBody = node('div', 'recordings-admin-body');
      const progressStatus = node('div', 'recordings-form-status');
      progressStatus.setAttribute('role', 'status');
      const refreshStatus = button(node, 'Refresh status', async () => {
        refreshStatus.disabled = true;
        try {
          const latest = await requestJson(`${ADMIN}/status`, { page: false });
          if (wrapper.isConnected) {
            drawTranscription(latest);
            progressStatus.replaceChildren();
          }
        } catch (error) {
          if (wrapper.isConnected) progressStatus.replaceChildren(makeNotice(error.message ||
            'Transcription status could not be loaded. Try again.', 'error'));
        } finally { refreshStatus.disabled = false; }
      });
      progressBody.append(transcriptionProgress, progressStatus,
        adminActions(adminLink('Recording settings', 'recordings')));
      wrapper.append(section('Transcription progress', progressBody, refreshStatus),
        section('Transcription settings', transcriptionBody));
    } catch (error) {
      if (wrapper.isConnected) status.replaceChildren(makeNotice(error.message ||
        'Transcription settings could not be loaded. Try again.', 'error'));
    }
  }

  return { renderSearchPage, renderAdminRecordings, renderAdminTranscription, stopAudio, playback };
}
