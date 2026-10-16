const REFRESH_MILLISECONDS = 3000;

async function legacyRequest(path, method = 'GET', body) {
  const response = await fetch(path, {
    method,
    credentials: 'same-origin',
    cache: 'no-store',
    headers: { Accept: 'application/json', ...(body === undefined ? {} : { 'Content-Type': 'application/json' }) },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const value = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(typeof value.error === 'string' ? value.error :
    value.error?.message || `Request failed (${response.status})`);
  return value;
}

function field(node, label, name, type = 'text', value = '', options = {}) {
  const input = node('input', 'ui-input');
  input.name = name;
  input.type = type;
  input.value = value ?? '';
  if (options.required) input.required = true;
  if (options.minimum !== undefined) input.min = String(options.minimum);
  if (options.placeholder) input.placeholder = options.placeholder;
  if (options.autocomplete) input.autocomplete = options.autocomplete;
  const host = node('label', `ui-field${options.wide ? ' streaming-sdrtrunk-wide' : ''}`);
  host.append(node('span', 'ui-field-label', label), input);
  if (options.help) host.append(node('small', 'ui-field-detail', options.help));
  return { host, input };
}

function toggle(node, label, name, checked = false) {
  const input = node('input');
  input.type = 'checkbox';
  input.name = name;
  input.checked = checked === true;
  const host = node('label', 'ui-toggle streaming-sdrtrunk-toggle');
  host.append(input, node('span', '', label));
  return { host, input };
}

function selectField(node, label, name, choices, selected = '') {
  const input = node('select', 'ui-select');
  input.name = name;
  choices.forEach(([value, text]) => {
    const option = node('option', '', text);
    option.value = value;
    input.append(option);
  });
  input.value = selected || choices[0]?.[0] || '';
  const host = node('label', 'ui-field');
  host.append(node('span', 'ui-field-label', label), input);
  return { host, input };
}

function button(node, label, action, primary = false) {
  const control = node('button', `ui-button ui-button-${primary ? 'primary' : 'secondary'}`, label);
  control.type = 'button';
  if (action) control.addEventListener('click', action);
  return control;
}

function timestamp(value) {
  const number = Number(value);
  return Number.isFinite(number) && number > 0 ? new Date(number).toLocaleString() : 'Never';
}

export function createStreamingWorkspace(deps) {
  const { node, openReadOnlyModal, signal } = deps;
  const host = node('div', 'streaming-page data-workspace');
  const toolbar = node('div', 'streaming-toolbar streaming-page-toolbar ui-catalog-toolbar');
  const summary = node('div', 'streaming-summary');
  const actions = node('div', 'ui-action-row');
  const message = node('div');
  message.setAttribute('role', 'status');
  const standardHost = node('div', 'streaming-destination-grid');
  const remoteHost = node('div', 'streaming-destination-grid');
  const standardSection = node('section', 'section ui-section');
  const remoteSection = node('section', 'section ui-section');
  standardSection.append(node('h2', '', 'Streaming destinations'),
    node('p', 'muted', 'Rdio Scanner and other configured streaming services. Queued Rdio uploads are retained while the destination reconnects.'), standardHost);
  remoteSection.append(node('h2', '', 'Remote Call API destinations'),
    node('p', 'muted', 'Completed MP3 calls, metadata, heartbeats, and optional transcription are delivered to these APIs.'), remoteHost);
  let timer = null;
  let stopped = false;
  let loading = false;
  let remoteRows = [];

  const error = (cause) => {
    message.className = 'ui-notice ui-notice-danger';
    message.textContent = cause?.message || 'The streaming configuration could not be loaded.';
  };

  async function mutate(path, body) {
    message.className = '';
    message.textContent = 'Saving…';
    try {
      await legacyRequest(path, 'POST', body);
      message.textContent = 'Saved.';
      await refresh();
    } catch (cause) {
      error(cause);
    }
  }

  function metric(label, value) {
    const item = node('div', 'ui-metric ui-metric-compact');
    item.append(node('span', 'ui-metric-label', label), node('strong', 'ui-metric-value', String(value ?? 0)));
    return item;
  }

  function drawStandard(rows) {
    if (!rows.length) {
      standardHost.replaceChildren(node('div', 'ui-feedback', 'No streaming destinations configured.'));
      return;
    }
    standardHost.replaceChildren(...rows.map((row) => {
      const card = node('article', 'streaming-destination-card ui-surface');
      const heading = node('header', 'streaming-destination-header');
      const identity = node('div', 'streaming-identity');
      identity.append(node('h3', 'streaming-destination-title', row.name || 'Unnamed destination'),
        node('small', 'muted', `${row.type || 'Streaming'} · ${row.host || 'No URL configured'}`));
      const state = node('span', `ui-status ${row.state === 'CONNECTED' ? 'ui-status-success' : 'ui-status-neutral'}`,
        row.enabled ? row.state || 'Starting' : 'Disabled');
      heading.append(identity, state);
      const metrics = node('div', 'ui-metric-grid ui-metric-grid-4');
      metrics.append(metric('Queued', row.queue), metric('Sent', row.sent), metric('Aged off', row.agedOff),
        metric('Errors', row.errors));
      const controls = node('footer', 'ui-action-row streaming-destination-actions');
      controls.append(button(node, row.enabled ? 'Disable' : 'Enable', () =>
        mutate('/api/v1/broadcasters', { name: row.name, action: row.enabled ? 'disable' : 'enable' })),
      button(node, 'Reconnect', () => mutate('/api/v1/broadcasters', { name: row.name, action: 'restart' })),
      button(node, 'Clone', () => mutate('/api/v1/broadcasters', { name: row.name, action: 'clone' })),
      button(node, 'Edit', () => openStandardEditor(row)));
      card.append(heading, metrics, controls);
      return card;
    }));
  }

  function openStandardEditor(existing = null) {
    const form = node('form', 'admin-form streaming-sdrtrunk-form');
    const fields = node('div', 'ui-settings-grid streaming-sdrtrunk-fields');
    const controls = {};
    const typeChoices = [
      ['RDIOSCANNER_CALL', 'Rdio Scanner'], ['BROADCASTIFY_CALL', 'Broadcastify Calls'],
      ['OPENMHZ', 'OpenMHz'], ['BROADCASTIFY', 'Broadcastify Feed'],
      ['ICECAST_HTTP', 'Icecast HTTP'], ['ICECAST_TCP', 'Icecast TCP'],
      ['SHOUTCAST_V1', 'Shoutcast v1'], ['SHOUTCAST_V2', 'Shoutcast v2']
    ];
    const type = selectField(node, 'Destination type', 'type', typeChoices, existing?.type || 'RDIOSCANNER_CALL');
    type.input.disabled = Boolean(existing);
    controls.type = type.input;
    fields.append(type.host);
    const definitions = [
      ['Name', 'name', 'text', existing?.name, { required: true }],
      ['Server or upload URL', 'host', 'text', existing?.host, { required: true, wide: true }],
      ['Port', 'port', 'number', existing?.port || 80, { minimum: 1, types: 'feeds' }],
      ['API key', 'apiKey', 'password', '', { wide: true, types: 'calls',
        placeholder: existing?.apiKeyConfigured ? 'Leave blank to keep saved key' : 'Enter API key',
        help: 'The complete key is accepted; SDRTrunk does not impose a length limit.' }],
      ['System ID', 'systemId', 'number', existing?.systemId || 0, { minimum: 1, types: 'system-id' }],
      ['OpenMHz system name', 'systemName', 'text', existing?.systemName, { types: 'openmhz' }],
      ['Heartbeat URL', 'heartbeatUrl', 'url', existing?.heartbeatUrl, { wide: true, types: 'rdio',
        help: 'Leave blank to derive /api/heartbeat from the call-upload URL.' }],
      ['Heartbeat interval seconds', 'heartbeatIntervalSeconds', 'number', existing?.heartbeatIntervalSeconds || 60,
        { minimum: 5, types: 'rdio' }],
      ['Maximum queued-call age seconds', 'maximumRecordingAgeSeconds', 'number',
        existing?.maximumRecordingAgeSeconds || 600, { minimum: 1 }],
      ['Delay seconds', 'delaySeconds', 'number', existing?.delaySeconds || 0, { minimum: 0 }],
      ['Password', 'password', 'password', '', { types: 'feeds',
        placeholder: existing?.passwordConfigured ? 'Leave blank to keep saved password' : 'Enter password' }],
      ['Username', 'userName', 'text', existing?.userName || 'source', { types: 'icecast' }],
      ['Mount point', 'mountPoint', 'text', existing?.mountPoint || '/stream', { types: 'icecast' }],
      ['Feed ID', 'feedId', 'number', existing?.feedId || 0, { minimum: 0, types: 'broadcastify-feed' }],
      ['Stream ID', 'streamId', 'number', existing?.streamId || 1, { minimum: 0, types: 'shoutcast2' }],
      ['User ID', 'userId', 'text', existing?.userId, { types: 'shoutcast2' }],
      ['Description', 'description', 'text', existing?.description, { types: 'metadata' }],
      ['Genre', 'genre', 'text', existing?.genre, { types: 'metadata' }],
      ['Public site URL', 'siteUrl', 'url', existing?.siteUrl, { wide: true, types: 'site-url' }],
      ['Channels', 'channels', 'number', existing?.channels || 1, { minimum: 1, types: 'audio-stream' }],
      ['Bit rate (kbps)', 'bitRate', 'number', existing?.bitRate || 16, { minimum: 1, types: 'audio-stream' }],
      ['Sample rate', 'sampleRate', 'number', existing?.sampleRate || 8000, { minimum: 1, types: 'icecast' }],
      ['Broadcastify test interval', 'testInterval', 'number', existing?.testInterval || 15,
        { minimum: 1, types: 'broadcastify-call' }]
    ];
    definitions.forEach(([label, name, inputType, value, options]) => {
      const created = field(node, label, name, inputType, value, options || {});
      created.host.dataset.types = options?.types || 'all';
      fields.append(created.host);
      controls[name] = created.input;
    });
    const talkerAliasSource = selectField(node, 'Talker alias sent to Broadcastify', 'talkerAliasSource', [
      ['OVER_THE_AIR', 'Over-the-air talker alias'], ['PLAYLIST', 'Stored playlist alias']
    ], existing?.talkerAliasSource || 'OVER_THE_AIR');
    talkerAliasSource.host.dataset.types = 'broadcastify-call';
    fields.append(talkerAliasSource.host);
    controls.talkerAliasSource = talkerAliasSource.input;
    const toggles = node('div', 'ui-action-row streaming-sdrtrunk-toggles');
    [['Enabled', 'enabled', existing?.enabled ?? false, 'all'],
      ['Send heartbeat', 'heartbeatEnabled', existing?.heartbeatEnabled, 'rdio'],
      ['Broadcastify test calls', 'testEnabled', existing?.testEnabled, 'broadcastify-call'],
      ['Store OTA aliases in playlist', 'storeTalkerAliases', existing?.storeTalkerAliases, 'broadcastify-call'],
      ['Public stream', 'public', existing?.public, 'metadata'],
      ['Inline metadata', 'inline', existing?.inline ?? true, 'icecast'],
      ['Verbose logging', 'verboseLogging', existing?.verboseLogging, 'broadcastify-feed']]
      .forEach(([label, name, checked, types]) => {
        const created = toggle(node, label, name, checked);
        created.host.dataset.types = types;
        controls[name] = created.input;
        toggles.append(created.host);
      });
    const typeGroups = (selected) => {
      const groups = new Set(['all']);
      if (['RDIOSCANNER_CALL', 'BROADCASTIFY_CALL', 'OPENMHZ'].includes(selected)) groups.add('calls');
      if (['RDIOSCANNER_CALL', 'BROADCASTIFY_CALL'].includes(selected)) groups.add('system-id');
      if (selected === 'RDIOSCANNER_CALL') groups.add('rdio');
      if (selected === 'BROADCASTIFY_CALL') groups.add('broadcastify-call');
      if (selected === 'OPENMHZ') groups.add('openmhz');
      if (['BROADCASTIFY', 'ICECAST_HTTP', 'ICECAST_TCP', 'SHOUTCAST_V1', 'SHOUTCAST_V2'].includes(selected)) groups.add('feeds');
      if (['BROADCASTIFY', 'ICECAST_HTTP', 'ICECAST_TCP'].includes(selected)) groups.add('icecast');
      if (selected === 'BROADCASTIFY') groups.add('broadcastify-feed');
      if (selected === 'SHOUTCAST_V2') groups.add('shoutcast2');
      if (['BROADCASTIFY', 'ICECAST_HTTP', 'ICECAST_TCP', 'SHOUTCAST_V1', 'SHOUTCAST_V2'].includes(selected)) {
        groups.add('metadata'); groups.add('audio-stream');
      }
      if (['BROADCASTIFY', 'ICECAST_HTTP', 'ICECAST_TCP', 'SHOUTCAST_V2'].includes(selected)) groups.add('site-url');
      return groups;
    };
    const updateVisibility = () => {
      const groups = typeGroups(controls.type.value);
      form.querySelectorAll('[data-types]').forEach((item) => { item.hidden = !groups.has(item.dataset.types); });
    };
    controls.type.addEventListener('change', updateVisibility);
    const result = node('div');
    result.setAttribute('role', 'status');
    const cancel = button(node, 'Cancel');
    const remove = button(node, 'Delete');
    remove.classList.add('ui-button-danger');
    const save = node('button', 'ui-button ui-button-primary', existing ? 'Save and reconnect' : 'Add destination');
    save.type = 'submit';
    const footer = node('footer', 'ui-modal-footer ui-action-row');
    if (existing) footer.append(remove);
    footer.append(cancel, save);
    form.append(fields, toggles, result, footer);
    const modal = openReadOnlyModal(existing ? 'Edit streaming destination' : 'Add streaming destination', form,
      { id: 'sdrtrunk-native-stream-editor', className: 'streaming-editor-modal modal-size-large' });
    if (!modal) return;
    updateVisibility();
    cancel.addEventListener('click', () => modal.close());
    remove.addEventListener('click', async () => {
      if (!window.confirm(`Delete ${existing.name}? Alias routing to this destination will also be removed.`)) return;
      modal.setBusy(true);
      try {
        await legacyRequest('/api/v1/broadcasters', 'POST',
          { action: 'delete', originalName: existing.name, name: existing.name });
        modal.setBusy(false); modal.close(); await refresh();
      } catch (cause) { modal.setBusy(false); result.className = 'ui-notice ui-notice-danger'; result.textContent = cause.message; }
    });
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      modal.setBusy(true);
      const body = { action: existing ? 'update' : 'create', originalName: existing?.name || '', type: controls.type.value };
      definitions.forEach(([, name, inputType]) => {
        body[name] = inputType === 'number' ? Number(controls[name].value) : controls[name].value;
      });
      body.talkerAliasSource = controls.talkerAliasSource.value;
      ['enabled', 'heartbeatEnabled', 'testEnabled', 'storeTalkerAliases', 'public', 'inline', 'verboseLogging']
        .forEach((name) => { body[name] = controls[name].checked; });
      try {
        await legacyRequest('/api/v1/broadcasters', 'POST', body);
        modal.setBusy(false); modal.close(); await refresh();
      } catch (cause) { modal.setBusy(false); result.className = 'ui-notice ui-notice-danger'; result.textContent = cause.message; }
    });
  }

  function drawRemote(rows) {
    remoteRows = rows;
    if (!rows.length) {
      remoteHost.replaceChildren(node('div', 'ui-feedback', 'No Remote Call API destinations configured.'));
      return;
    }
    remoteHost.replaceChildren(...rows.map((row) => {
      const card = node('article', 'streaming-destination-card ui-surface');
      const heading = node('header', 'streaming-destination-header');
      const identity = node('div', 'streaming-identity');
      identity.append(node('h3', 'streaming-destination-title', row.name || 'Unnamed destination'),
        node('small', 'muted', row.url || 'No POST URL configured'));
      heading.append(identity, node('span', `ui-status ${String(row.state).toLowerCase().includes('connect') ?
        'ui-status-success' : 'ui-status-neutral'}`, row.state || 'Starting'));
      const metrics = node('div', 'ui-metric-grid ui-metric-grid-4');
      metrics.append(metric('Queued calls', row.queuedCalls), metric('Active uploads', row.activeUploads),
        metric('Reconnects', row.reconnectCount), metric('Failures', row.consecutiveFailures));
      const facts = node('dl', 'ui-facts streaming-sdrtrunk-facts');
      [['Heartbeat', row.heartbeatEnabled ? `Every ${row.heartbeatIntervalSeconds}s` : 'Off'],
        ['Last heartbeat', timestamp(row.lastHeartbeatSuccess)], ['Last call upload', timestamp(row.lastCallSuccess)],
        ['Transcription', row.openAiEnabled ? (row.translateToEnglish ? 'OpenAI translate' : 'OpenAI transcribe') :
          row.localWhisperExecutable ? 'Local Whisper' : 'Off']].forEach(([label, value]) => {
        const fact = node('div', 'ui-fact');
        fact.append(node('dt', '', label), node('dd', '', value));
        facts.append(fact);
      });
      if (row.lastHeartbeatError || row.lastCallError) {
        card.append(node('div', 'ui-notice ui-notice-danger', row.lastHeartbeatError || row.lastCallError));
      }
      const controls = node('footer', 'ui-action-row streaming-destination-actions');
      controls.append(button(node, 'Reconnect now', () => mutate('/api/v1/remote-destinations',
        { action: 'reconnect', originalName: row.name })), button(node, 'Edit', () => openRemoteEditor(row)));
      card.append(heading, metrics, facts, controls);
      return card;
    }));
  }

  function openRemoteEditor(existing = null) {
    const form = node('form', 'admin-form streaming-sdrtrunk-form');
    const fields = node('div', 'ui-settings-grid streaming-sdrtrunk-fields');
    const defaults = existing || { enabled: true, retryIndefinitely: true, heartbeatIntervalSeconds: 60,
      maximumRetries: 5, maximumConcurrentUploads: 2, requestTimeoutSeconds: 60,
      maximumRecordingAgeSeconds: 600, apiKeyEnvironmentVariable: 'SDRTRUNK_REMOTE_API_KEY',
      authenticationHeader: 'Authorization', authenticationPrefix: 'Bearer ',
      openAiKeyEnvironmentVariable: 'OPENAI_API_KEY' };
    const definitions = [
      ['Name', 'name', 'text', defaults.name, { required: true }],
      ['Completed-call POST URL', 'url', 'url', defaults.url, { required: true, wide: true }],
      ['API key', 'apiKey', 'password', '', { wide: true, autocomplete: 'new-password',
        placeholder: defaults.apiKeyConfigured ? 'Leave blank to keep saved key' : 'Enter API key',
        help: 'Keys are not length-limited. The environment variable below takes priority.' }],
      ['API key environment variable', 'apiKeyEnvironmentVariable', 'text', defaults.apiKeyEnvironmentVariable],
      ['Authentication header', 'authenticationHeader', 'text', defaults.authenticationHeader],
      ['Authentication prefix', 'authenticationPrefix', 'text', defaults.authenticationPrefix],
      ['Fast retries', 'maximumRetries', 'number', defaults.maximumRetries, { minimum: 0 }],
      ['Concurrent uploads', 'maximumConcurrentUploads', 'number', defaults.maximumConcurrentUploads, { minimum: 1 }],
      ['Request timeout seconds', 'requestTimeoutSeconds', 'number', defaults.requestTimeoutSeconds, { minimum: 1 }],
      ['Maximum queued-call age seconds', 'maximumRecordingAgeSeconds', 'number', defaults.maximumRecordingAgeSeconds, { minimum: 1 }],
      ['Heartbeat interval seconds', 'heartbeatIntervalSeconds', 'number', defaults.heartbeatIntervalSeconds, { minimum: 5 }],
      ['OpenAI key environment variable', 'openAiKeyEnvironmentVariable', 'text', defaults.openAiKeyEnvironmentVariable],
      ['Local Whisper executable', 'localWhisperExecutable', 'text', defaults.localWhisperExecutable, { wide: true }],
      ['Local Whisper model', 'localWhisperModel', 'text', defaults.localWhisperModel]
    ];
    const controls = {};
    definitions.forEach(([label, name, type, value, options]) => {
      const created = field(node, label, name, type, value, options || {});
      fields.append(created.host);
      controls[name] = created.input;
    });
    const toggles = node('div', 'ui-action-row streaming-sdrtrunk-toggles');
    [['Enabled', 'enabled'], ['Send heartbeat', 'heartbeatEnabled'],
      ['Keep retrying queued calls', 'retryIndefinitely'], ['OpenAI Whisper', 'openAiEnabled'],
      ['Translate to English', 'translateToEnglish']].forEach(([label, name]) => {
      const created = toggle(node, label, name, defaults[name]);
      controls[name] = created.input;
      toggles.append(created.host);
    });
    const result = node('div');
    result.setAttribute('role', 'status');
    const cancel = button(node, 'Cancel');
    const remove = button(node, 'Delete');
    remove.classList.add('ui-button-danger');
    const save = node('button', 'ui-button ui-button-primary', existing ? 'Save and reconnect' : 'Add destination');
    save.type = 'submit';
    const footer = node('footer', 'ui-modal-footer ui-action-row');
    if (existing) footer.append(remove);
    footer.append(cancel, save);
    form.append(fields, toggles, node('p', 'muted',
      'Heartbeats use an authenticated JSON POST. Queued calls are retained across reconnects and service restarts when continuous retry is enabled.'),
    result, footer);
    const modal = openReadOnlyModal(existing ? 'Edit Remote Call API destination' : 'Add Remote Call API destination',
      form, { id: 'sdrtrunk-remote-api-editor', className: 'streaming-editor-modal' });
    if (!modal) return;
    cancel.addEventListener('click', () => modal.close());
    remove.addEventListener('click', async () => {
      if (!window.confirm(`Delete ${existing.name}?`)) return;
      modal.setBusy(true);
      try {
        await legacyRequest('/api/v1/remote-destinations', 'POST',
          { action: 'delete', originalName: existing.name });
        modal.setBusy(false);
        modal.close();
        await refresh();
      } catch (cause) {
        modal.setBusy(false);
        result.className = 'ui-notice ui-notice-danger';
        result.textContent = cause.message;
      }
    });
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      modal.setBusy(true);
      const body = { action: existing ? 'update' : 'create', originalName: existing?.name || '' };
      definitions.forEach(([, name, type]) => {
        body[name] = type === 'number' ? Number(controls[name].value) : controls[name].value;
      });
      ['enabled', 'heartbeatEnabled', 'retryIndefinitely', 'openAiEnabled', 'translateToEnglish']
        .forEach((name) => { body[name] = controls[name].checked; });
      try {
        await legacyRequest('/api/v1/remote-destinations', 'POST', body);
        modal.setBusy(false);
        modal.close();
        await refresh();
      } catch (cause) {
        modal.setBusy(false);
        result.className = 'ui-notice ui-notice-danger';
        result.textContent = cause.message;
      }
    });
  }

  async function refresh() {
    if (stopped || loading) return;
    loading = true;
    try {
      const [standard, remote] = await Promise.all([
        legacyRequest('/api/v1/broadcasters'), legacyRequest('/api/v1/remote-destinations')
      ]);
      drawStandard(Array.isArray(standard) ? standard : []);
      drawRemote(Array.isArray(remote) ? remote : []);
      const connected = standard.filter((row) => row.enabled && row.state === 'CONNECTED').length;
      summary.textContent = `${standard.length} streaming · ${remote.length} remote API · ${connected} connected`;
      message.className = '';
      if (message.textContent !== 'Saved.') message.textContent = '';
    } catch (cause) {
      error(cause);
    } finally {
      loading = false;
    }
  }

  async function poll() {
    if (!document.hidden) await refresh();
    if (!stopped) timer = window.setTimeout(poll, REFRESH_MILLISECONDS);
  }

  actions.append(button(node, 'Refresh', refresh),
    button(node, 'Add Streaming Destination', () => openStandardEditor()),
    button(node, 'Add Remote Call API', () => openRemoteEditor(), true));
  toolbar.append(summary, actions);
  host.append(toolbar, message, standardSection, remoteSection);
  signal?.addEventListener('abort', () => { stopped = true; window.clearTimeout(timer); }, { once: true });
  void poll();
  return { element: host, dispose: () => { stopped = true; window.clearTimeout(timer); }, refresh };
}
