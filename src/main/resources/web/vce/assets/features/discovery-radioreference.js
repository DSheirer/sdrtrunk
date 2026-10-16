const API = '/api/v1/admin/radioreference';
const DIRECTORY_TIMEOUT_MS = 15_000;

export function createDiscoveryRadioReferenceContext(ui, request) {
  const { node, uiSelect, uiSelectFrame, formField, uiActionButton, anchor, href } = ui;
  const element = node('details', 'ui-section-disclosure ui-section-disclosure-flat');
  element.append(node('summary', 'ui-section-summary', 'RadioReference names (optional)'));
  const status = node('p', 'ui-field-hint', 'Loading RadioReference settings…');
  let retryLoad = null;
  const retry = uiActionButton('Retry loading regions', '', () => {
    retry.hidden = true;
    void retryLoad?.();
  }, 'ui-button ui-button-secondary');
  retry.hidden = true;
  element.append(status, retry);
  let stateId = null;
  let loading = null;
  let regionLoading = null;
  let busy = false;
  const controls = new Map([[retry, true]]);
  const enable = (control, value) => {
    controls.set(control, value);
    control.disabled = busy || !value;
  };
  const setBusy = (value) => {
    busy = value;
    controls.forEach((enabled, control) => { control.disabled = busy || !enabled; });
  };
  const rows = (document) => Array.isArray(document) ? document : document?.items || document?.rows || [];
  const load = () => {
    if (loading) return loading;
    retry.hidden = true;
    status.textContent = 'Loading RadioReference settings…';
    loading = (async () => {
      try {
        const configuration = await request(API, { csrf: false, timeoutMs: DIRECTORY_TIMEOUT_MS });
        const stored = Number(configuration?.state_id);
        stateId = Number.isSafeInteger(stored) && stored > 0 ? stored : null;
        if (configuration?.account?.state !== 'VALID_PREMIUM') {
          status.replaceChildren('Connect a Premium account to add directory names. ',
            anchor('RadioReference settings', href('radioreference')));
          return;
        }
        if (!stateId || !configuration.country_id) element.open = true;
        const select = uiSelect([{ value: '', label: 'Skip frequency matching' }]);
        select.setAttribute('aria-label', 'RadioReference state or province');
        enable(select, false);
        select.addEventListener('change', () => { stateId = Number(select.value) || null; });
        const stateField = formField('State or province', uiSelectFrame(select));
        let regionLoad = 0;
        const loadStates = (countryId, selectedState = null) => {
          regionLoading = (async () => {
            const generation = ++regionLoad;
            stateId = null;
            retry.hidden = true;
            enable(select, false);
            status.textContent = 'Loading states and provinces…';
            try {
              const states = rows(await request(`${API}/states?country_id=${encodeURIComponent(countryId)}`,
                { csrf: false, timeoutMs: DIRECTORY_TIMEOUT_MS }));
              if (generation !== regionLoad) return;
              select.replaceChildren(node('option', '', 'Skip frequency matching'), ...states.map((state) => {
                const option = node('option', '', state.name);
                option.value = String(state.id);
                return option;
              }));
              select.firstChild.value = '';
              select.value = states.some((state) => Number(state.id) === selectedState) ? String(selectedState) : '';
              stateId = Number(select.value) || null;
              if (!stateId) element.open = true;
              enable(select, Boolean(states.length));
              status.textContent = states.length ?
                'Choose your state or province to help match nearby systems.' :
                'No states or provinces are available. Choose another country in RadioReference settings.';
            } catch (_) {
              if (generation !== regionLoad) return;
              stateId = selectedState;
              if (selectedState) {
                const saved = node('option', '', 'Saved lookup region');
                saved.value = String(selectedState);
                select.replaceChildren(saved);
              }
              status.textContent = selectedState ?
                'States and provinces could not be loaded. Your saved lookup region will be used.' :
                'States and provinces could not be loaded. Retry, or continue without frequency matching.';
              retryLoad = () => loadStates(countryId, selectedState);
              retry.hidden = false;
              element.open = true;
            }
          })();
          return regionLoading;
        };
        if (configuration.country_id) {
          element.append(stateField);
          await loadStates(configuration.country_id, stateId);
        } else {
          const countries = rows(await request(`${API}/countries`,
            { csrf: false, timeoutMs: DIRECTORY_TIMEOUT_MS }));
          const country = uiSelect([{ value: '', label: 'Choose a country' },
            ...countries.map((value) => ({ value: value.id, label: value.name }))]);
          enable(country, true);
          country.setAttribute('aria-label', 'RadioReference country');
          country.addEventListener('change', () => {
            if (country.value) void loadStates(country.value);
            else {
              regionLoad += 1;
              regionLoading = null;
              retry.hidden = true;
              stateId = null;
              select.value = '';
              enable(select, false);
              status.textContent = 'Choose a country to look up nearby systems.';
            }
          });
          element.append(formField('Country', uiSelectFrame(country)), stateField);
          status.textContent = 'Choose a country to look up nearby systems.';
        }
        const settings = node('p', 'ui-field-hint');
        settings.append(anchor('Save a default lookup region', href('radioreference')));
        element.append(settings);
      } catch (_) {
        loading = null;
        retryLoad = load;
        retry.hidden = false;
        element.open = true;
        status.textContent = 'Lookup regions could not be loaded. Retry, or continue without choosing a lookup region.';
      }
    })();
    return loading;
  };
  const ready = async () => {
    await load();
    let pending;
    do {
      pending = regionLoading;
      if (pending) await pending;
    } while (pending !== regionLoading);
  };
  return { element, load: ready, stateId: () => stateId, setBusy };
}

const renderedDirectoryResults = new WeakMap();

export function discoveryRadioReferenceSystemUrl(value) {
  const result = value?.radio_reference;
  if (result?.state !== 'matched') return null;
  try {
    const parsed = new URL(result.match?.url);
    return parsed.protocol === 'https:' && /(^|\.)radioreference\.com$/i.test(parsed.hostname) ? parsed.href : null;
  } catch (_) { return null; }
}

export function discoveryRadioReferenceResult(ui, value, existing = null) {
  const result = value?.radio_reference;
  if (!result) return null;
  const { node, anchor, href } = ui;
  const details = existing || node('details', 'ui-section-disclosure ui-section-disclosure-flat');
  if (!existing) details.append(node('summary', 'ui-section-summary', 'RadioReference'));
  const match = result.state === 'matched' ? result.match : null;
  const systemUrl = discoveryRadioReferenceSystemUrl(value);
  const signature = JSON.stringify(match ? [match.system_name, systemUrl] : [result.state]);
  if (renderedDirectoryResults.get(details) === signature) return details;
  renderedDirectoryResults.set(details, signature);
  const children = [];
  if (!match) {
    const fallback = {
      location_required: 'Choose a state or province to look up this frequency.',
      login_required: 'Connect RadioReference to look up directory names.',
      premium_required: 'A Premium account is required for directory names.',
      identity_required: 'More consistent on-air identity is needed for a directory match.',
      no_match: 'No RadioReference site matches this system and frequency.',
      ambiguous: 'Multiple directory entries match. No directory names were applied.',
      unavailable: 'RadioReference is unavailable. The on-air result is still available.',
      pending: 'Looking up the system name…'
    };
    children.push(node('p', 'muted', fallback[result.state] || 'Directory names are unavailable.'));
    if (href && ['location_required', 'login_required', 'premium_required'].includes(result.state)) {
      const action = node('p', 'ui-field-hint');
      action.append(anchor('RadioReference settings', href('radioreference')));
      children.push(action);
    }
  } else {
    const label = node('p');
    const systemName = match.system_name || 'Matched system';
    label.append(systemUrl ? anchor(systemName, systemUrl) : systemName);
    children.push(label);
  }
  if (existing) details.replaceChildren(details.firstElementChild, ...children);
  else details.append(...children);
  return details;
}


export const ALIAS_LIST_NAME_MAX_LENGTH = 128;

export function discoveryAliasListName(system, fallback = '') {
  const value = String(system || '').trim() || String(fallback || '').trim();
  let bounded = value.slice(0, ALIAS_LIST_NAME_MAX_LENGTH);
  if (bounded.length < value.length && /[\uD800-\uDBFF]$/.test(bounded)) bounded = bounded.slice(0, -1);
  return bounded.trim();
}

export function discoveryAliasImportChoice(ui, targets, selected = true) {
  const { node } = ui;
  const matched = targets.some((target) => target.matched);
  const element = node('div', 'spectrum-discovery-review');
  element.append(node('p', 'muted', 'Import all talkgroups from each matched RadioReference system into its new Alias List.'));
  targets.forEach((target) => {
    const lists = node('dl', 'ui-fact-list');
    lists.append(node('dt', '', 'Alias List'), node('dd', '', target.name), node('dt', '', 'System'),
      node('dd', '', target.matched ? target.systemName : 'No RadioReference match. Import these talkgroups later.'));
    element.append(lists);
  });
  const choices = node('fieldset', 'spectrum-discovery-protocols');
  choices.setAttribute('aria-label', 'Import talkgroups');
  let wantsImport = matched && selected;
  [
    [true, 'Yes, import talkgroup names'],
    [false, "No, I'll import these later"]
  ].forEach(([value, label]) => {
    const option = node('label', 'ui-choice-card spectrum-discovery-protocol-option');
    const radio = node('input', 'ui-choice-radio');
    radio.type = 'radio'; radio.name = 'discovery-import-talkgroups'; radio.value = String(value);
    radio.checked = wantsImport === value;
    radio.disabled = value && !matched;
    if (radio.disabled) radio.dataset.importUnavailable = 'true';
    radio.setAttribute('aria-label', label);
    radio.addEventListener('change', () => { wantsImport = value; });
    option.append(radio, node('strong', '', label));
    choices.append(option);
  });
  element.append(choices);
  if (!matched) element.append(node('p', 'ui-field-hint',
    'A RadioReference match and Premium account are needed to import talkgroups. Your channels can still be added.'));
  return { element, wantsImport: () => wantsImport };
}

export function discoveryAliasImportResults(ui, result) {
  const { node } = ui;
  const element = node('section', 'ui-form-section spectrum-discovery-review');
  element.append(node('h4', '', 'Talkgroup imports'));
  (result?.targets || []).forEach((target) => {
    const row = node('div', 'ui-notice' + (target.state === 'imported' ? ' ui-notice-success' :
      ['failed', 'unavailable', 'restart_required'].includes(target.state) ? ' ui-notice-warning' : ''));
    row.append(node('strong', '', target.alias_list_name || target.system_name || 'Alias List'));
    const text = target.state === 'imported' ?
      target.message || `${target.added} added, ${target.updated} updated, ${target.unchanged} unchanged.` :
      target.state === 'importing' ? 'Importing all system talkgroups…' :
      target.state === 'pending' ? 'Waiting to import talkgroups.' : target.message || 'Import these talkgroups later.';
    row.append(node('p', '', text));
    element.append(row);
  });
  return element;
}

export async function importDiscoveryAliases(request, url, initial, onProgress, current = () => true) {
  let result = initial || { targets: [], complete: true };
  const ids = result.targets.filter((target) => !['imported', 'unavailable', 'restart_required'].includes(target.state))
    .map((target) => target.alias_list_id);
  for (const id of ids) {
    if (!current()) break;
    if (result.targets.find((target) => target.alias_list_id === id)?.state === 'restart_required') continue;
    result = { ...result, targets: result.targets.map((target) => target.alias_list_id === id ?
      { ...target, state: 'importing' } : target) };
    onProgress(result);
    try {
      result = await request(url, { method: 'POST', body: { alias_list_id: id }, timeoutMs: 90_000 });
    } catch (cause) {
      if (!current()) break;
      result = { ...result, complete: false, targets: result.targets.map((target) => target.alias_list_id === id ?
        { ...target, state: 'failed', message: cause.message || 'Talkgroups could not be imported. Try again.' } : target) };
    }
    if (current()) onProgress(result);
  }
  return result;
}
