import { systemName, systemLabel, systemIdentity } from '../core/system-labels.js?v=1';

const ROOT = '/api/v1/admin/retained-statistics';
const PAGE_SIZE = 25;
const CHOICE_SIZE = 50;
const JOB_POLL_MS = 1500;
const JOB_STORAGE_KEY = 'retained-statistics-job';
const HISTORY_TYPES = new Set(['control_quality', 'hourly_history', 'detailed_events']);

const TYPE_GROUPS = Object.freeze({
  radio_system: [
    { label: 'Observed setup', types: [
      ['site_state', 'Sites & state'], ['frequencies', 'Frequencies'],
      ['band_plans', 'Band plans'], ['foreign_band_plans', 'Foreign band plans'],
      ['neighbors', 'Neighbors'], ['patches', 'Patch groups'],
      ['control_quality', 'Control quality']
    ] },
    { label: 'Identities & summaries', types: [
      ['radios', 'Radio IDs'], ['talkgroups', 'Talkgroups'],
      ['relationships', 'Radio relationships'], ['affiliations', 'Affiliations & presence']
    ] },
    { label: 'Activity & history', types: [
      ['call_activity', 'Call totals'], ['signaling_activity', 'Signaling totals'],
      ['hourly_history', 'Hourly history'], ['detailed_events', 'Detailed events'],
      ['issi_assignment_history', 'ISSI assignment history']
    ] }
  ],
  saved_channel: [
    { label: 'Channel observations', types: [['frequencies', 'Frequencies']] },
    { label: 'Identities & summaries', types: [
      ['call_activity', 'Call totals'], ['signaling_activity', 'Signaling totals'],
      ['radios', 'DMR radio IDs'],
      ['talkgroups', 'DMR talkgroups']
    ] },
    { label: 'Activity & history', types: [
      ['hourly_history', 'Hourly history'], ['detailed_events', 'Detailed events']
    ] }
  ],
  alias_activity: [
    { label: 'Receiver-wide', types: [['alias_activity', 'Alias Activity']] }
  ]
});
function typeLabel(sourceKind, type) {
  if (type === 'all') return 'All retained data';
  return TYPE_GROUPS[sourceKind]?.flatMap((group) => group.types)
    .find(([value]) => value === type)?.[1] || 'Results';
}
const SITE_TYPES = new Set(['site_state', 'frequencies', 'band_plans',
  'foreign_band_plans', 'neighbors', 'patches', 'control_quality']);
const P25_TYPES = new Set(['band_plans', 'foreign_band_plans', 'patches', 'issi_assignment_history']);
const PART_LABELS = Object.freeze({ current: 'Current observations',
  summary: 'Summary statistics', buckets: 'History buckets', events: 'Detailed events' });
const PART_ORDER = Object.keys(PART_LABELS);

function partLabel(sourceKind, type, part) {
  if (type === 'alias_activity' && part === 'summary') return 'Activity counters & timestamps';
  if (type === 'issi_assignment_history' && part === 'summary') return 'Saved mappings & counts';
  if (['band_plans', 'foreign_band_plans'].includes(type)) {
    return ({ current: 'Current learned plan', summary: 'Lifetime summary' })[part] ||
      PART_LABELS[part];
  }
  if (type === 'frequencies') {
    return ({ current: 'Current frequency', summary: 'Lifetime summary',
      buckets: sourceKind === 'saved_channel' ? 'Hourly history' : 'History buckets' })[part] ||
      PART_LABELS[part];
  }
  if (type === 'control_quality' && part === 'buckets') return 'Quality history';
  if (type === 'hourly_history' && part === 'buckets') return 'Hourly history';
  if (['radios', 'talkgroups'].includes(type) && part === 'summary') return 'Identity summary';
  return PART_LABELS[part] || part;
}

function typeAvailable(sourceKind, source, type) {
  if (sourceKind === 'alias_activity') return type === 'alias_activity';
  if (!source) return false;
  if (P25_TYPES.has(type) && String(source.protocol || '').toUpperCase() !== 'P25') return false;
  if (sourceKind === 'radio_system') return true;
  return !['radios', 'talkgroups'].includes(type) ||
    String(source.protocol || '').toUpperCase() === 'DMR';
}

function queryPath(path, values) {
  const query = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) query.set(key, String(value));
  });
  return `${ROOT}${path}?${query}`;
}

function targetForRemoval(row, partsOrHistory) {
  const target = row?.target;
  if (!target || typeof target !== 'object' || Array.isArray(target)) {
    throw new Error('This row cannot be deleted. Refresh the results and try again.');
  }
  if (target.kind === 'scoped_data') {
    const supported = Array.isArray(target.parts) ? target.parts : [];
    const parts = Array.isArray(partsOrHistory) ? partsOrHistory.filter((part) =>
      supported.includes(part)) : supported;
    if (!parts.length) throw new Error(target.source_kind === 'alias_activity' ?
      'Choose at least one saved part to reset.' : 'Choose at least one saved part to delete.');
    return { ...target, parts };
  }
  if (target.kind === 'learned_site' || target.kind === 'saved_site' || target.kind === 'system') {
    return { ...target, include_channel_history: Boolean(partsOrHistory) };
  }
  return { ...target };
}

function requiresSite(sourceKind, source, type) {
  return sourceKind === 'radio_system' && Boolean(source) && SITE_TYPES.has(type);
}

function canShowResults(sourceKind, source, type, site) {
  if (sourceKind === 'alias_activity') return type === 'alias_activity';
  return Boolean(source && type && (!requiresSite(sourceKind, source, type) ||
    site?.configuration_id));
}

function retainedSystemIdentityFacts(row, source = null) {
  const known = (value) => value != null && value !== '' && Number.isSafeInteger(Number(value)) && Number(value) >= 0;
  const hex = (value, width) => known(value) ? Number(value).toString(16).toUpperCase().padStart(width, '0') : '';
  const identities = [
    ['', row.system_identity || source?.system_identity],
    ['Home ', row.home_system || (known(row.home_wacn) && known(row.home_system_id) ? {
      system_name: row.home_system_name, protocol: 'P25', wacn: row.home_wacn, system_id: row.home_system_id
    } : null)],
    ['Foreign ', row.foreign_system || (known(row.foreign_wacn) && known(row.foreign_system_id) ? {
      system_name: row.foreign_system_name, protocol: 'P25', wacn: row.foreign_wacn, system_id: row.foreign_system_id
    } : null)]
  ];
  return identities.flatMap(([prefix, identity]) => {
    if (!identity) return [];
    const p25 = String(identity.protocol || '').toUpperCase().startsWith('P25') || identity.wacn != null;
    return [
      [prefix ? `${prefix}system` : 'System', systemLabel(identity), ...(identity.entity_ref ? [identity] : [])],
      [`${prefix}WACN`, hex(identity.wacn, 5)],
      [`${prefix}${p25 ? 'SysID' : 'System ID'}`, p25 ? hex(identity.system_id, 3) : identity.system_id],
      [`${prefix}Network ID`, identity.network_id],
      [`${prefix}Model`, identity.model ?? identity.network_model],
      [`${prefix}Location category`, identity.location_category]
    ].filter(([, value]) => value !== null && value !== undefined && value !== '');
  });
}

// Reuse map: the Administration settings shell owns navigation; shared ui-field,
// ui-select, ui-segmented, ui-data-table, ui-pager, ui-metric, ui-feedback and modal controls
// own appearance and interaction. This module adds only workspace geometry.
export function createRetainedStatisticsWorkspace(deps) {
  const { node, formField, uiSelectFrame, uiSegmentedControl, section, sectionActionHost,
    table, openReadOnlyModal, modalFooter, metrics, requestJson, formatNumber, formatDateTime,
    renderItem, renderSource, renderAliasList, renderAlias, browsingWorkflows, signal } = deps;
  const host = node('div', 'retained-statistics-page data-workspace');
  const pickerBody = node('div', 'retained-statistics-picker-body');
  const sourceBlock = node('div', 'retained-statistics-source-block');
  const typeBlock = node('div', 'retained-statistics-type-block');
  const siteBlock = node('div', 'retained-statistics-site-block');
  const resultsBody = node('div', 'retained-statistics-results-body');
  const resultsActions = sectionActionHost();
  const resultsSection = section('Results', resultsBody, resultsActions);
  const resultsTitle = resultsSection.querySelector('.ui-section-title').firstChild;
  const selectionSection = section('Choose data', pickerBody);
  const jobNotice = node('div', 'retained-statistics-job-notice');
  jobNotice.setAttribute('role', 'status');
  jobNotice.setAttribute('aria-live', 'polite');
  host.append(selectionSection, jobNotice, resultsSection);

  const state = {
    sourceKind: 'radio_system', source: null, sourceRows: [], sourceOffset: 0,
    sourceSearch: '', sourceMore: false, sourceLoading: false, sourceError: '', sourceRequest: 0,
    type: '', parts: null, site: null, siteRows: [], siteOffset: 0, siteSearch: '',
    siteMore: false, siteLoading: false, siteError: '', siteRequest: 0,
    resultSearch: '', resultRows: [], resultOffset: 0, resultMore: false,
    resultTotal: null, resultLoading: false, resultRequest: 0, resultError: '',
    job: null, jobTarget: null, jobSource: null, jobTimer: null, jobActionPending: false,
    jobRequest: 0,
    choiceTimer: null, resultTimer: null,
    disposed: false
  };

  const button = (label, action, classes = 'ui-button ui-button-secondary') => {
    const control = node('button', classes, label);
    control.type = 'button';
    if (action) control.addEventListener('click', action);
    return control;
  };
  const select = (label) => {
    const control = node('select', 'ui-select');
    control.setAttribute('aria-label', label);
    return control;
  };
  const option = (value, label) => {
    const item = node('option', '', label);
    item.value = value;
    return item;
  };
  const feedback = (message, tone = '') => node('div', tone ? `ui-feedback ui-feedback-${tone}` :
    'ui-feedback', message);
  const read = (path, values, requestSignal = signal) => requestJson(queryPath(path, values),
    { csrf: false, signal: requestSignal });
  const mhz = (hz) => Number.isFinite(Number(hz)) && Number(hz) > 0 ?
    `${(Number(hz) / 1_000_000).toFixed(5)} MHz` : '';
  const choiceName = (row) => [...new Set([row.source_kind === 'radio_system' ? systemLabel(row) || row.label : row.label,
    row.source_kind === 'saved_channel' ? systemName(row) : '',
    row.source_kind === 'saved_channel' ? row.site_name : '', row.protocol,
    row.source_kind === 'saved_channel' ? mhz(row.primary_frequency_hz) : ''].filter(Boolean))]
    .join(' · ');
  const choiceLabel = (row, rows, key) => {
    const label = choiceName(row);
    return rows.some((other) => other[key] !== row[key] && choiceName(other) === choiceName(row)) ?
      `${label} · ${row.source_kind === 'radio_system' ? systemIdentity(row) || row[key] : row[key]}` : label;
  };

  const sourceMode = uiSegmentedControl([
    { value: 'radio_system', label: 'Trunked system' },
    { value: 'saved_channel', label: 'Conventional channel' },
    { value: 'alias_activity', label: 'Alias Activity' }
  ], state.sourceKind, (value) => {
    if (value === state.sourceKind) return;
    state.sourceKind = value;
    state.source = null;
    state.type = value === 'alias_activity' ? 'alias_activity' : '';
    state.parts = null;
    clearSite();
    clearResults();
    state.sourceSearch = '';
    sourceSearch.value = '';
    sourceBlock.hidden = value === 'alias_activity';
    drawSourceChoices();
    drawTypes();
    drawSite();
    drawResults();
    void loadSources(true);
    if (value === 'alias_activity') void loadResults();
  });
  sourceMode.setAttribute('aria-label', 'Statistics scope');
  sourceMode.classList.add('retained-statistics-source-mode');
  const modeBlock = node('div', 'retained-statistics-mode-block');
  modeBlock.append(node('h3', 'retained-statistics-label', 'Where?'), sourceMode);
  const sourceSearch = node('input', 'ui-input');
  sourceSearch.type = 'search';
  sourceSearch.autocomplete = 'off';
  sourceSearch.placeholder = 'Search radio systems';
  sourceSearch.setAttribute('aria-label', 'Search sources');
  const sourceSelect = select('Radio system');
  const sourceMoreButton = button('More sources', () => void loadSources(Boolean(state.sourceError)));
  const sourceMessage = node('small', 'ui-field-detail');
  sourceMessage.setAttribute('role', 'status');
  sourceMessage.setAttribute('aria-live', 'polite');
  const sourceChoices = node('div', 'retained-statistics-source-choices');
  sourceChoices.append(uiSelectFrame(sourceSelect), sourceMoreButton);
  const sourceLabel = node('h3', 'retained-statistics-label', 'Radio system');
  sourceBlock.append(sourceLabel,
    sourceSearch, sourceChoices, sourceMessage);

  const typeChoices = node('div', 'retained-statistics-catalog');
  typeChoices.setAttribute('aria-label', 'Data types');
  const allButton = button('All retained data in scope', () => {
    if (state.sourceKind === 'alias_activity') reviewGlobalAlias();
    else chooseType('all');
  });
  allButton.classList.add('retained-statistics-all-button');
  const catalogHeading = node('div', 'retained-statistics-catalog-heading');
  catalogHeading.append(node('h3', 'retained-statistics-label', 'What data?'), allButton);
  typeBlock.append(catalogHeading, typeChoices);

  const siteSearch = node('input', 'ui-input');
  siteSearch.type = 'search';
  siteSearch.autocomplete = 'off';
  siteSearch.placeholder = 'Find a site';
  siteSearch.setAttribute('aria-label', 'Find a site');
  const siteSelect = select('Site');
  const siteMoreButton = button('More sites', () => void loadSites(Boolean(state.siteError)));
  const siteMessage = node('small', 'ui-field-detail');
  siteMessage.setAttribute('role', 'status');
  siteMessage.setAttribute('aria-live', 'polite');
  const siteChoices = node('div', 'retained-statistics-site-choices');
  siteChoices.append(uiSelectFrame(siteSelect), siteMoreButton);
  siteBlock.append(node('h3', 'retained-statistics-label', 'Site'),
    siteSearch, siteChoices, siteMessage);
  const sourceRow = node('div', 'retained-statistics-source-row');
  sourceRow.append(modeBlock, sourceBlock);
  pickerBody.append(sourceRow, typeBlock, siteBlock);

  const resultSearch = node('input', 'ui-input');
  resultSearch.type = 'search';
  resultSearch.autocomplete = 'off';
  resultSearch.placeholder = 'Search results';
  resultSearch.setAttribute('aria-label', 'Search results');
  const resultSearchForm = node('form', 'retained-statistics-search');
  resultSearchForm.append(resultSearch);
  const resultContext = node('div', 'retained-statistics-context muted');
  const resultCount = node('span', 'muted retained-statistics-result-count');
  const resultStatus = node('div', 'retained-statistics-result-status');
  resultStatus.setAttribute('role', 'status');
  resultStatus.setAttribute('aria-live', 'polite');
  const resultTable = node('div', 'retained-statistics-table');
  const partsBlock = node('div', 'retained-statistics-parts');
  partsBlock.setAttribute('role', 'group');
  partsBlock.setAttribute('aria-label', 'Saved parts to delete');
  const resultPager = browsingWorkflows.createBrowsingPager({ node,
    className: 'retained-statistics-pager', ariaLabel: 'Results pages' });
  resultsBody.append(resultContext, resultSearchForm, resultCount, partsBlock, resultStatus,
    resultTable, resultPager);

  const selectedSource = () => state.source;
  function appendSystemIdentityFacts(host, row) {
    const facts = retainedSystemIdentityFacts(row, selectedSource());
    if (!facts.length) return;
    const list = node('dl', 'ui-fact-list');
    facts.forEach(([label, value, identity]) => {
      const description = node('dd');
      description.append(identity ? renderSource(identity, String(value)) : String(value));
      list.append(node('dt', '', label), description);
    });
    host.append(list);
  }
  const selectedSite = () => state.site;
  const busyWithJob = () => Boolean(state.job && ['queued', 'running', 'cancelling'].includes(state.job.state));
  const tableController = {};

  function clearSite() {
    state.site = null;
    state.siteRows = [];
    state.siteOffset = 0;
    state.siteMore = false;
    state.siteSearch = '';
    state.siteRequest += 1;
    siteSearch.value = '';
  }

  function clearResults() {
    state.resultRows = [];
    state.resultOffset = 0;
    state.resultMore = false;
    state.resultTotal = null;
    state.resultSearch = '';
    state.resultError = '';
    state.parts = null;
    state.resultRequest += 1;
    resultSearch.value = '';
  }

  function drawSourceChoices() {
    if (state.sourceKind === 'alias_activity') return;
    const selected = selectedSource();
    const system = state.sourceKind === 'radio_system';
    sourceLabel.textContent = system ? 'Radio system' : 'Conventional channel';
    sourceSearch.placeholder = system ? 'Search by system name or ID' :
      'Search by channel, site, protocol, or frequency';
    sourceSearch.setAttribute('aria-label', system ? 'Search radio systems' :
      'Search conventional channels');
    sourceSelect.setAttribute('aria-label', system ? 'Radio system' : 'Conventional channel');
    sourceSelect.replaceChildren(option('', state.sourceKind === 'radio_system' ?
      'Choose radio system' : 'Choose conventional channel'));
    const choices = selected ? [...state.sourceRows, selected] : state.sourceRows;
    if (selected && !state.sourceRows.some((row) => row.source_key === selected.source_key)) {
      sourceSelect.append(option(selected.source_key, choiceLabel(selected, choices, 'source_key')));
    }
    state.sourceRows.forEach((row) => sourceSelect.append(option(row.source_key,
      choiceLabel(row, choices, 'source_key'))));
    sourceSelect.value = selected?.source_key || '';
    sourceSelect.disabled = state.sourceLoading && !state.sourceRows.length;
    sourceMoreButton.hidden = !state.sourceMore && !state.sourceError;
    sourceMoreButton.textContent = state.sourceError ? 'Retry' : 'More sources';
    sourceMoreButton.disabled = state.sourceLoading;
    sourceMessage.textContent = state.sourceLoading ? 'Loading sources…' : state.sourceError ||
      (!state.sourceRows.length && !selected ? 'No sources found.' : '');
  }

  function drawTypes() {
    const groups = TYPE_GROUPS[state.sourceKind] || [];
    typeChoices.replaceChildren(...groups.map((entry) => {
      const group = node('div', 'ui-fact retained-statistics-catalog-group');
      const choices = node('div', 'retained-statistics-catalog-items');
      choices.setAttribute('role', 'group');
      choices.setAttribute('aria-label', entry.label);
      choices.append(...entry.types.map(([value, label]) => {
        const control = button(label, () => chooseType(value),
          'ui-button ui-button-secondary retained-statistics-type-option');
        control.disabled = !typeAvailable(state.sourceKind, selectedSource(), value);
        control.setAttribute('aria-pressed', String(state.type === value));
        return control;
      }));
      group.append(node('strong', 'retained-statistics-group-label', entry.label), choices);
      return group;
    }));
    allButton.textContent = state.sourceKind === 'alias_activity' ?
      'Reset all Alias Activity…' : 'All retained data in scope';
    allButton.disabled = state.sourceKind === 'alias_activity' ?
      selectedSource()?.target?.kind !== 'scoped_data' : !selectedSource();
    allButton.setAttribute('aria-pressed', String(state.type === 'all'));
  }

  function drawSite() {
    siteBlock.hidden = state.sourceKind !== 'radio_system' || !selectedSource() ||
      (state.type !== 'all' && !requiresSite(state.sourceKind, selectedSource(), state.type));
    if (siteBlock.hidden) return;
    const selected = selectedSite();
    siteSelect.replaceChildren(option('', state.type === 'all' ?
      'Entire radio system · all sites' : 'Choose site'));
    const choices = selected ? [...state.siteRows, selected] : state.siteRows;
    if (selected && !state.siteRows.some((row) => row.configuration_id === selected.configuration_id)) {
      siteSelect.append(option(selected.configuration_id,
        choiceLabel(selected, choices, 'configuration_id')));
    }
    state.siteRows.forEach((row) => siteSelect.append(option(row.configuration_id,
      choiceLabel(row, choices, 'configuration_id'))));
    siteSelect.value = selected?.configuration_id || '';
    siteSelect.disabled = state.siteLoading && !state.siteRows.length;
    siteMoreButton.hidden = !state.siteMore && !state.siteError;
    siteMoreButton.textContent = state.siteError ? 'Retry' : 'More sites';
    siteMoreButton.disabled = state.siteLoading;
    siteMessage.textContent = state.siteLoading ? 'Loading sites…' : state.siteError ||
      (!state.siteRows.length && !selected ? 'No sites found.' : '');
  }

  async function loadSources(reset) {
    if (state.disposed || state.sourceLoading && !reset) return;
    const generation = ++state.sourceRequest;
    state.sourceError = '';
    if (reset) {
      state.sourceRows = [];
      state.sourceOffset = 0;
      state.sourceMore = false;
    }
    state.sourceLoading = true;
    drawSourceChoices();
    try {
      const response = await read('/sources', {
        kind: state.sourceKind, q: state.sourceSearch, limit: CHOICE_SIZE,
        offset: state.sourceOffset
      });
      if (state.disposed || generation !== state.sourceRequest) return;
      const rows = Array.isArray(response.rows) ? response.rows : [];
      state.sourceRows = state.sourceRows.concat(rows);
      state.sourceOffset = Number(response.next_offset ?? state.sourceOffset + rows.length);
      state.sourceMore = response.has_more === true;
      if (state.sourceKind === 'alias_activity') state.source = rows[0] || null;
    } catch (error) {
      if (state.disposed || generation !== state.sourceRequest || error?.name === 'AbortError') return;
      state.sourceError = error.message || 'Sources could not be loaded.';
    } finally {
      if (generation === state.sourceRequest) {
        state.sourceLoading = false;
        drawSourceChoices();
        drawTypes();
        if (state.job) drawJob();
      }
    }
  }

  async function loadSites(reset) {
    if (state.disposed || state.sourceKind !== 'radio_system' || !selectedSource() ||
      state.siteLoading && !reset) return;
    const generation = ++state.siteRequest;
    state.siteError = '';
    if (reset) {
      state.siteRows = [];
      state.siteOffset = 0;
      state.siteMore = false;
    }
    state.siteLoading = true;
    drawSite();
    try {
      const response = await read('/sites', {
        source_kind: state.sourceKind, source_key: selectedSource().source_key,
        q: state.siteSearch, limit: CHOICE_SIZE, offset: state.siteOffset
      });
      if (state.disposed || generation !== state.siteRequest) return;
      const rows = Array.isArray(response.rows) ? response.rows : [];
      state.siteRows = state.siteRows.concat(rows);
      state.siteOffset = Number(response.next_offset ?? state.siteOffset + rows.length);
      state.siteMore = response.has_more === true;
    } catch (error) {
      if (state.disposed || generation !== state.siteRequest || error?.name === 'AbortError') return;
      state.siteError = error.message || 'Sites could not be loaded.';
    } finally {
      if (generation === state.siteRequest) {
        state.siteLoading = false;
        drawSite();
      }
    }
  }

  function chooseType(value) {
    if (state.type === value || !typeAvailable(state.sourceKind, selectedSource(), value) &&
      value !== 'all') return;
    if (value === 'all' && !selectedSource()) return;
    state.type = value;
    clearSite();
    clearResults();
    drawTypes();
    drawSite();
    drawResults();
    if (requiresSite(state.sourceKind, selectedSource(), state.type)) void loadSites(true);
    else {
      if (state.sourceKind === 'radio_system' && state.type === 'all') void loadSites(true);
      void loadResults();
    }
  }

  function resultsTable() {
    const identity = ['radios', 'talkgroups'].includes(state.type);
    const numeric = (...names) => ({ id: names[0], label: names[names.length - 1],
      className: 'numeric', sortable: false, render: (row) => {
        const value = names.slice(0, -1).map((name) => row[name]).find((item) => item != null);
        return value == null ? '—' : formatNumber(value);
      } });
    const field = (id, label, render) => ({ id, label, render, sortable: false });
    const time = (label) => field('last-seen', label, (row) =>
      formatDateTime(row.last_seen_ms) || '—');
    const stateValue = (row) => row.state ?
      String(row.state).toLowerCase().replace(/^./, (letter) => letter.toUpperCase()) : '—';
    const item = field('item', state.type === 'alias_activity' ? 'Saved alias' :
      identity ? (state.type === 'radios' ? 'Radio' : 'Talkgroup') :
        state.type === 'band_plans' || state.type === 'foreign_band_plans' ? 'Band' :
          state.type === 'frequencies' ? 'Frequency' : 'Item',
    (row) => state.type === 'alias_activity' ? renderAlias(row) : renderItem(row));
    let columns = [item];
    if (state.type === 'band_plans' || state.type === 'foreign_band_plans') {
      columns.push(field('base', 'Base MHz', (row) => mhz(row.base_hz) || '—'),
        field('spacing', 'Space kHz', (row) => row.spacing_hz == null ? '—' :
          formatNumber(Number(row.spacing_hz) / 1000)));
      if (state.type === 'band_plans') columns.push(numeric('bandwidth_hz', 'BW Hz'));
      columns.push(
        field('offset', 'Offset MHz', (row) => row.transmit_offset_hz == null ? '—' :
          (Number(row.transmit_offset_hz) / 1_000_000).toFixed(3)));
      if (state.type === 'band_plans') columns.push(numeric('timeslots', 'Slots'));
      else columns.push(field('channel-type', 'Channel type', (row) =>
        row.channel_type_code ?? '—'));
      columns.push(field('state', 'State', stateValue),
        numeric('observation_count', 'Observations'), time('Last seen'));
    } else if (state.type === 'frequencies' && state.sourceKind === 'radio_system') {
      columns.push(field('channel', 'LCN', (row) => row.channel_key ||
        (Array.isArray(row.channel_keys) ? row.channel_keys.join(', ') : row.descriptor) || '—'),
        field('use', 'Use', (row) => {
          const tags = Array.isArray(row.current_tags) && row.current_tags.length ?
            row.current_tags : row.tags;
          return Array.isArray(tags) ? tags.join(', ') || '—' : tags || '—';
        }),
        field('uplink', 'Up MHz', (row) => mhz(row.uplink_hz) || '—'),
        numeric('timeslots', 'Slots'), field('state', 'State', stateValue),
        numeric('observation_count', 'Decoder obs.'),
        numeric('voice_grant_observations', 'Voice grants'), time('Last observed'));
    } else if (state.type === 'frequencies') {
      if (state.resultRows.some((row) => Array.isArray(row.timeslots) && row.timeslots.length ||
        Number(row.timeslot) > 0)) {
        columns.push(field('slots', 'Slots', (row) => Array.isArray(row.timeslots) &&
          row.timeslots.length ? row.timeslots.join(', ') : Number(row.timeslot) > 0 ?
            String(row.timeslot) : '—'));
      }
      columns.push(numeric('logical_call_count', 'observation_count', 'Calls'),
        time('Last heard'));
    } else if (state.type === 'alias_activity') {
      columns.push(field('alias-list', 'Alias list', (row) => renderAliasList(row) || '—'),
        numeric('logical_call_count', 'Calls'),
        numeric('signaling_observation_count', 'Signaling'), time('Last heard'));
    } else if (identity) {
      columns.push(field('alias-group', 'Alias group', (row) => row.alias_group || '—'),
        field('alias-list', 'Alias list', (row) => renderAliasList(row) || '—'),
        numeric('logical_call_count', 'observation_count', 'Calls'), time('Last heard'));
    } else {
      columns.push(field('saved-records', 'Saved records', (row) =>
        row.observation_count == null && row.record_count == null ? '—' :
          formatNumber(row.record_count ?? row.observation_count)), time('Last seen'));
    }
    columns.splice(1, 0, { id: 'action', label: 'Action', render: (row) => {
      const resetActivity = row.target?.source_kind === 'alias_activity';
      const action = button(resetActivity ? 'Reset…' : 'Delete…', () => reviewRow(row),
        'ui-button ui-button-danger');
      action.disabled = busyWithJob() || row.target?.kind === 'scoped_data' &&
        !selectedPartsFor(row).length;
      action.setAttribute('aria-label', `${resetActivity ? 'Reset activity' : 'Delete statistics'} for ${row.label || 'item'}`);
      return action;
    }, sortable: false });
    return table(state.resultRows, columns, 'No matching results', {
      type: `retained-statistics-v3.${state.sourceKind}.${state.type}`,
      layoutMenuHost: resultsActions,
      tableClass: 'ui-data-table-quiet', mobileCards: true, sortable: false,
      controller: tableController
    });
  }

  function aggregateResult(row) {
    const summary = node('div', 'ui-fact retained-statistics-aggregate');
    const text = node('div', 'retained-statistics-aggregate-text');
    text.append(node('strong', '', row.label || 'Selected data'));
    if (row.detail) text.append(node('small', 'muted', row.detail));
    const action = button('Delete…', () => reviewRow(row), 'ui-button ui-button-danger');
    action.setAttribute('aria-label', `Delete statistics for ${row.label || 'selected data'}`);
    action.disabled = busyWithJob() || row.target?.kind === 'scoped_data' &&
      !selectedPartsFor(row).length;
    summary.append(text, action);
    return summary;
  }

  function selectedPartsFor(row) {
    const supported = Array.isArray(row?.target?.parts) ? row.target.parts : [];
    return (state.parts || supported).filter((part) => supported.includes(part));
  }

  function drawParts() {
    partsBlock.replaceChildren();
    const supported = [...new Set(state.resultRows.flatMap((row) =>
      row.target?.kind === 'scoped_data' && Array.isArray(row.target.parts) ?
        row.target.parts : []))];
    if (!supported.length) {
      partsBlock.hidden = true;
      return;
    }
    partsBlock.hidden = false;
    if (state.parts === null) state.parts = supported.slice();
    const resetActivity = state.sourceKind === 'alias_activity';
    partsBlock.setAttribute('aria-label', resetActivity ? 'Activity to reset' : 'Saved parts to delete');
    partsBlock.append(node('strong', 'retained-statistics-parts-label',
      resetActivity ? 'Activity to reset' : 'Data to delete'));
    PART_ORDER.filter((part) => supported.includes(part)).forEach((part) => {
      const label = node('label', 'retained-statistics-part');
      const input = node('input', 'ui-selection-check');
      input.type = 'checkbox';
      input.value = part;
      input.checked = state.parts.includes(part);
      input.addEventListener('change', () => {
        state.parts = [...partsBlock.querySelectorAll('input:checked')].map((control) => control.value);
        drawResults();
      });
      label.append(input, node('span', '', partLabel(state.sourceKind, state.type, part)));
      partsBlock.append(label);
    });
  }

  function drawPager() {
    const length = state.resultRows.length;
    resultPager.update({
      countText: browsingWorkflows.pageRangeText({ offset: state.resultOffset, visible: length,
        total: state.resultTotal, format: formatNumber }),
      previous: { enabled: !state.resultLoading && state.resultOffset > 0, onClick: () => {
        state.resultOffset = Math.max(0, state.resultOffset - PAGE_SIZE); void loadResults();
      } },
      next: { enabled: !state.resultLoading && state.resultMore, onClick: () => {
        state.resultOffset += PAGE_SIZE; void loadResults();
      } }
    });
    resultPager.hidden = !length && !state.resultOffset ||
      (state.resultOffset === 0 && !state.resultMore);
  }

  function drawResults() {
    const ready = canShowResults(state.sourceKind, selectedSource(), state.type, selectedSite());
    resultsTitle.textContent = state.type ? typeLabel(state.sourceKind, state.type) : 'Results';
    resultContext.hidden = !ready;
    resultSearchForm.hidden = !ready;
    resultCount.hidden = !ready;
    resultTable.replaceChildren();
    if (!ready) {
      resultContext.textContent = '';
      resultCount.textContent = '';
      partsBlock.hidden = true;
      resultStatus.replaceChildren(feedback(!selectedSource() || !state.type ?
        'Choose a system or channel and a data type.' : 'Choose a site.'));
      resultPager.hidden = true;
      return;
    }
    resultSearch.placeholder = ({ frequencies: 'Frequency in MHz or Hz',
      radios: 'Radio alias, over-the-air name, or ID', talkgroups: 'Talkgroup alias or ID',
      band_plans: 'Band or base MHz', foreign_band_plans: 'Foreign system or band',
      alias_activity: 'Alias name or list' })[state.type] || 'Search results';
    resultSearchForm.hidden = ['all', 'issi_assignment_history'].includes(state.type);
    if (state.sourceKind === 'alias_activity') {
      resultContext.replaceChildren('Receiver-wide');
    } else resultContext.replaceChildren(renderSource(selectedSource(), selectedSource()?.label));
    if (selectedSite()) resultContext.append(' · ', renderSource(selectedSite(),
      selectedSite().label));
    else if (state.sourceKind === 'radio_system' && ['all', 'issi_assignment_history'].includes(state.type)) {
      resultContext.append(' · All sites');
    }
    const contextList = state.sourceKind === 'alias_activity' ? null :
      renderAliasList(selectedSite() || selectedSource());
    if (contextList) resultContext.append(' · ', contextList);
    resultStatus.replaceChildren();
    if (state.resultLoading) {
      resultStatus.append(feedback('Loading results…', 'loading'));
    } else if (state.resultError) {
      resultStatus.append(feedback(state.resultError, 'error'));
      resultStatus.append(button('Retry', () => void loadResults()));
    } else if (!state.resultRows.length) {
      resultStatus.append(feedback('No matching results.'));
    } else {
      const aggregate = state.resultRows.length === 1 &&
        state.resultRows[0].target?.kind === 'scoped_data' &&
        state.resultRows[0].target?.record_key == null && state.type !== 'alias_activity';
      resultTable.append(aggregate ? aggregateResult(state.resultRows[0]) : resultsTable());
    }
    drawParts();
    if (state.resultTotal != null) {
      resultCount.textContent = `${formatNumber(state.resultTotal)} result${state.resultTotal === 1 ? '' : 's'}`;
    } else resultCount.textContent = state.resultRows.length ? 'Results' : '';
    drawPager();
  }

  async function loadResults() {
    if (state.disposed || !canShowResults(state.sourceKind, selectedSource(),
      state.type, selectedSite())) return;
    const generation = ++state.resultRequest;
    state.resultLoading = true;
    state.resultError = '';
    drawResults();
    try {
      const response = await read('/results', {
        source_kind: state.sourceKind, source_key: state.sourceKind === 'alias_activity' ?
          undefined : selectedSource().source_key,
        data_type: state.type, site_configuration_id: selectedSite()?.configuration_id,
        q: state.resultSearch, limit: PAGE_SIZE, offset: state.resultOffset
      });
      if (state.disposed || generation !== state.resultRequest) return;
      state.resultRows = Array.isArray(response.rows) ? response.rows : [];
      state.resultMore = response.has_more === true;
      state.resultTotal = response.total_count != null && Number.isInteger(Number(response.total_count)) ?
        Number(response.total_count) : null;
      if (!state.resultRows.length && state.resultOffset > 0) {
        state.resultOffset = Math.max(0, state.resultOffset - PAGE_SIZE);
        void loadResults();
        return;
      }
    } catch (error) {
      if (state.disposed || generation !== state.resultRequest || error?.name === 'AbortError') return;
      state.resultRows = [];
      state.resultError = error.message || 'Results could not be loaded.';
    } finally {
      if (generation === state.resultRequest) {
        state.resultLoading = false;
        drawResults();
      }
    }
  }

  function confirmationPhrase(row, includeChannelHistory) {
    if (row.target?.kind === 'system') return row.label;
    if (['saved_site', 'learned_site'].includes(row.target?.kind) && includeChannelHistory) return 'DELETE SITE';
    return '';
  }

  function removalImpact(row, includeChannelHistory) {
    switch (row.target?.kind) {
      case 'frequency': return 'Deletes this observed frequency from the selected site.';
      case 'radio': return 'Deletes this radio ID and its directly owned retained data.';
      case 'talkgroup': return 'Deletes this talkgroup and its directly owned retained data.';
      case 'conventional_radio':
      case 'conventional_talkgroup':
        return 'Deletes this ID’s saved summary for the listed frequency and timeslot. Call history may remain.';
      case 'learned_site': return includeChannelHistory ?
        'Deletes this learned site and associated saved-channel history.' :
        'Deletes this learned site. Saved-site inventory remains.';
      case 'saved_site': return includeChannelHistory ?
        'Deletes site inventory and saved-channel history.' :
        'Deletes site inventory, including frequencies. Channel history remains.';
      case 'channel': return 'Deletes directly owned statistics for this saved channel. Its configuration remains.';
      case 'system': return includeChannelHistory ?
        'Deletes this system and associated saved-channel history.' : 'Deletes system-owned statistics.';
      default: return 'Deletes this retained statistic.';
    }
  }

  function reviewGlobalAlias() {
    const target = selectedSource()?.target;
    if (!target || target.kind !== 'scoped_data') {
      jobNotice.replaceChildren(feedback('Alias Activity could not be loaded. Try again.', 'error'));
      return;
    }
    reviewScopedRow({ label: 'All Alias Activity',
      detail: 'Call counts and last-heard times across saved aliases', target }, target.parts);
  }

  function reviewScopedRow(row, forcedParts = null) {
    if (busyWithJob()) return;
    let target;
    try {
      target = targetForRemoval(row, forcedParts || selectedPartsFor(row));
    } catch (error) {
      jobNotice.replaceChildren(feedback(error.message, 'error'));
      return;
    }
    const resetActivity = target.source_kind === 'alias_activity';
    const body = node('div', 'retained-statistics-review editor-workspace');
    const summary = node('div', 'ui-fact retained-statistics-review-target');
    summary.append(node('strong', '', row.label || 'Selected data'));
    if (row.detail) summary.append(node('small', 'muted', row.detail));
    appendSystemIdentityFacts(summary, row);
    const scope = node('p', 'retained-statistics-impact',
      target.source_kind === 'alias_activity' ? 'Alias Activity' :
        `${selectedSource()?.label || 'Selected source'}${selectedSite() ?
          ` · ${selectedSite().label}` : ''}`);
    const parts = node('div', 'retained-statistics-review-parts');
    parts.append(node('strong', '', resetActivity ? 'Reset' : 'Delete'), node('span', '', target.parts.map((part) =>
      partLabel(target.source_kind, target.data_type, part)).join(' · ')));
    const previewHost = node('div', 'retained-statistics-preview');
    previewHost.setAttribute('role', 'status');
    const keep = node('div', 'ui-notice', target.source_kind === 'alias_activity' ?
      'Saved aliases and alias lists remain. New activity may return while receiving continues.' :
      'Saved aliases and alias lists remain. New observations after cleanup starts are kept.');
    const errorHost = node('div', 'retained-statistics-review-error');
    errorHost.setAttribute('role', 'alert');
    const cancel = button('Cancel');
    const confirm = button(resetActivity ? 'Reset activity' : 'Delete statistics', null,
      'ui-button ui-button-danger');
    confirm.disabled = true;
    const footer = modalFooter(cancel, confirm);
    footer.classList.add('retained-statistics-review-actions');
    const phrase = target.source_kind === 'alias_activity' && target.record_key == null ?
      'RESET ALIAS ACTIVITY' : target.data_type === 'all' && target.site_configuration_id ?
        'DELETE SITE' : target.data_type === 'all' && target.source_kind === 'radio_system' ?
          selectedSource()?.label || 'DELETE SYSTEM' : target.data_type === 'all' ?
            'DELETE CHANNEL STATISTICS' : '';
    let phraseInput = null;
    if (phrase) {
      phraseInput = node('input', 'ui-input');
      phraseInput.type = 'text';
      phraseInput.autocomplete = 'off';
      phraseInput.spellcheck = false;
      const field = formField(`Type ${phrase} to confirm`, phraseInput);
      field.classList.add('retained-statistics-phrase');
      phraseInput.addEventListener('input', updateConfirm);
      body.append(summary, scope, parts, previewHost, field, keep, errorHost, footer);
    } else body.append(summary, scope, parts, previewHost, keep, errorHost, footer);
    let modal = null;
    let previewReady = false;
    let requestId = null;
    let previewRequest = 0;
    let historyFields = null;
    if (HISTORY_TYPES.has(target.data_type)) {
      const limits = node('details', 'retained-statistics-history-limits');
      limits.append(node('summary', '', 'Limit history (optional)'));
      const fields = node('div', 'retained-statistics-history-fields');
      const from = node('input', 'ui-input');
      from.type = 'datetime-local';
      const before = node('input', 'ui-input');
      before.type = 'datetime-local';
      const frequency = node('input', 'ui-input');
      frequency.type = 'number';
      frequency.min = '0';
      frequency.step = '0.000001';
      frequency.placeholder = 'Any frequency';
      historyFields = { from, before, frequency };
      fields.append(formField('From', from), formField('Before', before),
        formField('Frequency in MHz', frequency));
      const refresh = button('Update preview', () => void loadPreview());
      limits.append(fields, refresh);
      body.insertBefore(limits, previewHost);
      Object.values(historyFields).forEach((input) => input.addEventListener('input', () => {
        previewRequest += 1;
        previewReady = false;
        updateConfirm();
        previewHost.replaceChildren(feedback('Update the preview to check these limits.'));
      }));
    }

    function applyHistoryLimits() {
      if (!historyFields) return;
      const { from, before, frequency } = historyFields;
      const fromMs = from.value ? new Date(from.value).getTime() : null;
      const toMs = before.value ? new Date(before.value).getTime() : null;
      const hz = frequency.value ? Math.round(Number(frequency.value) * 1_000_000) : null;
      if (fromMs !== null && (!Number.isFinite(fromMs) || fromMs < 0) ||
        toMs !== null && (!Number.isFinite(toMs) || toMs < 0)) {
        throw new Error('Enter valid dates.');
      }
      if (fromMs !== null && toMs !== null && fromMs >= toMs) {
        throw new Error('Before must be later than From.');
      }
      if (hz !== null && (!Number.isSafeInteger(hz) || hz <= 0)) {
        throw new Error('Enter a frequency greater than zero.');
      }
      delete target.from_ms;
      delete target.to_ms;
      delete target.frequency_hz;
      if (fromMs !== null) target.from_ms = fromMs;
      if (toMs !== null) target.to_ms = toMs;
      if (hz !== null) target.frequency_hz = hz;
      requestId = null;
    }

    function updateConfirm() {
      confirm.disabled = !previewReady || Boolean(phrase && phraseInput?.value !== phrase);
    }

    async function loadPreview() {
      if (!modal || (modal.ready && !await modal.ready) || !body.isConnected) return;
      const generation = ++previewRequest;
      previewReady = false;
      updateConfirm();
      previewHost.replaceChildren(feedback('Checking affected records…', 'loading'));
      errorHost.replaceChildren();
      try {
        applyHistoryLimits();
        const preview = await requestJson(`${ROOT}/preview`, {
          method: 'POST', body: { target }, signal, timeoutMs: 30000
        });
        if (state.disposed || !body.isConnected || generation !== previewRequest) return;
        if (preview.outcome !== 'found') {
          const message = preview.outcome === 'stale_site' ?
            'This site changed. Choose it again.' : preview.outcome === 'too_large' ?
              'This receiver needs an update to clean up large selections.' :
              'These records are already gone.';
          previewHost.replaceChildren(feedback(message));
          return;
        }
        const counts = preview.counts_by_part || {};
        const partIcons = { current: 'icon-spectrum', summary: 'icon-dashboard',
          buckets: 'icon-replay', events: 'icon-scan-lists' };
        const list = metrics(target.parts.map((part) => [
          partLabel(target.source_kind, target.data_type, part), counts[part] ?? 0,
          undefined, { icon: partIcons[part] }
        ]), true);
        list.classList.add('retained-statistics-preview-counts');
        const total = node('strong', 'retained-statistics-preview-total',
          `${formatNumber(preview.rows_total ?? 0)} directly matched records`);
        previewHost.replaceChildren(total, list);
        if (!resetActivity) previewHost.append(node('small', 'muted',
          'Cleanup runs in the background while receiving continues.'));
        if (Array.isArray(preview.effects) && preview.effects.length) {
          const effects = node('ul', 'retained-statistics-effects');
          preview.effects.forEach((effect) => effects.append(node('li', '', String(effect))));
          previewHost.append(effects);
        }
        previewReady = Number(preview.rows_total) > 0;
        updateConfirm();
      } catch (error) {
        if (state.disposed || !body.isConnected || generation !== previewRequest ||
          error?.name === 'AbortError') return;
        previewHost.replaceChildren(feedback('Affected records could not be checked.', 'error'));
        if (historyFields && !error.status) errorHost.append(feedback(error.message, 'error'));
        previewHost.append(button('Retry', () => void loadPreview()));
      }
    }

    cancel.addEventListener('click', () => modal?.close());
    confirm.addEventListener('click', async () => {
      if (confirm.disabled || !modal) return;
      modal.setBusy(true);
      confirm.disabled = true;
      errorHost.replaceChildren();
      try {
        requestId ||= crypto.randomUUID();
        const response = await requestJson(`${ROOT}/deletions`, {
          method: 'POST', body: { request_id: requestId, target },
          signal, timeoutMs: 30000
        });
        if (state.disposed) return;
        modal.setBusy(false);
        modal.close();
        state.job = response;
        state.jobTarget = target;
        state.jobSource = { kind: state.sourceKind, key: selectedSource()?.source_key,
          label: [selectedSource()?.label, selectedSite()?.label].filter(Boolean).join(' · ') };
        rememberJob();
        drawJob();
        void pollJob();
      } catch (error) {
        if (state.disposed || error?.name === 'AbortError') return;
        modal.setBusy(false);
        errorHost.replaceChildren(feedback(error.status === 409 ?
          'Saved data changed. Close this dialog and choose it again.' :
          error.message || (resetActivity ? 'Activity reset could not be started.' :
            'Deletion could not be started.'), 'error'));
        if (error.status === 409) previewReady = false;
        updateConfirm();
      }
    });
    modal = openReadOnlyModal(`${resetActivity ? 'Reset' : 'Delete'} ${row.label || 'saved data'}?`, body, {
      id: 'retained-statistics-review', className: 'retained-statistics-modal',
      returnFocusSelector: target.source_kind === 'alias_activity' &&
        target.record_key == null ? '.retained-statistics-all-button' :
          '.retained-statistics-result-count'
    });
    if (modal) void loadPreview();
  }

  function reviewRow(row) {
    if (busyWithJob()) return;
    if (row.target?.kind === 'scoped_data') return reviewScopedRow(row);
    const body = node('div', 'retained-statistics-review editor-workspace');
    const summary = node('div', 'ui-fact retained-statistics-review-target');
    summary.append(node('strong', '', row.label || 'Selected item'));
    if (row.detail) summary.append(node('small', 'muted', row.detail));
    appendSystemIdentityFacts(summary, row);
    const impact = node('p', 'retained-statistics-impact');
    const reappearance = node('div', 'ui-notice ui-notice-warning',
      'Receiving continues; deleted rows may return. Alias Activity totals may remain.');
    const errorHost = node('div', 'retained-statistics-review-error');
    errorHost.setAttribute('role', 'alert');
    const cancel = button('Cancel');
    const confirm = button('Delete statistics', null, 'ui-button ui-button-danger');
    const footer = modalFooter(cancel, confirm);
    footer.classList.add('retained-statistics-review-actions');
    let includeChannelHistory = false;
    let phraseInput = null;
    let modal = null;
    let submittedTarget = null;
    let requestId = null;
    const scoped = ['learned_site', 'saved_site', 'system'].includes(row.target?.kind);
    const scopeChoices = row.target?.kind === 'system' ? [
      { value: 'record', label: 'System statistics' },
      { value: 'history', label: 'System + channel history' }
    ] : row.target?.kind === 'saved_site' ? [
      { value: 'record', label: 'Site inventory' },
      { value: 'history', label: 'Site inventory + channel history' }
    ] : [
      { value: 'record', label: 'Learned site' },
      { value: 'history', label: 'Learned site + channel history' }
    ];
    const scope = scoped ? uiSegmentedControl(scopeChoices, 'record', (value) => {
      includeChannelHistory = value === 'history';
      drawReview();
    }) : null;
    if (scope) {
      scope.setAttribute('aria-label', 'Deletion scope');
      scope.classList.add('retained-statistics-scope-control');
      const scopeField = node('div', 'admin-form-field ui-field');
      scopeField.append(node('span', 'admin-form-label ui-field-label', 'Scope'), scope);
      body.append(scopeField);
    }
    body.prepend(summary);
    body.append(impact, reappearance, errorHost, footer);

    function drawReview() {
      impact.textContent = removalImpact(row, includeChannelHistory);
      const old = body.querySelector('.retained-statistics-phrase');
      old?.remove();
      phraseInput = null;
      const phrase = confirmationPhrase(row, includeChannelHistory);
      if (phrase) {
        phraseInput = node('input', 'ui-input');
        phraseInput.type = 'text';
        phraseInput.autocomplete = 'off';
        phraseInput.spellcheck = false;
        phraseInput.addEventListener('input', updateConfirm);
        const field = formField(`Type ${phrase} to confirm`, phraseInput);
        field.classList.add('retained-statistics-phrase');
        body.insertBefore(field, reappearance);
      }
      updateConfirm();
    }

    function updateConfirm() {
      const phrase = confirmationPhrase(row, includeChannelHistory);
      confirm.disabled = Boolean(phrase && phraseInput?.value !== phrase);
    }

    cancel.addEventListener('click', () => modal?.close());
    confirm.addEventListener('click', async () => {
      if (confirm.disabled || !modal) return;
      modal.setBusy(true);
      confirm.disabled = true;
      errorHost.replaceChildren();
      try {
        if (!submittedTarget) {
          submittedTarget = targetForRemoval(row, includeChannelHistory);
          if (submittedTarget.kind === 'frequency') {
            submittedTarget.expected_site_key = submittedTarget.expected_site_key || selectedSite()?.site_key;
          }
          requestId = crypto.randomUUID();
          scope?.querySelectorAll('button').forEach((control) => { control.disabled = true; });
        }
        const response = await requestJson(`${ROOT}/deletions`, {
          method: 'POST', body: { request_id: requestId, target: submittedTarget },
          signal, timeoutMs: 30000
        });
        if (state.disposed) return;
        modal.setBusy(false);
        modal.close();
        state.job = response;
        state.jobTarget = submittedTarget;
        state.jobSource = { kind: state.sourceKind, key: selectedSource()?.source_key,
          label: [selectedSource()?.label, selectedSite()?.label].filter(Boolean).join(' · ') };
        rememberJob();
        drawJob();
        void pollJob();
      } catch (error) {
        if (state.disposed || error?.name === 'AbortError') return;
        if (error?.status === 409) {
          modal.setBusy(false);
          errorHost.replaceChildren(feedback('Deletion request conflicted. Close this dialog and choose the item again.',
            'error'));
          confirm.disabled = true;
          return;
        }
        errorHost.replaceChildren(feedback(error.message || 'Deletion could not be started.', 'error'));
        modal.setBusy(false);
        updateConfirm();
      }
    });
    modal = openReadOnlyModal(`Delete ${row.label || 'item'}?`, body, {
      id: 'retained-statistics-review', className: 'retained-statistics-modal',
      returnFocusSelector: '.retained-statistics-result-count'
    });
    if (!modal) return;
    drawReview();
  }

  function rememberJob() {
    try {
      if (state.job?.job_id && state.job.state !== 'succeeded') {
        window.sessionStorage.setItem(JOB_STORAGE_KEY, state.job.job_id);
      } else window.sessionStorage.removeItem(JOB_STORAGE_KEY);
    } catch { /* Cleanup still works when browser storage is unavailable. */ }
  }

  function setJob(response) {
    state.job = response;
    if (response.target) {
      state.jobTarget = response.target;
      state.jobSource = { kind: response.target.source_kind, key: response.target.source_key,
        label: state.jobSource?.label };
    }
    rememberJob();
  }

  async function changeJob(action) {
    if (!state.job?.job_id || state.jobActionPending) return;
    state.jobActionPending = true;
    state.jobRequest += 1;
    drawJob();
    try {
      const response = await requestJson(`${ROOT}/deletions/${encodeURIComponent(state.job.job_id)}/${action}`,
        { method: 'POST', body: {}, signal, timeoutMs: 30000 });
      if (state.disposed) return;
      setJob(response);
      window.clearTimeout(state.jobTimer);
      drawJob();
      if (busyWithJob()) void pollJob();
      else void loadResults();
    } catch (error) {
      if (state.disposed || error?.name === 'AbortError') return;
      jobNotice.append(feedback(action === 'cancel' ?
        'Cleanup could not be stopped. Try again.' : 'Cleanup could not be resumed. Try again.', 'error'));
      if (busyWithJob()) {
        window.clearTimeout(state.jobTimer);
        void pollJob();
      }
    } finally {
      state.jobActionPending = false;
      if (!state.disposed) jobNotice.querySelectorAll('button').forEach((control) => {
        control.disabled = false;
      });
    }
  }

  function drawJob() {
    jobNotice.replaceChildren();
    if (!state.job) return;
    const stateName = String(state.job.state || '').toLowerCase();
    const resetActivity = state.jobTarget?.source_kind === 'alias_activity';
    if (state.jobTarget?.data_type) {
      const sourceName = state.jobSource?.label || (state.sourceKind === state.jobTarget.source_kind ?
        state.sourceRows.find((row) => row.source_key === state.jobTarget.source_key)?.label : '');
      const context = state.job.label || [sourceName, typeLabel(state.jobTarget.source_kind, state.jobTarget.data_type)]
        .filter(Boolean).join(' · ');
      jobNotice.append(node('strong', '', context));
    }
    const deleted = Number(state.job.rows_deleted ?? 0);
    const total = Number(state.job.rows_total);
    const hasProgress = Number.isFinite(total) && total > 0;
    const progress = hasProgress ? `${formatNumber(deleted)}${stateName === 'succeeded' ? '' :
      ` of ${formatNumber(total)}`} records ${resetActivity ? 'reset' : 'deleted'}` : '';
    if (stateName === 'queued') jobNotice.append(feedback(resetActivity ? 'Activity reset queued…' :
      'Deletion queued…', 'loading'));
    else if (stateName === 'running') jobNotice.append(feedback(resetActivity ? 'Resetting activity…' :
      'Deleting statistics…', 'loading'));
    else if (stateName === 'cancelling') jobNotice.append(feedback('Stopping after the current batch…', 'loading'));
    else if (stateName === 'cancelled') jobNotice.append(feedback('Cleanup stopped. Deleted records stay deleted.'));
    else if (stateName === 'succeeded') {
      const message = state.job.outcome === 'stale_site' ? 'Site changed. Choose it again.' :
        state.job.outcome === 'too_large' ?
          'This receiver needs an update to clean up large selections.' :
          state.job.outcome === 'not_found' ? 'The item was already gone.' :
            resetActivity ? 'Activity reset.' : 'Statistics deleted.';
      jobNotice.append(feedback(message));
    } else if (stateName === 'failed') {
      jobNotice.append(feedback(state.job.error || (resetActivity ? 'Activity reset failed. Try again.' :
        'Deletion failed. Try again.'), 'error'));
    }
    if (progress) jobNotice.append(node('strong', 'retained-statistics-job-progress', progress));
    const retained = Number(state.job.rows_retained ?? 0);
    if (retained > 0) jobNotice.append(node('small', 'muted',
      `${formatNumber(retained)} record${retained === 1 ? '' : 's'} kept because ${
        retained === 1 ? 'it changed' : 'they changed'} during cleanup.`));
    if (state.job.cutoff_ms && !resetActivity) {
      const cutoff = node('small', 'muted');
      cutoff.append('New observations after ', formatDateTime(state.job.cutoff_ms), ' are kept.');
      jobNotice.append(cutoff);
    }
    const actions = node('div', 'ui-action-row');
    if (['queued', 'running'].includes(stateName)) {
      actions.append(button('Stop cleanup', () => void changeJob('cancel')));
    } else if (['cancelled', 'failed'].includes(stateName) && state.job.can_resume) {
      actions.append(button('Resume cleanup', () => void changeJob('resume')));
    }
    actions.querySelectorAll('button').forEach((control) => {
      control.disabled = state.jobActionPending;
    });
    if (actions.childElementCount) jobNotice.append(actions);
    drawResults();
  }

  async function pollJob() {
    if (state.disposed || !busyWithJob()) return;
    const generation = ++state.jobRequest;
    try {
      const response = await requestJson(`${ROOT}/deletions/${encodeURIComponent(state.job.job_id)}`,
        { csrf: false, signal });
      if (state.disposed || generation !== state.jobRequest) return;
      setJob(response);
      drawJob();
      if (busyWithJob()) state.jobTimer = window.setTimeout(pollJob, JOB_POLL_MS);
      else {
        const target = state.jobTarget;
        const submittedSource = state.jobSource;
        const staleSite = state.job.state === 'succeeded' && state.job.outcome === 'stale_site';
        const tooLarge = state.job.state === 'succeeded' && state.job.outcome === 'too_large';
        const sourceIsCurrent = selectedSource()?.source_key === submittedSource?.key &&
          state.sourceKind === submittedSource?.kind;
        const removedSource = state.job.state === 'succeeded' && !staleSite && !tooLarge &&
          sourceIsCurrent &&
          (target?.kind === 'system' && state.sourceKind === 'radio_system' ||
            target?.kind === 'channel' && state.sourceKind === 'saved_channel');
        if (removedSource) {
          state.source = null;
          state.type = '';
          clearSite();
          clearResults();
          drawSourceChoices();
          drawTypes();
          drawSite();
          drawResults();
          void loadSources(true);
        } else {
          const removedSelectedSite = staleSite || state.job.state === 'succeeded' && !tooLarge &&
            (['saved_site', 'channel'].includes(target?.kind) &&
              selectedSite()?.configuration_id === target?.configuration_id);
          if (removedSelectedSite) {
            clearSite();
            clearResults();
            drawSite();
            drawResults();
          } else void loadResults();
          if (requiresSite(state.sourceKind, selectedSource(), state.type) ||
            state.sourceKind === 'radio_system' && state.type === 'all') void loadSites(true);
        }
      }
    } catch (error) {
      if (state.disposed || generation !== state.jobRequest || error?.name === 'AbortError') return;
      if (error.status === 404) {
        state.job = null;
        rememberJob();
        jobNotice.replaceChildren(feedback(
          'Cleanup status is no longer available. Choose the same data again to delete any remaining records.',
          'error'));
        drawResults();
        return;
      }
      jobNotice.replaceChildren(feedback(state.jobTarget?.source_kind === 'alias_activity' ?
        'Activity reset status is unavailable.' : 'Deletion status is unavailable.', 'error'),
        button('Retry status', () => void pollJob()));
    }
  }

  async function recoverJob() {
    let id;
    try { id = window.sessionStorage.getItem(JOB_STORAGE_KEY); } catch { return; }
    if (!id) return;
    state.job = { job_id: id, state: 'running' };
    drawJob();
    await pollJob();
  }

  sourceSelect.addEventListener('change', () => {
    state.source = state.sourceRows.find((row) => row.source_key === sourceSelect.value) ||
      (state.source?.source_key === sourceSelect.value ? state.source : null);
    state.type = '';
    clearSite();
    clearResults();
    drawTypes();
    drawSite();
    drawResults();
  });
  sourceSearch.addEventListener('input', () => {
    window.clearTimeout(state.choiceTimer);
    state.sourceSearch = sourceSearch.value.trim();
    state.choiceTimer = window.setTimeout(() => void loadSources(true), 250);
  });
  siteSelect.addEventListener('change', () => {
    state.site = state.siteRows.find((row) => row.configuration_id === siteSelect.value) ||
      (state.site?.configuration_id === siteSelect.value ? state.site : null);
    clearResults();
    drawSite();
    drawResults();
    if (selectedSite() || state.type === 'all') void loadResults();
  });
  siteSearch.addEventListener('input', () => {
    window.clearTimeout(state.choiceTimer);
    state.siteSearch = siteSearch.value.trim();
    state.choiceTimer = window.setTimeout(() => void loadSites(true), 250);
  });
  resultSearchForm.addEventListener('submit', (event) => {
    event.preventDefault();
    window.clearTimeout(state.resultTimer);
    state.resultSearch = resultSearch.value.trim();
    state.resultOffset = 0;
    void loadResults();
  });
  resultSearch.addEventListener('input', () => {
    window.clearTimeout(state.resultTimer);
    state.resultTimer = window.setTimeout(() => {
      state.resultSearch = resultSearch.value.trim();
      state.resultOffset = 0;
      void loadResults();
    }, 300);
  });
  signal?.addEventListener('abort', () => {
    state.disposed = true;
    window.clearTimeout(state.choiceTimer);
    window.clearTimeout(state.resultTimer);
    window.clearTimeout(state.jobTimer);
  }, { once: true });
  drawSourceChoices();
  drawTypes();
  drawSite();
  drawResults();
  void loadSources(true);
  void recoverJob();
  return host;
}

export { canShowResults, queryPath, targetForRemoval, retainedSystemIdentityFacts };
