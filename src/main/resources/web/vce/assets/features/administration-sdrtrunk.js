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
    node('p', 'muted', 'Change the password or restart the embedded web service. Changing the password signs out every browser session.'));
  const passwordForm = node('form', 'admin-form');
  const password = field(node, 'New password', 'password', '', 'password');
  password.input.autocomplete = 'new-password'; password.input.required = true;
  const webActions = node('div', 'ui-action-row');
  const savePassword = node('button', 'ui-button ui-button-primary', 'Change password'); savePassword.type = 'submit';
  const restart = node('button', 'ui-button ui-button-secondary', 'Restart web interface'); restart.type = 'button';
  webActions.append(savePassword, restart); passwordForm.append(password.host, webActions); webSection.append(passwordForm);

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
  host.append(status, webSection, whisperSection, systemSection);

  function show(message, danger = false) {
    status.className = message ? `ui-notice ${danger ? 'ui-notice-danger' : 'ui-notice-success'}` : '';
    status.textContent = message || '';
  }

  async function load() {
    try {
      const [whisper, web, system] = await Promise.all([
        request('/api/v1/whisper-settings'), request('/api/v1/web-settings'), request('/api/v1/status')
      ]);
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
  restart.addEventListener('click', async () => {
    try {
      await request('/api/v1/web-settings', 'POST', { action: 'restart' });
      show('Web interface is restarting…');
      window.setTimeout(() => window.location.reload(), 1500);
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
  signal?.addEventListener('abort', () => {}, { once: true });
  void load();
  return { element: host, close: () => {} };
}
