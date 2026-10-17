import { systemLabel } from './system-labels.js?v=1';
import { formatSourceName } from './source-names.js?v=1';

/* Reuse map: global workspace composition; ui-button/ui-audio-visibility/ui-disclosure-button/ui-icon-button, ui-segmented, ui-select,
 * ui-range, ui-feedback, ui-fact-list, ui-select-list-item, ui-toggle-field and ui-section-disclosure. Existing scanner
 * and recording-choice modal retain their lifecycle. One live engine and one
 * recording adapter; the dock owns presentation only, in light/dark and all sizes. */
export function createAudioDock({ node, iconButton, uiToggleField, recordings, getLivePlayer, access, openRecordings,
  entityRefHref, canViewRadio, href, getTitlePreference, setTitlePreference,
  getSourceNameDisplay = () => 'talker_alias',
  radioIdentifier = (_call, value) => value }) {
  const dock = node('section', 'audio-dock ui-audio-surface');
  dock.id = 'audio-dock';
  dock.setAttribute('aria-label', 'Audio player');
  const presentation = node('div', 'audio-dock-presentation');
  presentation.id = 'audio-dock-presentation';
  let size = 'minimal';
  // Desktop hiding is presentation only: retain the size, source and both engines.
  let superCollapsed = false;
  const mobileLayout = window.matchMedia('(max-width: 900px), (max-height: 500px) and (pointer: coarse)');
  let source = 'live';
  let panel = 'details';
  let liveState = {};
  let recordingState = recordings.viewState();
  let previousRecordingActive = false;
  let previousLiveActive = false;
  let boundLive = null;
  let unsubscribeLive = () => {};
  let panelKey = '';
  let renderedPanel = '';
  const disclosureStates = new Map();
  const lastVolume = { live: 1, recordings: 1 };
  let sharedQueueKey = '';

  const command = (label, icon, action, primary = false) => {
    const button = iconButton(`icon-${icon}`, label,
      `ui-button ui-button-secondary ui-icon-button ui-audio-command${primary ? ' ui-audio-command-primary' : ''}`);
    button.addEventListener('click', action);
    return button;
  };
  const textButton = (label, action) => {
    const button = node('button', 'ui-button', label);
    button.type = 'button';
    button.addEventListener('click', action);
    return button;
  };
  const setLabel = (button, label, icon) => {
    button.setAttribute('aria-label', label);
    button.title = label;
    if (icon) button.querySelector('use')?.setAttribute('href', `#icon-${icon}`);
  };
  const state = () => source === 'live' ? liveState : recordingState;
  const current = () => state().displayCall || state().current;
  const identityType = (form, fallback = 'ID') =>
    ({ TALKGROUP: 'TGID', PATCH_GROUP: 'Patch', RADIO: 'Radio' })[String(form || '').toUpperCase()] || fallback;
  const sourceId = (call) => {
    const value = call?.source_id ?? call?.radio_id;
    return value === undefined || value === null || value === '' ? value : radioIdentifier(call, value, 'source');
  };
  const targetId = (call) => String(call?.target_form || '').toUpperCase() === 'RADIO' || call?.target_canonical_identity ||
    call?.destination_radio_id !== undefined && call?.destination_radio_id !== null ?
    radioIdentifier(call, call?.target_id ?? call?.destination_radio_id, 'target') : call?.target_id;
  const title = (call) => {
    if (!call) return 'Nothing queued';
    const analog = ['NBFM', 'AM'].includes(String(call.decoder || call.protocol || '').toUpperCase());
    if (analog) return call.channel_name || call.channel || call.playback_target?.label || 'Recorded channel';
    const alias = call.talkgroup_alias || call.group_alias || call.destination_radio_alias || call.target_alias || call.target_display;
    if (alias) return String(alias);
    const target = call.talkgroup_id ?? call.group_id ?? call.destination_radio_id ?? call.target_id;
    const kind = call.call_type === 'PATCH' ? 'Patch' : call.destination_radio_id !== undefined &&
      call.destination_radio_id !== null ? 'Radio' : identityType(call.target_form,
        call.talkgroup_id !== undefined && call.talkgroup_id !== null ? 'TGID' : 'ID');
    if (target !== undefined && target !== null && target !== '') return `${kind} ${kind === 'Radio' ? targetId(call) : target}`;
    return call.playback_target?.label || call.channel_name || call.channel || 'Recorded call';
  };
  const sourceName = (call) => formatSourceName(call?.source_alias || call?.radio_alias,
    call?.source_ota_alias || call?.source_ota_ta || call?.talker_alias || call?.ota_alias, getSourceNameDisplay()) ||
    (call?.source_id !== undefined && call?.source_id !== null ? `Radio ${sourceId(call)}` : '');
  const sourceSummary = (call) => {
    if (!call) return '';
    const alias = call.source_alias || call.radio_alias;
    const id = sourceId(call);
    const ota = call.source_ota_alias || call.source_ota_ta || call.talker_alias || call.ota_alias;
    const name = formatSourceName(alias, ota, getSourceNameDisplay());
    return [name, id !== undefined && id !== null && id !== '' ? `${identityType(call.source_form, 'Radio')} ${id}` : '']
      .filter(Boolean).join(' · ');
  };
  const time = (seconds) => {
    const value = Math.max(0, Math.floor(Number(seconds) || 0));
    return `${Math.floor(value / 60).toString().padStart(2, '0')}:${(value % 60).toString().padStart(2, '0')}`;
  };
  const date = (call) => {
    const value = call?.started_at_ms ?? call?.start_ms;
    return Number.isFinite(Number(value)) && Number(value) > 0 ? new Date(Number(value)).toLocaleString() : '';
  };
  const description = (call) => {
    const text = String(call?.target_description || call?.talkgroup_description || call?.group_description ||
      call?.destination_radio_description || '').trim();
    return text && text.toLocaleLowerCase() !== String(title(call)).trim().toLocaleLowerCase() ? text : '';
  };
  const subtitle = (call) => [sourceSummary(call), call?.channel || call?.channel_name]
    .filter(Boolean).join(' · ');
  const queueCount = () => source === 'live' ? Number(liveState.queuedCount) || 0 : recordingState.queue?.length || 0;
  const isAllowed = () => Boolean(access()[source]);
  const setSize = (value) => {
    const returnFocus = dock.contains(document.activeElement);
    size = value; panelKey = ''; render();
    if (returnFocus) handle.focus({ preventScroll: true });
  };
  const sizes = ['collapsed', 'minimal', 'full'];
  const shiftSize = (direction) => setSize(sizes[Math.max(0, Math.min(2, sizes.indexOf(size) + direction))]);
  const switchSource = (value) => {
    if (source === value || !access()[value]) return;
    if (source === 'recordings') recordings.stop();
    else if (getLivePlayer() && !liveState.stopped) void getLivePlayer().togglePlayback();
    source = value;
    if (panel === 'transcript' && source === 'live') panel = 'details';
    if (['listening', 'settings'].includes(panel) && source === 'recordings') panel = 'details';
    panelKey = '';
    render();
  };
  const toggle = () => {
    if (!isAllowed()) return;
    if (source === 'recordings') void recordings.toggle();
    else {
      const player = getLivePlayer();
      if (!player) return;
      if (liveState.stopped) void player.togglePlayback();
      else void player.togglePause();
    }
  };
  const next = () => source === 'recordings' ? void recordings.next() : getLivePlayer()?.skip();
  const changeVolume = (value, persist = true) => {
    const bounded = Math.max(0, Math.min(1, Number(value)));
    if (!Number.isFinite(bounded)) return;
    if (bounded > 0) lastVolume[source] = bounded;
    if (source === 'recordings') recordings.setVolume(bounded);
    else getLivePlayer()?.setVolume(bounded, persist);
    render();
  };
  const mute = () => {
    const volume = Number(state().volume);
    if (volume > 0) { lastVolume[source] = volume; changeVolume(0); }
    else changeVolume(lastVolume[source] || 1);
  };

  const handle = node('button', 'ui-audio-handle');
  handle.type = 'button';
  handle.setAttribute('aria-label', 'Change audio player size');
  handle.setAttribute('aria-controls', 'audio-dock-content');
  handle.setAttribute('aria-describedby', 'audio-dock-size-hint');
  const grip = node('span', 'ui-audio-grip');
  grip.setAttribute('aria-hidden', 'true');
  const sizeHint = node('span', 'visually-hidden');
  sizeHint.id = 'audio-dock-size-hint';
  const collapsedTitle = node('strong', 'audio-dock-single-line');
  const collapsedMeta = node('span', 'audio-dock-meta audio-dock-collapsed-meta');
  handle.append(grip, sizeHint);
  let gesture = null;
  let suppressClick = false;
  handle.addEventListener('pointerdown', (event) => {
    if (!event.isPrimary || event.button !== 0) return;
    suppressClick = false;
    gesture = { id: event.pointerId, x: event.clientX, y: event.clientY };
    handle.setPointerCapture(event.pointerId);
  });
  handle.addEventListener('pointermove', (event) => {
    if (!gesture || gesture.id !== event.pointerId) return;
    if (Math.hypot(event.clientX - gesture.x, event.clientY - gesture.y) > 8) {
      dock.dataset.dragging = 'true';
      suppressClick = true;
    }
  });
  handle.addEventListener('pointerup', (event) => {
    if (!gesture || gesture.id !== event.pointerId) return;
    const vertical = event.clientY - gesture.y;
    const horizontal = event.clientX - gesture.x;
    gesture = null;
    delete dock.dataset.dragging;
    if (handle.hasPointerCapture(event.pointerId)) handle.releasePointerCapture(event.pointerId);
    if (Math.abs(vertical) >= 32 && Math.abs(vertical) > Math.abs(horizontal)) {
      suppressClick = true;
      shiftSize(vertical < 0 ? 1 : -1);
    }
  });
  const cancelGesture = () => {
    if (gesture) suppressClick = true;
    gesture = null;
    delete dock.dataset.dragging;
  };
  handle.addEventListener('pointercancel', cancelGesture);
  handle.addEventListener('lostpointercapture', cancelGesture);
  handle.addEventListener('click', (event) => {
    if (suppressClick && event.detail !== 0) { suppressClick = false; return; }
    suppressClick = false;
    setSize(sizes[(sizes.indexOf(size) + 1) % sizes.length]);
  });
  handle.addEventListener('keydown', (event) => {
    if (!['ArrowUp', 'ArrowDown', 'Home', 'End'].includes(event.key)) return;
    event.preventDefault();
    if (event.key === 'Home' || event.key === 'End') setSize(event.key === 'Home' ? 'collapsed' : 'full');
    else shiftSize(event.key === 'ArrowUp' ? 1 : -1);
  });

  const header = node('div', 'audio-dock-header');
  const sourceButtons = node('div', 'ui-segmented ui-audio-sources');
  sourceButtons.setAttribute('role', 'group');
  sourceButtons.setAttribute('aria-label', 'Audio source');
  const chooseSource = (value) => {
    if (!access()[value] || (value === 'recordings' && openRecordings?.() === false)) {
      render();
      return;
    }
    if (source === value) render();
    else switchSource(value);
  };
  const sourcePicker = node('select', 'ui-select ui-audio-source-picker');
  sourcePicker.setAttribute('aria-label', 'Audio source');
  // A source choice is also a navigation action. The hidden current label lets
  // choosing Recordings navigate even when that source is already selected.
  const currentSourceChoice = node('option');
  currentSourceChoice.value = ''; currentSourceChoice.hidden = true;
  sourcePicker.append(currentSourceChoice);
  sourcePicker.addEventListener('change', () => chooseSource(sourcePicker.value));
  const sourceControls = {};
  const sourceOptions = {};
  ['live', 'recordings'].forEach((value) => {
    const label = value === 'live' ? 'Live' : 'Recordings';
    const button = node('button', 'ui-segmented-option', label);
    button.type = 'button';
    button.addEventListener('click', () => chooseSource(value));
    const option = node('option', '', label);
    option.value = value;
    sourceControls[value] = button;
    sourceOptions[value] = option;
    sourceButtons.append(button);
    sourcePicker.append(option);
  });
  let focusedSourceControl = null;
  [sourceButtons, sourcePicker].forEach((control) => {
    control.addEventListener('focusin', (event) => { focusedSourceControl = event.target; });
    control.addEventListener('focusout', (event) => {
      // Resizing can hide the focused control before the media-query callback.
      // Retain that focus long enough to transfer it to the visible counterpart.
      if (event.target.getClientRects().length) focusedSourceControl = null;
    });
  });
  const compactSourceLayout = window.matchMedia('(max-width: 359px)');
  const updateSourceFocus = () => {
    if (dock.hidden || (superCollapsed && !mobileLayout.matches) || size === 'collapsed') return;
    if (compactSourceLayout.matches && sourceButtons.contains(focusedSourceControl)) {
      sourcePicker.focus({ preventScroll: true });
    } else if (!compactSourceLayout.matches && focusedSourceControl === sourcePicker) {
      sourceControls[source].focus({ preventScroll: true });
    }
  };
  compactSourceLayout.addEventListener('change', updateSourceFocus);
  const count = textButton('Queue 0', () => { panel = 'queue'; setSize('full'); });
  count.classList.add('ui-audio-count');
  const headerStart = node('div', 'audio-dock-header-start');
  const headerEnd = node('div', 'audio-dock-header-end');
  const setSuperCollapsed = (value) => {
    if (mobileLayout.matches) return;
    superCollapsed = value;
    render();
    (value ? restore : hide).focus({ preventScroll: true });
  };
  const hide = iconButton('icon-close', 'Hide audio player',
    'ui-disclosure-button ui-icon-button ui-audio-dismiss');
  hide.title = 'Hide audio player. Audio keeps playing.';
  hide.setAttribute('aria-controls', presentation.id);
  hide.addEventListener('click', () => setSuperCollapsed(true));
  const restore = iconButton('icon-chevron-down', 'Show audio player',
    'ui-button ui-button-primary ui-audio-visibility ui-audio-restore');
  restore.prepend(node('span', '', 'Player'));
  restore.setAttribute('aria-controls', presentation.id);
  restore.addEventListener('click', () => setSuperCollapsed(false));
  let focusedPresentationControl = null;
  [hide, restore].forEach((control) => {
    control.addEventListener('focusin', () => { focusedPresentationControl = control; });
    control.addEventListener('focusout', () => {
      if (control.getClientRects().length) focusedPresentationControl = null;
    });
  });
  const updatePresentationLayout = () => {
    const returnFocus = dock.contains(document.activeElement) || Boolean(focusedPresentationControl || focusedSourceControl);
    render();
    if (!returnFocus || dock.hidden) return;
    if (superCollapsed && !mobileLayout.matches) restore.focus({ preventScroll: true });
    else if (focusedPresentationControl || !document.activeElement.getClientRects().length) {
      handle.focus({ preventScroll: true });
      focusedPresentationControl = null;
    }
  };
  headerStart.append(collapsedTitle, sourceButtons, sourcePicker);
  headerEnd.append(collapsedMeta, count, hide);
  header.append(headerStart, handle, headerEnd);

  const body = node('div', 'audio-dock-body');
  body.id = 'audio-dock-content';
  const now = node('div', 'audio-dock-now');
  const copy = node('div', 'audio-dock-copy');
  const nowTitle = node('strong', 'audio-dock-title');
  const nowDescription = node('span', 'audio-dock-description');
  const nowSubtitle = node('span', 'audio-dock-meta');
  const nowSystem = node('span', 'audio-dock-meta audio-dock-system');
  const status = node('span', 'audio-dock-status');
  status.setAttribute('role', 'status');
  copy.append(nowTitle, nowDescription, nowSubtitle, nowSystem, status);
  const miniControls = node('div', 'audio-dock-mini-controls');
  const miniPlay = command('Play audio', 'play', toggle, true);
  const miniNext = command('Next call', 'skip', next);
  const miniMute = command('Mute audio', 'speaker', mute);
  miniControls.append(miniPlay, miniNext, miniMute);
  now.append(copy, miniControls);

  const timing = node('div', 'audio-dock-timing');
  const elapsed = node('span', 'audio-dock-time');
  const seek = node('input', 'ui-range ui-audio-range');
  seek.type = 'range'; seek.min = '0'; seek.max = '1'; seek.step = '0.1'; seek.value = '0';
  seek.setAttribute('aria-label', 'Recording position');
  seek.addEventListener('input', () => recordings.seek(Number(seek.value)));
  const progress = node('progress', 'ui-audio-progress');
  progress.max = 1; progress.value = 0; progress.setAttribute('aria-label', 'Live call progress');
  const duration = node('span', 'audio-dock-time');
  timing.append(elapsed, seek, progress, duration);

  const transport = node('div', 'audio-dock-transport');
  transport.setAttribute('role', 'group');
  transport.setAttribute('aria-label', 'Audio controls');
  const previous = command('Previous recording', 'previous', () => void recordings.previous());
  const rewind = command('Back 10s', 'rewind', () => recordings.skipSeconds(-10));
  const replay = command('Replay last call', 'replay', () => void getLivePlayer()?.replayLastCall());
  const play = command('Play audio', 'play', toggle, true);
  const forward = command('Forward 10s', 'forward', () => recordings.skipSeconds(10));
  const skip = command('Next call', 'skip', next);
  const stop = command('Stop recording', 'stop', () => recordings.stop());
  transport.append(previous, rewind, replay, play, forward, skip, stop);

  const volumeRow = node('div', 'audio-dock-volume');
  const volumeMute = command('Mute audio', 'speaker', mute);
  const volumeLabel = node('label', 'audio-dock-meta', 'Volume');
  const volume = node('input', 'ui-range ui-audio-range');
  volume.id = 'audio-dock-volume'; volume.type = 'range'; volume.min = '0'; volume.max = '1'; volume.step = '0.05';
  volumeLabel.htmlFor = volume.id;
  volume.addEventListener('input', () => changeVolume(volume.value, false));
  volume.addEventListener('change', () => { if (source === 'live') getLivePlayer()?.writePreferences({ volume: true }); });
  const volumeOutput = node('output', 'audio-dock-meta');
  volumeOutput.htmlFor = volume.id;
  volumeRow.append(volumeMute, volumeLabel, volume, volumeOutput);

  const actions = node('div', 'audio-dock-actions');
  const stopLive = textButton('Stop listening', () => void getLivePlayer()?.togglePlayback());
  const hold = textButton('Hold', () => getLivePlayer()?.toggleHold());
  const avoid = textButton('Avoid', () => getLivePlayer()?.avoidCurrent());
  const copyLink = textButton('Copy call link', () => void share(false, true));
  const shareCall = textButton('Share call', () => void share(true, true));
  actions.append(stopLive, hold, avoid, copyLink, shareCall);

  const tabs = node('div', 'ui-audio-tabs');
  tabs.setAttribute('role', 'tablist'); tabs.setAttribute('aria-label', 'Audio information');
  const tabButtons = {};
  const panelHost = node('div', 'audio-dock-panel');
  panelHost.id = 'audio-dock-panel'; panelHost.setAttribute('role', 'tabpanel'); panelHost.tabIndex = 0;
  Object.entries({ details: 'Details', transcript: 'Transcript', queue: 'Queue', listening: 'Scan Lists',
    settings: 'Settings' }).forEach(([value, label]) => {
    const button = node('button', 'ui-audio-tab', label);
    button.type = 'button'; button.id = `audio-dock-tab-${value}`;
    button.setAttribute('role', 'tab'); button.setAttribute('aria-controls', panelHost.id);
    button.addEventListener('click', () => { panel = value; panelKey = ''; render(); });
    button.addEventListener('keydown', (event) => {
      if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
      event.preventDefault();
      const available = Object.values(tabButtons).filter((tab) => !tab.hidden);
      const index = available.indexOf(button);
      const target = event.key === 'Home' ? available[0] : event.key === 'End' ? available.at(-1) :
        available[(index + (event.key === 'ArrowRight' ? 1 : -1) + available.length) % available.length];
      target.click(); target.focus();
    });
    tabButtons[value] = button; tabs.append(button);
  });
  body.append(now, timing, transport, volumeRow, actions, tabs, panelHost);
  presentation.append(header, body);
  dock.append(presentation, restore);
  document.querySelector('.app-shell').append(dock);

  const message = (text, error = false) => {
    const item = node('p', `ui-feedback${error ? ' ui-feedback-error' : ''}`, text);
    item.setAttribute('role', error ? 'alert' : 'status');
    return item;
  };
  async function share(useNative, oneCall = false) {
    const call = recordingState.current;
    const path = oneCall && call ? href('recordings', { recording_queue: String(call.id) }) : recordings.queueHref();
    if (!path) return;
    const url = new URL(path, window.location.origin).href;
    try {
      if (useNative && navigator.share) await navigator.share({ title: oneCall ? String(title(call)) : 'Recording queue', url });
      else { await navigator.clipboard.writeText(url); status.textContent = oneCall ? 'Call link copied' : 'Queue link copied'; }
    } catch (error) {
      if (error?.name !== 'AbortError') {
        const link = node('a', 'ui-button', 'Open shareable link'); link.href = path;
        panelHost.prepend(message('The link could not be copied. Open it to copy the address.'), link);
      }
    }
  }
  function factList(rows, { empty = false, includeEmpty = false } = {}) {
    const list = node('dl', 'ui-fact-list audio-dock-facts');
    rows.forEach(([label, value, reference]) => {
      if (!includeEmpty && (value === null || value === undefined || value === '')) return;
      const description = node('dd');
      const hasValue = value !== null && value !== undefined && value !== '';
      const target = !empty && hasValue && reference && canViewRadio() ? entityRefHref(reference) : null;
      if (target) { const link = node('a', '', String(value)); link.href = target; description.append(link); }
      else description.textContent = empty ? '' : String(value ?? '');
      list.append(node('dt', '', label), description);
    });
    return list;
  }
  function group(label, rows, options) {
    const list = factList(rows, options);
    if (!list.children.length) return;
    const disclosure = node('details', 'ui-section-disclosure');
    disclosure.append(node('summary', 'ui-section-summary', label), list);
    panelHost.append(disclosure);
  }
  function renderLiveDetails() {
    const call = liveState.current || {};
    const options = { empty: !liveState.current || liveState.stopped, includeEmpty: true };
    panelHost.append(factList([
      ['Target', title(call), call.target_entity_ref], ['Target ID', targetId(call), call.target_entity_ref],
      ['Source', sourceName(call), call.source_entity_ref], ['Source ID', sourceId(call), call.source_entity_ref],
      ['System', systemLabel(call), call.radio_system_entity_ref], ['Channel', call.channel, call.entity_ref],
      ['Started', date(call)], ['Duration', call.duration_ms !== undefined ? `${Number(call.duration_ms) / 1000}s` : null],
      ['Scan lists', (liveState.scanLists || []).filter((item) => (call._matchedScanListIds || call.scan_list_ids || [])
        .map(String).includes(String(item.id))).map((item) => item.name).join(', ')]
    ], options));
    group('Identity & aliases', [
      ['Target type', call.target_form], ['Target description', call.target_description], ['Target group', call.target_group],
      ['Source type', call.source_form], ['Source description', call.source_description], ['Source group', call.source_group],
      ['Talker Alias', call.talker_alias], ['Alias List', call.alias_list || call.alias_list_name],
      ['Playback target', call.playback_target?.label], ['Patch group', call.patch_group_id]
    ], options);
    group('Radio & channel', [
      ['Protocol', call.protocol], ['Decoder', call.decoder], ['Modulation', call.modulation],
      ['Frequency', Number(call.frequency_hz) > 0 ? `${(Number(call.frequency_hz) / 1e6).toFixed(5)} MHz` : null],
      ['Site', call.site], ['Network ID', call.network_id ?? call.network], ['Site identity', call.site_identity],
      ['Model', call.model ?? call.network_model], ['Location category', call.location_category],
      ['WACN', call.wacn], ['SysID', call.system_id], ['RFSS', call.rfss_id], ['Site ID', call.site_id], ['RAN', call.ran],
      ['NAC', call.nac], ['LCN', call.logical_channel_number ?? call.lcn], ['Timeslot', call.timeslot],
      ['Encrypted', typeof call.encrypted === 'boolean' ? call.encrypted ? 'Yes' : 'No' : null]
    ], options);
    group('Voice quality', [
      ['Quality', call.vc_quality_pct !== undefined ? `${call.vc_quality_pct}%` : null],
      ['Decoded frames', call.vc_decoded_frames], ['Repeated frames', call.vc_repeated_frames],
      ['Concealed frames', call.vc_concealed_frames], ['Missing frames', call.vc_missing_frames],
      ['FEC errors', call.vc_fec_errors], ['FEC protected bits', call.vc_fec_protected_bits]
    ], options);
    group('Call', [
      ['Call ID', call.call_id], ['Identifier', call.id], ['Configuration ID', call.configuration_id, call.entity_ref],
      ['Radio system key', call.radio_system_key, call.radio_system_entity_ref],
      ['Scan List IDs', Array.isArray(call.scan_list_ids) ? call.scan_list_ids.join(', ') : null],
      ['Start timestamp', call.started_at_ms],
      ['Completed', Number.isFinite(Number(call.completed_at_ms)) && Number(call.completed_at_ms) > 0 ?
        new Date(Number(call.completed_at_ms)).toLocaleString() : null],
      ['Completion timestamp', call.completed_at_ms]
    ], options);
  }
  function renderQueue() {
    const tools = node('div', 'audio-dock-queue-tools');
    if (source === 'recordings') {
      const label = node('label', 'audio-dock-meta', 'When a call is clicked');
      const select = node('select', 'ui-select'); select.id = 'audio-dock-click-mode';
      select.setAttribute('aria-label', 'When a call is clicked');
      [['queue', 'Queue clicked calls'], ['once', 'Play once'], ['continue', 'Continue matching calls']].forEach(([value, text]) => {
        const option = node('option', '', text); option.value = value; select.append(option);
      });
      select.value = recordingState.clickMode;
      select.addEventListener('change', () => recordings.setClickMode(select.value));
      label.append(select); tools.append(label);
      const shareQueue = textButton('Share queue', () => void share(true));
      shareQueue.disabled = !recordings.queueHref(); tools.append(shareQueue);
    }
    const clear = textButton('Clear queue', () => source === 'recordings' ? recordings.clearQueue() : getLivePlayer()?.clearQueue());
    clear.disabled = !queueCount() && !state().continuation; tools.append(clear);
    panelHost.append(tools);
    if (recordingState.continuation && source === 'recordings') panelHost.append(message('Continuing through matching calls. More calls are loaded as you listen.'));
    const playingCall = state().current;
    const rows = [playingCall, ...(state().queue || [])].filter(Boolean);
    if (!rows.length) { panelHost.append(message('Queue is empty.')); return; }
    const list = node('ol', 'audio-dock-queue-list');
    rows.forEach((call, index) => {
      const row = node('li', 'audio-dock-queue-row');
      const copy = node('div', 'audio-dock-copy');
      copy.append(node('span', 'audio-dock-meta', index === 0 && playingCall ? state().stopped ? 'Last played' : state().paused ? 'Paused' : 'Now playing' : 'Up next'),
        node('strong', '', String(title(call))), node('span', 'audio-dock-meta', subtitle(call)),
        node('span', 'audio-dock-meta', [date(call), call.duration_ms ? `${Number(call.duration_ms) / 1000}s` : ''].filter(Boolean).join(' · ')));
      if (source === 'live') {
        const frequency = Number(call.frequency_hz);
        const names = (liveState.scanLists || []).filter((item) => (call._matchedScanListIds || call.scan_list_ids || [])
          .map(String).includes(String(item.id))).map((item) => item.name).join(', ');
        copy.append(node('span', 'audio-dock-meta', [frequency > 0 ? `${(frequency / 1e6).toFixed(5)} MHz` : '',
          call.decoder || call.protocol, call.timeslot !== undefined && call.timeslot !== null ? `Slot ${call.timeslot}` : '',
          typeof call.encrypted === 'boolean' ? call.encrypted ? 'Encrypted' : 'Unencrypted' : '', names]
          .filter(Boolean).join(' · ')));
      }
      row.append(copy);
      if (source === 'recordings' && !(index === 0 && playingCall)) {
        const entryKey = call._queueEntryKey || call.id;
        row.append(command(`Play ${title(call)}`, 'play', () => recordings.playQueued(entryKey)),
          command(`Remove ${title(call)} from queue`, 'close', () => recordings.removeQueued(entryKey)));
      }
      list.append(row);
    });
    panelHost.append(list);
  }
  function renderListening() {
    const lists = liveState.scanLists || [];
    const maximum = liveState.maximumSelectedScanLists || 16;
    const selected = lists.filter((item) => item.selected).length;
    panelHost.append(node('p', 'audio-dock-meta audio-dock-selection-summary', `${selected} selected · Up to ${maximum} lists`));
    if (!liveState.scanListCatalogReady) panelHost.append(message(liveState.scanListCatalogState === 'unavailable' ?
      'Scan lists are unavailable. Try listening again when the receiver is available.' : 'Scan lists are loading…'));
    else if (!lists.length) panelHost.append(message('No scan lists are available.'));
    const choices = node('div');
    lists.forEach((item) => {
      const label = node('label', 'ui-select-list-item');
      const checkbox = node('input', 'ui-selection-check'); checkbox.type = 'checkbox'; checkbox.checked = item.selected;
      checkbox.id = `audio-dock-scan-list-${item.id}`;
      checkbox.disabled = !item.enabled || (selected >= maximum && !item.selected);
      checkbox.addEventListener('change', () => getLivePlayer()?.setScanListSelected(item.id, checkbox.checked));
      const copy = node('span', 'audio-dock-copy'); copy.append(node('strong', '', item.name));
      if (item.description) copy.append(node('small', 'muted', item.description));
      label.append(checkbox, copy); choices.append(label);
    });
    panelHost.append(choices);
    const avoidList = node('section', 'audio-dock-subsection');
    avoidList.append(node('h3', '', 'Avoid List'));
    panelHost.append(avoidList);
    const avoids = liveState.avoids || [];
    if (!avoids.length) avoidList.append(message('No targets avoided.'));
    avoids.forEach((item) => {
      const row = node('div', 'audio-dock-queue-row');
      const copy = node('div', 'audio-dock-copy');
      copy.append(node('strong', '', item.label || item.key), node('span', 'muted',
        systemLabel(item) || item.system_scope || item.systemName || 'All systems'));
      row.append(copy, command(`Remove ${item.label || item.key} from avoid list`, 'close', () => getLivePlayer()?.removeAvoid(item.key)));
      avoidList.append(row);
    });
  }
  function renderSettings() {
    const settings = node('div', 'audio-dock-settings');
    panelHost.append(settings);
    const groupField = uiToggleField('Group calls by target', liveState.targetGrouping !== false);
    const grouping = groupField.querySelector('input'); grouping.id = 'audio-dock-target-grouping';
    grouping.addEventListener('change', () => {
      const player = getLivePlayer(); if (!player) return;
      player.applyPreferences({ target_grouping: grouping.checked }); player.writePreferences();
    });
    settings.append(groupField);
    const burstLabel = node('label', 'ui-field', 'Calls per target');
    const burst = node('input', 'ui-input'); burst.id = 'audio-dock-target-burst'; burst.type = 'number'; burst.min = '1'; burst.max = '20'; burst.value = liveState.targetBurstLimit || 4;
    burst.setAttribute('aria-label', 'Calls per target');
    burst.addEventListener('change', () => {
      if (!burst.checkValidity()) { burst.reportValidity(); return; }
      const player = getLivePlayer(); if (!player) return;
      player.applyPreferences({ target_burst_limit: Number(burst.value) }); player.writePreferences();
    });
    burstLabel.append(burst); settings.append(burstLabel);
    const titleField = uiToggleField('Playing call in page title', getTitlePreference());
    const prepend = titleField.querySelector('input'); prepend.id = 'audio-dock-title-preference';
    prepend.addEventListener('change', () => {
      const refresh = () => { panelKey = ''; render(); };
      void Promise.resolve(setTitlePreference(prepend.checked)).then(refresh, refresh);
    });
    settings.append(titleField);
  }
  function updateProgress() {
    if (dock.hidden || (superCollapsed && !mobileLayout.matches) || size === 'collapsed') return;
    const snapshot = source === 'live' ? getLivePlayer()?.viewState() || liveState : recordings.viewState();
    const total = Math.max(0, Number(snapshot.duration) || 0);
    const position = Math.max(0, Number(snapshot.currentTime) || 0);
    elapsed.textContent = time(position); duration.textContent = time(total);
    progress.value = total ? Math.min(1, position / total) : 0;
    seek.max = String(total || 1);
    if (document.activeElement !== seek) seek.value = String(Math.min(position, total));
    seek.disabled = !isAllowed() || !total;
    seek.setAttribute('aria-valuetext', `${time(position)} of ${time(total)}`);
  }
  function render() {
    const permissions = access();
    if (!permissions[source] && (permissions.live || permissions.recordings)) {
      source = permissions.live ? 'live' : 'recordings';
    }
    dock.hidden = document.body.dataset.view === 'scanner' || document.body.dataset.view === 'access-landing' ||
      (!permissions.live && !permissions.recordings);
    const compact = superCollapsed && !mobileLayout.matches;
    dock.dataset.superCollapsed = String(compact);
    dock.dataset.state = size; dock.dataset.source = source;
    document.body.classList.toggle('has-audio-dock', !dock.hidden);
    presentation.hidden = compact;
    restore.hidden = !compact;
    hide.hidden = mobileLayout.matches;
    [hide, restore].forEach((button) => button.setAttribute('aria-expanded', String(!compact)));
    if (dock.hidden) focusedPresentationControl = null;
    collapsedTitle.hidden = collapsedMeta.hidden = size !== 'collapsed';
    sourceButtons.hidden = sourcePicker.hidden = count.hidden = size === 'collapsed';
    body.hidden = compact || size === 'collapsed';
    if (dock.hidden || compact || size === 'collapsed') focusedSourceControl = null;
    handle.setAttribute('aria-expanded', String(size !== 'collapsed'));
    const sizeDescription = { collapsed: 'Collapsed player', minimal: 'Minimal controls', full: 'Full controls' }[size];
    sizeHint.textContent = `${sizeDescription}. Drag up or down, or click to change player size.`;
    handle.title = sizeHint.textContent;
    const call = current(); const active = state();
    const name = String(title(call));
    collapsedTitle.textContent = name;
    collapsedMeta.textContent = `${source === 'live' ? 'Live audio' : 'Recording'} · ${queueCount()} queued`;
    collapsedTitle.title = name;
    count.textContent = `Queue ${queueCount()}`;
    count.title = `${queueCount()} calls queued`;
    Object.entries(sourceControls).forEach(([value, button]) => {
      button.disabled = !permissions[value]; button.setAttribute('aria-pressed', String(source === value));
      sourceOptions[value].disabled = !permissions[value];
    });
    const sourceLabel = source === 'live' ? 'Live' : 'Recordings';
    if (currentSourceChoice.textContent !== sourceLabel) currentSourceChoice.textContent = sourceLabel;
    if (sourcePicker.value !== '') sourcePicker.value = '';
    sourcePicker.disabled = !permissions.live && !permissions.recordings;
    nowTitle.textContent = name; nowTitle.title = name;
    nowDescription.textContent = description(call);
    nowDescription.hidden = size !== 'full' || !nowDescription.textContent;
    nowSubtitle.textContent = subtitle(call) || (source === 'live' ? 'Select a scan list' : 'Choose a recording to play');
    nowSubtitle.title = nowSubtitle.textContent;
    nowSystem.textContent = call ? systemLabel(call) : '';
    nowSystem.hidden = size !== 'full' || !nowSystem.textContent;
    status.textContent = [active.status, size === 'full' ? date(call) : ''].filter(Boolean).join(' · ');
    const playing = source === 'live' ? !active.stopped && !active.paused : active.playing;
    const playLabel = playing ? source === 'live' ? 'Pause live audio' : 'Pause recording' : source === 'live' ?
      active.paused ? 'Resume live audio' : 'Listen live' : 'Play recording';
    [play, miniPlay].forEach((button) => {
      setLabel(button, playLabel, playing ? 'pause' : 'play');
      button.disabled = !isAllowed() || (source === 'recordings' && !active.current && !queueCount());
    });
    [miniNext, skip].forEach((button) => { button.disabled = !isAllowed() || (source === 'recordings' ?
      !active.canNext : !active.current && !queueCount()); });
    previous.disabled = !active.canPrevious;
    rewind.disabled = forward.disabled = !active.current || !active.duration;
    replay.disabled = !liveState.lastCallReady || liveState.paused;
    stop.disabled = !active.current && !queueCount();
    [previous, rewind, forward, stop].forEach((button) => { button.hidden = source !== 'recordings'; });
    replay.hidden = source !== 'live';
    transport.hidden = volumeRow.hidden = actions.hidden = tabs.hidden = panelHost.hidden = size !== 'full';
    miniControls.hidden = size === 'full';
    seek.hidden = source !== 'recordings'; progress.hidden = source !== 'live';
    stopLive.hidden = hold.hidden = avoid.hidden = source !== 'live';
    copyLink.hidden = shareCall.hidden = source !== 'recordings';
    stopLive.disabled = active.stopped;
    hold.disabled = !liveState.currentReady && !liveState.holdTarget;
    hold.textContent = liveState.holdTarget ? 'Release hold' : 'Hold';
    hold.setAttribute('aria-pressed', String(Boolean(liveState.holdTarget)));
    avoid.disabled = !liveState.currentReady || liveState.replayingLast;
    copyLink.disabled = shareCall.disabled = !recordingState.current;
    const muted = !(Number(active.volume) > 0);
    [miniMute, volumeMute].forEach((button) => { setLabel(button, muted ? 'Unmute audio' : 'Mute audio', muted ? 'speaker-muted' : 'speaker'); button.setAttribute('aria-pressed', String(muted)); button.disabled = !isAllowed(); });
    if (document.activeElement !== volume) volume.value = String(active.volume ?? 1);
    volumeOutput.textContent = `${Math.round(Number(active.volume ?? 1) * 100)}%`;
    if ((source === 'live' && panel === 'transcript') || (source === 'recordings' && ['listening', 'settings'].includes(panel))) panel = 'details';
    Object.entries(tabButtons).forEach(([value, button]) => {
      button.hidden = (value === 'transcript' && source !== 'recordings') ||
        (['listening', 'settings'].includes(value) && source !== 'live');
      button.setAttribute('aria-selected', String(panel === value)); button.tabIndex = panel === value ? 0 : -1;
    });
    panelHost.setAttribute('aria-labelledby', tabButtons[panel].id);
    if (size === 'full' && !dock.hidden && !compact) {
      const panelState = panel === 'listening' ? [active.scanLists, active.avoids,
        active.maximumSelectedScanLists, active.scanListCatalogReady, active.scanListCatalogState] :
        panel === 'settings' ? [liveState.targetGrouping, liveState.targetBurstLimit, getTitlePreference()] :
        panel === 'queue' ? [active.current, active.queue,
        active.scanLists, active.clickMode, active.continuation, active.stopped, active.paused] : panel === 'transcript' ?
        [call?.id, call?.transcription, active.detailsLoading, active.detailsError] :
        [active.current, active.stopped, active.scanLists, active.detailsLoading, active.detailsError];
      const nextKey = JSON.stringify([source, panel, getSourceNameDisplay(), ...panelState]);
      if (panelKey !== nextKey) {
        panelKey = nextKey;
        const scrollTop = panelHost.scrollTop;
        const focused = panelHost.contains(document.activeElement) ? document.activeElement : null;
        const focusId = focused?.id;
        const focusName = focused?.getAttribute('aria-label') || focused?.textContent;
        const focusTag = focused?.tagName;
        panelHost.querySelectorAll('details').forEach((item) => {
          disclosureStates.set(`${renderedPanel}:${item.querySelector('summary')?.textContent}`, item.open);
        });
        panelHost.replaceChildren();
        renderedPanel = `${source}:${panel}`;
        if (panel === 'details') {
          if (source === 'recordings') {
            if (active.detailsLoading) panelHost.append(message('Loading call details…'));
            if (active.detailsError) panelHost.append(message(active.detailsError, true));
            recordings.renderDetails(panelHost, active.current, { shell: true, empty: !active.current || active.stopped });
          } else renderLiveDetails();
        } else if (panel === 'transcript') recordings.renderTranscript(panelHost);
        else if (panel === 'queue') renderQueue();
        else if (panel === 'listening') renderListening();
        else renderSettings();
        panelHost.querySelectorAll('details').forEach((item) => {
          const key = `${renderedPanel}:${item.querySelector('summary')?.textContent}`;
          item.open = disclosureStates.get(key) === true;
          item.addEventListener('toggle', () => {
            if (item.isConnected && panelHost.contains(item)) disclosureStates.set(key, item.open);
          });
        });
        if (focused) {
          const restored = [...panelHost.querySelectorAll('button, input, select, a, summary, [tabindex]')].find((control) =>
            focusId ? control.id === focusId : control.tagName === focusTag &&
              (control.getAttribute('aria-label') || control.textContent) === focusName);
          (restored || tabButtons[panel]).focus({ preventScroll: true });
        }
        panelHost.scrollTop = scrollTop;
      }
    }
    updateProgress(); measure();
  }
  function measure() {
    // Measured geometry reserves the actual dock height, including safe-area padding.
    const height = dock.hidden ? 0 : Math.ceil(dock.getBoundingClientRect().height);
    document.documentElement.style.setProperty('--audio-dock-height', `${height}px`);
  }
  const resizeObserver = new ResizeObserver(measure); resizeObserver.observe(dock);
  mobileLayout.addEventListener('change', updatePresentationLayout);
  const routeObserver = new MutationObserver(() => { panelKey = ''; render(); });
  routeObserver.observe(document.body, { attributes: true, attributeFilter: ['data-view'] });
  const progressTimer = window.setInterval(() => { if (!document.hidden) updateProgress(); }, 250);
  const unsubscribeRecordings = recordings.subscribeState((value) => {
    recordingState = value;
    const queueOnly = !value.current && value.queue?.length && liveState.stopped !== false;
    const active = Boolean(value.playing || value.loading || queueOnly);
    if (active && !previousRecordingActive && access().recordings) { source = 'recordings'; panelKey = ''; }
    previousRecordingActive = active;
    render();
  });
  function synchronize() {
    if (!access().recordings) sharedQueueKey = '';
    const player = getLivePlayer();
    if (player !== boundLive) {
      unsubscribeLive(); boundLive = player;
      unsubscribeLive = player?.subscribeState((value) => {
        liveState = value;
        if (!value.stopped && !previousLiveActive && access().live) { source = 'live'; panelKey = ''; }
        previousLiveActive = !value.stopped;
        render();
      }) || (() => {});
    }
    render();
    const params = new URLSearchParams(window.location.search);
    const queue = params.get('recording_queue');
    if (access().recordings && queue && queue !== sharedQueueKey) {
      sharedQueueKey = queue; source = 'recordings'; size = 'full'; panel = 'queue';
      void recordings.loadSharedQueue(params).then(render);
    }
  }
  synchronize();
  return { synchronize, destroy() {
    compactSourceLayout.removeEventListener('change', updateSourceFocus);
    mobileLayout.removeEventListener('change', updatePresentationLayout);
    unsubscribeLive(); unsubscribeRecordings(); resizeObserver.disconnect(); routeObserver.disconnect();
    window.clearInterval(progressTimer); dock.remove(); document.body.classList.remove('has-audio-dock');
    document.documentElement.style.removeProperty('--audio-dock-height');
  } };
}
