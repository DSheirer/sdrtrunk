async function request(path) {
  const response = await fetch(path, { credentials: 'same-origin', cache: 'no-store', headers: { Accept: 'application/json' } });
  const value = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(typeof value.error === 'string' ? value.error :
    value.error?.message || `Request failed (${response.status})`);
  return value;
}

function megahertz(value) {
  const frequency = Number(value);
  return Number.isFinite(frequency) && frequency > 0 ? `${(frequency / 1_000_000).toFixed(5)} MHz` : '';
}

function megabytes(value) {
  const bytes = Number(value);
  return Number.isFinite(bytes) && bytes >= 0 ? `${(bytes / 1_048_576).toFixed(1)} MB` : 'Unavailable';
}

function timestamp(value) {
  const time = Number(value);
  return Number.isFinite(time) && time > 0 ? new Date(time).toLocaleTimeString() : '';
}

export function createSdrTrunkDashboard(deps) {
  const { node, signal } = deps;
  const host = node('div', 'sdrtrunk-dashboard');
  const scanner = node('section', 'section ui-section sdrtrunk-scanner');
  const scannerHeader = node('header', 'sdrtrunk-scanner-header');
  const scannerTitle = node('div');
  scannerTitle.append(node('h2', '', 'Live Traffic Scanner'),
    node('p', 'muted', 'Completed calls play in order. New calls wait in the queue instead of interrupting the current call.'));
  const scanState = node('span', 'sdrtrunk-scan-state', 'SCANNING');
  scannerHeader.append(scannerTitle, scanState);
  const fields = node('div', 'sdrtrunk-scanner-grid');
  const values = {};
  [['channel', 'Channel'], ['talkgroup', 'Talkgroup ID'], ['alias', 'Talkgroup alias'],
    ['protocol', 'Protocol'], ['frequency', 'Frequency'], ['source', 'Source radio'],
    ['signal', 'RF signal'], ['audio', 'Audio level']].forEach(([key, label]) => {
    const item = node('div', 'sdrtrunk-scanner-field');
    values[key] = node('strong', 'sdrtrunk-scanner-value', '');
    item.append(node('span', 'muted', label), values[key]);
    fields.append(item);
  });
  const audioControls = node('div', 'ui-action-row sdrtrunk-audio-controls');
  const liveButton = node('button', 'ui-button ui-button-primary', 'Start Live Listening');
  liveButton.type = 'button';
  const liveStatus = node('span', 'muted', 'Off');
  const queueStatus = node('span', 'ui-status ui-status-neutral', '0 queued');
  const holdLabel = node('label', 'ui-field sdrtrunk-hold');
  const hold = node('input', 'ui-input');
  hold.type = 'number';
  hold.min = '0';
  hold.max = '10';
  hold.step = '0.1';
  hold.value = localStorage.getItem('sdrtrunkLiveHold') || '0.7';
  holdLabel.append(node('span', 'ui-field-label', 'Hold between calls (seconds)'), hold);
  const player = node('audio', 'sdrtrunk-audio-player');
  player.controls = true;
  audioControls.append(liveButton, liveStatus, queueStatus, holdLabel);
  scanner.append(scannerHeader, fields, audioControls, player);

  const transcript = node('section', 'section ui-section sdrtrunk-transcript');
  const transcriptHeader = node('header', 'sdrtrunk-transcript-header');
  const transcriptStatus = node('span', 'ui-status ui-status-neutral', 'Checking Whisper');
  transcriptHeader.append(node('h2', '', 'Live Call Transcription'), transcriptStatus);
  const transcriptMeta = node('div', 'muted', 'Waiting for a transcribed call');
  const transcriptText = node('div', 'sdrtrunk-transcript-text',
    'Transcribed radio traffic will appear here after Whisper is configured.');
  transcript.append(transcriptHeader, transcriptMeta, transcriptText);

  const metrics = node('div', 'ui-metric-grid ui-metric-grid-4 sdrtrunk-host-metrics');
  const metricValues = {};
  [['cpu', 'Host CPU'], ['memory', 'Host memory'], ['tuners', 'Tuners'], ['channels', 'Active channels']]
    .forEach(([key, label]) => {
      const item = node('div', 'ui-metric');
      metricValues[key] = node('strong', 'ui-metric-value', '—');
      item.append(node('span', 'ui-metric-label', label), metricValues[key]);
      metrics.append(item);
    });

  const activitySection = node('section', 'section ui-section');
  activitySection.append(node('h2', '', 'Recent Traffic'));
  const activityTable = node('table', 'ui-table');
  const head = node('thead');
  const headRow = node('tr');
  ['Time', 'Channel', 'Talkgroup', 'Alias', 'Source', 'Frequency', 'Event'].forEach((label) =>
    headRow.append(node('th', '', label)));
  head.append(headRow);
  const activityBody = node('tbody');
  activityTable.append(head, activityBody);
  const tableFrame = node('div', 'ui-table-scroll');
  tableFrame.append(activityTable);
  activitySection.append(tableFrame);
  host.append(scanner, transcript, metrics, activitySection);

  let stopped = false;
  let live = false;
  let sequence = -1;
  let activeTranscriptSequence = -1;
  let objectUrl = null;
  let finishAudio = null;
  let refreshTimer = null;
  let liveTimer = null;
  let playingMetadata = null;

  function clearScanner() {
    Object.values(values).forEach((value) => { value.textContent = ''; });
    scanState.textContent = 'SCANNING';
    scanState.className = 'sdrtrunk-scan-state';
  }

  function showCall(call) {
    scanState.textContent = 'RECEIVING';
    scanState.className = 'sdrtrunk-scan-state is-receiving';
    values.channel.textContent = call.channel || '';
    values.talkgroup.textContent = call.talkgroup || '';
    values.alias.textContent = call.alias && call.alias !== 'Unidentified' ? call.alias : '';
    values.protocol.textContent = call.protocol || '';
    values.frequency.textContent = megahertz(call.frequency);
    values.source.textContent = call.source || '';
    const signalAvailable = call.signalAvailable === true || call.rfSignalAvailable === true;
    const signalDbm = call.signalDbm ?? call.rfSignalDbm;
    values.signal.textContent = signalAvailable && signalDbm != null ? `${signalDbm} dBm` : '';
    const audioLevel = Number(call.audioLevelDbfs);
    values.audio.textContent = Number.isFinite(audioLevel) ? `${audioLevel.toFixed(1)} dBFS` : '';
  }

  async function refresh() {
    if (stopped) return;
    try {
      const [status, tuners, channels, activity, transcripts, whisper] = await Promise.all([
        request('/api/v1/status'), request('/api/v1/tuners'), request('/api/v1/channels'),
        request('/api/v1/activity'), request('/api/v1/transcripts'), request('/api/v1/whisper-settings')
      ]);
      metricValues.cpu.textContent = status.cpuAvailable ?
        (Number(status.cpu) < .005 ? '<1%' : `${(Number(status.cpu) * 100).toFixed(1)}%`) : 'Unavailable';
      metricValues.memory.textContent = status.memoryAvailable ?
        `${megabytes(status.memoryUsed)} / ${megabytes(status.memoryMaximum)}` : 'Unavailable';
      metricValues.tuners.textContent = Array.isArray(tuners) ? String(tuners.length) : '0';
      metricValues.channels.textContent = Array.isArray(channels) ?
        String(channels.filter((channel) => channel.processing).length) : '0';
      const rows = Array.isArray(activity) ? activity : [];
      const current = rows.length && Date.now() - Math.max(Number(rows[0].time) || 0, Number(rows[0].end) || 0) < 5000 ?
        rows[0] : playingMetadata;
      if (current) showCall(current); else clearScanner();
      activityBody.replaceChildren(...rows.slice(0, 30).map((row) => {
        const tr = node('tr');
        [timestamp(row.time), row.channel || '', row.talkgroup || '',
          row.alias === 'Unidentified' ? '' : row.alias || '', row.source || '', megahertz(row.frequency), row.type || '']
          .forEach((value) => tr.append(node('td', '', value)));
        return tr;
      }));
      const transcriptRows = Array.isArray(transcripts) ? transcripts : [];
      const selected = activeTranscriptSequence >= 0 ?
        transcriptRows.find((item) => Number(item.sequence) === activeTranscriptSequence) : transcriptRows[0];
      if (selected) {
        transcriptStatus.textContent = `Transcribed with ${selected.model || whisper.model || 'Whisper'}`;
        transcriptStatus.className = 'ui-status ui-status-success';
        transcriptMeta.textContent = [selected.alias || selected.talkgroup,
          selected.talkgroup && selected.alias ? `Talkgroup ${selected.talkgroup}` : '',
          timestamp(selected.started || selected.time)].filter(Boolean).join(' · ');
        transcriptText.textContent = selected.text || 'No speech was recognized.';
      } else if (!whisper.enabled) {
        transcriptStatus.textContent = 'Whisper disabled';
        transcriptStatus.className = 'ui-status ui-status-neutral';
        transcriptMeta.textContent = 'Enable transcription in Administration';
      } else if (!whisper.available) {
        transcriptStatus.textContent = 'Whisper unavailable';
        transcriptStatus.className = 'ui-status ui-status-warning';
        transcriptMeta.textContent = whisper.configurationError || 'Check the Whisper executable setting';
      } else {
        transcriptStatus.textContent = whisper.busy ? 'Transcribing' : 'Whisper ready';
        transcriptStatus.className = 'ui-status ui-status-neutral';
        transcriptMeta.textContent = 'Waiting for a completed call';
      }
    } catch (cause) {
      liveStatus.textContent = cause.message;
    } finally {
      if (!stopped) refreshTimer = window.setTimeout(refresh, 2000);
    }
  }

  async function playCall(response, metadata) {
    playingMetadata = metadata;
    showCall(metadata);
    if (objectUrl) URL.revokeObjectURL(objectUrl);
    objectUrl = URL.createObjectURL(await response.blob());
    player.src = objectUrl;
    activeTranscriptSequence = Number(metadata.sequence ?? sequence);
    liveStatus.textContent = `Playing ${metadata.alias || metadata.talkgroup || 'live traffic'}`;
    transcriptStatus.textContent = 'Transcribing';
    transcriptMeta.textContent = metadata.alias || metadata.talkgroup || 'Current call';
    transcriptText.textContent = 'Waiting for Whisper to finish this call.';
    const ended = new Promise((resolve) => {
      finishAudio = resolve;
      player.onended = resolve;
      player.onerror = resolve;
    });
    try { await player.play(); } catch (_) { liveStatus.textContent += ' · press Play'; }
    await ended;
    finishAudio = null;
    playingMetadata = null;
    if (!live) return;
    const seconds = Math.max(0, Number(hold.value) || 0);
    liveStatus.textContent = seconds ? `Holding ${seconds.toFixed(1)} seconds` : 'Loading next call';
    await new Promise((resolve) => window.setTimeout(resolve, seconds * 1000));
  }

  async function pollLive() {
    if (!live || stopped) return;
    try {
      const response = await fetch(`/api/v1/live-audio?after=${sequence}`,
        { credentials: 'same-origin', cache: 'no-store' });
      if (response.status === 200) {
        sequence = Number(response.headers.get('X-Audio-Sequence'));
        queueStatus.textContent = `${response.headers.get('X-Audio-Queued') || '0'} queued`;
        const metadata = await request(`/api/v1/live-status?sequence=${sequence}`);
        await playCall(response, metadata);
      }
    } catch (cause) {
      liveStatus.textContent = cause.message;
    } finally {
      if (live && !stopped) liveTimer = window.setTimeout(pollLive, 250);
    }
  }

  hold.addEventListener('change', () => localStorage.setItem('sdrtrunkLiveHold',
    String(Math.max(0, Number(hold.value) || 0))));
  liveButton.addEventListener('click', async () => {
    live = !live;
    liveButton.textContent = live ? 'Stop Live Listening' : 'Start Live Listening';
    liveStatus.textContent = live ? 'Waiting for the next call' : 'Off';
    if (live) {
      if (sequence < 0) {
        try { sequence = Number((await request('/api/v1/live-status')).sequence || 0); } catch (_) { sequence = 0; }
      }
      void pollLive();
    } else {
      player.pause();
      finishAudio?.();
    }
  });

  const close = () => {
    stopped = true;
    live = false;
    playingMetadata = null;
    player.pause();
    finishAudio?.();
    window.clearTimeout(refreshTimer);
    window.clearTimeout(liveTimer);
    if (objectUrl) URL.revokeObjectURL(objectUrl);
  };
  signal?.addEventListener('abort', close, { once: true });
  void refresh();
  return { element: host, close };
}
