const API_ROOT = '/api/v1/admin/radioreference';

export const RADIO_REFERENCE_IMPORT_PATHS = Object.freeze({
  configuration: API_ROOT,
  countries: `${API_ROOT}/countries`,
  states: `${API_ROOT}/states`,
  counties: `${API_ROOT}/counties`,
  browse: `${API_ROOT}/browse`,
  browseCatalog: `${API_ROOT}/browse/catalog`,
  systemDetails: `${API_ROOT}/systems/details`,
  systemSites: `${API_ROOT}/systems/sites`,
  siteCatalog: `${API_ROOT}/systems/sites/catalog`,
  systemTalkgroups: `${API_ROOT}/systems/talkgroups`,
  talkgroupCatalog: `${API_ROOT}/systems/talkgroups/catalog`,
  bookmarks: `${API_ROOT}/bookmarks`,
  systemPreferences: `${API_ROOT}/system-preferences`,
  conventionalCategories: `${API_ROOT}/conventional/categories`,
  conventionalFrequencies: `${API_ROOT}/conventional/frequencies`,
  sitePreview: `${API_ROOT}/imports/site/preview`,
  conventionalPreview: `${API_ROOT}/imports/conventional/preview`,
  talkgroupsPreview: `${API_ROOT}/imports/talkgroups/preview`,
  apply: (previewId) => `${API_ROOT}/imports/${encodeURIComponent(String(previewId))}/apply`
});

const SITE_LIMIT = 50;
const TALKGROUP_LIMIT = 50;
const FREQUENCY_LIMIT = 50;
const COMMON_COUNTRY_CODES = Object.freeze(['US', 'CA', 'AU', 'UK', 'GB']);

export function sortRadioReferenceCountries(countries) {
  const rank = country => {
    const code = String(country?.abbreviation ?? country?.code ?? '').trim().toUpperCase();
    const index = COMMON_COUNTRY_CODES.indexOf(code);
    return index < 0 ? COMMON_COUNTRY_CODES.length : index;
  };
  const compareNames = (left, right) => {
    if (left == null) return right == null ? 0 : 1;
    if (right == null) return -1;
    const first = String(left).toLowerCase();
    const second = String(right).toLowerCase();
    return first < second ? -1 : first > second ? 1 : 0;
  };
  return (Array.isArray(countries) ? [...countries] : []).filter(Boolean).sort((left, right) =>
    rank(left) - rank(right) || compareNames(left.name, right.name) || Number(left.id) - Number(right.id));
}

function rows(documentValue) {
  if (Array.isArray(documentValue)) return documentValue;
  for (const key of ['items', 'rows', 'results', 'talkgroups', 'frequencies', 'sites', 'categories']) {
    if (Array.isArray(documentValue?.[key])) return documentValue[key];
  }
  return [];
}

function firstValue(value, keys, fallback = null) {
  for (const key of keys) {
    const candidate = value?.[key];
    if (candidate !== null && candidate !== undefined && candidate !== '') return candidate;
  }
  return fallback;
}

function integerValue(value, keys) {
  const numeric = Number(firstValue(value, keys));
  return Number.isSafeInteger(numeric) && numeric > 0 ? numeric : null;
}

function textValue(value, keys, fallback = '') {
  const result = firstValue(value, keys, fallback);
  return result === null || result === undefined ? fallback : String(result).trim();
}

function totalValue(documentValue, fallback) {
  const total = Number(firstValue(documentValue, ['total_count', 'total_items', 'total'], fallback));
  return Number.isSafeInteger(total) && total >= 0 ? total : fallback;
}

function hasMore(documentValue, offset, limit, visible) {
  const explicit = firstValue(documentValue, ['has_more', 'hasMore']);
  if (typeof explicit === 'boolean') return explicit;
  return offset + visible < totalValue(documentValue, offset + visible);
}

function aliasListId(value) {
  return integerValue(value, ['aliasListId', 'alias_list_id', 'id']);
}

function normalizedAliasFamily(value) {
  const family = String(value || '').trim().toUpperCase();
  return family === 'ANALOG' ? 'NBFM' : family;
}

function aliasListFamily(value) {
  return normalizedAliasFamily(textValue(value, ['family', 'protocol_family', 'protocol']));
}

function compatibleFamily(value) {
  const family = textValue(value, ['alias_list_family', 'protocol_family', 'family', 'protocol',
    'decoder_type', 'recommended_decoder', 'mode_name', 'mode', 'type']).toUpperCase();
  if (family.includes('P25') || family.includes('PROJECT 25')) return 'P25';
  if (family.includes('DMR')) return 'DMR';
  if (family.includes('NXDN')) return 'NXDN';
  if (family.includes('AM') || family.includes('FM') || family.includes('ANALOG')) return 'NBFM';
  return '';
}

function defaultAliasListName(family) {
  const normalized = normalizedAliasFamily(family);
  if (!normalized) return '';
  return normalized === 'NBFM' ? 'Default Analog' : `Default ${normalized}`;
}

function systemTypeLabel(value) {
  const type = textValue(value, ['system_type', 'systemType', 'type_name']) ||
    (entryKind(value) === 'TRUNKED_SYSTEM' ? '' : textValue(value, ['type']));
  const family = compatibleFamily({ type });
  return family === 'P25' || family === 'DMR' || family === 'NXDN' ? family : type;
}

function directoryFamily(value) {
  return entryKind(value) === 'CONVENTIONAL_AGENCY' ? 'CONVENTIONAL' : compatibleFamily({
    type: textValue(value, ['system_type', 'systemType'])
  });
}

function directoryUpdated(value) {
  const timestamp = Number(firstValue(value, ['last_updated_epoch_millis', 'lastUpdatedEpochMillis'], 0));
  return Number.isFinite(timestamp) && timestamp > 0 ? timestamp : 0;
}

function query(path, values) {
  const parameters = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') parameters.set(key, String(value));
  });
  const suffix = parameters.toString();
  return suffix ? `${path}?${suffix}` : path;
}

function previewId(value) {
  return textValue(value, ['preview_id', 'previewId', 'id']);
}

function systemId(value) {
  // System details also contain a radio network's "system_id"; its RadioReference database ID is "id".
  return integerValue(value?.detail || value, ['id', 'sid', 'system_id', 'systemId']);
}

function ownerId(value) {
  return integerValue(value?.detail || value, ['owner_id', 'ownerId', 'agency_id', 'agencyId', 'id']);
}

function entryKind(value) {
  const kind = textValue(value, ['type', 'kind'], textValue(value?.detail, ['type', 'kind'])).toUpperCase();
  return kind.includes('TRUNK') || kind.includes('SYSTEM') ? 'TRUNKED_SYSTEM' : 'CONVENTIONAL_AGENCY';
}

function frequencyHz(value) {
  if (typeof value === 'number') return value < 100000 ? Math.round(value * 1_000_000) : Math.round(value);
  const numeric = Number(firstValue(value, ['frequency_hz', 'frequencyHz', 'downlink_hz', 'downlinkHz',
    'output_hz', 'outputHz', 'frequency']));
  if (!Number.isFinite(numeric) || numeric <= 0) return 0;
  return numeric < 100000 ? Math.round(numeric * 1_000_000) : Math.round(numeric);
}

function siteChannels(site, detail = null) {
  const candidates = [site?.channels, site?.frequencies, detail?.channels, detail?.frequencies];
  return candidates.find(Array.isArray) || [];
}

function siteModulation(site) {
  const modulation = textValue(site, ['detected_modulation', 'detectedModulation', 'p25_modulation',
    'p25Modulation', 'modulation']);
  return modulation;
}

function siteUsesTdmaControl(site, detail = null) {
  const value = firstValue(site, ['tdma_control_channel', 'tdmaControlChannel'],
    firstValue(detail?.site || detail, ['tdma_control_channel', 'tdmaControlChannel']));
  if (value !== null && value !== undefined) {
    return value === true || value === 1 || String(value).toLowerCase() === 'true';
  }
  const protocol = textValue(site, ['protocol_id', 'protocol', 'decoder_type'],
    textValue(detail?.site || detail, ['protocol_id', 'protocol', 'decoder_type'])).toLowerCase();
  return protocol.includes('phase ii') || protocol.includes('phase 2') || protocol.includes('p25-phase2') ||
    protocol.includes('tdma');
}

function detectedSiteModulation(system, site, detail = null) {
  const family = compatibleFamily(system) || compatibleFamily(detail) || compatibleFamily(site);
  if (family !== 'P25' || siteUsesTdmaControl(site, detail)) return '';
  const description = [textValue(site, ['name', 'description']),
    textValue(detail?.site || detail, ['name', 'description'])].join(' ').toLowerCase();
  if (description.includes('simul')) return 'CQPSK';
  const structured = [siteModulation(site), siteModulation(detail?.site || detail)].join(' ').toUpperCase();
  return structured.includes('CQPSK') || structured.includes('LSM') ? 'CQPSK' : 'C4FM';
}

function talkgroupId(value) {
  return integerValue(value?.talkgroup || value, ['talkgroup_id', 'talkgroupId', 'id']);
}

function talkgroupValue(value) {
  return value?.talkgroup || value;
}

function talkgroupCategory(value) {
  const category = firstValue(value, ['category', 'category_name', 'group']);
  if (category && typeof category === 'object') {
    return textValue(category, ['name', 'category_name', 'label'], '—');
  }
  return category === null || category === undefined || category === '' ? '—' : String(category);
}

function talkgroupChanges(value) {
  if (!Array.isArray(value?.changes)) return [];
  const owned = new Set(['name', 'description', 'group']);
  return value.changes.filter((change) => owned.has(textValue(change, ['field']).toLowerCase()));
}

const IMPORTED_FIELD_LABELS = { name: 'Alpha tag', description: 'Description', group: 'Category' };

function importedChangeValues(change) {
  const before = String(change?.before ?? '');
  const after = String(change?.after ?? '');
  const visibleText = (value) => value.replace(/\p{Cf}/gu, '').replace(/\s+/gu, ' ').trim();
  const hiddenDifference = before !== after && visibleText(before) === visibleText(after);
  const escaped = (value) => JSON.stringify(value).replace(/ /g, '␠')
    .replace(/[\p{Cf}\p{White_Space}]/gu,
      (character) => character.split('').map((unit) =>
        `\\u${unit.charCodeAt(0).toString(16).padStart(4, '0')}`).join(''));
  return { before: hiddenDifference ? escaped(before) : before || 'Empty',
    after: hiddenDifference ? escaped(after) : after || 'Empty', hiddenDifference };
}

function frequencyId(value) {
  return integerValue(value, ['frequency_id', 'frequencyId', 'id']);
}

function importStatus(value) {
  const status = textValue(value, ['import_status', 'importStatus', 'status'], 'UNCOMPARED').toUpperCase();
  if (status === 'UNCOMPARED') return { label: 'Choose Alias List', tone: 'neutral' };
  if (status === 'IDENTICAL') return { label: 'Identical', tone: 'success' };
  if (status === 'DIFFERENT') return { label: 'Different', tone: 'warning' };
  if (status === 'NOT_COMPATIBLE' || status === 'INCOMPATIBLE') {
    return { label: 'Not compatible', tone: 'danger' };
  }
  return { label: 'Not present', tone: 'neutral' };
}

function bookmarkKey(value) {
  return [value?.kind, firstValue(value, ['ownerKind', 'owner_kind'], ''),
    Number(firstValue(value, ['parentId', 'parent_id'], 0)), Number(value?.id || 0)].join(':');
}

function normalizeBookmark(value) {
  return {
    kind: textValue(value, ['kind']), id: integerValue(value, ['id']),
    parentId: Number(firstValue(value, ['parent_id', 'parentId'], 0)),
    ownerKind: textValue(value, ['owner_kind', 'ownerKind']),
    name: textValue(value, ['name']),
    parentName: textValue(value, ['parent_name', 'parentName']),
    preferredAliasListId: integerValue(value, ['preferred_alias_list_id', 'preferredAliasListId'])
  };
}

function resultCount(value, key) {
  const summary = value?.summary || value?.counts || value || {};
  const numeric = Number(firstValue(summary, [key, key === 'add' ? 'added' : key === 'update' ? 'updated' :
    key === 'unchanged' ? 'identical' : key], 0));
  return Number.isSafeInteger(numeric) && numeric >= 0 ? numeric : 0;
}

export function createRadioReferenceImportWorkspace(dependencies) {
  const {
    node, iconGlyph, metricCard, formField, uiSelectFrame, uiPill, uiSegmentedControl, table,
    openReadOnlyModal, closeReadOnlyModal, requestJson, formatFrequency, formatNumber, href, anchor,
    modalFooter, createAliasListPopupTrigger, browsingWorkflows, directoryTimeoutMs = 15_000, mutationTimeoutMs = 65_000,
    onLocationSaved = null
  } = dependencies;

  const host = node('div', 'radioreference-import-workspace data-workspace');
  const state = {
    configuration: null,
    aliasLists: [],
    aliasListRevision: 0,
    countries: [],
    initialized: false,
    initializing: false,
    country: null,
    region: null,
    county: null,
    browseRows: [],
    browseDocument: null,
    browseTab: 'browse',
    directorySort: 'updated',
    directoryType: 'ALL',
    bookmarks: [],
    systemAliasListPreferences: new Map(),
    clearedSystemPreferences: new Set(),
    preferenceSaves: new Map(),
    preferenceRevisions: new Map(),
    preferenceErrors: new Map(),
    preferenceLoadError: '',
    siteCatalogs: new Map(),
    talkgroupCatalogs: new Map(),
    talkgroupLoads: new Map(),
    talkgroupCatalogId: null,
    siteSearch: '',
    siteSort: 'number',
    selectedCountyId: null,
    selectedCountyName: '',
    talkgroupStatus: 'ALL',
    browseSequence: 0,
    detailSequence: 0,
    activeSystemId: null,
    selectedTalkgroups: new Set(),
    talkgroupAliasListId: null,
    talkgroupOffset: 0,
    talkgroupSearch: '',
    talkgroupCategoryId: null,
    initializeSequence: 0
  };

  const feedback = (message, kind = '') => node('div', `ui-feedback${kind ? ` ui-feedback-${kind}` : ''}`,
    message);
  const empty = (title, message) => {
    const value = node('div', 'ui-empty-state');
    value.append(node('h3', '', title), node('p', '', message));
    return value;
  };
  const directoryState = (value) => {
    const stateHost = node('div', 'radioreference-directory-state');
    stateHost.append(value);
    return stateHost;
  };
  const button = (label, className = 'ui-button ui-button-secondary') => {
    const control = node('button', className, label);
    control.type = 'button';
    return control;
  };
  const select = () => node('select', 'ui-select');
  const input = (type = 'text') => {
    const control = node('input', 'ui-input');
    control.type = type;
    return control;
  };
  const selectFrame = (control) => uiSelectFrame(control);
  const setOptions = (control, values, selected, placeholder = '', optional = false) => {
    control.replaceChildren();
    if (optional || !values.length) {
      const option = node('option', '', placeholder);
      option.value = '';
      control.append(option);
    }
    values.forEach((value) => {
      const id = integerValue(value, ['id', 'country_id', 'state_id', 'county_id', 'category_id',
        'sub_category_id']);
      if (!id) return;
      const abbreviation = textValue(value, ['abbreviation', 'abbr']);
      const label = textValue(value, ['name', 'label', 'category_name', 'sub_category_name'], `Item ${id}`);
      const option = node('option', '', `${label}${abbreviation ? ` (${abbreviation})` : ''}`);
      option.value = String(id);
      control.append(option);
    });
    const requested = String(selected || '');
    if (requested && [...control.options].some((option) => option.value === requested)) control.value = requested;
    else if (!optional && control.options.length) control.selectedIndex = 0;
    return Boolean(control.value);
  };
  const aliasOptions = (family = '') => {
    const normalized = normalizedAliasFamily(family);
    const exact = state.aliasLists.filter((value) => !normalized || aliasListFamily(value) === normalized);
    return normalized ? exact : state.aliasLists;
  };
  const populateAliasSelect = (control, family = '', selectedId = null, useDefault = true) => {
    const options = aliasOptions(family);
    control.replaceChildren();
    const placeholder = node('option', '', options.length ? 'Choose an Alias List' : 'No compatible Alias Lists');
    placeholder.value = '';
    control.append(placeholder);
    options.forEach((value) => {
      const id = aliasListId(value);
      if (!id) return;
      const option = node('option', '', textValue(value, ['name'], `Alias List ${id}`));
      option.value = String(id);
      control.append(option);
    });
    const requested = String(selectedId || '');
    if (requested && [...control.options].some((option) => option.value === requested)) control.value = requested;
    else if (useDefault) {
      const canonicalName = defaultAliasListName(family).toLowerCase();
      const canonical = options.find((value) => textValue(value, ['name']).toLowerCase() === canonicalName);
      const canonicalId = aliasListId(canonical);
      if (canonicalId) control.value = String(canonicalId);
      else if (control.options.length === 2) control.selectedIndex = 1;
    }
    return control;
  };
  const aliasSelect = (family = '', selectedId = null, useDefault = true) => {
    const control = select();
    control.required = true;
    populateAliasSelect(control, family, selectedId, useDefault);
    return control;
  };
  const currentAliasListRevision = async () => {
    const documentValue = await requestJson('/api/v1/admin/alias-lists?include_counts=false', { csrf: false });
    const revision = Number(documentValue?.revision);
    if (!Number.isSafeInteger(revision) || revision < 0) {
      throw new Error('The current Alias List revision is unavailable. Reload and try again.');
    }
    state.aliasListRevision = revision;
    return revision;
  };
  const aliasListField = (label, control, family, helperText) => {
    control.setAttribute('aria-label', label);
    const field = node('div', 'admin-form-field ui-field');
    field.append(node('span', 'admin-form-label ui-field-label', label), selectFrame(control),
      createAliasListPopupTrigger({
        select: control,
        family: normalizedAliasFamily(family),
        getRevision: currentAliasListRevision,
        triggerLabel: 'New list',
        submitLabel: 'Create and use',
        helperText,
        onCreated: ({ aliasList, revision }) => {
          const id = aliasListId(aliasList);
          if (!id) return;
          state.aliasLists = state.aliasLists.filter((value) => aliasListId(value) !== id);
          state.aliasLists.push(aliasList);
          const nextRevision = Number(revision);
          if (Number.isSafeInteger(nextRevision) && nextRevision >= 0) state.aliasListRevision = nextRevision;
          populateAliasSelect(control, family, id);
          control.dispatchEvent(new Event('change', { bubbles: true }));
        }
      }));
    return field;
  };
  const api = (path, options = {}) => requestJson(path, {
    csrf: options.method && options.method !== 'GET' ? undefined : false,
    timeoutMs: options.timeoutMs || directoryTimeoutMs,
    ...options
  });
  const showPreferenceError = (hostValue, systemIdValue) => {
    hostValue.replaceChildren();
    const error = state.preferenceErrors.get(systemIdValue);
    if (!error) return;
    const retry = button('Retry saving');
    retry.addEventListener('click', () => {
      retry.disabled = true;
      void saveSystemAliasListPreference(systemIdValue, error.aliasListId, hostValue).catch(() => {});
    });
    hostValue.append(feedback(`Alias List preference could not be saved: ${error.message}`, 'error'), retry);
  };
  const saveSystemAliasListPreference = (systemIdValue, selectedId, notice) => {
    if (!systemIdValue) return Promise.resolve();
    if (selectedId) {
      state.systemAliasListPreferences.set(systemIdValue, selectedId);
      state.clearedSystemPreferences.delete(systemIdValue);
    } else {
      state.systemAliasListPreferences.delete(systemIdValue);
      state.clearedSystemPreferences.add(systemIdValue);
    }
    state.preferenceErrors.delete(systemIdValue);
    if (notice) notice.replaceChildren();
    const revision = (state.preferenceRevisions.get(systemIdValue) || 0) + 1;
    state.preferenceRevisions.set(systemIdValue, revision);
    const previous = state.preferenceSaves.get(systemIdValue) || Promise.resolve();
    // Serialize changes so an earlier response cannot overwrite the last choice on the server.
    const pending = previous.catch(() => {}).then(() => api(RADIO_REFERENCE_IMPORT_PATHS.systemPreferences, {
      method: selectedId ? 'PUT' : 'DELETE', page: false,
      body: selectedId ? { system_id: systemIdValue, preferred_alias_list_id: selectedId } :
        { system_id: systemIdValue }
    })).then((response) => {
      if (integerValue(response, ['preferred_alias_list_id', 'preferredAliasListId']) !== selectedId) {
        throw new Error('RadioReference did not confirm the selected Alias List.');
      }
      if (state.preferenceRevisions.get(systemIdValue) === revision) {
        state.preferenceErrors.delete(systemIdValue);
        if (notice?.isConnected) notice.replaceChildren();
      }
      return response;
    }).catch((error) => {
      if (state.preferenceRevisions.get(systemIdValue) === revision) {
        state.preferenceErrors.set(systemIdValue, { aliasListId: selectedId, message: error.message });
        if (notice?.isConnected) showPreferenceError(notice, systemIdValue);
      }
      throw error;
    });
    state.preferenceSaves.set(systemIdValue, pending);
    void pending.catch(() => {});
    return pending;
  };
  const chooseSystemAliasList = (systemIdValue, selectedId, notice) => {
    state.talkgroupAliasListId = selectedId;
    return saveSystemAliasListPreference(systemIdValue, selectedId, notice);
  };
  const ensureSystemAliasListPreference = (systemIdValue, selectedId, notice) => {
    if (state.systemAliasListPreferences.get(systemIdValue) === selectedId &&
        !state.preferenceErrors.has(systemIdValue)) {
      return state.preferenceSaves.get(systemIdValue) || Promise.resolve();
    }
    return chooseSystemAliasList(systemIdValue, selectedId, notice);
  };
  const preferredAliasListIdForSystem = (systemIdValue) => {
    if (state.clearedSystemPreferences.has(systemIdValue)) return null;
    const saved = state.systemAliasListPreferences.get(systemIdValue);
    if (saved) return saved;
    if (!state.preferenceLoadError) return null;
    const systemBookmark = state.bookmarks.find((entry) =>
      entry.kind === 'TRUNKED_SYSTEM' && entry.id === systemIdValue && entry.preferredAliasListId);
    if (systemBookmark) return systemBookmark.preferredAliasListId;
    return state.bookmarks.find((entry) =>
      entry.kind === 'TALKGROUP_CATEGORY' && entry.parentId === systemIdValue &&
      entry.preferredAliasListId)?.preferredAliasListId || null;
  };
  const bookmarked = (value) => state.bookmarks.some((entry) => bookmarkKey(entry) === bookmarkKey(value));
  const bookmarkForEntry = (entry) => {
    const trunked = entryKind(entry) === 'TRUNKED_SYSTEM';
    return {
      kind: trunked ? 'TRUNKED_SYSTEM' : 'CONVENTIONAL_AGENCY',
      id: trunked ? systemId(entry) : ownerId(entry),
      parentId: 0,
      ownerKind: trunked ? '' : textValue(entry?.detail || entry, ['kind', 'owner_kind'], 'AGENCY'),
      name: textValue(entry, ['name'], 'Unnamed'),
      parentName: textValue(entry, ['breadcrumb', 'parentName', 'parent_name'])
    };
  };
  const toggleBookmark = async (value, control, onChanged) => {
    if (control.disabled) return;
    control.disabled = true;
    try {
      const targetSystem = value.kind === 'TALKGROUP_CATEGORY' ? value.parentId : value.id;
      const preferred = targetSystem === state.activeSystemId &&
        (value.kind === 'TRUNKED_SYSTEM' || value.kind === 'TALKGROUP_CATEGORY') ?
        state.talkgroupAliasListId : null;
      const next = await api(RADIO_REFERENCE_IMPORT_PATHS.bookmarks, {
        method: bookmarked(value) ? 'DELETE' : 'PUT',
        body: { ...value, preferredAliasListId: preferred }
      });
      state.bookmarks = rows(next).map(normalizeBookmark);
      const saved = bookmarked(value);
      control.textContent = saved ? '★' : '☆';
      control.setAttribute('aria-pressed', String(saved));
      control.setAttribute('aria-label', `${saved ? 'Remove' : 'Add'} bookmark: ${value.name}`);
      control.title = `${saved ? 'Remove bookmark' : 'Bookmark'}: ${value.name}`;
      control.disabled = false;
      onChanged?.();
    } catch (error) {
      control.disabled = false;
      control.title = `Bookmark failed: ${error.message}`;
    }
  };
  const starButton = (value, onChanged) => {
    const saved = bookmarked(value);
    const control = button(saved ? '★' : '☆',
      'ui-button ui-button-secondary ui-icon-button radioreference-bookmark');
    control.setAttribute('aria-label', `${saved ? 'Remove' : 'Add'} bookmark: ${value.name}`);
    control.setAttribute('aria-pressed', String(saved));
    control.title = `${saved ? 'Remove bookmark' : 'Bookmark'}: ${value.name}`;
    control.addEventListener('click', () => toggleBookmark(value, control, onChanged));
    return control;
  };

  const internalPager = ({ offset, limit, visible, total, more, label, onPage }) => {
    return browsingWorkflows.createBrowsingPager({ node, className: 'radioreference-pager ui-pager-surface',
      ariaLabel: `${label} pages`,
      countText: browsingWorkflows.pageRangeText({ offset, visible, total, label, format: formatNumber }),
      previous: { enabled: offset > 0, onClick: () => onPage(Math.max(0, offset - limit)) },
      next: { enabled: more === true, onClick: () => onPage(offset + limit) }
    });
  };

  const renderPreviewDetails = (preview, kind) => {
    const wrapper = node('div', 'radioreference-preview');
    const operation = textValue(preview, ['operation', 'action']);
    const summary = node('div', 'radioreference-preview-summary ui-metric-grid ui-metric-grid-embedded');
    if (kind === 'talkgroups') {
      [['Add', resultCount(preview, 'add')], ['Update', resultCount(preview, 'update')],
        ['Unchanged', resultCount(preview, 'unchanged')]].forEach(([label, count]) => {
        summary.append(metricCard(label, count, formatNumber(count), {
          icon: label === 'Add' ? 'icon-plus' : label === 'Update' ? 'icon-refresh' : 'icon-aliases'
        }));
      });
      wrapper.append(summary, node('p', 'muted',
        'Updates replace only RadioReference-owned name, description, and group fields. Local handling stays intact.'));
      const previewRows = Array.isArray(preview?.rows) ? preview.rows : [];
      const changed = previewRows.length === 1 && importStatus(previewRows[0]).label === 'Different' ?
        talkgroupChanges(previewRows[0]) : [];
      if (changed.length) {
        const detail = node('div', 'radioreference-talkgroup-changes');
        detail.append(node('strong', '', 'RadioReference fields changing'));
        const list = node('dl');
        changed.forEach((change) => {
          const field = textValue(change, ['field']);
          const { before, after, hiddenDifference } = importedChangeValues(change);
          const values = node('dd');
          values.append(node('span', '', before), node('span', 'muted', '→'), node('strong', '', after));
          if (hiddenDifference) values.append(node('small', 'ui-field-detail',
            'Spacing or hidden characters differ; ␠ marks a space and escapes show hidden characters.'));
          list.append(node('dt', '', IMPORTED_FIELD_LABELS[field]), values);
        });
        detail.append(list);
        wrapper.append(detail);
      }
    } else {
      const channel = preview?.channel || preview?.candidate || preview?.result || {};
      const name = textValue(channel, ['name', 'channel_name'], textValue(preview, ['channel_name'], 'Channel'));
      const protocol = textValue(channel, ['protocol', 'protocol_name', 'protocol_id', 'decoder_type'],
        textValue(preview, ['protocol', 'decoder_type']));
      const frequencies = firstValue(channel?.source, ['frequencies_hz', 'frequency_hz'],
        firstValue(channel, ['frequencies_hz', 'frequency_hz'],
          firstValue(preview, ['frequencies_hz', 'frequency_hz'], [])));
      const values = Array.isArray(frequencies) ? frequencies : [frequencies];
      const facts = node('dl', 'radioreference-preview-facts');
      const factsValues = [['Action', operation ? operation.toLowerCase().replace(/_/g, ' ') : 'Create or refresh'],
        ['Channel', name], ['Protocol', protocol || 'Detected by RadioReference'],
        ['Frequencies', values.filter(Number).map((value) => `${formatFrequency(value)} MHz`).join(', ') ||
          'Selected RadioReference frequencies']];
      const modulation = textValue(preview, ['detected_modulation', 'detectedModulation']);
      if (modulation) factsValues.push(['Detected modulation', modulation]);
      factsValues.forEach(([label, value]) => {
        facts.append(node('dt', '', label), node('dd', '', value));
      });
      wrapper.append(facts);
      if (String(operation).toUpperCase().includes('UPDATE')) wrapper.append(node('div', 'ui-notice',
        kind === 'site' ?
          'Refreshing this channel replaces its saved RadioReference frequencies. The channel keeps its current ' +
          'Alias List, recording, band-plan overrides, decoder details, and other local settings. The Alias List ' +
          'chosen for this system is used for future imports.' :
          'The RadioReference frequency set will replace the saved set. Recording, Alias List, band-plan overrides, ' +
          'decoder details, and other local settings remain unchanged.'));
    }
    const warnings = Array.isArray(preview?.warnings) ? preview.warnings : [];
    warnings.forEach((warning) => wrapper.append(node('div', 'ui-notice ui-notice-warning', String(warning))));
    return wrapper;
  };

  const completionLink = (response, kind, aliasListIdValue = null) => {
    const actions = node('div', 'ui-action-row radioreference-completion-actions');
    const configurationId = textValue(response, ['configuration_id', 'configurationId'],
      textValue(response?.channel, ['configuration_id', 'configurationId']));
    if (configurationId) actions.append(anchor('Open channel', href('channel-setup', { channel: configurationId }),
      'ui-button ui-button-primary'));
    const listId = integerValue(response, ['alias_list_id', 'aliasListId']) || Number(aliasListIdValue);
    if (kind === 'talkgroups' && Number.isSafeInteger(listId) && listId > 0) {
      actions.append(anchor('Open Alias List', href('aliases', { list: listId, aliasTab: 'configure' }),
        'ui-button ui-button-primary'));
    }
    return actions;
  };

  const openPreview = async ({ title, path, body, kind, returnFocusSelector, aliasListIdValue, onApplied }) => {
    const loading = feedback('Preparing the RadioReference import preview…', 'loading');
    const modal = openReadOnlyModal(title, loading, {
      id: `radioreference-${kind}-preview`, className: 'radioreference-import-modal', returnFocusSelector
    });
    if (!modal || (modal.ready && !await modal.ready)) return;
    modal.setBusy(true);
    try {
      const preview = await api(path, { method: 'POST', body, timeoutMs: mutationTimeoutMs });
      if (!modal.dialog.isConnected) return;
      const id = previewId(preview);
      if (!id) throw new Error('RadioReference did not return a usable import preview.');
      const message = node('div', 'admin-form-message');
      message.setAttribute('role', 'alert');
      const cancel = button('Cancel');
      const apply = button(kind === 'talkgroups' ? 'Apply Import' : 'Apply Channel',
        'ui-button ui-button-primary');
      cancel.addEventListener('click', modal.close);
      apply.addEventListener('click', async () => {
        if (apply.disabled) return;
        apply.disabled = true;
        cancel.disabled = true;
        modal.setBusy(true);
        message.textContent = 'Applying the reviewed RadioReference import…';
        try {
          const response = await api(RADIO_REFERENCE_IMPORT_PATHS.apply(id), {
            method: 'POST', body: {}, timeoutMs: mutationTimeoutMs
          });
          modal.setBusy(false);
          const directValue = firstValue(response, ['created', 'applied']);
          const directCount = Number(directValue);
          const changedCount = Number(firstValue(response, ['added'], 0)) +
            Number(firstValue(response, ['updated'], 0));
          const count = directValue !== null && directValue !== undefined && Number.isFinite(directCount) &&
            directCount >= 0 ? directCount : changedCount;
          const completed = node('div', 'radioreference-import-complete');
          const completedIcon = node('span', 'ui-icon-tile radioreference-completion-icon');
          completedIcon.append(iconGlyph(kind === 'talkgroups' ? 'icon-aliases' : 'icon-channel'));
          completed.append(completedIcon,
            node('h3', '', kind === 'talkgroups' ? 'Talkgroups imported' : 'Channel saved'),
            node('p', 'muted', kind === 'talkgroups' ?
              `${formatNumber(Number.isFinite(count) ? count : 0)} talkgroup change${count === 1 ? '' : 's'} applied.` :
              'The channel is ready in Channels.'), completionLink(response, kind, aliasListIdValue));
          modal.dialog.classList.add('radioreference-import-modal-complete');
          modal.dialog.querySelector('.modal-header h2').textContent = 'Import complete';
          modal.content.replaceChildren(completed);
          onApplied?.(response);
        } catch (error) {
          modal.setBusy(false);
          const close = button('Close', 'ui-button ui-button-secondary');
          close.addEventListener('click', modal.close);
          const invalid = node('div', 'radioreference-import-complete');
          invalid.append(uiPill('Preview no longer valid', 'danger'),
            node('h3', '', 'Create a fresh preview'),
            feedback(`${error.message} This preview cannot be reused. Close this window and choose Import again.`,
              'error'), modalFooter(close));
          modal.content.replaceChildren(invalid);
          close.focus();
        }
      });
      const actions = modalFooter(cancel, apply);
      modal.content.replaceChildren(renderPreviewDetails(preview, kind), message, actions);
      modal.setBusy(false);
      apply.focus();
    } catch (error) {
      modal.setBusy(false);
      modal.content.replaceChildren(feedback(error.message, 'error'));
    }
  };

  const siteFrequencyDescription = (value) => {
    const use = textValue(value, ['use', 'description', 'channel_use']);
    const tags = [];
    if (value?.primary_control === true || value?.primaryControl === true || use.toLowerCase() === 'd')
      tags.push('Control');
    if (value?.alternate_control === true || value?.alternateControl === true || use.toLowerCase() === 'a')
      tags.push('Alternate');
    return tags.join(' · ') || use || 'Frequency';
  };

  const hasPrimaryControl = (value) => value?.primary_control === true || value?.primaryControl === true ||
    textValue(value, ['use']).toLowerCase() === 'd';

  const capacityPlus = (system) => textValue(system, ['flavor']).toLowerCase().includes('capacity plus');
  const conventionalNetworked = (system) => textValue(system, ['flavor']).toLowerCase()
    .includes('conventional networked');

  const openSiteImport = (system, site, detail) => {
    const systemIdValue = systemId(system);
    const family = compatibleFamily(detail) || compatibleFamily(system);
    const channels = siteChannels(site, detail).filter((value) => frequencyHz(value) > 0);
    const uniqueChannels = [...new Map(channels.map((value) => [frequencyHz(value), value])).values()];
    const form = node('form', 'radioreference-site-form editor-workspace');
    const systemName = input();
    const siteName = input();
    const channelName = input();
    systemName.value = textValue(detail, ['system_name'], textValue(system, ['name'], ''));
    siteName.value = textValue(site, ['name', 'description'], `Site ${textValue(site, ['number', 'site_number'])}`);
    channelName.value = textValue(detail, ['channel_name'], `${systemName.value} · ${siteName.value}`);
    [systemName, siteName, channelName].forEach((control) => { control.required = true; control.maxLength = 256; });
    const wasCleared = state.clearedSystemPreferences.has(systemIdValue);
    const aliases = aliasSelect(family, wasCleared ? null : preferredAliasListIdForSystem(systemIdValue) ||
      state.talkgroupAliasListId || integerValue(detail, ['default_alias_list_id', 'defaultAliasListId']),
    !wasCleared);
    if (aliases.value) state.talkgroupAliasListId = Number(aliases.value);
    const modulation = detectedSiteModulation(system, site, detail);
    const hasControl = !capacityPlus(system) && !conventionalNetworked(system) &&
      channels.some(hasPrimaryControl);
    const defaultMode = hasControl ? 'CONTROL_AND_ALTERNATES' : 'ALL';
    const modeInputs = new Map();
    const modeGrid = node('div', 'radioreference-frequency-modes');
    const modeDetails = new Map([
      ['CONTROL', ['Control', 'Use only the primary control frequency.']],
      ['CONTROL_AND_ALTERNATES', ['Control + alternates', 'Use marked control and alternate frequencies.']],
      ['SELECTED', ['Selected', 'Choose an exact set of site frequencies.']],
      ['ALL', ['All', 'Use every listed frequency for this site.']]
    ]);
    const modeOrder = hasControl ? ['CONTROL_AND_ALTERNATES', 'CONTROL', 'SELECTED', 'ALL'] :
      ['ALL', 'SELECTED', 'CONTROL', 'CONTROL_AND_ALTERNATES'];
    modeOrder.forEach((value) => {
      const [label, description] = modeDetails.get(value);
      const choice = node('label', 'ui-choice-card radioreference-frequency-mode');
      const radio = node('input', 'ui-choice-radio');
      radio.type = 'radio';
      radio.name = 'frequency-mode';
      radio.value = value;
      radio.checked = value === defaultMode;
      radio.disabled = !hasControl && (value === 'CONTROL' || value === 'CONTROL_AND_ALTERNATES');
      if (radio.disabled) choice.title = capacityPlus(system) ?
        'Capacity Plus uses a moving rest channel, not a dedicated control channel.' :
        conventionalNetworked(system) ? 'Conventional networked systems do not use a control channel.' :
        'RadioReference does not identify a primary control frequency for this site.';
      choice.append(radio, node('span', '', label), node('small', 'muted', radio.disabled ?
        'Unavailable: no dedicated control frequency.' : description));
      modeInputs.set(value, radio);
      modeGrid.append(choice);
    });
    const selectedFrequencies = node('div', 'radioreference-frequency-choices');
    uniqueChannels.forEach((value) => {
      const choice = node('label', 'radioreference-frequency-choice');
      const checkbox = node('input', 'ui-selection-check');
      checkbox.type = 'checkbox';
      checkbox.value = String(frequencyHz(value));
      choice.append(checkbox, node('span', '', `${formatFrequency(frequencyHz(value))} MHz`),
        node('small', 'muted', siteFrequencyDescription(value)));
      selectedFrequencies.append(choice);
    });
    const frequencyPanel = node('div', 'radioreference-selected-frequencies');
    frequencyPanel.hidden = true;
    frequencyPanel.append(node('strong', '', 'Choose frequencies'), selectedFrequencies);
    modeGrid.addEventListener('change', () => {
      frequencyPanel.hidden = !modeInputs.get('SELECTED').checked;
    });
    const detection = node('div', 'radioreference-detection');
    const detectionFacts = node('dl', 'ui-facts radioreference-site-facts');
    const flavor = textValue(system, ['flavor']);
    const voice = textValue(system, ['voice']);
    const appendFact = (label, value, className = '') => {
      const fact = node('div', `ui-fact${className ? ` ${className}` : ''}`);
      const description = node('dd');
      description.append(value);
      fact.append(node('dt', '', label), description);
      detectionFacts.append(fact);
    };
    const protocol = systemTypeLabel(system) || family || 'Trunked';
    appendFact('Protocol', uiPill(protocol, 'protocol'));
    appendFact('System type', flavor || protocol);
    if (voice && voice.toLowerCase() !== protocol.toLowerCase())
      appendFact('Voice', voice, 'radioreference-site-fact-wide');
    if (modulation) appendFact('P25 modulation', uiPill(modulation,
      modulation.toUpperCase().includes('CQPSK') || modulation.toUpperCase().includes('LSM') ? 'blue' : 'neutral'));
    const frequencyMetrics = node('div', 'ui-metric-grid ui-metric-grid-embedded');
    const frequencyCount = metricCard('Frequencies', uniqueChannels.length,
      formatNumber(uniqueChannels.length));
    frequencyCount.querySelector('.ui-metric-copy').append(node('small', 'ui-metric-detail', 'Available'));
    frequencyMetrics.append(frequencyCount);
    detection.append(detectionFacts, frequencyMetrics);
    if (!hasControl) detection.append(node('p', 'radioreference-detection-help muted', capacityPlus(system) ?
      'Capacity Plus uses a moving rest channel. All site frequencies are selected by default.' :
      conventionalNetworked(system) ?
        'This conventional networked system has no control channel. All site frequencies are selected by default.' :
      'No primary control frequency is marked for this site. All site frequencies are selected by default.'));
    const message = node('div', 'admin-form-message');
    message.setAttribute('role', 'alert');
    const preferenceMessage = node('div', 'admin-form-message');
    preferenceMessage.setAttribute('role', 'alert');
    showPreferenceError(preferenceMessage, systemIdValue);
    const preview = button('Review Channel', 'ui-button ui-button-primary');
    preview.type = 'submit';
    const footer = modalFooter(button('Cancel'), preview);
    footer.firstElementChild.addEventListener('click', () => closeReadOnlyModal());
    const nameGrid = node('div', 'radioreference-form-grid');
    nameGrid.append(formField('System name', systemName), formField('Site name', siteName),
      formField('Channel name', channelName), aliasListField('Alias List', aliases, family,
        'Creates a compatible Alias List immediately and selects it for this channel import.'));
    form.append(node('div', 'radioreference-modal-intro',
      `Create one combined channel for ${siteName.value || 'this site'}.`), nameGrid, preferenceMessage, detection,
      formField('Frequencies', modeGrid), frequencyPanel, message, footer);
    const modal = openReadOnlyModal(`Import ${siteName.value || 'site'}`, form, {
      id: 'radioreference-site-import', className: 'radioreference-import-modal',
      returnFocusSelector: '.radioreference-site-import'
    });
    if (!modal) return;
    form.addEventListener('input', (event) => {
      if (event.target !== aliases) modal.setDirty(true);
    });
    form.addEventListener('change', (event) => {
      if (event.target !== aliases) modal.setDirty(true);
    });
    aliases.addEventListener('change', () => {
      void chooseSystemAliasList(systemIdValue, Number(aliases.value) || null, preferenceMessage)
        .catch(() => {});
    });
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      const mode = [...modeInputs].find(([, control]) => control.checked)?.[0] || '';
      const selected = [...selectedFrequencies.querySelectorAll('input:checked')].map((control) => Number(control.value));
      if (!form.reportValidity() || !mode || mode === 'SELECTED' && !selected.length) {
        message.textContent = 'Choose an Alias List, a frequency mode, and at least one selected frequency.';
        return;
      }
      try {
        await ensureSystemAliasListPreference(systemIdValue, Number(aliases.value), preferenceMessage);
      } catch {
        showPreferenceError(preferenceMessage, systemIdValue);
        return;
      }
      modal.setDirty(false);
      closeReadOnlyModal(true);
      await openPreview({
        title: `Review ${channelName.value}`,
        path: RADIO_REFERENCE_IMPORT_PATHS.sitePreview,
        kind: 'site',
        returnFocusSelector: '.radioreference-site-import',
        body: {
          system_id: systemIdValue, site_id: integerValue(site, ['site_id', 'siteId', 'id']),
          alias_list_id: Number(aliases.value), frequency_mode: mode,
          selected_frequency_hz: selected, system_name: systemName.value.trim(), site_name: siteName.value.trim(),
          channel_name: channelName.value.trim()
        }
      });
    });
  };

  const renderSites = async (system, target, systemDocument, refreshRemote = false) => {
    const toolbar = node('div', 'radioreference-sites-toolbar ui-catalog-toolbar');
    const tableController = {};
    const searchFrame = node('label', 'ui-search');
    searchFrame.append(iconGlyph('icon-search'));
    const search = input('search');
    search.placeholder = 'Filter site or channel name';
    search.setAttribute('aria-label', search.placeholder);
    search.value = state.siteSearch;
    search.disabled = true;
    searchFrame.append(search);
    const sort = select();
    [['number', 'Site number'], ['alphabetical', 'Alphabetical']].forEach(([value, label]) => {
      const option = node('option', '', label);
      option.value = value;
      sort.append(option);
    });
    sort.value = state.siteSort;
    sort.disabled = true;
    const count = node('span', 'muted', 'Loading sites…');
    const refresh = button('Refresh sites');
    refresh.prepend(iconGlyph('icon-refresh'));
    refresh.setAttribute('aria-label', 'Refresh sites from RadioReference');
    refresh.disabled = true;
    refresh.addEventListener('click', () => void renderSites(system, target, systemDocument, true));
    toolbar.append(formField('Search sites & channels', searchFrame),
      formField('Sort sites', selectFrame(sort)), count, refresh);
    const list = node('div', 'radioreference-sites-list');
    list.append(feedback('Loading sites for this system. Large systems may take a minute.', 'loading'));
    target.replaceChildren(toolbar, list);
    try {
      const id = systemId(system);
      if (refreshRemote || !state.siteCatalogs.has(id)) {
        const response = await api(query(RADIO_REFERENCE_IMPORT_PATHS.siteCatalog,
          { system_id: id, refresh: refreshRemote ? true : undefined }), { timeoutMs: 65_000 });
        state.siteCatalogs.set(id, rows(response).map((site) => ({
          site,
          searchText: [
            textValue(site, ['name', 'description']), textValue(site, ['number', 'site_number']),
            textValue(site, ['county_name', 'county']),
            ...siteChannels(site).flatMap((channel) => [
              textValue(channel, ['channel_id', 'channelId']),
              textValue(channel, ['name', 'alpha_tag', 'description', 'use']),
              formatFrequency(frequencyHz(channel))
            ])
          ].join(' ').toLowerCase()
        })));
      }
      if (state.activeSystemId !== id || target.dataset.rrTab !== 'sites' ||
          target.firstElementChild !== toolbar) return;
      const catalog = state.siteCatalogs.get(id);
      const systemDetails = systemDocument?.system || systemDocument || system;
      search.disabled = false;
      sort.disabled = false;
      refresh.disabled = false;
      let offset = 0;
      const draw = () => {
        const term = search.value.trim().toLowerCase();
        state.siteSearch = search.value;
        const matches = (term ? catalog.filter((value) => value.searchText.includes(term)) : catalog).slice();
        const matchingCounty = (value) => state.selectedCountyId &&
          integerValue(value.site, ['county_id', 'countyId']) === state.selectedCountyId;
        matches.sort((a, b) => {
          const countyOrder = Number(Boolean(matchingCounty(b))) - Number(Boolean(matchingCounty(a)));
          if (countyOrder) return countyOrder;
          if (sort.value === 'alphabetical') return textValue(a.site, ['name', 'description'])
            .localeCompare(textValue(b.site, ['name', 'description']), undefined, { numeric: true });
          return (Number(firstValue(a.site, ['number', 'site_number'], 0)) || 0) -
            (Number(firstValue(b.site, ['number', 'site_number'], 0)) || 0);
        });
        const values = matches.slice(offset, offset + SITE_LIMIT).map((value) => value.site);
        const countyCount = state.selectedCountyId ? matches.filter(matchingCounty).length : 0;
        count.textContent = `${formatNumber(matches.length)} of ${formatNumber(catalog.length)} sites` +
          (countyCount ? ` · ${formatNumber(countyCount)} ${state.selectedCountyName} first` : '');
        const rendered = table(values, [
        { id: 'site', label: 'Site', render: (site) => {
          const identity = node('span', 'radioreference-row-identity');
          const open = button(textValue(site, ['name', 'description'],
            `Site ${textValue(site, ['number', 'site_number'], '')}`), 'link-button radioreference-site-import');
          open.title = 'Review this site for import';
          open.addEventListener('click', () => openSiteImport(systemDetails, site, site));
          identity.append(open);
          const context = [textValue(site, ['county_name', 'county']),
            textValue(site, ['number', 'site_number']) ? `Site ${textValue(site, ['number', 'site_number'])}` : '']
            .filter(Boolean).join(' · ');
          if (context) identity.append(node('small', 'muted', context));
          return identity;
        } },
        { id: 'system', label: 'Type', fullLabel: 'System type', render: (site) => {
          const detail = node('span', 'radioreference-row-identity');
          detail.append(uiPill(systemTypeLabel(systemDetails) || 'Trunked', 'protocol'));
          const flavor = textValue(systemDetails, ['flavor']);
          if (flavor) detail.append(node('small', 'muted', flavor));
          const modulation = detectedSiteModulation(systemDetails, site);
          if (modulation) detail.append(node('small', 'muted', `${modulation} control`));
          return detail;
        } },
        { id: 'frequencies', label: 'Frequencies', render: (site) => {
          const channels = siteChannels(site);
          return channels.length ? `${formatNumber(channels.length)} available` : 'Loaded during preview';
        } }
        ], 'No sites match this filter.',
        { type: 'radioreference-sites', sortable: false, mobileCards: true,
          controller: tableController, layoutMenuHost: toolbar });
        rendered.querySelector('table')?.classList.add('ui-data-table-quiet');
        list.replaceChildren(rendered, internalPager({
          offset, limit: SITE_LIMIT, visible: values.length, total: matches.length,
          more: offset + SITE_LIMIT < matches.length, label: 'Sites',
          onPage: (next) => { offset = next; draw(); }
        }));
      };
      search.addEventListener('input', () => { offset = 0; draw(); });
      sort.addEventListener('change', () => { state.siteSort = sort.value; offset = 0; draw(); });
      draw();
    } catch (error) {
      if (state.activeSystemId === systemId(system) && target.dataset.rrTab === 'sites' &&
          target.firstElementChild === toolbar) {
        count.textContent = 'Sites unavailable';
        const notice = feedback(error.message, 'error');
        const retry = button('Retry sites');
        refresh.disabled = false;
        retry.addEventListener('click', () => void renderSites(system, target, systemDocument, refreshRemote));
        notice.append(retry);
        list.replaceChildren(notice);
      }
    }
  };

  const renderTalkgroups = async (system, target, systemDocument) => {
    const systemIdValue = systemId(system);
    const systemName = textValue(systemDocument?.system || systemDocument || system, ['name'], 'System');
    const toolbar = node('div', 'radioreference-talkgroup-toolbar ui-catalog-toolbar');
    const family = compatibleFamily(systemDocument?.system || systemDocument) || compatibleFamily(system);
    const aliasList = aliasSelect(family, state.talkgroupAliasListId,
      !state.clearedSystemPreferences.has(systemIdValue));
    if (aliasList.value) state.talkgroupAliasListId = Number(aliasList.value);
    const category = select();
    const categoryStar = node('span', 'radioreference-category-star');
    const categoryField = node('div', 'radioreference-category-field');
    categoryField.append(formField('Category', selectFrame(category)), categoryStar);
    const search = input('search');
    search.placeholder = 'Filter talkgroup ID, name, or description';
    search.setAttribute('aria-label', search.placeholder);
    search.value = state.talkgroupSearch;
    const searchFrame = node('label', 'ui-search radioreference-talkgroup-search');
    searchFrame.append(iconGlyph('icon-search'), search);
    const selectionBadge = uiPill('0 selected', 'blue');
    selectionBadge.classList.add('radioreference-selection-badge');
    const clear = button('Clear selection');
    const importSelected = button('Import selected', 'ui-button ui-button-primary');
    const importAll = button('Import all system talkgroups', 'ui-button ui-button-primary');
    const status = node('div', 'admin-form-message');
    status.setAttribute('role', 'status');
    const preferenceMessage = node('div', 'admin-form-message');
    preferenceMessage.setAttribute('role', 'alert');
    showPreferenceError(preferenceMessage, systemIdValue);
    const tableHost = node('div', 'radioreference-talkgroup-table');
    const tableController = {};
    let offset = 0;
    let catalog = [];
    let catalogState = 'loading';
    let loadSequence = 0;
    const filter = uiSegmentedControl([
      { value: 'ALL', label: 'All' },
      { value: 'NOT_PRESENT', label: 'Not in list' },
      { value: 'DIFFERENT', label: 'Different' },
      { value: 'IDENTICAL', label: 'Identical' }
    ], state.talkgroupStatus, (value) => {
      state.talkgroupStatus = value;
      offset = 0;
      draw();
    });
    filter.classList.add('radioreference-status-filter');
    filter.setAttribute('aria-label', 'Filter by import status');
    const statusFilter = node('div', 'radioreference-talkgroup-status-control');
    statusFilter.append(node('span', 'ui-field-label', 'Import status'), filter);
    const tableTools = node('div', 'radioreference-talkgroup-table-tools');
    const commandTools = node('div', 'radioreference-talkgroup-command-tools');
    const refresh = button('Refresh talkgroups');
    refresh.prepend(iconGlyph('icon-refresh'));
    refresh.setAttribute('aria-label', 'Refresh talkgroups from RadioReference');
    commandTools.append(tableTools, refresh, importAll);
    const commandRow = node('div', 'radioreference-talkgroup-command-row');
    commandRow.append(statusFilter, commandTools);
    const actions = node('div', 'radioreference-talkgroup-actions ui-selection-bar');
    actions.append(selectionBadge, clear, importSelected);
    const aliasField = aliasListField('Compare with Alias List', aliasList, family,
      'Creates a compatible Alias List immediately, selects it for this import, and refreshes the comparison.');
    aliasField.classList.add('radioreference-talkgroup-alias-field');
    const searchField = formField('Search talkgroups', searchFrame);
    searchField.classList.add('radioreference-talkgroup-search-field');
    toolbar.append(aliasField, categoryField, searchField, commandRow);
    target.replaceChildren(toolbar, actions, preferenceMessage, status, tableHost);

    const selected = state.selectedTalkgroups;
    const updateSelection = () => {
      const count = selected.size;
      const ready = catalogState === 'ready';
      selectionBadge.querySelector('span').textContent = formatNumber(count) + ' selected';
      actions.hidden = count === 0;
      clear.disabled = count === 0;
      importSelected.disabled = !ready || count === 0 || !aliasList.value || !state.talkgroupCatalogId;
      importAll.disabled = !ready || !aliasList.value || !state.talkgroupCatalogId;
      const selectionLocksAliasList = count > 0 && Boolean(aliasList.value);
      aliasList.disabled = catalogState === 'loading' || selectionLocksAliasList;
      aliasList.title = selectionLocksAliasList ? 'Clear the current selection before changing Alias Lists.' : '';
      filter.dataset.value = state.talkgroupStatus;
      filter.querySelectorAll('button').forEach((control) => {
        const active = control.dataset.value === state.talkgroupStatus;
        control.classList.toggle('active', active);
        control.setAttribute('aria-pressed', String(active));
        control.disabled = !aliasList.value && control.dataset.value !== 'ALL';
      });
    };
    const categoryRows = rows(systemDocument?.talkgroup_categories || systemDocument?.categories ||
      systemDocument?.talkgroupCategories);
    setOptions(category, categoryRows, state.talkgroupCategoryId, 'All categories', true);
    const updateCategoryStar = () => {
      categoryStar.replaceChildren();
      if (!category.value) return;
      const bookmark = {
        kind: 'TALKGROUP_CATEGORY', id: Number(category.value), parentId: systemIdValue,
        ownerKind: 'TRUNKED_SYSTEM', name: category.selectedOptions[0]?.textContent || 'Category',
        parentName: [textValue(system, ['breadcrumb']), systemName].filter(Boolean).join(' > ')
      };
      categoryStar.append(starButton(bookmark, () => {
        updateCategoryStar();
        state.onBookmarksChanged?.();
      }));
    };
    updateCategoryStar();
    updateSelection();

    const comparisonValue = (talkgroup, field, incoming) => {
      if (state.talkgroupStatus !== 'DIFFERENT') return incoming;
      const value = node('span', 'ui-record-card-copy');
      const importedField = field === 'alpha_tag' ? 'name' : field === 'category' ? 'group' : field;
      const change = talkgroupChanges(talkgroup).find((entry) => entry.field === importedField);
      const changedValues = change ? importedChangeValues(change) : null;
      if (change) value.append(uiPill('Changed', 'warning'));
      value.append(node('span', '', changedValues?.after ?? (incoming || '—')));
      if (changedValues?.hiddenDifference) value.append(node('small', 'ui-field-detail',
        'Spacing or hidden characters differ; ␠ marks a space and escapes show hidden characters.'));
      return value;
    };

    const draw = () => {
      if (catalogState !== 'ready') {
        updateSelection();
        return;
      }
      if (!catalog.length) {
        tableHost.replaceChildren(empty('No talkgroups',
          'RadioReference returned no talkgroups for this system.'));
        updateSelection();
        return;
      }
      const term = search.value.trim().toLowerCase();
      const categoryId = Number(category.value) || null;
      const filtered = catalog.filter((item) =>
        (!categoryId || item.categoryId === categoryId) &&
        (state.talkgroupStatus === 'ALL' || item.status === state.talkgroupStatus) &&
        (!term || item.searchText.includes(term)));
      if (offset >= filtered.length) offset = Math.max(0,
        Math.floor(Math.max(0, filtered.length - 1) / TALKGROUP_LIMIT) * TALKGROUP_LIMIT);
      state.talkgroupOffset = offset;
      const values = filtered.slice(offset, offset + TALKGROUP_LIMIT).map((item) => item.row);
      const pageToggle = node('input', 'ui-selection-check');
      pageToggle.type = 'checkbox';
      pageToggle.setAttribute('aria-label', 'Select every compatible talkgroup on this page');
      const pageIds = values.filter((value) => importStatus(value).tone !== 'danger')
        .map(talkgroupId).filter(Boolean);
      const refreshPageToggle = () => {
        const checked = pageIds.filter((id) => selected.has(id)).length;
        pageToggle.checked = pageIds.length > 0 && checked === pageIds.length;
        pageToggle.indeterminate = checked > 0 && checked < pageIds.length;
      };
      pageToggle.addEventListener('change', () => {
        pageIds.forEach((id) => pageToggle.checked ? selected.add(id) : selected.delete(id));
        tableHost.querySelectorAll('tbody input[type="checkbox"]').forEach((control) => {
          control.checked = selected.has(Number(control.dataset.talkgroupId));
        });
        refreshPageToggle();
        updateSelection();
      });
      refreshPageToggle();
      const grid = table(values, [
        { id: 'selected', label: '', layoutLabel: 'Select talkgroups', essential: true,
          sortable: false, renderHeader: () => pageToggle,
          render: (talkgroup) => {
            const id = talkgroupId(talkgroup);
            const control = node('input', 'ui-selection-check');
            control.type = 'checkbox';
            control.checked = selected.has(id);
            control.disabled = !id || importStatus(talkgroup).tone === 'danger';
            control.dataset.talkgroupId = String(id || '');
            control.setAttribute('aria-label', 'Select ' + textValue(talkgroupValue(talkgroup),
              ['alpha_tag', 'alphaTag', 'name'], 'talkgroup ' + (id || '')));
            control.addEventListener('change', () => {
              if (control.checked) selected.add(id); else selected.delete(id);
              updateSelection();
              refreshPageToggle();
            });
            return control;
          } },
        { id: 'talkgroup', label: 'TG ID', fullLabel: 'Talkgroup ID',
          render: (talkgroup) => String(firstValue(talkgroupValue(talkgroup),
            ['value', 'decimal', 'talkgroup_value'], talkgroupId(talkgroup) || 0)) },
        { id: 'alpha-tag', label: 'Alpha tag',
          render: (talkgroup) => comparisonValue(talkgroup, 'alpha_tag',
            textValue(talkgroupValue(talkgroup), ['alpha_tag', 'alphaTag', 'name'], 'Unnamed')) },
        { id: 'description', label: 'Description',
          render: (talkgroup) => comparisonValue(talkgroup, 'description',
            textValue(talkgroupValue(talkgroup), ['description'], '—')) },
        { id: 'category', label: 'Category',
          render: (talkgroup) => comparisonValue(talkgroup, 'category', talkgroupCategory(talkgroup)) },
        { id: 'status', label: 'Status', fullLabel: 'Import status', render: (talkgroup) => {
          const value = importStatus(talkgroup);
          const content = node('span', 'radioreference-talkgroup-status');
          content.append(uiPill(value.label, value.tone));
          return content;
        } }
      ], 'No talkgroups match these filters.',
      { type: 'radioreference-talkgroups', sortable: false, mobileCards: true,
        controller: tableController, layoutMenuHost: tableTools });
      grid.querySelector('table')?.classList.add('ui-data-table-quiet');
      tableHost.replaceChildren(grid, internalPager({
        offset, limit: TALKGROUP_LIMIT, visible: values.length, total: filtered.length,
        more: offset + TALKGROUP_LIMIT < filtered.length, label: 'Talkgroups',
        onPage: (next) => { offset = next; draw(); }
      }));
      status.textContent = formatNumber(filtered.length) + ' matching of ' +
        formatNumber(catalog.length) + ' loaded talkgroups. ' +
        (aliasList.value ? 'Selections persist across filters and pages.' :
          'Choose an Alias List to compare import status; you can select talkgroups now.');
      updateSelection();
    };

    const load = async (force = false, refreshRemote = false) => {
      const sequence = ++loadSequence;
      const selectedAliasListId = aliasList.value;
      const key = systemIdValue + ':' + (selectedAliasListId || 'raw');
      catalogState = 'loading';
      updateSelection();
      refresh.disabled = true;
      tableHost.replaceChildren(feedback(selectedAliasListId ? 'Comparing talkgroups with Alias List…' :
        'Loading all talkgroups for instant filtering…', 'loading'));
      status.textContent = '';
      try {
        if (refreshRemote || force || !state.talkgroupCatalogs.has(key)) {
          let pending = state.talkgroupLoads.get(key);
          if (!pending) {
            pending = api(query(RADIO_REFERENCE_IMPORT_PATHS.talkgroupCatalog, {
              system_id: systemIdValue, alias_list_id: selectedAliasListId || undefined,
              catalog_id: refreshRemote ? undefined : state.talkgroupCatalogId || undefined,
              refresh: refreshRemote ? true : undefined
            }), { timeoutMs: 65_000 });
            state.talkgroupLoads.set(key, pending);
          }
          let response;
          try {
            response = await pending;
          } finally {
            if (state.talkgroupLoads.get(key) === pending) state.talkgroupLoads.delete(key);
          }
          const loadedCatalogId = textValue(response, ['catalog_id', 'catalogId']) || null;
          if (refreshRemote) {
            for (const cachedKey of state.talkgroupCatalogs.keys()) {
              if (cachedKey.startsWith(systemIdValue + ':')) state.talkgroupCatalogs.delete(cachedKey);
            }
          }
          state.talkgroupCatalogs.set(key, {
            catalogId: loadedCatalogId,
            categories: Array.isArray(response?.categories) ? response.categories : [],
            items: rows(response).map((row) => {
              const talkgroup = talkgroupValue(row);
              return {
                row,
                categoryId: Number(firstValue(talkgroup, ['category_id', 'categoryId'])) || null,
                status: textValue(row, ['status', 'import_status', 'importStatus']).toUpperCase(),
                searchText: [firstValue(talkgroup, ['value', 'decimal', 'talkgroup_value']),
                  textValue(talkgroup, ['alpha_tag', 'alphaTag', 'name']),
                  textValue(talkgroup, ['description']), talkgroupCategory(row)].join(' ').toLowerCase()
              };
            })
          });
        }
        if (sequence !== loadSequence || state.activeSystemId !== systemIdValue ||
            target.dataset.rrTab !== 'talkgroups' || target.firstElementChild !== toolbar ||
            aliasList.value !== selectedAliasListId) return;
        const saved = state.talkgroupCatalogs.get(key);
        state.talkgroupCatalogId = saved.catalogId;
        catalog = saved.items;
        catalogState = 'ready';
        if (refreshRemote) {
          const available = new Set(catalog.filter((item) => importStatus(item.row).tone !== 'danger')
            .map((item) => talkgroupId(item.row)));
          for (const id of selected) if (!available.has(id)) selected.delete(id);
        }
        refresh.disabled = false;
        if (refreshRemote || !categoryRows.length && saved.categories.length && category.options.length <= 1) {
          setOptions(category, saved.categories, state.talkgroupCategoryId, 'All categories', true);
        }
        updateCategoryStar();
        offset = 0;
        draw();
      } catch (error) {
        if (sequence !== loadSequence || state.activeSystemId !== systemIdValue ||
            target.dataset.rrTab !== 'talkgroups' || target.firstElementChild !== toolbar) return;
        if (state.talkgroupCatalogId && /catalog is no longer available/i.test(error.message)) {
          state.talkgroupCatalogId = null;
          state.talkgroupCatalogs.delete(key);
          await load(true);
          return;
        }
        refresh.disabled = false;
        catalogState = 'error';
        tableHost.replaceChildren(feedback(error.message, 'error'));
        updateSelection();
      }
    };

    const previewTalkgroups = async (all) => {
      if (!aliasList.value) {
        status.textContent = 'Choose a compatible Alias List first.';
        aliasList.focus();
        return;
      }
      const selectedIds = [...selected];
      if (!all && !selectedIds.length) return;
      try {
        await ensureSystemAliasListPreference(systemIdValue, Number(aliasList.value), preferenceMessage);
      } catch {
        showPreferenceError(preferenceMessage, systemIdValue);
        return;
      }
      openPreview({
        title: all ? 'Import all system talkgroups' :
          `Import ${formatNumber(selectedIds.length)} talkgroup${selectedIds.length === 1 ? '' : 's'}`,
        path: RADIO_REFERENCE_IMPORT_PATHS.talkgroupsPreview,
        kind: 'talkgroups',
        returnFocusSelector: all ? '.radioreference-import-all' : '.radioreference-import-selected',
        aliasListIdValue: Number(aliasList.value),
        body: { system_id: systemIdValue, alias_list_id: Number(aliasList.value),
          import_all: all, talkgroup_ids: all ? [] : selectedIds,
          catalog_id: state.talkgroupCatalogId },
        onApplied: () => {
          selected.clear();
          state.talkgroupCatalogs.delete(systemIdValue + ':' + aliasList.value);
          updateSelection();
          load(true);
        }
      });
    };
    refresh.addEventListener('click', () => void load(true, true));
    clear.addEventListener('click', () => { selected.clear(); draw(); });
    importSelected.classList.add('radioreference-import-selected');
    importAll.classList.add('radioreference-import-all');
    importSelected.addEventListener('click', () => previewTalkgroups(false));
    importAll.addEventListener('click', () => previewTalkgroups(true));
    aliasList.addEventListener('change', () => {
      void chooseSystemAliasList(systemIdValue, Number(aliasList.value) || null, preferenceMessage)
        .catch(() => {});
      if (!aliasList.value) {
        state.talkgroupStatus = 'ALL';
      }
      load();
    });
    category.addEventListener('change', () => {
      state.talkgroupCategoryId = Number(category.value) || null;
      updateCategoryStar();
      offset = 0;
      draw();
    });
    search.addEventListener('input', () => {
      state.talkgroupSearch = search.value;
      offset = 0;
      draw();
    });
    await load();
  };

  const renderSystem = async (system, detailHost, options = {}) => {
    const id = systemId(system);
    const sequence = ++state.detailSequence;
    state.activeSystemId = id;
    state.talkgroupCatalogId = null;
    state.selectedTalkgroups.clear();
    state.talkgroupAliasListId = preferredAliasListIdForSystem(id);
    state.talkgroupCategoryId = Number(options.categoryId) || null;
    state.talkgroupStatus = 'ALL';
    state.talkgroupSearch = '';
    state.siteSearch = '';
    detailHost.closest('.radioreference-browser')?.classList.add('radioreference-detail-open');
    detailHost.replaceChildren(feedback('Loading system details…', 'loading'));
    try {
      const documentValue = await api(query(RADIO_REFERENCE_IMPORT_PATHS.systemDetails, { system_id: id }));
      if (sequence !== state.detailSequence) return;
      const details = documentValue?.system || documentValue;
      const panel = node('section', 'radioreference-system-workspace ui-surface');
      const header = node('header', 'radioreference-detail-header ui-surface-header');
      const title = node('div');
      title.append(node('span', 'muted', [textValue(system, ['breadcrumb']), 'Trunked system']
        .filter(Boolean).join(' · ')),
        node('h2', '', textValue(details, ['name'], textValue(system, ['name'], `System ${id}`))),
        node('p', 'muted', textValue(details, ['protocol', 'type_name', 'type'])));
      const close = button('Back to results');
      close.classList.add('radioreference-back');
      close.addEventListener('click', () => {
        ++state.detailSequence;
        detailHost.replaceChildren(empty('Choose a result', 'Select a system or agency to review.'));
        detailHost.closest('.radioreference-browser')?.classList.remove('radioreference-detail-open');
      });
      header.append(title, starButton(bookmarkForEntry(system), () => state.onBookmarksChanged?.()), close);
      const initialTab = options.categoryId ? 'talkgroups' : 'sites';
      const tabs = uiSegmentedControl([
        { value: 'sites', label: 'Sites & Channels' },
        { value: 'talkgroups', label: 'Talkgroups & Aliases' }
      ], initialTab, (value) => {
        body.dataset.rrTab = value;
        if (value === 'sites') void renderSites(system, body, documentValue);
        else void renderTalkgroups(system, body, documentValue);
      });
      tabs.classList.add('radioreference-system-tabs');
      tabs.setAttribute('aria-label', 'System import sections');
      const body = node('div', 'radioreference-system-body');
      body.dataset.rrTab = initialTab;
      panel.append(header, tabs, body);
      detailHost.replaceChildren(panel);
      if (initialTab === 'sites') await renderSites(system, body, documentValue);
      else await renderTalkgroups(system, body, documentValue);
    } catch (error) {
      if (sequence === state.detailSequence) detailHost.replaceChildren(feedback(error.message, 'error'));
    }
  };

  const openConventionalImport = (entry, frequency, categoryId) => {
    const form = node('form', 'radioreference-conventional-form editor-workspace');
    const family = compatibleFamily(frequency) || compatibleFamily(entry);
    const systemName = input();
    const siteName = input();
    const channelName = input();
    systemName.value = textValue(entry, ['name'], '');
    siteName.value = textValue(entry, ['secondary', 'location'], '');
    channelName.value = textValue(frequency, ['alpha_tag', 'alphaTag', 'description'],
      `${formatFrequency(frequencyHz(frequency))} MHz`);
    [systemName, siteName, channelName].forEach((control) => { control.required = true; control.maxLength = 256; });
    const aliases = aliasSelect(family);
    const facts = node('dl', 'radioreference-preview-facts');
    [['Frequency', `${formatFrequency(frequencyHz(frequency))} MHz`],
      ['Mode', textValue(frequency, ['mode_name', 'mode', 'protocol'], 'Detected by RadioReference')],
      ['Description', textValue(frequency, ['description'], '—')]].forEach(([label, value]) => {
      facts.append(node('dt', '', label), node('dd', '', value));
    });
    const message = node('div', 'admin-form-message');
    message.setAttribute('role', 'alert');
    const cancel = button('Cancel');
    const review = button('Review Channel', 'ui-button ui-button-primary');
    review.type = 'submit';
    const footer = modalFooter(cancel, review);
    const nameGrid = node('div', 'radioreference-form-grid');
    nameGrid.append(formField('System name', systemName), formField('Site name', siteName),
      formField('Channel name', channelName), aliasListField('Alias List', aliases, family,
        'Creates a compatible Alias List immediately and selects it for this conventional import.'));
    form.append(facts, nameGrid, message, footer);
    const modal = openReadOnlyModal(`Import ${channelName.value}`, form, {
      id: 'radioreference-conventional-import', className: 'radioreference-import-modal',
      returnFocusSelector: '.radioreference-conventional-import'
    });
    if (!modal) return;
    cancel.addEventListener('click', modal.close);
    form.addEventListener('input', () => modal.setDirty(true));
    form.addEventListener('change', () => modal.setDirty(true));
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      if (!form.reportValidity()) {
        message.textContent = 'Choose a compatible Alias List before reviewing this channel.';
        return;
      }
      modal.setDirty(false);
      closeReadOnlyModal(true);
      await openPreview({
        title: `Review ${channelName.value}`,
        path: RADIO_REFERENCE_IMPORT_PATHS.conventionalPreview,
        kind: 'conventional',
        returnFocusSelector: '.radioreference-conventional-import',
        body: { owner_kind: textValue(entry?.detail || entry, ['owner_kind', 'kind'], 'AGENCY'),
          owner_id: ownerId(entry), sub_category_id: Number(categoryId), frequency_id: frequencyId(frequency),
          alias_list_id: Number(aliases.value), system_name: systemName.value.trim(),
          site_name: siteName.value.trim(), channel_name: channelName.value.trim() }
      });
    });
  };

  const renderConventional = async (entry, detailHost, preferredCategoryId = null) => {
    const id = ownerId(entry);
    const kind = textValue(entry?.detail || entry, ['owner_kind', 'kind'], 'AGENCY');
    const sequence = ++state.detailSequence;
    state.activeSystemId = null;
    detailHost.closest('.radioreference-browser')?.classList.add('radioreference-detail-open');
    detailHost.replaceChildren(feedback('Loading conventional categories…', 'loading'));
    try {
      const categoriesDocument = await api(query(RADIO_REFERENCE_IMPORT_PATHS.conventionalCategories,
        { owner_kind: kind, owner_id: id, offset: 0, limit: 500 }));
      if (sequence !== state.detailSequence) return;
      const categories = rows(categoriesDocument);
      const panel = node('section', 'radioreference-system-workspace ui-surface');
      const header = node('header', 'radioreference-detail-header ui-surface-header');
      const title = node('div');
      title.append(node('span', 'muted', [textValue(entry, ['breadcrumb']), 'Conventional agency']
        .filter(Boolean).join(' · ')),
        node('h2', '', textValue(entry, ['name'], `Agency ${id}`)),
        node('p', 'muted', textValue(entry, ['secondary', 'location'])));
      const close = button('Back to results');
      close.classList.add('radioreference-back');
      close.addEventListener('click', () => {
        ++state.detailSequence;
        detailHost.replaceChildren(empty('Choose a result', 'Select a system or agency to review.'));
        detailHost.closest('.radioreference-browser')?.classList.remove('radioreference-detail-open');
      });
      header.append(title, starButton(bookmarkForEntry(entry), () => state.onBookmarksChanged?.()), close);
      const category = select();
      categories.forEach((value) => {
        const categoryId = integerValue(value, ['sub_category_id', 'subCategoryId', 'category_id', 'id']);
        if (!categoryId) return;
        const name = [textValue(value, ['category_name', 'category']),
          textValue(value, ['sub_category_name', 'name'])].filter(Boolean).join(' · ');
        const option = node('option', '', name || `Category ${categoryId}`);
        option.value = String(categoryId);
        category.append(option);
      });
      if (preferredCategoryId && [...category.options].some((value) =>
        value.value === String(preferredCategoryId))) category.value = String(preferredCategoryId);
      const toolbar = node('div', 'radioreference-conventional-toolbar ui-catalog-toolbar');
      const tableController = {};
      const status = node('div', 'admin-form-message');
      status.setAttribute('role', 'status');
      const categoryStar = node('span', 'radioreference-category-star');
      const updateCategoryStar = () => {
        categoryStar.replaceChildren();
        if (!category.value) return;
        const bookmark = {
          kind: 'CONVENTIONAL_CATEGORY', id: Number(category.value), parentId: id,
          ownerKind: kind, name: category.selectedOptions[0]?.textContent || 'Category',
          parentName: [textValue(entry, ['breadcrumb']), textValue(entry, ['name'], 'Agency')]
            .filter(Boolean).join(' > ')
        };
        categoryStar.append(starButton(bookmark, () => {
          updateCategoryStar();
          state.onBookmarksChanged?.();
        }));
      };
      toolbar.append(formField('Category', selectFrame(category)), categoryStar, status);
      updateCategoryStar();
      const body = node('div', 'radioreference-system-body');
      panel.append(header, toolbar, body);
      detailHost.replaceChildren(panel);

      const load = async (offset = 0) => {
        tableController.layoutMenuCleanup?.();
        if (!category.value) {
          body.replaceChildren(empty('No conventional categories',
            'RadioReference did not return an importable category for this agency.'));
          return;
        }
        body.replaceChildren(feedback('Loading conventional frequencies…', 'loading'));
        try {
          const response = await api(query(RADIO_REFERENCE_IMPORT_PATHS.conventionalFrequencies,
            { sub_category_id: category.value, offset, limit: FREQUENCY_LIMIT }));
          if (sequence !== state.detailSequence) return;
          const values = rows(response);
          const grid = table(values, [
            { id: 'frequency', label: 'Frequency',
              render: (value) => `${formatFrequency(frequencyHz(value))} MHz` },
            { id: 'alpha-tag', label: 'Alpha tag', render: (value) => {
              const open = button(textValue(value, ['alpha_tag', 'alphaTag', 'name'], 'Unnamed'),
                'link-button radioreference-conventional-import');
              open.title = 'Review this channel for import';
              open.addEventListener('click', () => openConventionalImport(entry, value, category.value));
              return open;
            } },
            { id: 'description', label: 'Description',
              render: (value) => textValue(value, ['description'], '—') },
            { id: 'mode', label: 'Mode',
              render: (value) => uiPill(textValue(value, ['mode_name', 'mode', 'protocol'], 'Unknown'), 'protocol') }
          ], 'No frequencies were returned for this category.',
          { type: 'radioreference-conventional', sortable: false, mobileCards: true,
            controller: tableController, layoutMenuHost: toolbar });
          grid.querySelector('table')?.classList.add('ui-data-table-quiet');
          const total = totalValue(response, values.length);
          body.replaceChildren(grid, internalPager({ offset, limit: FREQUENCY_LIMIT, visible: values.length, total,
            more: hasMore(response, offset, FREQUENCY_LIMIT, values.length), label: 'Frequencies', onPage: load }));
          status.textContent = `${formatNumber(total)} frequency${total === 1 ? '' : 'ies'} available. ` +
            'Import one channel at a time.';
        } catch (error) {
          body.replaceChildren(feedback(error.message, 'error'));
        }
      };
      category.addEventListener('change', () => { updateCategoryStar(); load(0); });
      await load(0);
    } catch (error) {
      if (sequence === state.detailSequence) detailHost.replaceChildren(feedback(error.message, 'error'));
    }
  };

  const renderDirectory = (listHost, detailHost, values, location) => {
    const supported = values.filter((entry) => ['CONVENTIONAL', 'P25', 'DMR', 'NXDN']
      .includes(directoryFamily(entry)));
    const filtered = supported.filter((entry) => state.directoryType === 'ALL' ||
      directoryFamily(entry) === state.directoryType);
    if (!filtered.length) {
      listHost.replaceChildren(directoryState(empty('No systems or agencies here',
        'Try another system type or choose a broader browse area.')));
      return;
    }
    const compareNames = (left, right) => textValue(left, ['name']).localeCompare(textValue(right, ['name']),
      undefined, { sensitivity: 'base', numeric: true });
    const ordered = [...filtered].sort((left, right) => state.directorySort === 'alphabetical' ?
      compareNames(left, right) : directoryUpdated(right) - directoryUpdated(left) || compareNames(left, right));
    const itemFor = (entry, trail) => {
      entry.breadcrumb = trail;
      const item = node('div', 'radioreference-directory-item');
      const open = button('', 'link-button ui-row-action radioreference-result-open');
      open.setAttribute('aria-pressed', 'false');
      const identity = node('span', 'radioreference-row-identity');
      identity.append(node('strong', '', textValue(entry, ['name'], 'Unnamed')));
      const secondary = textValue(entry, ['secondary', 'location', 'description']);
      if (secondary) identity.append(node('small', 'muted', secondary));
      const updated = directoryUpdated(entry);
      if (updated) identity.append(node('small', 'muted',
        `Updated ${new Date(updated).toLocaleDateString()}`));
      const trunked = entryKind(entry) === 'TRUNKED_SYSTEM';
      const kindLabel = trunked ? [systemTypeLabel(entry), 'Trunked'].filter(Boolean).join(' · ') :
        'Conventional';
      open.append(identity, uiPill(kindLabel, trunked ? 'blue' : 'neutral'));
      open.addEventListener('click', () => {
        listHost.querySelectorAll('.radioreference-directory-item.is-active').forEach((value) => {
          value.classList.remove('is-active');
          value.querySelector('.radioreference-result-open')?.setAttribute('aria-pressed', 'false');
        });
        item.classList.add('is-active');
        open.setAttribute('aria-pressed', 'true');
        if (entryKind(entry) === 'TRUNKED_SYSTEM') void renderSystem(entry, detailHost);
        else void renderConventional(entry, detailHost);
      });
      item.append(open, starButton(bookmarkForEntry(entry), () => state.onBookmarksChanged?.()));
      return item;
    };
    const branch = (label, entries, trail, initiallyOpen = false) => {
      const group = node('details', 'radioreference-directory-branch');
      const count = entries.length;
      group.dataset.count = String(count);
      const summary = node('summary', '', '');
      summary.append(node('span', 'radioreference-directory-branch-label', label),
        uiPill(formatNumber(count), 'neutral'));
      group.append(summary);
      // Populate only opened branches; large counties need not create thousands of hidden buttons.
      const populate = () => {
        if (!group.open || group.dataset.populated) return;
        group.dataset.populated = 'true';
        if (!entries.length) return;
        const content = node('div', 'radioreference-directory-branch-content');
        entries.forEach((entry) => content.append(itemFor(entry, trail)));
        group.append(content);
      };
      group.addEventListener('toggle', populate);
      if (initiallyOpen) {
        group.open = true;
        populate();
      }
      return group;
    };
    const national = ordered.filter((entry) => textValue(entry, ['scope']).toUpperCase() === 'NATIONAL');
    const statewide = ordered.filter((entry) => textValue(entry, ['scope']).toUpperCase() === 'STATE');
    const countywide = ordered.filter((entry) => textValue(entry, ['scope']).toUpperCase() === 'COUNTY');
    const tree = node('div', 'radioreference-directory-tree');
    const countyTrail = [location.region, location.county].filter(Boolean).join(' > ') || 'County';
    if (countywide.length) tree.append(branch(`${location.county || 'County'} results`, countywide,
      countyTrail, true));
    if (statewide.length) tree.append(branch(`${location.region || 'State'} statewide`, statewide,
      location.region || 'Statewide', !countywide.length));
    if (national.length) tree.append(branch('National', national, 'National',
      !countywide.length && !statewide.length));
    listHost.replaceChildren(tree);
  };

  const buildBrowser = async (initializeSequence) => {
    const browser = node('div', 'radioreference-browser');
    const browseForm = node('div', 'radioreference-browse-form ui-surface');
    const country = select();
    const region = select();
    const county = select();
    const message = node('div', 'admin-form-message');
    message.setAttribute('role', 'status');
    const fields = node('div', 'radioreference-browse-fields');
    fields.append(formField('Country', selectFrame(country)),
      formField('State or region', selectFrame(region)), formField('County', selectFrame(county)));
    browseForm.append(node('strong', 'radioreference-browse-title', 'Browse area'), fields, message);
    const workbench = node('div', 'radioreference-workbench-grid');
    const resultHost = node('div', 'radioreference-directory-results ui-surface');
    const tabHost = node('div', 'radioreference-directory-tabs');
    const filterHost = node('div', 'radioreference-directory-filters');
    const listHost = node('div', 'radioreference-directory-list');
    resultHost.append(tabHost, filterHost, listHost);
    const detailHost = node('div', 'radioreference-import-detail');
    detailHost.append(empty('Choose a result', 'Select a system or agency to review.'));
    workbench.append(resultHost, detailHost);
    browser.append(browseForm, workbench);
    if (state.preferenceLoadError) browser.prepend(feedback(
      'Saved Alias List choices could not be loaded. Choose a list again or reload to retry.', 'error'));
    host.replaceChildren(browser);

    const showBookmarks = () => {
      if (!state.bookmarks.length) {
        listHost.replaceChildren(directoryState(empty('No bookmarks yet',
          'Use a star beside a system, agency, or category to save it here.')));
        return;
      }
      const fragmentValue = document.createDocumentFragment();
      let previousKind = '';
      const labels = {
        TRUNKED_SYSTEM: 'Trunked systems', CONVENTIONAL_AGENCY: 'Conventional agencies',
        TALKGROUP_CATEGORY: 'Talkgroup categories', CONVENTIONAL_CATEGORY: 'Conventional categories'
      };
      const sortedBookmarks = [...state.bookmarks].sort((left, right) => {
        const kind = left.kind.localeCompare(right.kind);
        return kind || left.name.localeCompare(right.name, undefined, { sensitivity: 'base', numeric: true });
      });
      sortedBookmarks.forEach((bookmark) => {
        if (bookmark.kind !== previousKind) {
          fragmentValue.append(node('div', 'radioreference-directory-group',
            labels[bookmark.kind] || 'Bookmarks'));
          previousKind = bookmark.kind;
        }
        const item = node('div', 'radioreference-directory-item');
        const open = button('', 'link-button ui-row-action radioreference-result-open');
        open.setAttribute('aria-pressed', 'false');
        const identity = node('span', 'radioreference-row-identity');
        const parentEntry = state.browseRows.find((entry) =>
          (bookmark.kind === 'TRUNKED_SYSTEM' || bookmark.kind === 'CONVENTIONAL_AGENCY' ?
            (bookmark.kind === 'TRUNKED_SYSTEM' ? systemId(entry) : ownerId(entry)) === bookmark.id :
            (bookmark.kind === 'TALKGROUP_CATEGORY' ? systemId(entry) : ownerId(entry)) === bookmark.parentId) &&
          entryKind(entry) === (bookmark.kind === 'TRUNKED_SYSTEM' || bookmark.kind === 'TALKGROUP_CATEGORY' ?
            'TRUNKED_SYSTEM' : 'CONVENTIONAL_AGENCY'));
        const savedPath = parentEntry ? [parentEntry.breadcrumb,
          bookmark.kind.includes('CATEGORY') ? textValue(parentEntry, ['name']) : ''].filter(Boolean).join(' > ') :
          bookmark.parentName;
        if (savedPath) identity.append(node('small', 'muted radioreference-bookmark-path', savedPath));
        identity.append(node('strong', '', bookmark.name));
        const preferredSystemId = bookmark.kind === 'TRUNKED_SYSTEM' ? bookmark.id :
          bookmark.kind === 'TALKGROUP_CATEGORY' ? bookmark.parentId : null;
        const preferredId = preferredAliasListIdForSystem(preferredSystemId);
        const preferredList = state.aliasLists.find((entry) => aliasListId(entry) === preferredId);
        if (preferredList) identity.append(node('small', 'muted',
          `Import talkgroups to ${textValue(preferredList, ['name'], 'Alias List')}`));
        const parentType = parentEntry ? systemTypeLabel(parentEntry) : '';
        const kindLabel = bookmark.kind === 'TRUNKED_SYSTEM' || bookmark.kind === 'TALKGROUP_CATEGORY' ?
          parentType || 'Trunked' : 'Conventional';
        open.append(identity, uiPill(kindLabel, 'neutral'));
        open.addEventListener('click', () => {
          listHost.querySelectorAll('.radioreference-directory-item.is-active').forEach((value) => {
            value.classList.remove('is-active');
            value.querySelector('.radioreference-result-open')?.setAttribute('aria-pressed', 'false');
          });
          item.classList.add('is-active');
          open.setAttribute('aria-pressed', 'true');
          if (bookmark.kind === 'TRUNKED_SYSTEM' || bookmark.kind === 'TALKGROUP_CATEGORY') {
            const system = {
              id: bookmark.kind === 'TRUNKED_SYSTEM' ? bookmark.id : bookmark.parentId,
              name: bookmark.kind === 'TRUNKED_SYSTEM' ? bookmark.name :
                bookmark.parentName.split(' > ').at(-1),
              type: 'TRUNKED_SYSTEM', breadcrumb: savedPath
            };
            void renderSystem(system, detailHost, { bookmark,
              categoryId: bookmark.kind === 'TALKGROUP_CATEGORY' ? bookmark.id : null });
          } else {
            const entry = {
              id: bookmark.kind === 'CONVENTIONAL_AGENCY' ? bookmark.id : bookmark.parentId,
              name: bookmark.kind === 'CONVENTIONAL_AGENCY' ? bookmark.name :
                bookmark.parentName.split(' > ').at(-1),
              type: 'CONVENTIONAL_AGENCY',
              breadcrumb: savedPath,
              detail: { kind: bookmark.ownerKind,
                id: bookmark.kind === 'CONVENTIONAL_AGENCY' ? bookmark.id : bookmark.parentId }
            };
            void renderConventional(entry, detailHost,
              bookmark.kind === 'CONVENTIONAL_CATEGORY' ? bookmark.id : null);
          }
        });
        item.append(open, starButton(bookmark, () => state.onBookmarksChanged?.()));
        fragmentValue.append(item);
      });
      listHost.replaceChildren(fragmentValue);
    };
    const showCurrent = () => {
      if (state.browseTab === 'bookmarks') showBookmarks();
      else if (state.browseDocument) renderDirectory(listHost, detailHost,
        state.browseRows, state.browseDocument.location);
    };
    const renderFilters = () => {
      filterHost.replaceChildren();
      if (state.browseTab !== 'browse') return;
      const type = select();
      type.setAttribute('aria-label', 'Filter system type');
      [['ALL', 'All supported'], ['CONVENTIONAL', 'Conventional'], ['P25', 'P25'],
        ['DMR', 'DMR'], ['NXDN', 'NXDN']].forEach(([value, label]) => {
        const option = node('option', '', label);
        option.value = value;
        type.append(option);
      });
      type.value = state.directoryType;
      type.addEventListener('change', () => { state.directoryType = type.value; showCurrent(); });
      const sort = select();
      sort.setAttribute('aria-label', 'Sort directory results');
      [['updated', 'Last updated'], ['alphabetical', 'Alphabetical']].forEach(([value, label]) => {
        const option = node('option', '', label);
        option.value = value;
        sort.append(option);
      });
      sort.value = state.directorySort;
      sort.addEventListener('change', () => { state.directorySort = sort.value; showCurrent(); });
      filterHost.append(formField('System type', selectFrame(type)),
        formField('Sort results', selectFrame(sort)));
    };
    const renderTabs = () => {
      const tabs = uiSegmentedControl([
        { value: 'browse', label: 'Browse' },
        { value: 'bookmarks', label: '★ Bookmarks (' + formatNumber(state.bookmarks.length) + ')' }
      ], state.browseTab, (value) => {
        state.browseTab = value;
        renderFilters();
        showCurrent();
      });
      tabs.setAttribute('aria-label', 'Browse or open RadioReference bookmarks');
      tabHost.replaceChildren(tabs);
    };
    state.onBookmarksChanged = () => { renderTabs(); showCurrent(); };
    renderTabs();
    renderFilters();

    let locationSequence = 0;
    const loadCounties = async (selected = null, sequence = locationSequence) => {
      county.disabled = true;
      setOptions(county, [], null, 'Loading counties…', true);
      if (!region.value) {
        setOptions(county, [], null, 'All counties', true);
        county.disabled = false;
        return;
      }
      const response = await api(query(RADIO_REFERENCE_IMPORT_PATHS.counties,
        { state_id: region.value, offset: 0, limit: 500 }));
      if (sequence !== locationSequence || initializeSequence !== state.initializeSequence) return;
      setOptions(county, rows(response), selected, 'All counties', true);
      county.disabled = false;
    };
    const loadStates = async (selectedRegion = null, selectedCounty = null, sequence = locationSequence) => {
      region.disabled = true;
      county.disabled = true;
      setOptions(region, [], null, 'Loading regions…', true);
      const response = await api(query(RADIO_REFERENCE_IMPORT_PATHS.states, { country_id: country.value }));
      if (sequence !== locationSequence || initializeSequence !== state.initializeSequence) return;
      setOptions(region, rows(response), selectedRegion, 'All states or regions', true);
      region.disabled = false;
      await loadCounties(selectedCounty, sequence);
    };

    const browse = async () => {
      if (!country.value) return;
      const sequence = ++state.browseSequence;
      message.textContent = 'Loading systems and agencies…';
      if (state.browseTab === 'browse') {
        listHost.replaceChildren(directoryState(feedback('Loading directory results…', 'loading')));
      }
      try {
        const response = await api(query(RADIO_REFERENCE_IMPORT_PATHS.browseCatalog, {
          country_id: country.value, state_id: region.value || null, county_id: county.value || null
        }), { timeoutMs: 65_000 });
        if (sequence !== state.browseSequence || initializeSequence !== state.initializeSequence) return;
        state.browseRows = rows(response);
        const location = {
          country: country.selectedOptions[0]?.textContent || '',
          region: region.value ? region.selectedOptions[0]?.textContent || '' : '',
          county: county.value ? county.selectedOptions[0]?.textContent || '' : ''
        };
        state.selectedCountyId = Number(county.value) || null;
        state.selectedCountyName = location.county;
        state.browseRows.forEach((entry) => {
          const scope = textValue(entry, ['scope']).toUpperCase();
          entry.breadcrumb = scope === 'NATIONAL' ? 'National' : scope === 'STATE' ?
            location.region || 'Statewide' : [location.region, location.county].filter(Boolean).join(' > ');
        });
        state.browseDocument = { location };
        ++state.detailSequence;
        state.activeSystemId = null;
        state.selectedTalkgroups.clear();
        detailHost.replaceChildren(empty('Choose a result', 'Select a system or agency to review.'));
        browser.classList.remove('radioreference-detail-open');
        showCurrent();
        message.textContent = formatNumber(state.browseRows.length) +
          ' systems and agencies grouped by coverage area.';
      } catch (error) {
        if (sequence !== state.browseSequence || initializeSequence !== state.initializeSequence) return;
        if (state.browseTab === 'browse') {
          listHost.replaceChildren(directoryState(feedback(error.message, 'error')));
        }
        message.textContent = 'RadioReference directory could not be loaded.';
      }
    };
    setOptions(country, state.countries, state.configuration?.country_id, 'Choose a country');
    try {
      await loadStates(state.configuration?.state_id, state.configuration?.county_id);
      if (initializeSequence !== state.initializeSequence) return;
      await browse();
    } catch (error) {
      if (initializeSequence === state.initializeSequence)
        listHost.replaceChildren(directoryState(feedback(error.message, 'error')));
    }
    country.addEventListener('change', async () => {
      const sequence = ++locationSequence;
      message.textContent = 'Loading browse area…';
      try {
        await loadStates(null, null, sequence);
        if (sequence === locationSequence) await browse();
      } catch (error) {
        if (sequence === locationSequence) {
          listHost.replaceChildren(directoryState(feedback(error.message, 'error')));
        }
      }
    });
    region.addEventListener('change', async () => {
      const sequence = ++locationSequence;
      message.textContent = 'Loading counties…';
      try {
        await loadCounties(null, sequence);
        if (sequence === locationSequence) await browse();
      } catch (error) {
        if (sequence === locationSequence) {
          listHost.replaceChildren(directoryState(feedback(error.message, 'error')));
        }
      }
    });
    county.addEventListener('change', () => void browse());
  };

  const initialize = async () => {
    if (state.initializing || state.initialized) return;
    if (state.configuration?.account?.state !== 'VALID_PREMIUM') {
      host.replaceChildren(empty('Connect RadioReference to import',
        'A current Premium account is required for directory browsing and imports.'));
      return;
    }
    state.initializing = true;
    const sequence = ++state.initializeSequence;
    host.replaceChildren(feedback('Loading the RadioReference directory…', 'loading'));
    try {
      const [aliasesDocument, countriesDocument, bookmarksDocument, preferencesResult] = await Promise.all([
        requestJson('/api/v1/admin/alias-lists?include_counts=false', { csrf: false }),
        api(RADIO_REFERENCE_IMPORT_PATHS.countries),
        api(RADIO_REFERENCE_IMPORT_PATHS.bookmarks),
        api(RADIO_REFERENCE_IMPORT_PATHS.systemPreferences)
          .then((value) => ({ value }), (error) => ({ error }))
      ]);
      if (sequence !== state.initializeSequence ||
          state.configuration?.account?.state !== 'VALID_PREMIUM') return;
      state.aliasLists = Array.isArray(aliasesDocument?.alias_lists) ? aliasesDocument.alias_lists :
        rows(aliasesDocument);
      const aliasListRevision = Number(aliasesDocument?.revision);
      state.aliasListRevision = Number.isSafeInteger(aliasListRevision) && aliasListRevision >= 0 ?
        aliasListRevision : 0;
      state.countries = sortRadioReferenceCountries(rows(countriesDocument));
      state.bookmarks = rows(bookmarksDocument).map(normalizeBookmark);
      state.preferenceLoadError = preferencesResult.error?.message || '';
      const preferences = rows(preferencesResult.value).map((value) => [
        integerValue(value, ['system_id', 'systemId']),
        integerValue(value, ['preferred_alias_list_id', 'preferredAliasListId'])
      ]).filter(([systemIdValue]) => systemIdValue);
      state.systemAliasListPreferences = new Map(preferences.filter(([, listId]) => listId));
      state.clearedSystemPreferences = new Set(preferences.filter(([, listId]) => !listId)
        .map(([systemIdValue]) => systemIdValue));
      state.initialized = true;
      await buildBrowser(sequence);
    } catch (error) {
      if (sequence === state.initializeSequence) host.replaceChildren(feedback(error.message, 'error'));
    } finally {
      if (sequence === state.initializeSequence) state.initializing = false;
    }
  };

  return {
    element: host,
    setConfiguration(configuration) {
      const wasConnected = state.configuration?.account?.state === 'VALID_PREMIUM';
      const previousUser = state.configuration?.account?.user_name;
      state.configuration = configuration || {};
      const connected = state.configuration?.account?.state === 'VALID_PREMIUM';
      if (!wasConnected && connected) {
        ++state.initializeSequence;
        state.initializing = false;
      }
      if (wasConnected && connected && previousUser !== state.configuration?.account?.user_name) {
        ++state.initializeSequence;
        ++state.detailSequence;
        ++state.browseSequence;
        state.initializing = false;
        state.initialized = false;
        state.activeSystemId = null;
        state.systemAliasListPreferences.clear();
        state.clearedSystemPreferences.clear();
        state.preferenceSaves.clear();
        state.preferenceRevisions.clear();
        state.preferenceErrors.clear();
        state.preferenceLoadError = '';
        state.siteCatalogs.clear();
        state.talkgroupCatalogs.clear();
        state.talkgroupLoads.clear();
        state.talkgroupCatalogId = null;
        state.selectedTalkgroups.clear();
      }
      if (!connected) {
        ++state.initializeSequence;
        ++state.detailSequence;
        ++state.browseSequence;
        state.initializing = false;
        state.initialized = false;
        state.activeSystemId = null;
        state.selectedTalkgroups.clear();
        state.siteCatalogs.clear();
        state.talkgroupCatalogs.clear();
        state.talkgroupLoads.clear();
        state.talkgroupCatalogId = null;
        state.bookmarks = [];
        state.systemAliasListPreferences.clear();
        state.clearedSystemPreferences.clear();
        state.preferenceSaves.clear();
        state.preferenceRevisions.clear();
        state.preferenceErrors.clear();
        state.preferenceLoadError = '';
        host.replaceChildren(empty('Connect RadioReference to import',
          'A current Premium account is required for directory browsing and imports.'));
        return;
      }
      if (!wasConnected || !state.initialized) void initialize();
    },
    reload() {
      ++state.initializeSequence;
      ++state.detailSequence;
      ++state.browseSequence;
      state.initializing = false;
      state.initialized = false;
      state.activeSystemId = null;
      state.siteCatalogs.clear();
      state.talkgroupCatalogs.clear();
      state.talkgroupLoads.clear();
      state.talkgroupCatalogId = null;
      state.selectedTalkgroups.clear();
      void initialize();
    }
  };
}
