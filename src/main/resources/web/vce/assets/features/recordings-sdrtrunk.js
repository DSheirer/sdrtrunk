async function request(path) {
  const response = await fetch(path, { credentials: 'same-origin', cache: 'no-store', headers: { Accept: 'application/json' } });
  const value = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(typeof value.error === 'string' ? value.error :
    value.error?.message || `Request failed (${response.status})`);
  return value;
}

function bytes(value) {
  const size = Number(value);
  if (!Number.isFinite(size)) return '';
  if (size < 1024) return `${size} B`;
  if (size < 1_048_576) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1_048_576).toFixed(1)} MB`;
}

export function createSdrTrunkRecordings(deps) {
  const { node, signal } = deps;
  const host = node('div', 'data-workspace');
  const playerSection = node('section', 'section ui-section');
  const playerTitle = node('h2', '', 'Recording Playback');
  const nowPlaying = node('div', 'muted', 'Select a recording to play');
  const player = node('audio'); player.controls = true; player.className = 'sdrtrunk-audio-player';
  playerSection.append(playerTitle, nowPlaying, player);
  const recordingSection = node('section', 'section ui-section');
  recordingSection.append(node('h2', '', 'Recorded Audio'));
  const recordingsHost = node('div', 'ui-table-scroll');
  recordingSection.append(recordingsHost);
  const transcriptSection = node('section', 'section ui-section');
  transcriptSection.append(node('h2', '', 'Scanner Transcripts'));
  const transcriptsHost = node('div', 'ui-table-scroll');
  transcriptSection.append(transcriptsHost);
  const status = node('div');
  host.append(status, playerSection, recordingSection, transcriptSection);
  let stopped = false;
  let objectUrl = null;

  function table(headers, rows) {
    const table = node('table', 'ui-table');
    const head = node('thead'); const tr = node('tr');
    headers.forEach((header) => tr.append(node('th', '', header))); head.append(tr);
    const body = node('tbody');
    rows.forEach((cells) => {
      const row = node('tr');
      cells.forEach((cell) => { const td = node('td'); if (cell instanceof Node) td.append(cell); else td.textContent = String(cell ?? ''); row.append(td); });
      body.append(row);
    });
    table.append(head, body); return table;
  }

  async function play(name) {
    const response = await fetch(`/api/v1/recording-audio?name=${encodeURIComponent(name)}`,
      { credentials: 'same-origin', cache: 'no-store' });
    if (!response.ok) throw new Error('Unable to load recording');
    if (objectUrl) URL.revokeObjectURL(objectUrl);
    objectUrl = URL.createObjectURL(await response.blob());
    player.src = objectUrl; nowPlaying.textContent = name;
    try { await player.play(); } catch (_) { nowPlaying.textContent = `${name} · press Play`; }
  }

  async function refresh() {
    try {
      const [recordings, transcripts] = await Promise.all([
        request('/api/v1/recordings'), request('/api/v1/transcripts')
      ]);
      recordingsHost.replaceChildren(table(['File', 'Recorded', 'Size', ''],
        (Array.isArray(recordings) ? recordings : []).map((item) => {
          const action = node('button', 'ui-button ui-button-secondary', 'Play'); action.type = 'button';
          action.addEventListener('click', () => play(item.name).catch((cause) => { status.textContent = cause.message; }));
          return [item.name, new Date(item.modified).toLocaleString(), bytes(item.size), action];
        })));
      transcriptsHost.replaceChildren(table(['Time', 'Talkgroup', 'Alias', 'Transcript'],
        (Array.isArray(transcripts) ? transcripts : []).map((item) => [
          new Date(item.time || item.started || 0).toLocaleString(), item.talkgroup, item.alias, item.text
        ])));
      status.className = ''; status.textContent = '';
    } catch (cause) {
      status.className = 'ui-notice ui-notice-danger'; status.textContent = cause.message;
    }
  }

  const close = () => { stopped = true; player.pause(); if (objectUrl) URL.revokeObjectURL(objectUrl); };
  signal?.addEventListener('abort', close, { once: true });
  if (!stopped) void refresh();
  return { element: host, close };
}
