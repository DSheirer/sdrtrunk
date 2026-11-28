// Calls: stored call history with paging and filters, live call updates on the newest page, replay of recordings,
// auto-play of new calls with a priority queue, and following a talkgroup.  Calls play on the browser's LEFT and
// RIGHT audio channels (or one channel in mono), and the audio panel shows what each channel plays.
import {api, query} from '../api.js';
import {live} from '../live.js';
import {duration, el, identifier, prefs, time} from '../format.js';
import {ColumnEditor, ColumnModel} from '../columns.js';
import {OpusPlayer} from '../opus-player.js';
import {AudioChannel} from '../audio-channels.js';

const PROTOCOLS = ['APCO25', 'APCO25_PHASE2', 'DMR', 'NXDN', 'NBFM', 'AM', 'LTR', 'LTR_NET', 'MPT1327', 'PASSPORT'];
const TYPES = ['GROUP', 'PATCH', 'PRIVATE', 'TELEPHONE'];
const QUIET_MS = 1500;

// Table columns in their default order.  name is shown in the column editor, heading in the table header.  Columns
// with hidden: true are available in the editor but off by default.
const COLUMNS = [
  {id: 'status', name: 'Status', heading: '', render: (call) => {
    const active = call.state === 'ACTIVE';
    const quiet = active && call.lastAudioMs > QUIET_MS;
    return el('td', {class: 'state', title: active ? (quiet ? 'In progress, quiet' : 'In progress') : call.state},
      el('span', {class: 'dot'}));
  }},
  {id: 'start', name: 'Start', render: (call) => el('td', {class: 'nowrap', text: time(call.startMs)})},
  {id: 'duration', name: 'Duration', class: 'num', render: (call) => {
    const shown = call.state === 'ACTIVE' ? (call.elapsedMs ?? Math.max(0, Date.now() - call.startMs)) :
      call.durationMs;
    return el('td', {class: 'nowrap num', text: duration(shown)});
  }},
  {id: 'audio', name: 'Audio', render: (call, context) => el('td', {class: 'nowrap'}, context.audio)},
  {id: 'to', name: 'To', render: (call, context) => el('td', {class: 'to-cell'}, identifier(call.to),
    context.followable ? el('button', {type: 'button', class: 'follow-button',
      title: 'Follow this talkgroup: list only its calls and play them in order', text: 'Follow'}) : null)},
  {id: 'from', name: 'From', render: (call) => el('td', {text: identifier(call.from)})},
  {id: 'talkerAlias', name: 'Talker Alias', render: (call) => el('td', {text: call.talkerAlias || ''})},
  {id: 'type', name: 'Type', render: (call) => el('td', {text: call.type || ''})},
  {id: 'patched', name: 'Patched', render: (call) => el('td', {text: (call.patchedTalkgroups || [])
    .map((patched) => patched.fullyQualified || String(patched.value)).join(', ')})},
  {id: 'protocol', name: 'Protocol', render: (call) =>
    el('td', {text: call.protocol && call.protocol !== 'UNKNOWN' ? call.protocol : ''})},
  {id: 'system', name: 'System', render: (call) => el('td', {text: call.system || ''})},
  {id: 'site', name: 'Site', render: (call) => el('td', {text: call.site || ''})},
  {id: 'channel', name: 'Channel', render: (call) => el('td', {text: call.channel || ''})},
  {id: 'frequency', name: 'Frequency', class: 'num', hidden: true, render: (call) =>
    el('td', {class: 'nowrap num', text: call.frequency > 0 ? (call.frequency / 1e6).toFixed(5) : ''})},
  {id: 'timeslot', name: 'Timeslot', heading: 'TS', class: 'num', hidden: true, render: (call) =>
    el('td', {class: 'num', text: call.timeslot > 0 ? String(call.timeslot) : ''})},
  {id: 'priority', name: 'Priority', class: 'num', hidden: true, render: (call) =>
    el('td', {class: 'num', text: call.priority > 0 ? String(call.priority) : ''})},
  {id: 'flags', name: 'Flags', heading: '', render: (call) => el('td', {class: 'flags', text: [
    call.encrypted ? 'Encrypted' : null, call.duplicate ? 'Duplicate' : null,
    call.state === 'INTERRUPTED' ? 'Interrupted' : null].filter(Boolean).join(', ')})},
  {id: 'recording', name: 'Recording', hidden: true, render: (call) => el('td', {text: call.recording || ''})}
];
const MAX_WAITING = 10;

export function mount(container, toast) {
  const state = {
    filters: prefs.get('calls.filters', {}),
    pageSize: prefs.get('calls.pageSize', 50),
    pages: [null],        // cursor of each page; page 0 is the newest
    index: 0,
    anchor: null,         // 'until' that keeps the offsets of older pages stable while new calls arrive
    rows: new Map(),      // row key -> {call, tr, live}
    order: [],            // row keys in display order, newest first
    byCallKey: new Map(), // live call key -> row key
    selected: null,
    newCount: 0,
    subscription: null,
    loading: false
  };
  const player = new OpusPlayer();

  // Playback: live calls waiting their turn, auto-play and following a talkgroup.  What plays is in each channel.
  const playback = {
    autoplay: prefs.get('calls.autoplay', false),
    continueNewer: prefs.get('calls.continueNewer', false),
    stereo: prefs.get('calls.stereo', true),
    follow: null,          // {value, protocol, system, label, catchUp}
    queue: [],             // live calls waiting: {callKey, priority, startMs}
    audioSub: null,
    manualKeys: new Set()  // call keys the user chose to hear live
  };
  const channelController = {
    skip: (channel) => skip(channel),
    follow: (channel) => {
      if (channel.current) {
        startFollow(channel.current.call);
      }
    },
    muteChanged: (channel) => prefs.set('calls.muted.' + channel.name.toLowerCase(), channel.muted),
    recordingEnded: (channel) => recordingEnded(channel),
    canFollow: (call) => !!call && !!call.to && call.to.value !== null && call.to.value !== undefined &&
      !playback.follow
  };
  const channels = [new AudioChannel(0, 'LEFT', channelController), new AudioChannel(1, 'RIGHT', channelController)];
  // The audio context starts suspended; the first user action that plays audio resumes it
  const audioContext = player.ensureContext();
  for (const channel of channels) {
    channel.connect(audioContext);
    channel.setMuted(prefs.get('calls.muted.' + channel.name.toLowerCase(), false));
  }

  // ---- Layout ----
  const field = (name, label, attributes = {}) => el('label', {}, label,
    el('input', Object.assign({name, value: state.filters[name] || ''}, attributes)));
  const choice = (name, label, values) => el('label', {}, label,
    el('select', {name}, el('option', {value: '', text: 'Any'}),
      ...values.map((value) => el('option', {value, text: value, selected: state.filters[name] === value}))));
  const form = el('form', {class: 'filters'},
    field('to', 'To', {placeholder: 'talkgroup or radio'}),
    field('from', 'From', {placeholder: 'radio'}),
    field('system', 'System'),
    choice('protocol', 'Protocol', PROTOCOLS),
    choice('type', 'Type', TYPES),
    field('since', 'Since', {type: 'datetime-local', value: localInput(state.filters.since)}),
    field('until', 'Until', {type: 'datetime-local', value: localInput(state.filters.until)}),
    el('div', {class: 'filter-buttons'},
      el('button', {type: 'submit', text: 'Search'}),
      el('button', {type: 'button', class: 'secondary', text: 'Clear', onclick: () => {
        form.querySelectorAll('input').forEach((input) => { input.value = ''; });
        form.querySelectorAll('select').forEach((s) => { s.value = ''; });
        applyFilters();
      }})));
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    applyFilters();
  });

  const liveBadge = el('span', {class: 'live-badge', hidden: true, text: 'Live'});
  const autoplayBox = el('input', {type: 'checkbox', checked: playback.autoplay});
  const continueBox = el('input', {type: 'checkbox', checked: playback.continueNewer});
  const stereoBox = el('input', {type: 'checkbox', checked: playback.stereo});
  const queueInfo = el('span', {class: 'queue-info'});
  const options = el('div', {class: 'playback-options'},
    el('label', {title: 'Play each new call as it arrives.  Calls that arrive while both channels play wait and ' +
      'play next, highest priority first.'}, autoplayBox, 'Auto-play new calls'),
    el('label', {title: 'After a recording finishes, play the next newer recording in the list'}, continueBox,
      'Continue with newer recordings'),
    el('label', {title: 'Play two calls at once, one on the left channel and one on the right.  Off, one call ' +
      'plays at a time on both speakers.'}, stereoBox, 'Stereo: left and right channels'));
  const audioPanel = el('div', {class: 'audio-panel'},
    el('div', {class: 'audio-panel-bar'},
      el('span', {class: 'audio-panel-title', text: 'Audio'}), queueInfo),
    el('div', {class: 'audio-channels'}, ...channels.map((channel) => channel.view)));
  // Above the table: play the recording newer or older than the selected row, or stop the audio
  const playButtons = el('div', {class: 'play-buttons'},
    el('button', {type: 'button', class: 'secondary', text: '▲ Play newer',
      title: 'Play the recording above the selected call', onclick: () => playNeighbor(-1)}),
    el('button', {type: 'button', class: 'secondary', text: '▼ Play older',
      title: 'Play the recording below the selected call', onclick: () => playNeighbor(1)}),
    el('button', {type: 'button', class: 'secondary', text: 'Stop', title: 'Stop both channels and the ' +
      'waiting calls', onclick: () => stopPlayback()}));
  const followBanner = el('div', {class: 'follow-banner', hidden: true});
  const newBanner = el('button', {class: 'new-banner', type: 'button', hidden: true, onclick: () => loadPage(0)});
  const columns = new ColumnModel(COLUMNS, 'calls.columns');
  const headerRow = el('tr');
  const tbody = el('tbody');
  const table = el('table', {class: 'calls', tabindex: '0'}, el('thead', {}, headerRow), tbody);
  const columnEditor = new ColumnEditor(columns, 'Calls Columns', headerRow);
  columns.onChange = () => refreshAll();
  const status = el('span', {class: 'note'});
  const countLabel = el('span', {class: 'call-count', 'aria-live': 'polite'});
  const newer = el('button', {type: 'button', text: '← Newer', onclick: () => loadPage(state.index - 1)});
  const older = el('button', {type: 'button', text: 'Older →', onclick: () => loadPage(state.index + 1)});
  const pageLabel = el('span', {class: 'page-label'});
  const pageSize = el('select', {onchange: () => {
    state.pageSize = Number(pageSize.value);
    prefs.set('calls.pageSize', state.pageSize);
    loadPage(0);
  }}, ...[25, 50, 100, 200].map((size) => el('option', {value: size, text: size + ' per page',
    selected: size === state.pageSize})));

  container.append(
    followBanner,
    el('section', {class: 'panel'}, el('h2', {}, 'Calls ', liveBadge), audioPanel, options, form),
    el('section', {class: 'panel'},
      el('div', {class: 'pager'}, newer, pageLabel, older, pageSize, countLabel, status, playButtons,
        columnEditor.button),
      newBanner,
      el('div', {class: 'table-wrap'}, table)));

  table.addEventListener('keydown', (event) => {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      move(event.key === 'ArrowDown' ? 1 : -1);
    } else if (event.key === 'Enter' && state.selected) {
      activate(state.selected);
    }
  });

  player.onStateChange = () => refreshAll();
  player.onFinished = (callKey) => {
    const channel = channels.find((c) => c.current && c.current.kind === 'live' && c.current.callKey === callKey);
    if (channel) {
      channel.stop();
      pump();
    }
  };
  autoplayBox.addEventListener('change', async () => {
    playback.autoplay = autoplayBox.checked;
    if (playback.autoplay && !(await enableAudio())) {
      autoplayBox.checked = playback.autoplay = false;
    }
    prefs.set('calls.autoplay', playback.autoplay);
    updateAudioSubscription();
    showQueue();
  });
  continueBox.addEventListener('change', () => {
    playback.continueNewer = continueBox.checked;
    prefs.set('calls.continueNewer', playback.continueNewer);
  });
  stereoBox.addEventListener('change', () => {
    playback.stereo = stereoBox.checked;
    prefs.set('calls.stereo', playback.stereo);
    applyStereo();
    pump();
  });

  // ---- Filters and paging ----
  function applyFilters() {
    const data = new FormData(form);
    const filters = {};
    for (const [name, value] of data.entries()) {
      if (value) {
        filters[name] = (name === 'since' || name === 'until') ? new Date(value).toISOString() : value.trim();
      }
    }
    state.filters = filters;
    prefs.set('calls.filters', filters);
    loadPage(0);
  }

  // The list's filters: the form's, or the followed talkgroup's
  function activeFilters() {
    if (playback.follow) {
      const {value, protocol, system} = playback.follow;
      return {toValue: value, protocol, system};
    }
    return state.filters;
  }

  // ---- Call count: all stored calls, or those that match the search ----
  let countTimer = null;
  let countRequest = 0;

  async function loadCount() {
    clearTimeout(countTimer);
    countTimer = null;
    const request = ++countRequest;
    const filters = activeFilters();
    const filtered = Object.values(filters).some((value) => value !== undefined && value !== null && value !== '');
    try {
      const result = await api('GET', '/calls/count' + query(filters));
      if (request !== countRequest) {
        return;
      }
      const number = result.count.toLocaleString();
      countLabel.textContent = filtered ?
        number + (result.count === 1 ? ' matching call' : ' matching calls') :
        number + (result.count === 1 ? ' call' : ' calls') + ' in the database';
      countLabel.title = filtered ? 'Calls that match the search' : 'All stored calls';
    } catch (e) {
      if (request === countRequest) {
        countLabel.textContent = '';
      }
    }
  }

  // New calls are stored often, so the count is read again at most every few seconds
  function scheduleCount() {
    if (!countTimer) {
      countTimer = setTimeout(loadCount, 3000);
    }
  }

  async function loadPage(index) {
    if (index < 0 || index >= state.pages.length || state.loading) {
      return;
    }
    state.loading = true;
    status.textContent = 'Loading…';
    const filters = activeFilters();
    if (index === 0) {
      loadCount();
      state.pages = [null];
      // Older pages list calls up to this moment, so calls arriving later don't shift them
      state.anchor = filters.until || new Date().toISOString();
    }
    try {
      const params = Object.assign({}, filters, {limit: state.pageSize, cursor: state.pages[index]});
      if (index > 0) {
        params.until = state.anchor;
      }
      const page = await api('GET', '/calls' + query(params));
      state.index = index;
      state.pages.length = index + 1;
      if (page.nextCursor) {
        state.pages.push(page.nextCursor);
      }
      showRows(page.items);
      state.newCount = 0;
      newBanner.hidden = true;
      status.textContent = page.items.length ? '' : 'No calls';
      updateLive();
    } catch (e) {
      status.textContent = '';
      toast(e.message, true);
    } finally {
      state.loading = false;
      pageLabel.textContent = 'Page ' + (state.index + 1);
      newer.disabled = state.index === 0;
      older.disabled = state.index + 1 >= state.pages.length;
    }
  }

  // ---- Live call updates (newest page, no 'until' filter) ----
  function liveActive() {
    return state.index === 0 && !activeFilters().until;
  }

  function liveParams() {
    const {to, toValue, from, system, protocol, type, since} = activeFilters();
    return {to, toValue, from, system, protocol, type, since, progressIntervalMs: 1000};
  }

  async function updateLive() {
    if (state.subscription) {
      state.subscription.update(liveParams());
    } else {
      state.subscription = await live.subscribe('calls', liveParams(), {
        json: onLive,
        error: (message) => toast(message.detail || message.title, true),
        resubscribed: () => loadPage(state.index)
      });
    }
    liveBadge.hidden = !liveActive();
  }

  function onLive(data) {
    switch (data.type) {
      case 'call.snapshot':
        data.calls.forEach((call) => upsert(call, liveActive()));
        break;
      case 'call.started':
        if (liveActive()) {
          upsert(data.call, true);
        } else if (!state.byCallKey.get(data.call.callKey)) {
          state.newCount++;
          newBanner.textContent = state.newCount + ' new call' + (state.newCount === 1 ? '' : 's') +
            ' — show the newest';
          newBanner.hidden = false;
        }
        break;
      case 'call.updated':
        upsert(data.call, false);
        break;
      case 'call.ended':
        if (data.audioMs !== undefined && data.audioMs !== null) {
          data.call.audioMs = data.audioMs;
        }
        upsert(data.call, false);
        break;
      case 'call.progress':
        progress(data);
        break;
      case 'call.stored':
        stored(data.callKey, data.id);
        scheduleCount();
        break;
      case 'call.removed': {
        const key = state.byCallKey.get(data.callKey);
        if (key && state.rows.get(key).live) {
          removeRow(key);
        }
        break;
      }
    }
  }

  // ---- Rows ----
  function keyOf(call) {
    return call.id ? 'i' + call.id : 'k' + call.callKey;
  }

  function showRows(calls) {
    tbody.replaceChildren();
    state.rows.clear();
    state.order = [];
    state.byCallKey.clear();
    for (const call of calls) {
      const key = keyOf(call);
      const row = {call, tr: null, live: false};
      row.tr = buildRow(key, row);
      state.rows.set(key, row);
      state.order.push(key);
      tbody.append(row.tr);
    }
    if (state.selected && state.rows.has(state.selected)) {
      state.rows.get(state.selected).tr.classList.add('selected');
    } else {
      state.selected = null;
    }
  }

  // Updates the row of a call, or adds it at the top when prepend is true
  function upsert(call, prepend) {
    let key = call.callKey ? state.byCallKey.get(call.callKey) : null;
    if (!key && call.id && state.rows.has('i' + call.id)) {
      key = 'i' + call.id;
    }
    if (!key && state.rows.has(keyOf(call))) {
      key = keyOf(call);
    }
    if (key) {
      const row = state.rows.get(key);
      row.call = Object.assign({}, row.call, call, {audioMs: call.audioMs ?? row.call.audioMs,
        elapsedMs: call.state === 'ACTIVE' ? row.call.elapsedMs : undefined,
        id: call.id ?? row.call.id});
      if (call.callKey) {
        state.byCallKey.set(call.callKey, key);
      }
      refresh(key);
      return;
    }
    if (!prepend) {
      return;
    }
    key = keyOf(call);
    const row = {call, tr: null, live: true};
    row.tr = buildRow(key, row);
    state.rows.set(key, row);
    state.order.unshift(key);
    state.byCallKey.set(call.callKey, key);
    tbody.prepend(row.tr);
    row.tr.classList.add('arrived');
    // The newest page keeps at most two pages of rows
    while (state.order.length > state.pageSize * 2) {
      removeRow(state.order[state.order.length - 1]);
    }
    status.textContent = '';
  }

  function progress(data) {
    const key = state.byCallKey.get(data.callKey) || (data.id ? 'i' + data.id : null);
    const row = key && state.rows.get(key);
    if (row) {
      row.call.audioMs = data.audioMs;
      row.call.elapsedMs = data.elapsedMs;
      row.call.lastAudioMs = data.lastAudioMs;
      if (data.id) {
        row.call.id = data.id;
      }
      refresh(key);
    }
  }

  function stored(callKey, id) {
    const key = state.byCallKey.get(callKey);
    if (!key) {
      return;
    }
    const row = state.rows.get(key);
    row.call.id = id;
    const newKey = 'i' + id;
    if (newKey !== key) {
      state.rows.delete(key);
      state.rows.set(newKey, row);
      state.order[state.order.indexOf(key)] = newKey;
      state.byCallKey.set(callKey, newKey);
      row.tr.dataset.key = newKey;
      if (state.selected === key) {
        state.selected = newKey;
      }
      for (const channel of channels) {
        if (channel.lastPlayedKey === key) {
          channel.lastPlayedKey = newKey;
        }
      }
    }
    refresh(newKey);
  }

  function removeRow(key) {
    const row = state.rows.get(key);
    if (row) {
      row.tr.remove();
      state.rows.delete(key);
      state.order.splice(state.order.indexOf(key), 1);
      if (row.call.callKey) {
        state.byCallKey.delete(row.call.callKey);
      }
      if (state.selected === key) {
        state.selected = null;
      }
    }
  }

  function buildRow(key, row) {
    const tr = el('tr', {'data-key': key, onclick: (event) => {
      const current = state.rows.get(tr.dataset.key);
      if (event.target.closest('.follow-button')) {
        // Follow button in the To column
        if (current) {
          startFollow(current.call);
        }
        return;
      }
      select(tr.dataset.key);
      activate(tr.dataset.key);
    }});
    fill(tr, row.call);
    return tr;
  }

  function refresh(key) {
    const row = state.rows.get(key);
    if (row) {
      fill(row.tr, row.call);
      for (const channel of channels) {
        if (channel.updateCall(key, row.call.callKey, row.call)) {
          channel.update();
        }
      }
    }
  }

  function refreshAll() {
    for (const key of state.order) {
      refresh(key);
    }
  }

  function fill(tr, call) {
    const active = call.state === 'ACTIVE';
    const quiet = active && call.lastAudioMs > QUIET_MS;
    const playing = channelOf(tr.dataset.key, call.callKey);
    const followable = call.to && call.to.value !== null && call.to.value !== undefined && !playback.follow;
    tr.classList.toggle('active', active);
    tr.classList.toggle('quiet', quiet);
    tr.classList.toggle('playing', !!playing);
    const context = {followable, audio: audioCell(call, playing)};
    tr.replaceChildren(...columns.visible().map((column) => column.render(call, context)));
  }

  // The audio cell keeps the width of its widest label, so the table doesn't change its layout as a call starts or
  // stops playing
  const AUDIO_LABELS = [['audio-action playing', '♪ Playing'], ['audio-action playing', '♪ Left'],
    ['audio-action playing', '♪ Right'], ['audio-action', '▶ Play'],
    ['audio-action live', '⏸ Waiting'], ['audio-action live', '◉ Listen'], ['note', 'Recording…'],
    ['note', 'Saving…'], ['note', 'No audio']];

  function audioCell(call, playing) {
    return el('span', {class: 'audio-cell'}, audioLabel(call, playing),
      ...AUDIO_LABELS.map(([cls, text]) => el('span', {class: cls + ' audio-sizer', text,
        'aria-hidden': 'true'})));
  }

  function audioLabel(call, playing) {
    if (playing) {
      // The channel that plays the call, in its panel color
      const side = playing.name === 'RIGHT' ? 'right' : 'left';
      return el('span', {class: 'audio-action playing ' + side, title: 'Playing on the ' +
        (playback.stereo ? playing.name.toLowerCase() + ' channel' : 'audio channel'),
        text: playback.stereo ? (side === 'right' ? '♪ Right' : '♪ Left') : '♪ Playing'});
    }
    if (call.audioAvailable && call.id) {
      return el('span', {class: 'audio-action', text: '▶ Play'});
    }
    if (call.state === 'ACTIVE' && !call.encrypted) {
      return el('span', {class: 'audio-action live', text: player.isHeld(call.callKey) ? '⏸ Waiting' :
        '◉ Listen'});
    }
    return el('span', {class: 'note', text: call.recording === 'RECORDING' ? 'Recording…' :
      call.recording === 'RECORDED' ? 'Saving…' : 'No audio'});
  }

  function select(key) {
    if (state.selected && state.rows.has(state.selected)) {
      state.rows.get(state.selected).tr.classList.remove('selected');
    }
    state.selected = key;
    const row = state.rows.get(key);
    if (row) {
      row.tr.classList.add('selected');
      row.tr.scrollIntoView({block: 'nearest'});
    }
  }

  // Moves the selection with the arrow keys
  function move(step) {
    const index = state.selected ? state.order.indexOf(state.selected) : -1;
    const next = index + step;
    if (next >= 0 && next < state.order.length) {
      select(state.order[next]);
    }
  }

  // ---- Playback ----
  function activeChannels() {
    return playback.stereo ? channels : channels.slice(0, 1);
  }

  // The channel that plays the row or live call, or null
  function channelOf(key, callKey) {
    return channels.find((channel) => channel.isPlaying(key, callKey)) || null;
  }

  // The channel for a call the user chose: a recording replaces the recording that plays, so the arrows step through
  // recordings on one channel.  Otherwise an idle channel, then the channel replaying a recording, then the left.
  function pickChannel(kind) {
    const active = activeChannels();
    const replaying = active.find((channel) => channel.current && channel.current.kind === 'recording');
    if (kind === 'recording' && replaying) {
      return replaying;
    }
    return active.find((channel) => channel.isIdle()) || replaying || active[0];
  }

  // Mono plays the left channel on both speakers and leaves the right channel unused
  function applyStereo() {
    channels[0].setPan(playback.stereo ? -1 : 0);
    channels[0].tag.textContent = playback.stereo ? 'LEFT' : 'MONO';
    channels[1].setPan(1);
    channels[1].view.hidden = !playback.stereo;
    if (!playback.stereo) {
      stopChannel(channels[1]);
    }
    refreshAll();
  }

  // Plays the chosen call: its recording, or its live audio while it is in progress.  The user's choice takes a
  // channel; waiting calls keep waiting and play afterwards.
  async function activate(key) {
    const row = state.rows.get(key);
    if (!row) {
      return;
    }
    const call = row.call;
    const playing = channelOf(key, call.callKey);
    if (call.audioAvailable && call.id) {
      if (await enableAudio(false)) {
        playRecording(key, playing || pickChannel('recording'));
      }
    } else if (call.state === 'ACTIVE' && !call.encrypted) {
      if (playing) {
        skip(playing);
      } else if (await enableAudio(true)) {
        playback.manualKeys.add(call.callKey);
        await updateAudioSubscription();
        playLive(call.callKey, key, pickChannel('live'));
      }
    } else {
      toast('This call has no recording');
    }
  }

  function playRecording(key, channel) {
    const call = state.rows.get(key).call;
    stopChannel(channel);
    channel.playRecording(key, call);
    select(key);
    refreshAll();
    showQueue();
  }

  function playLive(callKey, key, channel) {
    stopChannel(channel);
    const other = channelOf(null, callKey);
    if (other) {
      stopChannel(other);
    }
    playback.queue = playback.queue.filter((item) => item.callKey !== callKey);
    const row = key && state.rows.get(key);
    channel.playLive(key, callKey, row ? row.call : {callKey});
    player.play(callKey, channel.input);
    if (row) {
      select(key);
    }
    refreshAll();
    showQueue();
  }

  // Ends what the channel plays without starting anything
  function stopChannel(channel) {
    const current = channel.stop();
    if (current && current.kind === 'live') {
      player.discard(current.callKey);
    }
  }

  function recordingEnded(channel) {
    channel.stop();
    if ((playback.follow && playback.follow.catchUp) || playback.continueNewer) {
      if (playNewerRecording(channel)) {
        return;
      }
      if (playback.follow) {
        playback.follow.catchUp = false;
      }
    }
    pump();
  }

  // Fills idle channels: a followed talkgroup's newer recordings while catching up, then waiting live calls.  A
  // followed talkgroup plays one call at a time, in order.
  function pump() {
    const active = activeChannels();
    if (playback.follow && active.some((channel) => channel.current)) {
      refreshAll();
      showQueue();
      return;
    }
    if (playback.follow && playback.follow.catchUp) {
      if (playNewerRecording(active[0])) {
        return;
      }
      playback.follow.catchUp = false;
    }
    for (const channel of playback.follow ? active.slice(0, 1) : active) {
      while (channel.isIdle() && playback.queue.length) {
        const next = playback.queue.shift();
        if (player.isHeld(next.callKey)) {
          playLive(next.callKey, state.byCallKey.get(next.callKey), channel);
        }
      }
    }
    refreshAll();
    showQueue();
  }

  function skip(channel) {
    stopChannel(channel);
    if (playback.follow) {
      playback.follow.catchUp = false;
    }
    pump();
  }

  // Stops both channels and the waiting calls
  function stopPlayback() {
    channels.forEach(stopChannel);
    for (const item of playback.queue) {
      player.discard(item.callKey);
    }
    playback.queue = [];
    playback.manualKeys.clear();
    if (playback.follow) {
      playback.follow.catchUp = false;
    }
    updateAudioSubscription();
    refreshAll();
    showQueue();
  }

  // Plays the next newer recording after the last one the channel played.  Returns false when there is none.
  function playNewerRecording(channel) {
    const index = channel.lastPlayedKey ? state.order.indexOf(channel.lastPlayedKey) : -1;
    for (let next = index - 1; next >= 0; next--) {
      const call = state.rows.get(state.order[next]).call;
      if (call.audioAvailable && call.id && call.state !== 'ACTIVE') {
        playRecording(state.order[next], channel);
        return true;
      }
    }
    return false;
  }

  // Newer and Older buttons: the newer (-1) or older (+1) recording next to the selection
  async function playNeighbor(step) {
    const index = state.selected ? state.order.indexOf(state.selected) : -1;
    for (let next = index + step; next >= 0 && next < state.order.length; next += step) {
      const call = state.rows.get(state.order[next]).call;
      if (call.audioAvailable && call.id) {
        if (await enableAudio(false)) {
          playRecording(state.order[next], pickChannel('recording'));
        }
        return;
      }
    }
  }

  // ---- Live audio: auto-play, following and listening ----
  // Resumes audio from a user action.  Live audio also needs WebCodecs; recordings play without it.
  async function enableAudio(liveAudio = true) {
    if (liveAudio && !OpusPlayer.supported()) {
      toast('This browser can’t decode live audio (WebCodecs is needed)', true);
      return false;
    }
    await player.resume();
    return true;
  }

  function audioParams() {
    const {to, toValue, from, system, protocol, type} = activeFilters();
    return {to, toValue, from, system, protocol, type};
  }

  // Keeps the audio.live subscription open while anything needs live audio
  async function updateAudioSubscription() {
    const needed = playback.autoplay || !!playback.follow || playback.manualKeys.size > 0;
    if (!needed) {
      if (playback.audioSub) {
        playback.audioSub.close();
        playback.audioSub = null;
      }
      for (const item of playback.queue) {
        player.discard(item.callKey);
      }
      playback.queue = [];
      return;
    }
    if (playback.audioSub) {
      playback.audioSub.update(audioParams());
      return;
    }
    playback.audioSub = await live.subscribe('audio.live', audioParams(), {
      json: onAudio,
      binary: (view, buffer) => player.frame(view, buffer),
      error: (message) => toast(message.detail || message.title, true)
    });
  }

  function onAudio(data) {
    const callKey = data.callKey;
    if (data.type === 'audio.start') {
      if (playback.manualKeys.has(callKey)) {
        player.hold(callKey);
        const channel = channelOf(null, callKey);
        if (channel) {
          player.play(callKey, channel.input);
        }
      } else if (playback.autoplay || playback.follow) {
        player.hold(callKey);
        playback.queue.push({callKey, priority: priorityOf(data.call), startMs: data.call.startMs});
        // Highest priority first (1 is highest, 100 the default), then in the order they started
        playback.queue.sort((a, b) => a.priority - b.priority || a.startMs - b.startMs);
        while (playback.queue.length > MAX_WAITING) {
          player.discard(playback.queue.pop().callKey);
        }
        pump();
      }
      refreshAll();
      showQueue();
    } else if (data.type === 'audio.end') {
      player.ended(callKey);
      playback.manualKeys.delete(callKey);
    }
  }

  function priorityOf(call) {
    return call && call.priority > 0 ? call.priority : 100;
  }

  function showQueue() {
    const waiting = playback.queue.filter((item) => player.isHeld(item.callKey)).length;
    const mode = playback.follow ? 'Following ' + playback.follow.label : (playback.autoplay ? 'Auto-play' : '');
    const text = [mode, waiting ? waiting + ' waiting' : ''].filter(Boolean).join(' · ');
    queueInfo.textContent = text;
  }

  // ---- Following a talkgroup ----
  async function startFollow(call) {
    if (!call.to || call.to.value === null || call.to.value === undefined) {
      toast('This call has no talkgroup to follow', true);
      return;
    }
    if (!(await enableAudio())) {
      return;
    }
    channels.forEach(stopChannel);
    for (const item of playback.queue) {
      player.discard(item.callKey);
    }
    playback.queue = [];
    const startKey = keyOf(call);
    playback.follow = {value: call.to.value, protocol: call.protocol, system: call.system,
      label: call.to.alias || call.to.formatted, catchUp: true};
    followBanner.replaceChildren(
      el('span', {}, 'Following ', el('strong', {text: identifier(call.to)}), call.system ? ' on ' + call.system : '',
        ' — the list shows only its calls, and they play in order as they arrive.'),
      el('button', {type: 'button', class: 'secondary', text: 'Stop following', onclick: stopFollow}));
    followBanner.hidden = false;
    form.querySelectorAll('input, select, button').forEach((element) => { element.disabled = true; });
    state.loading = false;
    await loadPage(0);
    await updateAudioSubscription();
    // Catch up from the chosen call: its recording, then the newer ones, then live calls as they arrive
    const key = state.rows.has(startKey) ? startKey : state.byCallKey.get(call.callKey);
    const row = key && state.rows.get(key);
    if (row && row.call.audioAvailable && row.call.id) {
      playRecording(key, activeChannels()[0]);
    } else if (row && row.call.state === 'ACTIVE' && !row.call.encrypted) {
      playback.follow.catchUp = false;
      playback.manualKeys.add(row.call.callKey);
      playLive(row.call.callKey, key, activeChannels()[0]);
    } else {
      playback.follow.catchUp = false;
      pump();
    }
    showQueue();
  }

  function stopFollow() {
    playback.follow = null;
    channels.forEach((channel) => channel.update());
    followBanner.hidden = true;
    form.querySelectorAll('input, select, button').forEach((element) => { element.disabled = false; });
    updateAudioSubscription();
    showQueue();
    loadPage(0);
  }

  applyStereo();
  loadPage(0);
  // Browsers start audio only after a user action: the first click on the page enables it
  const resumeAudio = () => player.resume();
  document.addEventListener('click', resumeAudio, {once: true});
  if (playback.autoplay) {
    updateAudioSubscription();
  }
  showQueue();
  // Elapsed times and level meters
  const ticker = setInterval(() => channels.forEach((channel) => channel.tick()), 100);
  // Old calls are deleted by the age-off, so the count is also read again each minute
  const countRefresh = setInterval(loadCount, 60000);

  // Unmount
  return () => {
    clearInterval(countRefresh);
    clearTimeout(countTimer);
    countRequest++;
    stopPlayback();
    if (playback.audioSub) {
      playback.audioSub.close();
    }
    if (state.subscription) {
      state.subscription.close();
    }
    clearInterval(ticker);
    document.removeEventListener('click', resumeAudio);
    player.close();
    columnEditor.close();
  };
}

function localInput(iso) {
  if (!iso) {
    return '';
  }
  const date = new Date(iso);
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
}
