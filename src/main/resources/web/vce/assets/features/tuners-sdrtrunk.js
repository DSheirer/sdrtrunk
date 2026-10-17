async function legacyRequest(path, method = 'GET', body) {
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

function button(node, label, action, primary = false) {
  const control = node('button', `ui-button ui-button-${primary ? 'primary' : 'secondary'}`, label);
  control.type = 'button';
  if (action) control.addEventListener('click', action);
  return control;
}

function frequency(value) {
  const hz = Number(value);
  return Number.isFinite(hz) && hz > 0 ? `${(hz / 1_000_000).toFixed(5)} MHz` : 'Unavailable';
}

function sampleRate(value) {
  const hz = Number(value);
  return Number.isFinite(hz) && hz > 0 ? `${(hz / 1_000_000).toFixed(3)} MSPS` : 'Unavailable';
}

export function createSdrTrunkTuners(deps) {
  const { node, openReadOnlyModal, signal } = deps;
  const host = node('div', 'data-workspace');
  const toolbar = node('div', 'ui-catalog-toolbar');
  const summary = node('div');
  const actions = node('div', 'ui-action-row');
  const message = node('div');
  const cards = node('div', 'streaming-destination-grid');
  toolbar.append(summary, actions);
  host.append(toolbar, message, cards);
  let stopped = false;

  function setError(cause) {
    message.className = 'ui-notice ui-notice-danger';
    message.textContent = cause.message;
  }

  async function mutate(body) {
    try {
      await legacyRequest('/api/v1/tuners', 'POST', body);
      await refresh();
    } catch (cause) { setError(cause); }
  }

  function statusText(tuner) {
    const values = [tuner.status];
    if (tuner.network && tuner.connection) values.push(String(tuner.connection).toLowerCase());
    if (tuner.reconnectAttempt) values.push(`attempt ${tuner.reconnectAttempt}`);
    if (tuner.connectionError && tuner.connection !== 'CONNECTED') values.push(tuner.connectionError);
    return values.filter(Boolean).join(' · ');
  }

  function draw(rows) {
    summary.textContent = `${rows.length} tuner${rows.length === 1 ? '' : 's'} · ` +
      `${rows.filter((row) => row.enabled).length} enabled`;
    if (!rows.length) {
      cards.replaceChildren(node('div', 'ui-feedback', 'No tuners discovered.'));
      return;
    }
    cards.replaceChildren(...rows.map((tuner) => {
      const card = node('article', 'streaming-destination-card ui-surface');
      const header = node('header', 'streaming-destination-header');
      const identity = node('div', 'streaming-identity');
      identity.append(node('h2', 'streaming-destination-title', tuner.name || tuner.id),
        node('small', 'muted', `${tuner.type || tuner.class || 'Tuner'} · ${tuner.id}`));
      header.append(identity, node('span', `ui-status ${tuner.enabled ? 'ui-status-success' : 'ui-status-neutral'}`,
        statusText(tuner)));
      const facts = node('dl', 'ui-facts');
      [['Center frequency', frequency(tuner.frequency)], ['Sample rate', sampleRate(tuner.sampleRate)],
        ['Usable bandwidth', sampleRate(tuner.usableBandwidth)],
        ['Frequency correction', tuner.frequencyCorrection == null ? 'Unavailable' : `${tuner.frequencyCorrection} PPM`],
        ['Active channels', String(tuner.channelCount || 0)],
        ['Configuration lock', tuner.sampleRateLocked ? 'Locked while channels are active' : 'Available'],
        ...(tuner.rtl2832 ? [['Bias-T', tuner.biasT ? 'On' : 'Off'],
          ['Master gain', (tuner.masterGains || []).find((gain) => gain.value === tuner.masterGain)?.label ||
            tuner.masterGain || 'Device default']] : []),
        ...(tuner.network ? [['Network address', `${tuner.host || 'localhost'}:${tuner.port || 1234}`],
          ['Gain', tuner.gainAuto ? 'Automatic' : `${Number(tuner.gain || 0) / 10} dB`],
          ['Bias-T', tuner.biasT ? 'On' : 'Off'], ['Auto-reconnect', tuner.autoReconnect ? 'On' : 'Off']] : [])]
        .forEach(([label, value]) => {
          const fact = node('div', 'ui-fact');
          fact.append(node('dt', '', label), node('dd', '', value));
          facts.append(fact);
        });
      const controls = node('footer', 'ui-action-row');
      controls.append(button(node, tuner.enabled ? 'Disable' : 'Enable', () =>
        mutate({ id: tuner.id, action: tuner.enabled ? 'disable' : 'enable' })));
      controls.append(button(node, 'Log State', () => mutate({ id: tuner.id, action: 'log' })));
      if (tuner.rtl2832) controls.append(button(node, 'Edit RTL-SDR', () => localTunerEditor(tuner)));
      if (tuner.network) controls.append(button(node, 'Edit', () => networkTunerEditor(tuner)),
        button(node, 'Reconnect', () => mutate({ id: tuner.id, action: 'restart' })), button(node, 'Delete', () => {
        if (window.confirm(`Delete network tuner ${tuner.id}?`)) void mutate({ id: tuner.id, action: 'delete' });
      }));
      card.append(header, facts, controls);
      return card;
    }));
  }

  async function refresh() {
    if (stopped) return;
    try {
      const rows = await legacyRequest('/api/v1/tuners');
      draw(Array.isArray(rows) ? rows : []);
      message.className = '';
      message.textContent = '';
    } catch (cause) { setError(cause); }
  }

  function networkTunerEditor(existing = null) {
    const form = node('form', 'admin-form');
    const grid = node('div', 'ui-settings-grid sdrtrunk-editor-grid');
    const values = [
      ['Host or IP address', 'host', existing?.host || 'localhost', 'text'],
      ['Port', 'port', existing?.port || 1234, 'number'],
      ['Center frequency (Hz)', 'frequency', existing?.frequency || 100000000, 'number'],
      ['Sample rate (samples/second)', 'sampleRate', existing?.sampleRate || 2048000, 'number'],
      ['Manual gain (tenths of dB)', 'gain', existing?.gain ?? 0, 'number']
    ];
    const controls = {};
    values.forEach(([label, name, value, type]) => {
      const input = node('input', 'ui-input');
      input.name = name; input.type = type; input.value = String(value);
      const field = node('label', 'ui-field');
      field.append(node('span', 'ui-field-label', label), input);
      grid.append(field); controls[name] = input;
    });
    const gain = node('input'); gain.type = 'checkbox'; gain.checked = existing?.gainAuto ?? true;
    const gainLabel = node('label', 'ui-toggle'); gainLabel.append(gain, node('span', '', 'Automatic gain'));
    controls.gain.disabled = gain.checked;
    gain.addEventListener('change', () => { controls.gain.disabled = gain.checked; });
    const bias = node('input'); bias.type = 'checkbox';
    bias.checked = existing?.biasT ?? false;
    const biasLabel = node('label', 'ui-toggle'); biasLabel.append(bias, node('span', '', 'Bias-T'));
    const reconnect = node('input'); reconnect.type = 'checkbox'; reconnect.checked = existing?.autoReconnect ?? true;
    const reconnectLabel = node('label', 'ui-toggle');
    reconnectLabel.append(reconnect, node('span', '', 'Always reconnect after a network failure'));
    const result = node('div');
    const cancel = button(node, 'Cancel');
    const save = node('button', 'ui-button ui-button-primary', existing ? 'Save changes' : 'Add network tuner');
    save.type = 'submit';
    const footer = node('footer', 'ui-modal-footer ui-action-row'); footer.append(cancel, save);
    if (existing?.channelCount) form.append(node('div', 'ui-notice ui-notice-warning',
      `${existing.channelCount} channel(s) are active. Gain, Bias-T and reconnect may be changed now; ` +
      'stop the channels before changing the address, frequency, or sample rate.'));
    form.append(grid, gainLabel, biasLabel, reconnectLabel, result, footer);
    const modal = openReadOnlyModal(existing ? 'Edit rtl_tcp Network Tuner' : 'Add rtl_tcp Network Tuner', form,
      { id: 'sdrtrunk-network-tuner', className: 'modal-size-medium' });
    if (!modal) return;
    cancel.addEventListener('click', () => modal.close());
    form.addEventListener('submit', async (event) => {
      event.preventDefault(); modal.setBusy(true);
      try {
        await legacyRequest('/api/v1/tuners', 'POST', { action: existing ? 'update' : 'add',
          ...(existing ? { id: existing.id } : {}), host: controls.host.value.trim(),
          port: Number(controls.port.value), frequency: Number(controls.frequency.value),
          sampleRate: Number(controls.sampleRate.value), gainAuto: gain.checked, biasT: bias.checked,
          gain: Number(controls.gain.value), autoReconnect: reconnect.checked });
        modal.setBusy(false); modal.close(); await refresh();
      } catch (cause) { modal.setBusy(false); result.textContent = cause.message; }
    });
  }

  function localTunerEditor(tuner) {
    const form = node('form', 'admin-form');
    const grid = node('div', 'ui-settings-grid sdrtrunk-editor-grid');
    const frequencyInput = node('input', 'ui-input');
    frequencyInput.type = 'number'; frequencyInput.value = String(tuner.frequency || 100000000);
    frequencyInput.disabled = Boolean(tuner.channelCount);
    const frequencyField = node('label', 'ui-field');
    frequencyField.append(node('span', 'ui-field-label', 'Center frequency (Hz)'), frequencyInput);
    const correction = node('input', 'ui-input');
    correction.type = 'number'; correction.step = '0.1'; correction.value = String(tuner.frequencyCorrection || 0);
    const correctionField = node('label', 'ui-field');
    correctionField.append(node('span', 'ui-field-label', 'Frequency correction (PPM)'), correction);
    const rate = node('select', 'ui-select');
    (tuner.sampleRates || []).forEach((choice) => {
      const option = node('option', '', choice.label); option.value = String(choice.value);
      option.selected = Number(choice.value) === Math.round(Number(tuner.sampleRate)); rate.append(option);
    });
    rate.disabled = Boolean(tuner.channelCount);
    const rateField = node('label', 'ui-field'); rateField.append(node('span', 'ui-field-label', 'Sample rate'), rate);
    grid.append(frequencyField, correctionField, rateField);
    const bias = node('input'); bias.type = 'checkbox'; bias.checked = Boolean(tuner.biasT);
    const biasLabel = node('label', 'ui-toggle'); biasLabel.append(bias, node('span', '', 'Bias-T'));
    let gain = null;
    if ((tuner.masterGains || []).length) {
      gain = node('select', 'ui-select');
      tuner.masterGains.forEach((choice) => {
        const option = node('option', '', choice.label); option.value = choice.value;
        option.selected = choice.value === tuner.masterGain; gain.append(option);
      });
      const gainField = node('label', 'ui-field'); gainField.append(node('span', 'ui-field-label', 'Master gain'), gain);
      grid.append(gainField);
    }
    const result = node('div');
    if (tuner.channelCount) form.append(node('div', 'ui-notice ui-notice-warning',
      `${tuner.channelCount} channel(s) are active. Center frequency and sample rate are locked until they stop.`));
    const cancel = button(node, 'Cancel');
    const save = node('button', 'ui-button ui-button-primary', 'Save RTL-SDR settings'); save.type = 'submit';
    const footer = node('footer', 'ui-modal-footer ui-action-row'); footer.append(cancel, save);
    form.append(grid, biasLabel, result, footer);
    const modal = openReadOnlyModal('Edit Attached RTL-SDR', form,
      { id: 'sdrtrunk-local-rtl-tuner', className: 'modal-size-medium' });
    if (!modal) return;
    cancel.addEventListener('click', () => modal.close());
    form.addEventListener('submit', async (event) => {
      event.preventDefault(); modal.setBusy(true);
      try {
        await legacyRequest('/api/v1/tuners', 'POST', { action: 'configure', id: tuner.id,
          frequency: Number(frequencyInput.value), frequencyCorrection: Number(correction.value),
          sampleRate: Number(rate.value), biasT: bias.checked,
          ...(gain ? { masterGain: gain.value } : {}) });
        modal.setBusy(false); modal.close(); await refresh();
      } catch (cause) { modal.setBusy(false); result.textContent = cause.message; }
    });
  }

  actions.append(button(node, 'Refresh', refresh), button(node, 'Add Network Tuner', () => networkTunerEditor(), true));
  signal?.addEventListener('abort', () => { stopped = true; }, { once: true });
  void refresh();
  return { element: host, close: () => { stopped = true; } };
}

export function createSdrTrunkSpectrum(deps) {
  const { node, signal } = deps;
  const host = node('div', 'data-workspace sdrtrunk-spectrum');
  const toolbar = node('div', 'ui-catalog-toolbar sdrtrunk-spectrum-toolbar');
  const select = node('select', 'ui-select');
  const status = node('span', 'muted', 'Loading tuners…');
  const field = node('label', 'ui-field');
  field.append(node('span', 'ui-field-label', 'Tuner'), select);
  const fft = node('select', 'ui-select');
  [512, 1024, 2048, 4096, 8192].forEach((size) => {
    const option = node('option', '', `${size} bins`); option.value = String(size);
    if (size === 1024) option.selected = true; fft.append(option);
  });
  const fftField = node('label', 'ui-field');
  fftField.append(node('span', 'ui-field-label', 'FFT size'), fft);
  const windowType = node('select', 'ui-select');
  [['BLACKMAN_HARRIS_7', 'Blackman-Harris 7'], ['BLACKMAN', 'Blackman'], ['COSINE', 'Cosine'],
    ['HAMMING', 'Hamming'], ['HANN', 'Hann'], ['FLAT_TOP', 'Flat top'], ['NONE', 'None']]
    .forEach(([value, label]) => { const option = node('option', '', label); option.value = value; windowType.append(option); });
  const windowField = node('label', 'ui-field');
  windowField.append(node('span', 'ui-field-label', 'FFT window'), windowType);
  const zoom = node('select', 'ui-select');
  [1, 2, 4, 8, 16].forEach((value) => { const option = node('option', '', `${value}×`); option.value = String(value); zoom.append(option); });
  const zoomField = node('label', 'ui-field'); zoomField.append(node('span', 'ui-field-label', 'Zoom'), zoom);
  const pan = node('input', 'ui-input'); pan.type = 'range'; pan.min = '0'; pan.max = '1000'; pan.value = '500';
  const panField = node('label', 'ui-field'); panField.append(node('span', 'ui-field-label', 'Pan'), pan);
  const floor = node('input', 'ui-input'); floor.type = 'number'; floor.value = '-140'; floor.min = '-180'; floor.max = '-1';
  const ceiling = node('input', 'ui-input'); ceiling.type = 'number'; ceiling.value = '10'; ceiling.min = '-179'; ceiling.max = '50';
  const rangeField = node('label', 'ui-field sdrtrunk-spectrum-range');
  rangeField.append(node('span', 'ui-field-label', 'dB range'), floor, ceiling);
  const smoothing = node('input', 'ui-input'); smoothing.type = 'range'; smoothing.min = '0'; smoothing.max = '95'; smoothing.value = '35';
  const smoothField = node('label', 'ui-field'); smoothField.append(node('span', 'ui-field-label', 'Smoothing'), smoothing);
  const palette = node('select', 'ui-select');
  [['classic', 'Classic'], ['fire', 'Fire'], ['ice', 'Ice'], ['mono', 'Monochrome']].forEach(([value, label]) => {
    const option = node('option', '', label); option.value = value; palette.append(option);
  });
  const paletteField = node('label', 'ui-field'); paletteField.append(node('span', 'ui-field-label', 'Waterfall'), palette);
  const interval = node('select', 'ui-select');
  [[150, 'Fast'], [350, 'Normal'], [750, 'Low CPU']].forEach(([value, label]) => {
    const option = node('option', '', label); option.value = String(value); if (value === 350) option.selected = true;
    interval.append(option);
  });
  const intervalField = node('label', 'ui-field'); intervalField.append(node('span', 'ui-field-label', 'Update rate'), interval);
  const tune = button(node, 'Tune to Cursor', async () => {
    if (!Number.isFinite(cursorFrequency) || !select.value) return;
    if (!window.confirm(`Tune the selected idle tuner to ${frequency(cursorFrequency)}?`)) return;
    try {
      await legacyRequest('/api/v1/tuners', 'POST', { action: 'tune', id: select.value,
        frequency: Math.round(cursorFrequency) });
      cursorFrequency = null;
    } catch (cause) { status.textContent = cause.message; }
  }, true);
  tune.disabled = true;
  const reset = button(node, 'Reset View', () => {
    zoom.value = '1'; pan.value = '500'; floor.value = '-140'; ceiling.value = '10'; smoothing.value = '35';
    palette.value = 'classic'; cursorFrequency = null; tune.disabled = true; smoothedBins = [];
    waterfall.getContext('2d').clearRect(0, 0, waterfall.width, waterfall.height);
  });
  toolbar.append(field, fftField, windowField, zoomField, panField, rangeField, smoothField, paletteField,
    intervalField, tune, reset, status);
  const spectrum = node('canvas', 'sdrtrunk-spectrum-canvas');
  spectrum.width = 1200; spectrum.height = 320;
  const waterfall = node('canvas', 'sdrtrunk-waterfall-canvas');
  waterfall.width = 1200; waterfall.height = 300;
  host.append(toolbar, spectrum, waterfall);
  let stopped = false;
  let timer = null;
  let cursorFrequency = null;
  let smoothedBins = [];
  let visibleMinimum = 0;
  let visibleMaximum = 0;
  let lastValue = null;
  let resizeFrame = null;

  function resizeBackingStore(canvas) {
    const bounds = canvas.getBoundingClientRect();
    const density = Math.max(1, Math.min(2.5, Number(window.devicePixelRatio) || 1));
    const width = Math.max(1, Math.round(bounds.width * density));
    const height = Math.max(1, Math.round(bounds.height * density));
    if (canvas.width === width && canvas.height === height) return false;
    canvas.width = width;
    canvas.height = height;
    return true;
  }

  function color(value) {
    const minimum = Number(floor.value); const maximum = Math.max(minimum + 1, Number(ceiling.value));
    const normalized = Math.max(0, Math.min(1, (Number(value) - minimum) / (maximum - minimum)));
    if (palette.value === 'fire') return `hsl(${55 - normalized * 55} 100% ${8 + normalized * 62}%)`;
    if (palette.value === 'ice') return `hsl(${220 - normalized * 55} 90% ${8 + normalized * 67}%)`;
    if (palette.value === 'mono') return `hsl(0 0% ${5 + normalized * 90}%)`;
    return `hsl(${240 - normalized * 240} 90% ${18 + normalized * 42}%)`;
  }

  function draw(value) {
    lastValue = value;
    resizeBackingStore(spectrum);
    resizeBackingStore(waterfall);
    const incoming = value.bins || [];
    const alpha = 1 - Number(smoothing.value) / 100;
    if (smoothedBins.length !== incoming.length) smoothedBins = [...incoming];
    else incoming.forEach((bin, index) => { smoothedBins[index] = smoothedBins[index] * (1 - alpha) + Number(bin) * alpha; });
    const zoomFactor = Number(zoom.value);
    const visibleCount = Math.max(2, Math.floor(smoothedBins.length / zoomFactor));
    const start = Math.floor(Math.max(0, smoothedBins.length - visibleCount) * Number(pan.value) / 1000);
    const bins = smoothedBins.slice(start, start + visibleCount);
    const center = Number(value.frequency || 0); const rate = Number(value.sampleRate || 0);
    visibleMinimum = center - rate / 2 + rate * start / Math.max(1, smoothedBins.length);
    visibleMaximum = visibleMinimum + rate * visibleCount / Math.max(1, smoothedBins.length);
    const context = spectrum.getContext('2d');
    const density = Math.max(1, spectrum.width / Math.max(1, spectrum.getBoundingClientRect().width));
    context.fillStyle = '#07111d'; context.fillRect(0, 0, spectrum.width, spectrum.height);
    context.strokeStyle = '#243849'; context.lineWidth = density;
    const left = 58 * density; const right = spectrum.width - 12 * density;
    const top = 10 * density; const bottom = spectrum.height - 28 * density;
    const minimum = Number(floor.value); const maximum = Math.max(minimum + 1, Number(ceiling.value));
    context.font = `${12 * density}px sans-serif`; context.fillStyle = '#9db0c0';
    for (let index = 0; index <= 5; index += 1) {
      const y = top + index * (bottom - top) / 5;
      context.beginPath(); context.moveTo(left, y); context.lineTo(right, y); context.stroke();
      context.fillText(`${Math.round(maximum - index * (maximum - minimum) / 5)} dB`, 4 * density,
        y + 4 * density);
    }
    for (let index = 0; index <= 8; index += 1) {
      const x = left + index * (right - left) / 8;
      context.beginPath(); context.moveTo(x, top); context.lineTo(x, bottom); context.stroke();
      const label = ((visibleMinimum + index * (visibleMaximum - visibleMinimum) / 8) / 1_000_000).toFixed(3);
      context.fillText(label, Math.max(left, Math.min(right - 45 * density, x - 22 * density)),
        spectrum.height - 8 * density);
    }
    if (bins.length) {
      (value.channels || []).forEach((channel) => {
        const low = Number(channel.frequency) - Number(channel.bandwidth || 0) / 2;
        const high = Number(channel.frequency) + Number(channel.bandwidth || 0) / 2;
        if (high < visibleMinimum || low > visibleMaximum) return;
        const x1 = left + (Math.max(low, visibleMinimum) - visibleMinimum) / (visibleMaximum - visibleMinimum) * (right - left);
        const x2 = left + (Math.min(high, visibleMaximum) - visibleMinimum) / (visibleMaximum - visibleMinimum) * (right - left);
        context.fillStyle = 'rgba(255, 196, 74, .18)';
        context.fillRect(x1, top, Math.max(2 * density, x2 - x1), bottom - top);
        context.strokeStyle = 'rgba(255, 196, 74, .8)';
        context.strokeRect(x1, top, Math.max(2 * density, x2 - x1), bottom - top);
      });
      context.strokeStyle = '#6ee7a5'; context.lineWidth = 1.5 * density; context.beginPath();
      bins.forEach((bin, index) => {
        const x = left + index * (right - left) / Math.max(1, bins.length - 1);
        const y = bottom - Math.max(0, Math.min(1, (Number(bin) - minimum) / (maximum - minimum))) * (bottom - top);
        if (index) context.lineTo(x, y); else context.moveTo(x, y);
      });
      context.stroke();
      if (Number.isFinite(cursorFrequency) && cursorFrequency >= visibleMinimum && cursorFrequency <= visibleMaximum) {
        const x = left + (cursorFrequency - visibleMinimum) / (visibleMaximum - visibleMinimum) * (right - left);
        context.strokeStyle = '#ffcf66'; context.beginPath(); context.moveTo(x, top); context.lineTo(x, bottom); context.stroke();
      }
      const water = waterfall.getContext('2d');
      const waterfallDensity = Math.max(1,
        waterfall.width / Math.max(1, waterfall.getBoundingClientRect().width));
      const rowHeight = Math.max(1, Math.round(waterfallDensity));
      water.drawImage(waterfall, 0, 0, waterfall.width, waterfall.height - rowHeight,
        0, rowHeight, waterfall.width, waterfall.height - rowHeight);
      bins.forEach((bin, index) => {
        water.fillStyle = color(bin);
        water.fillRect(index * waterfall.width / bins.length, 0,
          Math.ceil(waterfall.width / bins.length), rowHeight);
      });
    }
    status.textContent = value.available ?
      `${value.name || 'Tuner'} · ${frequency(value.frequency)} · ${sampleRate(value.sampleRate)} · ` +
      `${(value.channels || []).length} active channel(s)` +
      (Number.isFinite(cursorFrequency) ? ` · cursor ${frequency(cursorFrequency)}` : '') :
      'No active tuner is available';
  }

  async function poll() {
    if (stopped) return;
    try {
      const value = await legacyRequest(`/api/v1/spectrum?tuner=${encodeURIComponent(select.value)}` +
        `&fft=${encodeURIComponent(fft.value)}&window=${encodeURIComponent(windowType.value)}`);
      draw(value);
    } catch (cause) { status.textContent = cause.message; }
    if (!stopped) timer = window.setTimeout(poll, Number(interval.value));
  }

  spectrum.addEventListener('click', (event) => {
    if (!(visibleMaximum > visibleMinimum)) return;
    const bounds = spectrum.getBoundingClientRect();
    const x = (event.clientX - bounds.left) * spectrum.width / bounds.width;
    const density = Math.max(1, spectrum.width / Math.max(1, bounds.width));
    const ratio = Math.max(0, Math.min(1,
      (x - 58 * density) / (spectrum.width - 70 * density)));
    cursorFrequency = visibleMinimum + ratio * (visibleMaximum - visibleMinimum);
    tune.disabled = false;
  });
  [select, fft, windowType, zoom].forEach((control) => control.addEventListener('change', () => {
    smoothedBins = []; cursorFrequency = null; tune.disabled = true;
    waterfall.getContext('2d').clearRect(0, 0, waterfall.width, waterfall.height);
  }));

  legacyRequest('/api/v1/tuners').then((rows) => {
    select.replaceChildren(...(Array.isArray(rows) ? rows : []).filter((row) => row.enabled && row.name).map((row) => {
      const option = node('option', '', row.name || row.id); option.value = row.id; return option;
    }));
    void poll();
  }).catch((cause) => { status.textContent = cause.message; });
  const resizeObserver = 'ResizeObserver' in window ? new ResizeObserver(() => {
    window.cancelAnimationFrame(resizeFrame);
    resizeFrame = window.requestAnimationFrame(() => { if (lastValue && !stopped) draw(lastValue); });
  }) : null;
  resizeObserver?.observe(spectrum);
  resizeObserver?.observe(waterfall);
  const close = () => {
    stopped = true;
    window.clearTimeout(timer);
    window.cancelAnimationFrame(resizeFrame);
    resizeObserver?.disconnect();
  };
  signal?.addEventListener('abort', close, { once: true });
  return { element: host, close };
}
