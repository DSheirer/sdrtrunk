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

function button(node, label, action, primary = false) {
  const control = node('button', `ui-button ui-button-${primary ? 'primary' : 'secondary'}`, label);
  control.type = 'button';
  if (action) control.addEventListener('click', action);
  return control;
}

function inputField(node, label, name, value = '', type = 'text') {
  const input = node('input', 'ui-input');
  input.name = name;
  input.type = type;
  input.value = value ?? '';
  const host = node('label', 'ui-field');
  host.append(node('span', 'ui-field-label', label), input);
  return { host, input };
}

function selectField(node, label, name, choices, selected = '') {
  const select = node('select', 'ui-select');
  select.name = name;
  choices.forEach((choice) => {
    const value = typeof choice === 'string' ? choice : choice.value;
    const text = typeof choice === 'string' ? choice : choice.label;
    const option = node('option', '', text);
    option.value = value;
    select.append(option);
  });
  select.value = selected ?? '';
  const host = node('label', 'ui-field');
  host.append(node('span', 'ui-field-label', label), select);
  return { host, input: select };
}

function toggle(node, label, name, checked = false) {
  const input = node('input');
  input.type = 'checkbox';
  input.name = name;
  input.checked = checked === true;
  const host = node('label', 'ui-toggle');
  host.append(input, node('span', '', label));
  return { host, input };
}

function mhz(value) {
  const frequency = Number(value);
  return Number.isFinite(frequency) && frequency > 0 ? `${(frequency / 1_000_000).toFixed(5)} MHz` : '';
}

export function createSdrTrunkChannelWorkspace(deps) {
  const { node, openReadOnlyModal, signal, page = 'channels' } = deps;
  const host = node('div', 'data-workspace sdrtrunk-channel-workspace');
  const status = node('div');
  status.setAttribute('role', 'status');
  const channelSection = node('section', 'section ui-section');
  const channelHeader = node('header', 'ui-section-heading sdrtrunk-section-heading');
  const channelActions = node('div', 'ui-action-row');
  channelHeader.append(node('div', '', ''), channelActions);
  channelHeader.firstChild.append(node('h2', '', 'Playlist Channels'),
    node('p', 'muted', 'Start, stop, add, and edit channels without deleting and recreating them.'));
  const channelBody = node('div', 'ui-table-scroll');
  channelSection.append(channelHeader, channelBody);
  const aliasSection = node('section', 'section ui-section');
  const aliasHeader = node('header', 'ui-section-heading sdrtrunk-section-heading');
  const aliasActions = node('div', 'ui-action-row');
  aliasHeader.append(node('div'), aliasActions);
  aliasHeader.firstChild.append(node('h2', '', 'Talkgroups and Aliases'),
    node('p', 'muted', 'Edit talkgroups, CTCSS identifiers, recording, and streaming destination routing.'));
  const aliasBody = node('div', 'ui-table-scroll');
  aliasSection.append(aliasHeader, aliasBody);
  host.append(status, page === 'aliases' ? aliasSection : channelSection);
  const channelSearch = node('input', 'ui-input sdrtrunk-table-search');
  channelSearch.type = 'search';
  channelSearch.placeholder = 'Filter channels';
  channelSearch.setAttribute('aria-label', 'Filter playlist channels');
  const aliasSearch = node('input', 'ui-input sdrtrunk-table-search');
  aliasSearch.type = 'search';
  aliasSearch.placeholder = 'Filter talkgroups and aliases';
  aliasSearch.setAttribute('aria-label', 'Filter talkgroups and aliases');
  let channels = [];
  let aliases = [];
  let destinations = [];
  let options = { nbfmBandwidths: [], nbfmDeemphasis: [], ctcss: [], dcs: [], preferredTuners: [] };
  let stopped = false;

  function setStatus(message, danger = false) {
    status.className = message ? `ui-notice ${danger ? 'ui-notice-danger' : 'ui-notice-success'}` : '';
    status.textContent = message || '';
  }

  function table(headers, rows) {
    const table = node('table', 'ui-table sdrtrunk-data-table');
    const thead = node('thead');
    const tr = node('tr');
    headers.forEach((header) => tr.append(node('th', '', header)));
    thead.append(tr);
    const tbody = node('tbody');
    rows.forEach((cells) => {
      const row = node('tr');
      cells.forEach((cell, index) => {
        const td = node('td');
        td.dataset.label = headers[index] || '';
        if (cell instanceof Node) td.append(cell); else td.textContent = String(cell ?? '');
        row.append(td);
      });
      tbody.append(row);
    });
    table.append(thead, tbody);
    return table;
  }

  function draw() {
    const channelTerm = channelSearch.value.trim().toLocaleLowerCase();
    const visibleChannels = channels.filter((channel) => !channelTerm ||
      [channel.system, channel.site, channel.name, channel.decoder, mhz(channel.frequency)]
        .some((value) => String(value || '').toLocaleLowerCase().includes(channelTerm)));
    channelBody.replaceChildren(table(['System', 'Site', 'Name', 'Protocol', 'Frequency', 'State', 'Actions'],
      visibleChannels.map((channel) => {
        const actions = node('div', 'ui-action-row');
        actions.append(button(node, channel.processing ? 'Stop' : 'Start', async () => {
          try {
            await legacyRequest('/api/v1/channel-control', 'POST',
              { name: channel.name, action: channel.processing ? 'stop' : 'start' });
            await refresh();
          } catch (cause) { setStatus(cause.message, true); }
        }));
        if (channel.type === 'STANDARD') actions.append(button(node, 'Edit', () => openChannel(channel)));
        return [channel.system, channel.site, channel.name, channel.decoder, mhz(channel.frequency),
          channel.processing ? 'Active' : 'Stopped', actions];
      })));
    channelSection.querySelector('h2').textContent = `Playlist Channels (${visibleChannels.length})`;
    const aliasTerm = aliasSearch.value.trim().toLocaleLowerCase();
    const visibleAliases = aliases.filter((alias) => !aliasTerm ||
      [alias.aliasList, alias.talkgroup, alias.name, alias.group, alias.protocol, alias.ctcssLabel,
        ...(alias.remoteCalls || [])].some((value) => String(value || '').toLocaleLowerCase().includes(aliasTerm)));
    aliasBody.replaceChildren(table(['Alias List', 'Talkgroup', 'Name', 'Group', 'Protocol', 'CTCSS', 'Record', 'Destinations', 'Actions'],
      visibleAliases.map((alias) => {
        const actions = node('div', 'ui-action-row');
        actions.append(button(node, 'Edit', () => openAlias(alias)));
        return [alias.aliasList, alias.talkgroup, alias.name, alias.group, alias.protocol, alias.ctcssLabel || '',
          alias.record ? 'Yes' : 'No', (alias.remoteCalls || []).join(', '), actions];
      })));
    aliasSection.querySelector('h2').textContent = `Talkgroups and Aliases (${visibleAliases.length})`;
  }

  async function refresh() {
    if (stopped) return;
    try {
      const [channelRows, aliasRows, standard, remote, optionRows] = await Promise.all([
        legacyRequest('/api/v1/channels'), legacyRequest('/api/v1/talkgroups'),
        legacyRequest('/api/v1/broadcasters'), legacyRequest('/api/v1/remote-destinations'),
        legacyRequest('/api/v1/channel-options')
      ]);
      channels = channelRows;
      aliases = aliasRows;
      destinations = [...standard, ...remote].sort((a, b) => String(a.name).localeCompare(String(b.name)));
      options = optionRows;
      draw();
      setStatus('');
    } catch (cause) {
      setStatus(cause.message, true);
    }
  }

  function openChannel(existing = null) {
    const form = node('form', 'admin-form');
    const controls = {};
    const section = (legend) => {
      const host = node('fieldset', 'ui-fieldset');
      const grid = node('div', 'ui-settings-grid sdrtrunk-editor-grid');
      host.append(node('legend', '', legend), grid);
      return { host, grid };
    };
    const add = (grid, created) => { grid.append(created.host); controls[created.input.name] = created.input; };
    const identity = section('Channel');
    add(identity.grid, inputField(node, 'Name', 'name', existing?.name));
    add(identity.grid, inputField(node, 'System', 'system', existing?.system));
    add(identity.grid, inputField(node, 'Site', 'site', existing?.site));
    add(identity.grid, inputField(node, 'Alias List', 'aliasList', existing?.aliasList));
    const source = section('Source');
    add(source.grid, inputField(node, 'Frequency (Hz)', 'frequency', existing?.frequency || '', 'number'));
    const preferredChoices = [{ value: '', label: 'Automatic (any compatible tuner)' },
      ...(options.preferredTuners || []).map((value) => ({ value, label: value }))];
    if (existing?.preferredTuner && !(options.preferredTuners || []).includes(existing.preferredTuner)) {
      preferredChoices.push({ value: existing.preferredTuner, label: existing.preferredTuner });
    }
    add(source.grid, selectField(node, 'Preferred tuner', 'preferredTuner', preferredChoices,
      existing?.preferredTuner || ''));
    const decoder = section('Decoder');
    add(decoder.grid, selectField(node, 'Decoder type', 'decoder',
      ['AM', 'DMR', 'LTR', 'LTR_NET', 'MPT1327', 'NBFM', 'NXDN', 'PASSPORT', 'P25_PHASE1', 'P25_PHASE2'],
      existing?.decoder || 'NBFM'));
    add(decoder.grid, selectField(node, 'Channel type', 'nxdnChannelMode', [
      { value: 'CONVENTIONAL', label: 'Conventional' }, { value: 'TRUNKED', label: 'Trunked' }
    ], existing?.nxdnChannelMode || 'TRUNKED'));
    add(decoder.grid, selectField(node, 'NXDN mode', 'nxdnTransmissionMode', [
      { value: 'M4800', label: '4800 baud' }, { value: 'M9600', label: '9600 baud' },
      { value: 'TYPE_D', label: 'Type-D (4800 baud)' }
    ], existing?.nxdnTransmissionMode || 'M4800'));
    add(decoder.grid, inputField(node, 'Maximum traffic channels', 'trafficChannelPoolSize',
      existing?.trafficChannelPoolSize ?? 20, 'number'));
    controls.trafficChannelPoolSize.min = '0';
    controls.trafficChannelPoolSize.max = '100';
    add(decoder.grid, selectField(node, 'Talker alias encoding', 'nxdnEncoding', [
      { value: 'UTF8', label: 'Latin / English (UTF-8)' }, { value: 'BIG5', label: 'Taiwan / China (BIG5)' }
    ], existing?.nxdnEncoding || 'UTF8'));
    const toggles = node('div', 'ui-action-row sdrtrunk-editor-toggles');
    [['Auto start', 'autoStart', existing?.autoStart],
      ['Ignore data calls', 'ignoreDataCalls', existing?.ignoreDataCalls],
      ['Ignore encrypted calls', 'ignoreEncryptedCalls', existing?.ignoreEncryptedCalls],
      ['Record unknown NXDN simplex radio IDs', 'recordUnknownSimplexCalls', existing?.recordUnknownSimplexCalls],
      ['DCS Monitor / Code Search', 'dcsMonitor', existing?.dcsMonitor]].forEach(([label, name, checked]) => {
      const created = toggle(node, label, name, checked);
      controls[name] = created.input;
      toggles.append(created.host);
    });
    const nbfm = node('fieldset', 'ui-fieldset');
    nbfm.append(node('legend', '', 'NBFM Squelch: Carrier / CTCSS / DCS'));
    const nbfmGrid = node('div', 'ui-settings-grid sdrtrunk-editor-grid');
    const addNbfm = (created) => { nbfmGrid.append(created.host); controls[created.input.name] = created.input; };
    addNbfm(selectField(node, 'Bandwidth', 'nbfmBandwidth', options.nbfmBandwidths || [], existing?.nbfmBandwidth));
    addNbfm(inputField(node, 'Talkgroup to assign', 'nbfmTalkgroup', existing?.nbfmTalkgroup || 1, 'number'));
    addNbfm(selectField(node, 'De-emphasis', 'nbfmDeemphasis', options.nbfmDeemphasis || [], existing?.nbfmDeemphasis));
    addNbfm(selectField(node, 'Squelch type', 'nbfmSquelchType', [
      { value: 'NONE', label: 'Carrier Squelch' }, { value: 'CTCSS', label: 'CTCSS' },
      { value: 'CTCSS_SEARCH', label: 'CTCSS Tone Search' }, { value: 'DCS', label: 'DCS' }
    ], existing?.nbfmSquelchType || 'NONE'));
    addNbfm(selectField(node, 'Squelch code', 'nbfmSquelchValue', [], existing?.nbfmSquelchValue));
    [['High-pass audio filter', 'nbfmAudioFilter', existing?.nbfmAudioFilter ?? true],
      ['Automatic level control', 'nbfmAudioALC', existing?.nbfmAudioALC]].forEach(([label, name, checked]) => {
      const created = toggle(node, label, name, checked);
      controls[name] = created.input;
      nbfmGrid.append(created.host);
    });
    nbfm.append(nbfmGrid);
    const eventLogs = node('fieldset', 'ui-fieldset');
    eventLogs.append(node('legend', '', 'Event logging'));
    const eventLogControls = [];
    [['CALL_EVENT', 'Call events'], ['DECODED_MESSAGE', 'Decoded messages'],
      ['TRAFFIC_CALL_EVENT', 'Traffic channel call events'],
      ['TRAFFIC_DECODED_MESSAGE', 'Traffic channel decoded messages']].forEach(([value, label]) => {
        const created = toggle(node, label, `event-${value}`, (existing?.eventLoggers || []).includes(value));
        eventLogControls.push({ value, input: created.input });
        eventLogs.append(created.host);
      });
    const recordings = node('fieldset', 'ui-fieldset');
    recordings.append(node('legend', '', 'Recording options'));
    const recorderControls = [];
    [['BASEBAND', 'Baseband I/Q (.wav)'], ['DEMODULATED_BIT_STREAM', 'Demodulated bitstream (.bits)'],
      ['MBE_CALL_SEQUENCE', 'MBE audio codec frames (.mbe)'],
      ['TRAFFIC_BASEBAND', 'Traffic channel baseband I/Q (.wav)'],
      ['TRAFFIC_DEMODULATED_BIT_STREAM', 'Traffic channel demodulated bitstream (.bits)'],
      ['TRAFFIC_MBE_CALL_SEQUENCE', 'Traffic channel MBE audio codec frames (.mbe)']]
      .forEach(([value, label]) => {
        const created = toggle(node, label, `recorder-${value}`, (existing?.recorders || []).includes(value));
        recorderControls.push({ value, input: created.input });
        recordings.append(created.host);
      });
    const updateDecoder = () => {
      nbfm.hidden = controls.decoder.value !== 'NBFM';
      const isNxdn = controls.decoder.value === 'NXDN';
      const hasTrafficPool = ['DMR', 'MPT1327', 'NXDN', 'P25_PHASE1', 'P25_PHASE2'].includes(controls.decoder.value);
      const canIgnoreData = ['DMR', 'NXDN', 'P25_PHASE1', 'P25_PHASE2'].includes(controls.decoder.value);
      controls.nxdnChannelMode.closest('.ui-field').hidden = !isNxdn;
      controls.nxdnTransmissionMode.closest('.ui-field').hidden = !isNxdn;
      controls.nxdnEncoding.closest('.ui-field').hidden = !isNxdn;
      controls.trafficChannelPoolSize.closest('.ui-field').hidden = !hasTrafficPool;
      controls.ignoreDataCalls.closest('.ui-toggle').hidden = !canIgnoreData;
      controls.ignoreEncryptedCalls.closest('.ui-toggle').hidden = !isNxdn;
      controls.recordUnknownSimplexCalls.closest('.ui-toggle').hidden = !isNxdn;
      const type = controls.nbfmSquelchType.value;
      const choices = type === 'CTCSS' ? options.ctcss : type === 'DCS' ? options.dcs : [];
      const selected = existing?.nbfmSquelchValue || '';
      controls.nbfmSquelchValue.replaceChildren(...(choices || []).map((choice) => {
        const option = node('option', '', choice.label);
        option.value = choice.value;
        return option;
      }));
      controls.nbfmSquelchValue.value = selected;
      controls.nbfmSquelchValue.closest('.ui-field').hidden = !['CTCSS', 'DCS'].includes(type);
    };
    controls.decoder.addEventListener('change', updateDecoder);
    controls.nbfmSquelchType.addEventListener('change', updateDecoder);
    const result = node('div');
    const cancel = button(node, 'Cancel');
    const remove = button(node, 'Delete');
    remove.classList.add('ui-button-danger');
    const save = node('button', 'ui-button ui-button-primary', existing ? 'Save channel' : 'Add channel');
    save.type = 'submit';
    const footer = node('footer', 'ui-modal-footer ui-action-row');
    if (existing) footer.append(remove);
    footer.append(cancel, save);
    form.append(identity.host, source.host, decoder.host, toggles, nbfm, eventLogs, recordings, result, footer);
    const modal = openReadOnlyModal(existing ? 'Edit Playlist Channel' : 'Add Playlist Channel', form,
      { id: 'sdrtrunk-channel-editor', className: 'modal-size-large' });
    if (!modal) return;
    cancel.addEventListener('click', () => modal.close());
    remove.addEventListener('click', async () => {
      if (!window.confirm(`Delete ${existing.name}?`)) return;
      modal.setBusy(true);
      try {
        await legacyRequest('/api/v1/channels', 'POST', { action: 'delete', id: existing.id });
        modal.setBusy(false); modal.close(); await refresh();
      } catch (cause) { modal.setBusy(false); result.textContent = cause.message; }
    });
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      modal.setBusy(true);
      const body = { action: existing ? 'update' : 'create', id: existing?.id,
        name: controls.name.value, system: controls.system.value, site: controls.site.value,
        frequency: Number(controls.frequency.value), decoder: controls.decoder.value,
        preferredTuner: controls.preferredTuner.value,
        aliasList: controls.aliasList.value, autoStart: controls.autoStart.checked,
        nxdnChannelMode: controls.nxdnChannelMode.value,
        nxdnTransmissionMode: controls.nxdnTransmissionMode.value,
        nxdnEncoding: controls.nxdnEncoding.value,
        trafficChannelPoolSize: Number(controls.trafficChannelPoolSize.value),
        ignoreDataCalls: controls.ignoreDataCalls.checked,
        ignoreEncryptedCalls: controls.ignoreEncryptedCalls.checked,
        recordUnknownSimplexCalls: controls.recordUnknownSimplexCalls.checked,
        dcsMonitor: controls.dcsMonitor.checked,
        eventLoggers: eventLogControls.filter((item) => item.input.checked).map((item) => item.value),
        recorders: recorderControls.filter((item) => item.input.checked).map((item) => item.value) };
      if (controls.decoder.value === 'NBFM') Object.assign(body, {
        nbfmBandwidth: controls.nbfmBandwidth.value, nbfmTalkgroup: Number(controls.nbfmTalkgroup.value),
        nbfmDeemphasis: controls.nbfmDeemphasis.value, nbfmSquelchType: controls.nbfmSquelchType.value,
        nbfmSquelchValue: controls.nbfmSquelchValue.value, nbfmAudioFilter: controls.nbfmAudioFilter.checked,
        nbfmAudioALC: controls.nbfmAudioALC.checked
      });
      try {
        await legacyRequest('/api/v1/channels', 'POST', body);
        modal.setBusy(false); modal.close(); await refresh();
      } catch (cause) { modal.setBusy(false); result.textContent = cause.message; }
    });
    updateDecoder();
  }

  function openAlias(existing = null) {
    const form = node('form', 'admin-form');
    const grid = node('div', 'ui-settings-grid sdrtrunk-editor-grid');
    const controls = {};
    const add = (created) => { grid.append(created.host); controls[created.input.name] = created.input; };
    add(inputField(node, 'Alias List', 'aliasList', existing?.aliasList));
    add(inputField(node, 'Talkgroup ID', 'talkgroup', existing?.talkgroup ?? '', 'number'));
    add(inputField(node, 'Talkgroup name / alias', 'name', existing?.name));
    add(inputField(node, 'Category / group', 'group', existing?.group));
    add(selectField(node, 'Protocol', 'protocol', [
      { value: 'APCO25', label: 'P25' }, 'DMR', 'NXDN', 'LTR', 'LTR_NET', 'MPT1327', 'PASSPORT', 'NBFM', 'AM'
    ], existing?.protocol || 'APCO25'));
    add(selectField(node, 'CTCSS alias identifier', 'ctcss', [
      { value: '', label: 'No CTCSS tone' }, ...(options.ctcss || [])
    ], existing?.ctcss || ''));
    add(inputField(node, 'Playback priority', 'priority', existing?.priority || 100, 'number'));
    const record = toggle(node, 'Record calls', 'record', existing?.record);
    controls.record = record.input;
    const remoteBox = node('fieldset', 'ui-fieldset');
    remoteBox.append(node('legend', '', 'Send calls to streaming destinations'));
    const remoteToggles = node('div', 'ui-action-row');
    const remoteControls = [];
    destinations.forEach((destination) => {
      const created = toggle(node, `${destination.name} · ${destination.typeLabel || destination.type || 'Remote API'}`,
        `destination-${destination.name}`, (existing?.remoteCalls || []).includes(destination.name));
      remoteControls.push({ name: destination.name, input: created.input });
      remoteToggles.append(created.host);
    });
    remoteBox.append(remoteToggles);
    const result = node('div');
    const cancel = button(node, 'Cancel');
    const remove = button(node, 'Delete');
    remove.classList.add('ui-button-danger');
    const save = node('button', 'ui-button ui-button-primary', existing ? 'Save talkgroup' : 'Add talkgroup');
    save.type = 'submit';
    const footer = node('footer', 'ui-modal-footer ui-action-row');
    if (existing) footer.append(remove);
    footer.append(cancel, save);
    form.append(grid, record.host, remoteBox, result, footer);
    const modal = openReadOnlyModal(existing ? 'Edit Talkgroup / Alias' : 'Add Talkgroup / Alias', form,
      { id: 'sdrtrunk-alias-editor', className: 'modal-size-large' });
    if (!modal) return;
    cancel.addEventListener('click', () => modal.close());
    remove.addEventListener('click', async () => {
      if (!window.confirm(`Delete ${existing.name}?`)) return;
      modal.setBusy(true);
      try {
        await legacyRequest('/api/v1/talkgroups', 'POST', { action: 'delete', id: existing.id });
        modal.setBusy(false); modal.close(); await refresh();
      } catch (cause) { modal.setBusy(false); result.textContent = cause.message; }
    });
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      modal.setBusy(true);
      try {
        await legacyRequest('/api/v1/talkgroups', 'POST', {
          action: existing ? 'update' : 'create', id: existing?.id, aliasList: controls.aliasList.value,
          talkgroup: Number(controls.talkgroup.value), name: controls.name.value, group: controls.group.value,
          protocol: controls.protocol.value, ctcss: controls.ctcss.value,
          priority: Number(controls.priority.value), record: controls.record.checked,
          remoteCalls: remoteControls.filter((remote) => remote.input.checked).map((remote) => remote.name)
        });
        modal.setBusy(false); modal.close(); await refresh();
      } catch (cause) { modal.setBusy(false); result.textContent = cause.message; }
    });
  }

  channelSearch.addEventListener('input', draw);
  aliasSearch.addEventListener('input', draw);
  channelActions.append(channelSearch, button(node, 'Refresh', refresh),
    button(node, 'Add Channel', () => openChannel(), true));
  aliasActions.append(aliasSearch, button(node, 'Refresh', refresh),
    button(node, 'Add Talkgroup', () => openAlias(), true));
  signal?.addEventListener('abort', () => { stopped = true; }, { once: true });
  void refresh();
  return { element: host, close: () => { stopped = true; } };
}
