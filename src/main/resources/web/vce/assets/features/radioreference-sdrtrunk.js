async function rr(body) {
  const response = await fetch('/api/v1/radioreference', {
    method: 'POST', credentials: 'same-origin', cache: 'no-store',
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' }, body: JSON.stringify(body)
  });
  const value = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(typeof value.error === 'string' ? value.error :
    value.error?.message || `RadioReference request failed (${response.status})`);
  return value;
}

async function statusRequest() {
  const response = await fetch('/api/v1/radioreference',
    { credentials: 'same-origin', cache: 'no-store', headers: { Accept: 'application/json' } });
  const value = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(value.error || 'Unable to load RadioReference account');
  return value;
}

function option(node, value, label) {
  const item = node('option', '', label); item.value = String(value ?? ''); return item;
}

export function createSdrTrunkRadioReference(deps) {
  const { node, signal } = deps;
  const host = node('div', 'data-workspace');
  const account = node('section', 'section ui-section');
  account.append(node('h2', '', 'RadioReference Account'),
    node('p', 'muted', 'Premium web-service access is required. Credentials use the same preference store as the desktop Playlist Editor.'));
  const accountForm = node('form', 'admin-form');
  const accountGrid = node('div', 'ui-settings-grid sdrtrunk-editor-grid');
  const username = node('input', 'ui-input'); username.autocomplete = 'username'; username.required = true;
  const password = node('input', 'ui-input'); password.type = 'password'; password.autocomplete = 'current-password'; password.required = true;
  const usernameField = node('label', 'ui-field'); usernameField.append(node('span', 'ui-field-label', 'Username'), username);
  const passwordField = node('label', 'ui-field'); passwordField.append(node('span', 'ui-field-label', 'Password'), password);
  accountGrid.append(usernameField, passwordField);
  const store = node('input'); store.type = 'checkbox'; store.checked = true;
  const storeLabel = node('label', 'ui-toggle'); storeLabel.append(store, node('span', '', 'Store login credentials'));
  const signIn = node('button', 'ui-button ui-button-primary', 'Sign in and test connection'); signIn.type = 'submit';
  const accountStatus = node('div', 'ui-notice');
  accountForm.append(accountGrid, storeLabel, signIn, accountStatus); account.append(accountForm);

  const browser = node('section', 'section ui-section');
  browser.append(node('h2', '', 'Import Talkgroups'));
  const selectors = node('div', 'ui-settings-grid sdrtrunk-editor-grid');
  const selects = {};
  [['country', 'Country'], ['state', 'State'], ['county', 'County'], ['system', 'Trunked System'],
    ['aliasList', 'Import to Alias List'], ['category', 'Category']].forEach(([name, label]) => {
    const select = node('select', 'ui-select'); select.name = name;
    const field = node('label', 'ui-field'); field.append(node('span', 'ui-field-label', label), select);
    selectors.append(field); selects[name] = select;
  });
  const tools = node('div', 'ui-action-row');
  const newList = node('input', 'ui-input'); newList.placeholder = 'New Alias List'; newList.maxLength = 25;
  const createList = node('button', 'ui-button ui-button-secondary', 'Create Alias List'); createList.type = 'button';
  const search = node('input', 'ui-input'); search.type = 'search'; search.placeholder = 'Search talkgroup, name, or description';
  const encryptedMuted = node('input'); encryptedMuted.type = 'checkbox';
  const encryptedLabel = node('label', 'ui-toggle'); encryptedLabel.append(encryptedMuted,
    node('span', '', 'Set fully encrypted talkgroups to Do Not Monitor'));
  tools.append(newList, createList, search, encryptedLabel);
  const selectionTools = node('div', 'ui-action-row');
  const selectAll = node('button', 'ui-button ui-button-secondary', 'Select all visible'); selectAll.type = 'button';
  const clearAll = node('button', 'ui-button ui-button-secondary', 'Clear selection'); clearAll.type = 'button';
  const importButton = node('button', 'ui-button ui-button-primary', 'Import selected talkgroups'); importButton.type = 'button';
  const selectedLabel = node('span', 'muted');
  selectionTools.append(selectAll, clearAll, importButton, selectedLabel);
  const result = node('div'); result.setAttribute('role', 'status');
  const tableHost = node('div', 'ui-table-scroll');
  browser.append(selectors, tools, selectionTools, result, tableHost);
  browser.hidden = true;
  host.append(account, browser);
  let stateSystems = [];
  let countySystems = [];
  let talkgroups = [];
  let categories = [];
  let stopped = false;

  function fill(select, rows, prompt, selected = '') {
    select.replaceChildren(option(node, '', prompt), ...(rows || []).map((row) =>
      option(node, row.id ?? row, row.name ?? row)));
    select.value = selected ? String(selected) : '';
    select.disabled = !(rows || []).length;
  }

  function combinedSystems() {
    const map = new Map([...stateSystems, ...countySystems].map((row) => [String(row.id), row]));
    fill(selects.system, [...map.values()], 'Select system');
  }

  function selectedCount() {
    const count = tableHost.querySelectorAll('input[type="checkbox"]:checked:not(:disabled)').length;
    selectedLabel.textContent = `${count} selected`;
    return count;
  }

  function renderTalkgroups() {
    const query = search.value.trim().toLowerCase();
    const category = selects.category.value;
    const visible = talkgroups.filter((row) => (!category || String(row.categoryId) === category) &&
      (!query || [row.value, row.name, row.description, row.category].some((value) => String(value || '').toLowerCase().includes(query))));
    const table = node('table', 'ui-table');
    const head = node('thead'); const headRow = node('tr');
    ['', 'Talkgroup', 'Alpha Tag', 'Description', 'Category', 'Mode', 'Encryption', 'Status']
      .forEach((label) => headRow.append(node('th', '', label))); head.append(headRow);
    const body = node('tbody');
    visible.forEach((row) => {
      const tr = node('tr');
      const check = node('input'); check.type = 'checkbox'; check.dataset.id = String(row.id);
      check.disabled = row.imported; check.addEventListener('change', selectedCount);
      [check, row.value, row.name, row.description, row.category, row.mode, row.encryption,
        row.imported ? 'Already imported' : 'Available'].forEach((value) => {
        const td = node('td'); if (value instanceof Node) td.append(value); else td.textContent = String(value ?? ''); tr.append(td);
      });
      body.append(tr);
    });
    table.append(head, body); tableHost.replaceChildren(table); selectedCount();
  }

  async function preview() {
    if (!selects.system.value) { talkgroups = []; renderTalkgroups(); return; }
    result.textContent = 'Loading system talkgroups…';
    try {
      const value = await rr({ action: 'preview', systemId: Number(selects.system.value),
        aliasList: selects.aliasList.value });
      talkgroups = value.talkgroups || []; categories = value.categories || [];
      fill(selects.category, categories, 'All categories');
      renderTalkgroups();
      result.className = value.supported ? 'ui-notice ui-notice-success' : 'ui-notice ui-notice-warning';
      result.textContent = `${value.system} · ${value.protocol} · ${talkgroups.length} talkgroups` +
        (value.supported ? '' : ' · this protocol is not supported');
    } catch (cause) { result.className = 'ui-notice ui-notice-danger'; result.textContent = cause.message; }
  }

  async function loadCountry(id, preferredState = '', preferredCounty = '') {
    fill(selects.state, [], 'Select state'); fill(selects.county, [], 'Select county');
    stateSystems = []; countySystems = []; combinedSystems();
    if (!id) return;
    const value = await rr({ action: 'country', countryId: Number(id) });
    fill(selects.state, value.states || [], 'Select state', preferredState);
    if (preferredState) await loadState(preferredState, preferredCounty);
  }

  async function loadState(id, preferredCounty = '') {
    fill(selects.county, [], 'Select county'); countySystems = [];
    if (!id) { stateSystems = []; combinedSystems(); return; }
    const value = await rr({ action: 'state', stateId: Number(id) });
    stateSystems = value.systems || []; fill(selects.county, value.counties || [], 'Select county', preferredCounty);
    combinedSystems(); if (preferredCounty) await loadCounty(preferredCounty);
  }

  async function loadCounty(id) {
    countySystems = [];
    if (id) countySystems = (await rr({ action: 'county', countyId: Number(id) })).systems || [];
    combinedSystems();
  }

  async function loadAccount() {
    try {
      const value = await statusRequest();
      username.value = value.userName || ''; store.checked = value.storeCredentials !== false;
      fill(selects.aliasList, value.aliasLists || [], 'Select Alias List');
      browser.hidden = !value.premium;
      accountStatus.className = `ui-notice ${value.premium ? 'ui-notice-success' : 'ui-notice-warning'}`;
      accountStatus.textContent = value.premium ? `Signed in as ${value.userName} · Premium${value.expires ? ` · expires ${value.expires}` : ''}` :
        `Account status: ${value.status}`;
      if (value.premium) {
        const countries = await rr({ action: 'countries' });
        fill(selects.country, countries.countries || [], 'Select country', value.preferredCountryId);
        if (value.preferredCountryId) await loadCountry(value.preferredCountryId,
          value.preferredStateId, value.preferredCountyId);
      }
    } catch (cause) { accountStatus.className = 'ui-notice ui-notice-danger'; accountStatus.textContent = cause.message; }
  }

  accountForm.addEventListener('submit', async (event) => {
    event.preventDefault(); accountStatus.textContent = 'Testing RadioReference connection…';
    try {
      const value = await rr({ action: 'credentials', username: username.value, password: password.value, store: store.checked });
      accountStatus.textContent = value.message; password.value = '';
      if (value.success) await loadAccount();
    } catch (cause) { accountStatus.textContent = cause.message; }
  });
  selects.country.addEventListener('change', () => loadCountry(selects.country.value).catch((cause) => { result.textContent = cause.message; }));
  selects.state.addEventListener('change', () => loadState(selects.state.value).catch((cause) => { result.textContent = cause.message; }));
  selects.county.addEventListener('change', () => loadCounty(selects.county.value).catch((cause) => { result.textContent = cause.message; }));
  selects.system.addEventListener('change', () => void preview());
  selects.aliasList.addEventListener('change', () => { if (selects.system.value) void preview(); });
  selects.category.addEventListener('change', renderTalkgroups); search.addEventListener('input', renderTalkgroups);
  selectAll.addEventListener('click', () => { tableHost.querySelectorAll('input[type="checkbox"]:not(:disabled)').forEach((box) => { box.checked = true; }); selectedCount(); });
  clearAll.addEventListener('click', () => { tableHost.querySelectorAll('input[type="checkbox"]:not(:disabled)').forEach((box) => { box.checked = false; }); selectedCount(); });
  createList.addEventListener('click', async () => {
    if (!newList.value.trim()) return;
    try {
      const value = await rr({ action: 'createAliasList', name: newList.value.trim() });
      fill(selects.aliasList, value.aliasLists || [], 'Select Alias List', value.selected); newList.value = '';
      if (selects.system.value) await preview();
    } catch (cause) { result.textContent = cause.message; }
  });
  importButton.addEventListener('click', async () => {
    const ids = [...tableHost.querySelectorAll('input[type="checkbox"]:checked:not(:disabled)')]
      .map((box) => Number(box.dataset.id));
    if (!selects.aliasList.value) { result.textContent = 'Select or create an Alias List first.'; return; }
    if (!ids.length) { result.textContent = 'Select at least one talkgroup.'; return; }
    result.textContent = 'Importing talkgroups…';
    try {
      const value = await rr({ action: 'import', systemId: Number(selects.system.value),
        aliasList: selects.aliasList.value, talkgroupIds: ids, encryptedMuted: encryptedMuted.checked });
      result.className = 'ui-notice ui-notice-success';
      result.textContent = `Imported ${value.imported} of ${value.selected} selected talkgroups into ${value.aliasList}.`;
      await preview();
    } catch (cause) { result.className = 'ui-notice ui-notice-danger'; result.textContent = cause.message; }
  });
  signal?.addEventListener('abort', () => { stopped = true; }, { once: true });
  if (!stopped) void loadAccount();
  return { element: host, close: () => { stopped = true; } };
}
