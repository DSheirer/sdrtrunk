const ROOT = '/api/v1/admin/streaming';
const POLL_MS = 3000;

// Reuse map: data-workspace, ui-catalog-toolbar, ui-surface, ui-metric and ui-status
// compose the destination cards; shared table, feedback, fields, toggles, select frames,
// segmented tabs, selection bar and modal lifecycle compose the editor. Only feature
// geometry lives in streaming.css; desktop/mobile and light/dark use shared tokens.
export function createStreamingWorkspace(deps) {
  const { node, formField, uiSelectFrame, uiToggleField, uiStatus, uiSegmentedControl, table,
    openReadOnlyModal, confirmAction, requestJson, modalFooter, metrics, formatNumber, href,
    browsingWorkflows, signal } = deps;
  const host = node('div', 'streaming-page data-workspace');
  const toolbar = node('div', 'streaming-toolbar streaming-page-toolbar ui-catalog-toolbar');
  const summary = node('div', 'streaming-summary');
  const actions = node('div', 'ui-action-row streaming-page-actions');
  const message = node('div');
  message.setAttribute('role', 'status');
  const destinationHost = node('div', 'streaming-destination-grid');
  const note = node('p', 'muted', 'Delivery counters belong to the current sender session and reset when the destination restarts.');
  const freshness = node('span', 'muted');
  const pageMeta = node('footer', 'streaming-page-meta');
  let catalog = null, timer = null, loading = false, disposed = false, signature = '', modalActive = false;
  const cells = new Map();
  const button = (label, action, primary = false) => {
    const control = node('button', `ui-button ui-button-${primary ? 'primary' : 'secondary'}`, label);
    control.type = 'button';
    if (action) control.addEventListener('click', action);
    return control;
  };
  const feedback = (text, tone = '') => node('div', tone === 'warning' ? 'ui-notice ui-notice-warning' :
    `ui-feedback${tone === 'error' || tone === 'loading' ? ` ui-feedback-${tone}` : ''}`, text);
  const input = (type = 'text') => { const value = node('input', 'ui-input'); value.type = type; return value; };
  const select = (values, current = '') => {
    const control = node('select', 'ui-select');
    values.forEach(([value, label]) => { const option = node('option', '', label); option.value = value; control.append(option); });
    control.value = current;
    return control;
  };
  const read = (path, options = {}) => requestJson(ROOT + path, { csrf: false, signal, ...options });
  const write = (path, method, body, options = {}) => requestJson(ROOT + path,
    { method, body, signal, timeoutMs: 65000, ...options });
  const statusTone = (row) => row.attention ? 'danger' : row.state === 'CONNECTED' ? 'success' : 'neutral';
  const errorTone = (row) => Number(row.errors) > 0 ? (row.attention ? 'danger' : 'warning') : 'blue';
  const count = (value) => value == null ? '—' : formatNumber(value);
  const showError = (target, error) => target.replaceChildren(feedback(error.message || 'The request failed', 'error'));

  function draw(documentValue) {
    catalog = documentValue;
    const rows = catalog.destinations || [];
    summary.replaceChildren(uiStatus(`${rows.length} destination${rows.length === 1 ? '' : 's'}`, 'neutral'),
      uiStatus(`${rows.filter(row => row.state === 'CONNECTED').length} connected`, 'success'),
      uiStatus(`${rows.filter(row => row.attention).length} need attention`, rows.some(row => row.attention) ? 'danger' : 'neutral'),
      uiStatus(`${rows.filter(row => !row.enabled).length} disabled`, 'neutral'));
    const nextSignature = JSON.stringify(rows.map(row => [row.configuration_id, row.name, row.provider, row.enabled]));
    if (signature !== nextSignature || !destinationHost.childElementCount) {
      signature = nextSignature;
      cells.clear();
      const cell = (row, key) => {
        const element = node('span');
        if (!cells.has(row.configuration_id)) cells.set(row.configuration_id, {});
        cells.get(row.configuration_id)[key] = element;
        return element;
      };
      if (!rows.length) {
        const empty = node('div', 'streaming-empty ui-surface');
        empty.append(node('div', 'ui-feedback', 'No streaming destinations configured. Add a destination to get started.'));
        destinationHost.replaceChildren(empty);
      } else {
        destinationHost.replaceChildren(...rows.map(row => {
          const card = node('article', 'streaming-destination-card ui-surface');
          const header = node('header', 'streaming-destination-header');
          const identity = node('div', 'streaming-identity');
          const title = node('h2', 'streaming-destination-title');
          const open = button(row.name || 'Unnamed destination', () => openEditor(row.configuration_id));
          open.className = 'link-button streaming-destination-name';
          title.append(open);
          identity.append(title, node('small', 'muted', row.provider_label));
          const statuses = node('div', 'streaming-destination-statuses');
          statuses.append(cell(row, 'state'), cell(row, 'enabled'));
          header.append(identity, statuses);

          const counters = metrics([
            ['queued', 'Queued', 'icon-replay'], ['sent', 'Sent / uploaded', 'icon-share'],
            ['aged_off', 'Aged off', 'icon-skip'], ['errors', 'Errors', 'icon-warning']
          ].map(([key, label, icon]) => [label, row[key], cell(row, key),
            { icon, tone: key === 'errors' ? errorTone(row) : 'blue' }]), true);
          counters.classList.add('streaming-destination-metrics');
          const error = node('div', 'streaming-destination-error ui-notice ui-notice-danger');
          error.append(node('strong', '', 'Last error'), cell(row, 'last_error'));
          const footer = node('footer', 'streaming-destination-actions ui-action-row');
          footer.append(button('Manage destination', () => openEditor(row.configuration_id)));
          card.append(header, counters, error, footer);
          return card;
        }));
      }
    }
    rows.forEach(row => {
      const target = cells.get(row.configuration_id);
      if (!target) return;
      target.state.replaceChildren(uiStatus(row.state_label, statusTone(row)));
      target.enabled.replaceChildren(uiStatus(row.enabled ? 'Enabled' : 'Disabled', row.enabled ? 'success' : 'neutral'));
      ['queued', 'sent', 'aged_off', 'errors'].forEach(key => { target[key].textContent = count(row[key]); });
      const errorsMetric = target.errors.closest('.ui-metric');
      ['blue', 'warning', 'danger'].forEach(tone => {
        errorsMetric.classList.toggle(`ui-metric-${tone}`, tone === errorTone(row));
      });
      const error = target.last_error.closest('.streaming-destination-error');
      target.last_error.textContent = row.last_error || '';
      error.hidden = !row.last_error;
    });
    freshness.textContent = 'Status updated just now';
  }

  async function refresh() {
    if (disposed || loading || modalActive) return;
    loading = true;
    try { const data = await read(''); if (!disposed) { draw(data); message.replaceChildren(); } }
    catch (error) { if (!disposed && error.name !== 'AbortError') { showError(message, error); freshness.textContent = 'Status unavailable · retrying'; } }
    finally { loading = false; }
  }
  async function poll() {
    if (!document.hidden) await refresh();
    if (!disposed) timer = setTimeout(poll, POLL_MS);
  }

  async function openEditor(id = null, initialTab = 'settings') {
    const content = node('div', 'streaming-editor editor-workspace');
    content.append(feedback('Loading destination…', 'loading'));
    const abort = new AbortController();
    let alive = true, busy = false, settingsDirty = false, assignmentDirty = false, revision, options, definition;
    let tabs = null, statusTimer = null;
    let activeTab = initialTab, aliasLoaded = false, aliasOffset = 0, aliasTotal = 0, aliasSequence = 0;
    const changedAliases = new Map(), aliasOriginal = new Map(), controls = new Map();
    const modal = openReadOnlyModal(id ? 'Streaming destination' : 'Add streaming destination', content, {
      id: 'streaming-destination', className: 'streaming-editor-modal',
      cleanup: () => { alive = false; abort.abort(); clearTimeout(statusTimer); modalActive = false; void refresh(); }
    });
    if (!modal || (modal.ready && !await modal.ready)) return;
    modalActive = true;
    const requestOptions = { signal: abort.signal, page: false };
    const localMessage = node('div');
    localMessage.setAttribute('role', 'status');
    const dirty = () => modal.setDirty(settingsDirty || assignmentDirty);
    const settingsPanel = node('section', 'streaming-panel');
    const aliasesPanel = node('section', 'streaming-panel');
    const statusPanel = node('section', 'streaming-panel');
    const panels = { settings: settingsPanel, aliases: aliasesPanel, status: statusPanel };
    const save = button('Save settings', () => void saveActive(), true);
    const test = button('Test connection', () => void run(async () => {
      const result = await write('/test', 'POST', { configuration_id: id, provider: definition.provider, settings: readChanges() }, requestOptions);
      if (alive) localMessage.replaceChildren(feedback(result.message, result.success ? 'success' : 'error'));
    }));
    const cancel = button('Cancel', modal.close);
    const remove = button('Delete destination', () => void removeDestination());
    remove.classList.remove('ui-button-secondary');
    remove.classList.add('ui-button-danger');
    const reload = button('Reload current values', () => {
      const reopen = () => void openEditor(id, activeTab);
      if (modal.close(reopen)) reopen();
    });
    reload.hidden = true;
    const footer = modalFooter(test, remove, cancel, save);
    const providerControl = select([], '');
    providerControl.setAttribute('aria-label', 'Provider');
    const fields = node('div', 'streaming-fields');
    const advanced = node('details', 'streaming-advanced');
    const advancedFields = node('div', 'streaming-fields');
    advanced.append(node('summary', '', 'Advanced settings'), advancedFields);
    const warning = feedback('Saving restarts this destination. Queued calls may be discarded and session counters reset. Receiving continues.', 'warning');
    const aliasSearch = input('search');
    aliasSearch.placeholder = 'Search name, identifier, or Alias List';
    const assignedOnly = select([['false', 'All aliases'], ['true', 'Assigned only']], 'false');
    const aliasRows = node('div');
    const aliasCount = node('span', 'muted');
    const aliasPager = browsingWorkflows.createBrowsingPager({ node,
      className: 'streaming-alias-pager', ariaLabel: 'Alias pages' });
    const drawAliasPager = (loading = false) => aliasPager.update({
      countText: browsingWorkflows.pageRangeText({ offset: aliasOffset, visible: visibleAliases.length,
        total: aliasTotal, label: 'Aliases', format: formatNumber }),
      previous: { enabled: !loading && aliasOffset > 0, onClick: () => {
        aliasOffset = Math.max(0, aliasOffset - 50); void loadAliases();
      } },
      next: { enabled: !loading && aliasOffset + 50 < aliasTotal, onClick: () => {
        aliasOffset += 50; void loadAliases();
      } }
    });
    const aliasQuery = node('form', 'streaming-alias-filters ui-catalog-toolbar');
    const searchButton = button('Search'); searchButton.type = 'submit';
    searchButton.classList.add('streaming-alias-search');
    const queryFields = [formField('Search aliases', aliasSearch), formField('Show', uiSelectFrame(assignedOnly))];
    queryFields.forEach(field => field.classList.add('streaming-query-field'));
    aliasQuery.append(...queryFields, searchButton);
    aliasQuery.addEventListener('submit', event => { event.preventDefault(); aliasOffset = 0; void loadAliases(); });
    assignedOnly.addEventListener('change', () => { aliasOffset = 0; void loadAliases(); });
    const aliasActions = node('div', 'streaming-alias-actions ui-selection-bar');
    const aliasLayoutHost = node('div', 'streaming-alias-layout');
    const aliasTableController = {};
    let pageToggle = null;
    let visibleAliases = [];
    const visibleAliasChecks = () => [...aliasRows.querySelectorAll('tbody input[type="checkbox"]')];
    const syncPageToggle = () => {
      if (!pageToggle) return;
      const checked = visibleAliases.filter(row => changedAliases.has(row.id) ?
        changedAliases.get(row.id) : row.assigned).length;
      pageToggle.checked = visibleAliases.length > 0 && checked === visibleAliases.length;
      pageToggle.indeterminate = checked > 0 && checked < visibleAliases.length;
      pageToggle.disabled = visibleAliases.length === 0;
    };
    const renderPageToggle = () => {
      const checkbox = node('input', 'ui-selection-check');
      checkbox.type = 'checkbox';
      checkbox.setAttribute('aria-label', 'Send all aliases on this page to this destination');
      checkbox.addEventListener('change', () => {
        const checked = checkbox.checked;
        visibleAliasChecks().forEach(control => {
          if (control.checked === checked) return;
          control.checked = checked;
          control.dispatchEvent(new Event('change'));
        });
        syncPageToggle();
      });
      pageToggle = checkbox;
      syncPageToggle();
      return checkbox;
    };
    aliasActions.append(aliasCount, aliasLayoutHost);
    const aliasTableRegion = node('div', 'streaming-alias-table-region');
    aliasTableRegion.append(aliasActions, aliasRows);
    aliasesPanel.append(aliasQuery, aliasTableRegion, aliasPager,
      node('p', 'muted', 'These are the same assignments shown in the Alias Editor. Only your explicit changes are saved, up to 500 per save.'));

    function setBusy(value) {
      busy = value; modal.setBusy(value);
      content.querySelectorAll('button,input,select,summary').forEach(control => {
        if (value) { control.dataset.wasDisabled = String(Boolean(control.disabled)); control.disabled = true; }
        else { control.disabled = control.dataset.wasDisabled === 'true'; delete control.dataset.wasDisabled; }
      });
      if (!value) syncButtons();
    }
    async function run(operation) {
      if (busy || !alive) return;
      setBusy(true); localMessage.replaceChildren(); reload.hidden = true;
      try { await operation(); }
      catch (error) { if (alive && error.name !== 'AbortError') { showError(localMessage, error); reload.hidden = error.code !== 'stale_revision'; } }
      finally { if (alive) setBusy(false); }
    }
    function syncButtons() {
      if (!definition) return;
      save.hidden = activeTab === 'status';
      save.textContent = activeTab === 'aliases' ? 'Save assignments' : !id ? 'Add destination' :
        controls.get('enabled')?.checked ? 'Save and reconnect' : 'Save settings';
      save.disabled = busy || (activeTab === 'aliases' ? !assignmentDirty : id && !settingsDirty);
      test.hidden = activeTab !== 'settings' || !options.providers.find(item => item.id === definition.provider)?.connection_test;
      remove.hidden = !id;
      providerControl.disabled = busy || Boolean(id);
      tabs?.querySelectorAll('button').forEach(control => { control.disabled = busy || (!id && control.dataset.value !== 'settings'); });
    }
    function switchTab(value) {
      activeTab = value;
      Object.entries(panels).forEach(([key, panel]) => { panel.hidden = key !== value; });
      syncButtons();
      if (value === 'aliases' && !aliasLoaded && id) void loadAliases();
      clearTimeout(statusTimer);
      if (value === 'status' && id) void updateStatus();
    }
    function bindField(field, target) {
      const initial = definition.settings[field.key];
      let control;
      if (field.type === 'boolean') {
        const toggle = uiToggleField(field.label, initial === true, field.label,
          field.key === 'ignore_certificate_errors' ? 'Disables certificate validation for this destination.' : '');
        control = toggle.querySelector('input'); target.append(toggle);
      } else if (field.key === 'alias_list_id' || field.key === 'channel_configuration_id') {
        const sites = options.sites || [];
        const choices = field.key === 'alias_list_id' ? [...new Map(sites.map(site => [String(site.alias_list_id), site.alias_list_name])).entries()] :
          sites.filter(site => Number(site.alias_list_id) === Number(controls.get('alias_list_id')?.value || definition.settings.alias_list_id))
            .map(site => [site.configuration_id, site.name]);
        if (initial && !choices.some(([key]) => String(key) === String(initial))) choices.unshift([String(initial), 'Saved selection (currently unavailable)']);
        control = select([['', 'Choose…'], ...choices], String(initial || ''));
        target.append(formField(field.label, uiSelectFrame(control), field.key === 'channel_configuration_id' ?
          'A call is sent only when an alias routes it here and the selected trunked site observed it. Calls heard only on other sites are excluded.' : ''));
      } else if (field.type === 'select') {
        control = select(field.choices.map(value => [value, value.replaceAll('_', ' ').toLowerCase()]), initial);
        target.append(formField(field.label, uiSelectFrame(control)));
      } else {
        control = input(field.type === 'password' ? 'password' : field.type === 'number' ? 'number' : 'text');
        control.value = field.type === 'password' ? '' : (initial ?? '');
        if (field.type === 'number') { control.min = String(field.minimum); control.max = String(field.maximum); control.step = '1'; }
        else control.maxLength = Number(field.maximum) || 1024;
        if (field.key === 'name') control.required = true;
        if (field.type === 'password') {
          control.autocomplete = 'new-password';
          const configured = definition.configured_credentials.includes(field.key);
          control.placeholder = configured ? 'Leave blank to keep saved credential' : 'Enter credential';
          target.append(formField(field.label, control, configured ? 'Saved credential configured. Enter a value to replace it.' : 'Stored only on the receiver.'));
        } else target.append(formField(field.label, control));
      }
      control.name = field.key;
      control.setAttribute('aria-label', field.label);
      controls.set(field.key, control);
      control.addEventListener('input', () => { settingsDirty = true; dirty(); syncButtons(); });
      control.addEventListener('change', () => {
        settingsDirty = true; dirty(); syncButtons();
        if (field.key === 'alias_list_id') {
          const site = controls.get('channel_configuration_id');
          const choices = (options.sites || []).filter(row => String(row.alias_list_id) === control.value);
          site.replaceChildren();
          const empty = node('option', '', 'Choose…'); empty.value = ''; site.append(empty);
          choices.forEach(row => { const option = node('option', '', row.name); option.value = row.configuration_id; site.append(option); });
        }
      });
    }
    function renderFields() {
      controls.clear(); fields.replaceChildren(); advancedFields.replaceChildren();
      const provider = options.providers.find(item => item.id === definition.provider);
      if (!provider) throw new Error('This provider cannot be edited.');
      provider.fields.forEach(field => bindField(field, field.advanced ? advancedFields : fields));
      if (definition.provider === 'RADIORESOLVE') fields.append(node('p', 'muted',
        'Delivery uses a durable queue: calls are kept for up to 24 hours, within a 2 GiB storage limit.'));
      advanced.hidden = !advancedFields.childElementCount;
      warning.hidden = !id;
      settingsDirty = false; dirty(); syncButtons();
    }
    function readChanges() {
      const settings = {};
      for (const field of options.providers.find(item => item.id === definition.provider).fields) {
        const control = controls.get(field.key);
        if (field.type === 'password' && !control.value) continue;
        let value = field.type === 'boolean' ? control.checked : control.value;
        if (field.type === 'number') {
          if (String(value) === String(definition.settings[field.key] ?? '')) continue;
          if (value === '' && (!id || definition.settings[field.key] == null)) continue;
          value = Number(value);
          if (!Number.isSafeInteger(value) || value < field.minimum || value > field.maximum) {
            control.focus(); throw new Error(`${field.label} is out of range`);
          }
        }
        if (value !== definition.settings[field.key] && !(value === '' && definition.settings[field.key] == null)) settings[field.key] = value;
      }
      return settings;
    }
    async function loadAliases() {
      if (!id || !alive || busy) return;
      const sequence = ++aliasSequence;
      searchButton.disabled = true;
      drawAliasPager(true);
      try {
        const query = new URLSearchParams({ q: aliasSearch.value, assigned: assignedOnly.value, offset: aliasOffset, limit: 50 });
        const result = await read(`/${id}/aliases?${query}`, requestOptions);
        if (!alive || sequence !== aliasSequence) return;
        if (revision !== result.revision) throw Object.assign(new Error('Configuration changed. Reload before changing assignments.'), { code: 'stale_revision' });
        aliasLoaded = true; aliasTotal = result.total;
        visibleAliases = result.items;
        aliasRows.replaceChildren(table(result.items, [
          { id: 'selected', label: '', layoutLabel: 'Assign aliases', essential: true, fixed: true,
            renderHeader: renderPageToggle, render: row => {
            aliasOriginal.set(row.id, row.assigned);
            const check = node('input', 'ui-selection-check');
            check.type = 'checkbox';
            check.checked = changedAliases.has(row.id) ? changedAliases.get(row.id) : row.assigned;
            check.setAttribute('aria-label', `Send ${row.name || row.identifier} to this destination`);
            check.addEventListener('change', () => {
              if (check.checked === aliasOriginal.get(row.id)) changedAliases.delete(row.id); else changedAliases.set(row.id, check.checked);
              assignmentDirty = changedAliases.size > 0; dirty(); syncButtons(); syncPageToggle();
            }); return check;
          } },
          { id: 'name', label: 'Alias', render: row => {
            const link = node('a', '', row.name || 'Unnamed alias');
            link.href = href('aliases', { list: row.alias_list_id, alias: row.id }); return link;
          } },
          { id: 'identifier', label: 'Identifier', render: row => row.identifier },
          { id: 'list', label: 'Alias List', render: row => row.alias_list_name }
        ], 'No matching aliases', { type: 'streaming-aliases', sortable: false, mobileCards: true,
          tableClass: 'ui-mobile-cards ui-data-table-quiet', layoutMenuHost: aliasLayoutHost,
          controller: aliasTableController }));
        syncPageToggle();
        aliasCount.textContent = `${formatNumber(result.total)} ${result.total === 1 ? 'alias' : 'aliases'}`;
      } catch (error) { if (alive && error.name !== 'AbortError') { showError(localMessage, error); reload.hidden = error.code !== 'stale_revision'; } }
      finally { if (alive && sequence === aliasSequence) { searchButton.disabled = false; drawAliasPager(); } }
    }
    function drawStatus(row) {
      if (!row) { statusPanel.replaceChildren(feedback('Destination no longer exists.', 'error')); return; }
      const counters = metrics([
        ['queued', 'Queued', 'icon-replay'], ['sent', 'Sent / uploaded', 'icon-share'],
        ['aged_off', 'Aged off', 'icon-skip'], ['errors', 'Errors', 'icon-warning']
      ].map(([key, label, icon]) => [label, row[key], count(row[key]),
        { icon, tone: key === 'errors' ? errorTone(row) : 'blue' }]));
      const facts = node('dl', 'ui-facts');
      for (const [label, value] of [['Last error', row.last_error || 'None'], ['Provider', row.provider_label]]) {
        const fact = node('div', 'ui-fact'); fact.append(node('dt', '', label), node('dd', '', value)); facts.append(fact);
      }
      statusPanel.replaceChildren(uiStatus(row.state_label, statusTone(row)), counters, facts,
        node('p', 'muted', 'Counts describe delivery by this sender, not the dashboard’s “Submitted to Streamer” total.'),
        button('Refresh status', () => void updateStatus()));
    }
    async function updateStatus() {
      try { if (!busy) { const result = await read(`/${id}`, requestOptions); if (alive) drawStatus(result.status); } }
      catch (error) { if (alive && error.name !== 'AbortError') showError(statusPanel, error); }
      finally {
        clearTimeout(statusTimer);
        if (alive && activeTab === 'status') statusTimer = setTimeout(() => void updateStatus(), POLL_MS);
      }
    }
    async function saveActive() {
      await run(async () => {
        let result;
        if (activeTab === 'aliases') {
          result = await write(`/${id}/aliases`, 'POST', { revision,
            add: [...changedAliases].filter(([, value]) => value).map(([key]) => key),
            remove: [...changedAliases].filter(([, value]) => !value).map(([key]) => key) }, requestOptions);
          revision = result.revision; changedAliases.clear(); assignmentDirty = false; aliasLoaded = false;
        } else {
          const settings = readChanges();
          result = await write(id ? `/${id}` : '', id ? 'PUT' : 'POST', { revision, provider: definition.provider, settings }, requestOptions);
          id = result.configuration_id; revision = result.revision; settingsDirty = false;
          const current = await read(`/${id}`, requestOptions);
          if (!alive) return;
          definition = current.destination; renderFields();
          providerControl.disabled = true;
          if (tabs) tabs.querySelectorAll('button').forEach(control => { control.disabled = false; });
        }
        if (!alive) return;
        dirty(); localMessage.replaceChildren(feedback('Changes saved.', 'success'));
      });
      if (alive && activeTab === 'aliases' && !aliasLoaded) void loadAliases();
    }
    async function removeDestination() {
      await run(async () => {
        const current = await read(`/${id}`, requestOptions);
        if (!alive) return;
        const references = current.references;
        if (references.aliases || references.alias_lists.length) {
          localMessage.replaceChildren(feedback(`This destination is used by ${formatNumber(references.aliases)} aliases. Remove its assignments before deleting.`, 'warning'));
          references.alias_lists.forEach(list => {
            const link = node('a', '', `${list.name} · ${[list.unmatched ? 'unknown calls' : '', list.new_aliases ? 'new alias defaults' : ''].filter(Boolean).join(', ')}`);
            link.href = href('aliases', { list: list.id }); localMessage.append(link);
          }); return;
        }
        if (!await confirmAction(`Delete ${definition.settings.name}? This removes its saved connection settings.`, {
          title: 'Delete streaming destination', confirmLabel: 'Delete destination'
        }) || !alive) return;
        await write(`/${id}`, 'DELETE', { revision }, requestOptions);
        modal.setDirty(false); settingsDirty = assignmentDirty = false;
        modal.setBusy(false); modal.close();
      });
    }
    try {
      options = await read('/options', requestOptions);
      const existing = id ? await read(`/${id}`, requestOptions) : null;
      definition = existing?.destination || await read('/templates/BROADCASTIFY_CALL', requestOptions);
      revision = existing?.revision || options.revision;
      if (!alive) return;
      providerControl.replaceChildren(...options.providers.map(provider => {
        const option = node('option', '', provider.label); option.value = provider.id; return option;
      }));
      providerControl.value = definition.provider; providerControl.disabled = Boolean(id);
      providerControl.addEventListener('change', () => void run(async () => {
        definition = await read(`/templates/${providerControl.value}`, requestOptions);
        if (alive) renderFields();
      }));
      settingsPanel.append(formField('Provider', uiSelectFrame(providerControl)), fields, advanced, warning);
      tabs = uiSegmentedControl([{ value: 'settings', label: 'Settings' }, { value: 'aliases', label: 'Aliases' },
        { value: 'status', label: 'Status' }], initialTab, switchTab);
      tabs.setAttribute('aria-label', 'Streaming destination sections');
      content.replaceChildren(tabs, settingsPanel, aliasesPanel, statusPanel, localMessage, reload, footer);
      renderFields();
      if (existing) drawStatus(existing.status);
      switchTab(initialTab);
    } catch (error) { if (alive && error.name !== 'AbortError') content.replaceChildren(feedback(error.message, 'error'),
      button('Retry', () => {
        const reopen = () => void openEditor(id, initialTab);
        if (modal.close(reopen)) reopen();
      })); }
  }

  async function findFeeds() {
    const body = node('div', 'streaming-feed-browser editor-workspace');
    let alive = true;
    const abort = new AbortController();
    const modal = openReadOnlyModal('Available Broadcastify feeds', body, {
      id: 'streaming-feeds', className: 'streaming-feeds-modal',
      cleanup: () => { alive = false; abort.abort(); modalActive = false; void refresh(); }
    });
    if (!modal || (modal.ready && !await modal.ready)) return;
    modalActive = true;
    async function loadFeeds() {
      body.replaceChildren(feedback('Loading available Broadcastify feeds…', 'loading'));
      try {
        const result = await write('/feeds/refresh', 'POST', {}, { signal: abort.signal, page: false });
        const options = await read('/options', { signal: abort.signal, page: false });
        if (!alive) return;
        const rows = result.items || [];
        const introduction = node('div', 'streaming-feed-introduction');
        const introductionText = node('div');
        introductionText.append(node('h3', '', 'Feeds assigned to this account'),
          node('p', 'muted', 'Add a feed as a disabled destination, then review its settings before connecting.'));
        introduction.append(introductionText, uiStatus(`${rows.length} feed${rows.length === 1 ? '' : 's'}`, 'neutral'));
        if (!rows.length) {
          const empty = node('div', 'streaming-feed-empty ui-empty-state');
          empty.append(node('h3', '', 'No assigned feeds'),
            node('p', '', 'No Broadcastify feeds are assigned to the saved RadioReference account.'));
          body.replaceChildren(introduction, empty);
          return;
        }
        const grid = node('div', 'streaming-feed-grid');
        rows.forEach(row => {
          const card = node('article', 'streaming-feed-card ui-surface');
          const header = node('header', 'streaming-feed-card-header');
          const identity = node('div', 'streaming-feed-identity');
          identity.append(node('h3', 'streaming-feed-name', row.name || 'Unnamed Broadcastify feed'),
            node('span', 'muted', 'Broadcastify feed'));
          header.append(identity, uiStatus(row.configured ? 'Configured' : 'Available', row.configured ? 'success' : 'neutral'));
          const facts = node('dl', 'ui-facts streaming-feed-facts');
          const feedId = node('div', 'ui-fact');
          feedId.append(node('dt', '', 'Feed ID'), node('dd', '', formatNumber(row.id)));
          facts.append(feedId);
          const cardMessage = node('div');
          cardMessage.setAttribute('role', 'status');
          const cardActions = node('footer', 'streaming-feed-card-actions ui-action-row');
          const add = button(row.configured ? 'Already added' : 'Add feed', async () => {
            modal.setBusy(true);
            body.querySelectorAll('button').forEach(control => {
              control.dataset.wasDisabled = String(control.disabled);
              control.disabled = true;
            });
            cardMessage.replaceChildren();
            try {
              const saved = await write('/feeds', 'POST', { feed_id: row.id, revision: options.revision },
                { signal: abort.signal, page: false });
              if (alive) { modal.setBusy(false); modal.close(); void openEditor(saved.configuration_id); }
            } catch (error) {
              if (alive && error.name !== 'AbortError') {
                cardMessage.replaceChildren(feedback(error.message || 'The feed could not be added', 'error'));
                modal.setBusy(false);
                body.querySelectorAll('button').forEach(control => {
                  control.disabled = control.dataset.wasDisabled === 'true';
                  delete control.dataset.wasDisabled;
                });
              }
            }
          }, !row.configured);
          add.classList.add('streaming-feed-add');
          add.disabled = row.configured;
          cardActions.append(add);
          card.append(header, facts, cardMessage, cardActions);
          grid.append(card);
        });
        body.replaceChildren(introduction, grid);
      } catch (error) {
        if (alive && error.name !== 'AbortError') {
          const errorState = node('div', 'streaming-feed-error');
          errorState.append(feedback(error.message || 'Available feeds could not be loaded', 'error'));
          const recovery = node('div', 'ui-action-row');
          const settings = node('a', 'ui-button ui-button-secondary', 'Open RadioReference settings');
          settings.href = href('radioreference');
          recovery.append(button('Try again', () => void loadFeeds()), settings);
          errorState.append(recovery);
          body.replaceChildren(errorState);
        }
      }
    }
    void loadFeeds();
  }

  actions.append(button('Find Broadcastify feeds', () => void findFeeds()), button('Add destination', () => void openEditor(), true));
  toolbar.append(summary, actions);
  pageMeta.append(note, freshness);
  host.append(toolbar, message, destinationHost, pageMeta);
  const dispose = () => { disposed = true; clearTimeout(timer); };
  signal?.addEventListener('abort', dispose, { once: true });
  void poll();
  return { element: host, dispose, refresh };
}
