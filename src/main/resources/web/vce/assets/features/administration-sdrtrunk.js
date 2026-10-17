async function request(path, method = 'GET', body) {
  const response = await fetch(path, {
    method, credentials: 'same-origin', cache: 'no-store',
    headers: { Accept: 'application/json', ...(body === undefined ? {} : { 'Content-Type': 'application/json' }) },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const value = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(typeof value.error === 'string' ? value.error :
    value.error?.message || `Request failed (${response.status})`);
  return value;
}

function field(node, label, name, value = '', type = 'text') {
  const input = node(type === 'textarea' ? 'textarea' : 'input', type === 'textarea' ? 'ui-textarea' : 'ui-input');
  input.name = name;
  if (type !== 'textarea') input.type = type;
  input.value = value ?? '';
  const host = node('label', 'ui-field');
  host.append(node('span', 'ui-field-label', label), input);
  return { host, input };
}

function toggle(node, label, name, checked = false) {
  const input = node('input'); input.type = 'checkbox'; input.name = name; input.checked = checked === true;
  const host = node('label', 'ui-toggle'); host.append(input, node('span', '', label));
  return { host, input };
}

export function createSdrTrunkAdministration(deps) {
  const { node, signal } = deps;
  const host = node('div', 'data-workspace sdrtrunk-administration');
  const status = node('div');
  const webSection = node('section', 'section ui-section');
  webSection.append(node('h2', '', 'Web Console Access'),
    node('p', 'muted', 'Manage the built-in administrator password, listening interface, port, and web console users.'));
  const passwordForm = node('form', 'admin-form');
  const password = field(node, 'New password', 'password', '', 'password');
  password.input.autocomplete = 'new-password'; password.input.required = true;
  const webActions = node('div', 'ui-action-row');
  const savePassword = node('button', 'ui-button ui-button-primary', 'Change password'); savePassword.type = 'submit';
  const restart = node('button', 'ui-button ui-button-secondary', 'Restart web interface'); restart.type = 'button';
  webActions.append(savePassword, restart); passwordForm.append(password.host, webActions); webSection.append(passwordForm);

  const networkForm = node('form', 'admin-form');
  const networkGrid = node('div', 'ui-settings-grid sdrtrunk-editor-grid');
  const bind = field(node, 'Bind address', 'bind', '127.0.0.1');
  bind.input.placeholder = '127.0.0.1 or 0.0.0.0';
  const port = field(node, 'Port', 'port', '10000', 'number');
  port.input.min = '1'; port.input.max = '65535';
  networkGrid.append(bind.host, port.host);
  const networkState = node('div', 'ui-notice');
  const saveNetwork = node('button', 'ui-button ui-button-primary', 'Save network settings');
  saveNetwork.type = 'submit';
  networkForm.append(node('h3', '', 'Listening Interface'), networkGrid, networkState, saveNetwork);
  webSection.append(networkForm);

  const usersSection = node('section', 'section ui-section');
  usersSection.append(node('h2', '', 'Web Console Users'),
    node('p', 'muted', 'Additional administrators can manage settings. Operators can use scanner, spectrum, and playback features without administration access.'));
  const userForm = node('form', 'admin-form');
  const userSelectHost = node('label', 'ui-field');
  const userSelect = node('select', 'ui-select');
  userSelectHost.append(node('span', 'ui-field-label', 'Account'), userSelect);
  const username = field(node, 'Username', 'username');
  const userPassword = field(node, 'New password', 'userPassword', '', 'password');
  userPassword.input.placeholder = 'Required for new users; blank keeps current password';
  const roleHost = node('label', 'ui-field');
  const role = node('select', 'ui-select'); role.name = 'role';
  role.append(node('option', '', 'Operator'), node('option', '', 'Administrator'));
  role.options[0].value = 'operator'; role.options[1].value = 'admin';
  roleHost.append(node('span', 'ui-field-label', 'Role'), role);
  const enabled = toggle(node, 'Enabled', 'enabled', true);
  const userGrid = node('div', 'ui-settings-grid sdrtrunk-editor-grid');
  userGrid.append(userSelectHost, username.host, userPassword.host, roleHost);
  const userActions = node('div', 'ui-action-row');
  const newUser = node('button', 'ui-button ui-button-secondary', 'New user'); newUser.type = 'button';
  const saveUser = node('button', 'ui-button ui-button-primary', 'Save user'); saveUser.type = 'submit';
  const deleteUser = node('button', 'ui-button ui-button-danger', 'Delete user'); deleteUser.type = 'button';
  userActions.append(newUser, saveUser, deleteUser);
  userForm.append(userGrid, enabled.host, userActions); usersSection.append(userForm);
  let userRecords = [];

  const whisperSection = node('section', 'section ui-section');
  whisperSection.append(node('h2', '', 'Whisper Transcription'),
    node('p', 'muted', 'Whisper is optional. When disabled or unavailable, calls continue normally and no transcription jobs are started.'));
  const whisperForm = node('form', 'admin-form');
  const whisperGrid = node('div', 'ui-settings-grid sdrtrunk-editor-grid');
  const controls = {};
  const definitions = [
    ['Whisper executable path', 'executable', 'text'], ['Model', 'model', 'text'],
    ['Language', 'language', 'text'], ['Task (transcribe or translate)', 'task', 'text'],
    ['Timeout seconds', 'timeoutSeconds', 'number'], ['Map city / region', 'city', 'text'],
    ['Scanner vocabulary prompt', 'prompt', 'textarea']
  ];
  definitions.forEach(([label, name, type]) => {
    const created = field(node, label, name, '', type); whisperGrid.append(created.host); controls[name] = created.input;
  });
  const whisperToggles = node('div', 'ui-action-row');
  [['Enable background transcription', 'enabled'], ['Normalize scanner numbers', 'normalize'],
    ['Redact personally identifiable information', 'redact']].forEach(([label, name]) => {
    const created = toggle(node, label, name); whisperToggles.append(created.host); controls[name] = created.input;
  });
  const whisperState = node('div', 'ui-notice');
  const saveWhisper = node('button', 'ui-button ui-button-primary', 'Save Whisper settings'); saveWhisper.type = 'submit';
  whisperForm.append(whisperGrid, whisperToggles, whisperState, saveWhisper); whisperSection.append(whisperForm);

  const systemSection = node('section', 'section ui-section');
  systemSection.append(node('h2', '', 'Host Status'));
  const systemFacts = node('dl', 'ui-facts'); systemSection.append(systemFacts);
  host.append(status, webSection, usersSection, whisperSection, systemSection);

  function show(message, danger = false) {
    status.className = message ? `ui-notice ${danger ? 'ui-notice-danger' : 'ui-notice-success'}` : '';
    status.textContent = message || '';
  }

  async function load() {
    try {
      const [whisper, web, users, system] = await Promise.all([
        request('/api/v1/whisper-settings'), request('/api/v1/web-settings'), request('/api/v1/users'), request('/api/v1/status')
      ]);
      bind.input.value = web.savedBind ?? web.bind ?? '127.0.0.1';
      port.input.value = web.savedPort ?? web.port ?? 10000;
      networkState.className = `ui-notice ${web.remoteAccess ? 'ui-notice-success' : 'ui-notice-warning'}`;
      networkState.textContent = `${web.running ? 'Listening' : 'Stopped'} at ${web.actualBind || web.bind}:${web.actualPort || web.port}` +
        (web.remoteAccess ? ' · available through network interfaces' : ' · local access only') +
        (web.bindOverridden || web.portOverridden ? ' · JVM startup properties override saved values' : '');
      userRecords = Array.isArray(users.users) ? users.users : [];
      renderUsers();
      definitions.forEach(([, name]) => { controls[name].value = whisper[name] ?? ''; });
      ['enabled', 'normalize', 'redact'].forEach((name) => { controls[name].checked = whisper[name] === true; });
      whisperState.className = `ui-notice ${whisper.enabled && !whisper.available ? 'ui-notice-warning' : ''}`;
      whisperState.textContent = !whisper.enabled ? 'Whisper is disabled.' : whisper.available ?
        `${whisper.status || 'Ready'} · ${whisper.resolvedExecutable || whisper.executable}` :
        `Unavailable · ${whisper.configurationError || 'check the executable path'}`;
      systemFacts.replaceChildren();
      [['Web service', `${web.running ? 'Running' : 'Stopped'} · ${web.bind}:${web.port}`],
        ['Operating system', system.operatingSystem || 'Unavailable'], ['Processors', system.processors],
        ['Host CPU', system.cpuAvailable ? `${(Number(system.cpu) * 100).toFixed(1)}%` : 'Unavailable'],
        ['Host memory', system.memoryAvailable ? `${(Number(system.memoryUsed) / 1_048_576).toFixed(1)} / ${(Number(system.memoryMaximum) / 1_048_576).toFixed(1)} MB` : 'Unavailable']]
        .forEach(([label, value]) => {
          const fact = node('div', 'ui-fact'); fact.append(node('dt', '', label), node('dd', '', String(value ?? ''))); systemFacts.append(fact);
        });
    } catch (cause) { show(cause.message, true); }
  }

  passwordForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    try {
      await request('/api/v1/web-settings', 'POST', { action: 'password', password: password.input.value });
      password.input.value = '';
      show('Password changed. Sign in again with the new password.');
      window.setTimeout(() => window.location.reload(), 900);
    } catch (cause) { show(cause.message, true); }
  });
  networkForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    try {
      await request('/api/v1/web-settings', 'POST', { action: 'network', bind: bind.input.value, port: Number(port.input.value) });
      show('Network settings saved. Restart the web interface to apply them.');
      await load();
    } catch (cause) { show(cause.message, true); }
  });
  restart.addEventListener('click', async () => {
    try {
      const destination = new URL(window.location.href);
      destination.port = String(Number(port.input.value) || 10000);
      await request('/api/v1/web-settings', 'POST', { action: 'restart' });
      show('Web interface is restarting…');
      window.setTimeout(() => { window.location.href = destination.toString(); }, 1500);
    } catch (cause) { show(cause.message, true); }
  });
  whisperForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const body = {};
    definitions.forEach(([, name, type]) => { body[name] = type === 'number' ? Number(controls[name].value) : controls[name].value; });
    ['enabled', 'normalize', 'redact'].forEach((name) => { body[name] = controls[name].checked; });
    try {
      const saved = await request('/api/v1/whisper-settings', 'POST', body);
      show(saved.enabled && !saved.available ? `Settings saved · ${saved.configurationError}` : 'Whisper settings saved.');
      await load();
    } catch (cause) { show(cause.message, true); }
  });

  function renderUsers(selectedId = '') {
    userSelect.replaceChildren();
    const create = node('option', '', 'Create a new user'); create.value = ''; userSelect.append(create);
    userRecords.forEach((user) => {
      const option = node('option', '', `${user.username} (${user.role}${user.enabled ? '' : ', disabled'})`);
      option.value = user.id; userSelect.append(option);
    });
    userSelect.value = selectedId;
    loadUserEditor();
  }

  function loadUserEditor() {
    const user = userRecords.find((item) => item.id === userSelect.value);
    username.input.value = user?.username || '';
    userPassword.input.value = '';
    role.value = user?.role || 'operator';
    enabled.input.checked = user?.enabled !== false;
    deleteUser.disabled = !user;
  }

  userSelect.addEventListener('change', loadUserEditor);
  newUser.addEventListener('click', () => { userSelect.value = ''; loadUserEditor(); username.input.focus(); });
  userForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    try {
      const result = await request('/api/v1/users', 'POST', { action: 'save', id: userSelect.value,
        username: username.input.value, password: userPassword.input.value, role: role.value,
        enabled: enabled.input.checked });
      show('Web user saved.');
      const response = await request('/api/v1/users'); userRecords = response.users || [];
      renderUsers(result.user?.id || '');
    } catch (cause) { show(cause.message, true); }
  });
  deleteUser.addEventListener('click', async () => {
    if (!userSelect.value || !window.confirm(`Delete ${username.input.value}?`)) return;
    try {
      await request('/api/v1/users', 'POST', { action: 'delete', id: userSelect.value });
      show('Web user deleted.');
      const response = await request('/api/v1/users'); userRecords = response.users || []; renderUsers();
    } catch (cause) { show(cause.message, true); }
  });
  signal?.addEventListener('abort', () => {}, { once: true });
  void load();
  return { element: host, close: () => {} };
}
