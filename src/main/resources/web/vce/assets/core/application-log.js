const LOG_PATH = '/api/v1/application-log';
const POLL_MILLISECONDS = 2_000;
const LEVEL_OPTIONS = [
  ['ALL', 'All levels'], ['WARN', 'Warnings & errors'], ['ERROR', 'Errors only'],
  ['INFO', 'Information'], ['DEBUG', 'Debug']
];

export function filterApplicationLogEntries(entries, search = '', level = 'ALL') {
  const query = String(search).trim().toLocaleLowerCase();
  return entries.filter((entry) => {
    const severity = String(entry.level || '').toUpperCase();
    const matchesLevel = level === 'ALL' || severity === level ||
      (level === 'WARN' && severity === 'ERROR');
    return matchesLevel && (!query || [entry.time, entry.level, entry.source, entry.message,
      entry.details, entry.text].some((value) => String(value || '').toLocaleLowerCase().includes(query)));
  });
}

export function applicationLogShownText(entries) {
  return entries.map((entry) => entry.text || [entry.time, entry.level, entry.source,
    entry.message, entry.details].filter(Boolean).join('\n')).join('\n');
}

export function mergeApplicationLogSnapshot(previous, next) {
  const incoming = next.entries.map((entry) => ({ ...entry, text: entry.text ??
    (entry.header_prefix !== undefined ?
      `${entry.header_prefix}${entry.message}${entry.multiline || entry.details ? `\n${entry.details || ''}` : ''}` : undefined) }));
  if (!next.incremental || !previous || next.gap || previous.log !== next.log) return { ...next, entries: incoming };
  const entries = new Map(previous.entries.map((entry) => [entry.id, entry]));
  incoming.forEach((entry) => entries.set(entry.id, entry));
  const floor = /^([0-9a-f]{16}):([0-9a-f]+)$/.exec(String(next.first_id || ''));
  const retained = [...entries.values()].filter((entry) => {
    const id = /^([0-9a-f]{16}):([0-9a-f]+)$/.exec(String(entry.id || ''));
    return floor && id && id[1] === floor[1] && BigInt(`0x${id[2]}`) >= BigInt(`0x${floor[2]}`);
  }).slice(-Math.max(1, Number(next.max_entries) || 500));
  return { ...next, entries: retained };
}

// Reuse map: Administration owns the settings workspace and navigation. Shared
// ui-field/select/input, ui-action-row/buttons, ui-selection-check, ui-status,
// ui-notice/feedback and disclosure controls own appearance and keyboard behavior.
// Message geometry alone adapts to desktop/mobile; shared tokens handle light/dark.
export function createApplicationLogWorkspace(deps) {
  const { node, formField, uiSelectFrame, uiStatus, iconButton, setIconButton,
    uiActionButton, api, signal } = deps;
  const element = node('div', 'application-log-workspace');
  const controls = node('div', 'application-log-controls');
  const search = node('input', 'ui-input');
  search.type = 'search';
  search.placeholder = 'Channel name, tuner, error text…';
  search.autocomplete = 'off';
  search.setAttribute('aria-label', 'Search messages');
  const searchField = formField('Search messages', search, 'Searches loaded messages and their details.');
  searchField.classList.add('application-log-search');
  const select = (options) => {
    const control = node('select', 'ui-select');
    options.forEach(([value, label]) => {
      const option = node('option', '', label);
      option.value = value;
      control.append(option);
    });
    return control;
  };
  const level = select(LEVEL_OPTIONS);
  const savedLog = select([['current', 'Current log'], ['previous', 'Previous log']]);
  level.setAttribute('aria-label', 'Level');
  savedLog.setAttribute('aria-label', 'Saved log');
  const stateHost = node('span', 'application-log-state');
  stateHost.setAttribute('role', 'status');
  const pause = iconButton('icon-pause', 'Pause log updates');
  pause.setAttribute('aria-pressed', 'false');
  const refreshButton = iconButton('icon-refresh', 'Refresh application log');
  const liveControls = node('div', 'ui-action-row application-log-live-controls');
  liveControls.append(stateHost, pause, refreshButton);
  controls.append(searchField, formField('Level', uiSelectFrame(level)),
    formField('Saved log', uiSelectFrame(savedLog)), liveControls);

  const options = node('div', 'application-log-options');
  const check = (label, checked) => {
    const wrapper = node('label', 'application-log-option ui-label');
    const input = node('input', 'ui-selection-check');
    input.type = 'checkbox';
    input.checked = checked;
    wrapper.append(input, node('span', '', label));
    options.append(wrapper);
    return input;
  };
  const follow = check('Follow newest', true);
  const expand = check('Expand details', false);
  const exportActions = node('div', 'ui-action-row');
  const copy = uiActionButton('Copy shown', 'icon-copy', () => void copyShown());
  const download = uiActionButton('Download shown', 'icon-download', downloadShown);
  exportActions.append(copy, download);
  const actions = node('div', 'application-log-actions');
  actions.append(options, exportActions);

  const notice = node('div', 'ui-notice ui-notice-warning');
  notice.hidden = true;
  notice.setAttribute('role', 'status');
  const errorNotice = node('div', 'ui-notice ui-notice-warning');
  errorNotice.hidden = true;
  errorNotice.setAttribute('role', 'status');
  const heading = node('div', 'application-log-heading ui-surface-header');
  const fileName = node('span', 'application-log-file ui-label ui-muted', 'Loading saved log…');
  const count = node('span', 'ui-label ui-muted');
  heading.append(fileName, count);
  const viewport = node('div', 'application-log-viewport');
  viewport.tabIndex = 0;
  viewport.setAttribute('role', 'region');
  viewport.setAttribute('aria-label', 'Application log messages');
  const rows = node('div', 'application-log-rows');
  viewport.append(rows);
  const panel = node('section', 'ui-surface application-log-panel');
  panel.setAttribute('aria-label', 'Saved application log');
  panel.append(heading, viewport);
  const footer = node('div', 'application-log-footer ui-label ui-muted');
  const windowInfo = node('span', '', 'Loading recent messages…');
  const updated = node('span', '', 'Not updated yet');
  footer.append(windowInfo, updated);
  const feedback = node('div', 'ui-feedback');
  feedback.hidden = true;
  feedback.setAttribute('role', 'status');
  element.append(controls, actions, notice, errorNotice, panel, footer, feedback);

  let snapshot = null;
  let shown = [];
  let paused = false;
  let disposed = false;
  let denied = false;
  let disconnected = false;
  let unavailable = false;
  let timer = null;
  let activeRequest = null;
  let requestSequence = 0;
  let resumeInterruptedRefresh = false;
  let retainedNotice = '';
  const entryNodes = new Map();
  const objectUrls = new Set();

  function stopTimer() {
    if (timer !== null) window.clearTimeout(timer);
    timer = null;
  }

  function cancelRequest() {
    requestSequence += 1;
    activeRequest?.abort();
    activeRequest = null;
  }

  function syncState() {
    const archived = savedLog.value === 'previous';
    stateHost.replaceChildren(uiStatus(denied ? 'Access denied' : disconnected ? 'Disconnected' :
      unavailable ? 'Unavailable' : archived ? 'Saved log' : paused ? 'Paused' :
        snapshot ? (snapshot.available ? 'Live' : 'No saved log') : 'Loading',
    denied ? 'danger' : disconnected || unavailable || (!archived && paused) ? 'warning' :
      snapshot?.available && !archived ? 'success' : 'neutral'));
    setIconButton(pause, paused ? 'icon-play' : 'icon-pause',
      paused ? 'Resume log updates' : 'Pause log updates');
    pause.setAttribute('aria-pressed', String(paused));
    pause.disabled = denied || archived;
    refreshButton.disabled = denied || Boolean(activeRequest);
    copy.disabled = download.disabled = denied || shown.length === 0;
  }

  function scrollToNewest() {
    viewport.scrollTop = viewport.scrollHeight;
  }

  function buildEntry(entry, expanded = expand.checked) {
    const row = node('article', 'application-log-entry');
    row.dataset.entryId = String(entry.id);
    const time = node('time', 'application-log-time ui-muted', entry.time ?
      new Date(entry.time).toLocaleString(undefined, { month: 'short', day: 'numeric',
        hour: '2-digit', minute: '2-digit', second: '2-digit', fractionalSecondDigits: 3 }) : '—');
    if (entry.time) {
      time.dateTime = entry.time;
      time.title = entry.time;
    }
    const severity = String(entry.level || 'TEXT').toUpperCase();
    const tone = severity === 'ERROR' ? 'danger' : severity === 'WARN' ? 'warning' : 'neutral';
    const levelHost = node('div', 'application-log-level');
    levelHost.append(uiStatus(severity, tone));
    const body = node('div', 'application-log-body');
    if (entry.source) body.append(node('div', 'application-log-source ui-label ui-muted', entry.source));
    body.append(node('div', 'application-log-message', entry.message || ''));
    if (entry.details) {
      const details = node('div', 'application-log-details');
      const toggle = node('button', 'ui-disclosure-toggle', 'Details');
      toggle.type = 'button';
      toggle.setAttribute('aria-label', `Details for log message ${entry.time || entry.id}`);
      toggle.setAttribute('aria-expanded', String(expanded));
      const trace = node('pre', 'application-log-trace', entry.details);
      trace.hidden = !expanded;
      toggle.addEventListener('click', () => {
        trace.hidden = !trace.hidden;
        toggle.setAttribute('aria-expanded', String(!trace.hidden));
      });
      details.append(toggle, trace);
      body.append(details);
    }
    row.append(time, levelHost, body);
    return row;
  }

  function drawRows() {
    shown = filterApplicationLogEntries(snapshot?.entries || [], search.value, level.value);
    const anchor = !follow.checked ? [...rows.children].find((row) =>
      row.dataset.entryId && row.getBoundingClientRect().bottom > viewport.getBoundingClientRect().top) : null;
    const anchorId = anchor?.dataset.entryId;
    const anchorTop = anchor?.getBoundingClientRect().top;
    const used = new Set();
    rows.querySelector('.ui-feedback')?.remove();
    let cursor = rows.firstChild;
    shown.forEach((entry) => {
      const id = String(entry.id);
      let record = entryNodes.get(id);
      if (!record || record.text !== entry.text || record.details !== entry.details) {
        const priorToggle = record?.element.querySelector('.ui-disclosure-toggle');
        const expanded = priorToggle ? priorToggle.getAttribute('aria-expanded') === 'true' : expand.checked;
        const focused = priorToggle === document.activeElement;
        if (record?.element === cursor) cursor = record.element.nextSibling;
        record?.element.remove();
        record = { element: buildEntry(entry, expanded), text: entry.text, details: entry.details, focused };
        entryNodes.set(id, record);
      }
      if (record.element !== cursor) rows.insertBefore(record.element, cursor);
      if (record.focused) {
        record.element.querySelector('.ui-disclosure-toggle')?.focus({ preventScroll: true });
        record.focused = false;
      }
      cursor = record.element.nextSibling;
      used.add(id);
    });
    entryNodes.forEach((record, id) => {
      if (!used.has(id)) {
        record.element.remove();
        entryNodes.delete(id);
      }
    });
    if (!shown.length) {
      const text = !snapshot ? (disconnected ? 'Application log could not be loaded. Try refreshing.' :
        'Loading application log…') : !snapshot.available ?
        `No ${savedLog.value === 'previous' ? 'previous' : 'current'} application log is available.` :
        snapshot.entries.length ? 'No loaded messages match these filters.' : snapshot.truncated ?
          'No complete messages fit within this viewer’s limit. The complete text remains in the saved application log.' :
          'No messages in this log yet.';
      rows.append(node('div', `ui-feedback ui-feedback-${!snapshot && !disconnected ? 'loading' : 'empty'}`, text));
    }
    count.textContent = `${shown.length} shown · ${snapshot?.entries.length || 0} loaded`;
    fileName.textContent = snapshot?.file_name ||
      (savedLog.value === 'previous' ? 'Previous application log' : 'Current application log');
    const shortened = snapshot?.entries.some((entry) => entry.truncated);
    windowInfo.textContent = snapshot ? [
      snapshot.truncated ? `Newest ${snapshot.entries.length} messages loaded` :
        `${snapshot.entries.length} messages loaded`,
      `limit ${snapshot.max_entries || 500}`, shortened ? 'Some messages were shortened' : ''
    ].filter(Boolean).join(' · ') : 'Loading recent messages…';
    if (follow.checked) scrollToNewest();
    else if (anchorId && entryNodes.has(anchorId)) {
      viewport.scrollTop += entryNodes.get(anchorId).element.getBoundingClientRect().top - anchorTop;
    }
    syncState();
  }

  function schedule() {
    stopTimer();
    if (!disposed && !denied && !paused && !document.hidden && savedLog.value === 'current') {
      timer = window.setTimeout(() => void refresh(), POLL_MILLISECONDS);
    }
  }

  async function refresh() {
    if (disposed || denied || activeRequest) return;
    if (document.hidden) {
      resumeInterruptedRefresh = true;
      return;
    }
    resumeInterruptedRefresh = false;
    stopTimer();
    const controller = new AbortController();
    activeRequest = controller;
    const sequence = ++requestSequence;
    syncState();
    try {
      const next = await api(LOG_PATH, { log: savedLog.value, compact: true, after: snapshot?.latest_id, revision: snapshot?.revision },
        { signal: controller.signal, page: false });
      if (disposed || sequence !== requestSequence) return;
      if (!next || !Array.isArray(next.entries) || typeof next.available !== 'boolean') {
        throw new Error('The receiver returned an invalid application log.');
      }
      if (!next.available && snapshot?.entries.length) {
        disconnected = false;
        unavailable = true;
        errorNotice.textContent = savedLog.value === 'current' ?
          'The saved log is temporarily unavailable. Last loaded messages are kept; updates will retry.' :
          'The saved log is unavailable. Last loaded messages are kept; use Refresh to try again.';
        errorNotice.hidden = false;
        return;
      }
      snapshot = mergeApplicationLogSnapshot(snapshot, next);
      disconnected = false;
      unavailable = false;
      errorNotice.hidden = true;
      if (next.change_reason === 'rotation') retainedNotice =
        'The saved log changed or rotated. The latest available messages are shown.';
      else if (next.change_reason === 'source_changed') retainedNotice =
        'The application log location changed. The latest available messages are shown.';
      else if (next.gap) retainedNotice =
        'Some messages arrived outside this viewer’s limit. The newest available messages are shown.';
      notice.textContent = retainedNotice;
      notice.hidden = !retainedNotice;
      updated.textContent = `Updated ${new Date(next.updated_at).toLocaleTimeString()}`;
      drawRows();
    } catch (error) {
      if (disposed || sequence !== requestSequence || error?.name === 'AbortError') return;
      if (error?.status === 401 || error?.status === 403) {
        denied = true;
        snapshot = null;
        retainedNotice = '';
        notice.hidden = true;
        entryNodes.clear();
        rows.replaceChildren();
        search.disabled = level.disabled = savedLog.disabled = follow.disabled = expand.disabled = true;
        fileName.textContent = 'Application log';
        count.textContent = '';
        windowInfo.textContent = '';
        updated.textContent = '';
        errorNotice.textContent = 'Application log access is no longer available. Sign in with an administrator account.';
        shown = [];
      } else {
        disconnected = true;
        errorNotice.textContent = snapshot ?
          'The receiver could not be reached. Last loaded messages are kept; updates will retry.' :
          'The application log could not be loaded. Updates will retry, or use Refresh.';
        if (!snapshot) drawRows();
      }
      errorNotice.hidden = false;
    } finally {
      if (sequence === requestSequence) {
        activeRequest = null;
        syncState();
        schedule();
      }
    }
  }

  async function copyShown() {
    const text = applicationLogShownText(shown);
    const copiedCount = shown.length;
    if (!text || denied) return;
    copy.disabled = true;
    try {
      await navigator.clipboard.writeText(text);
      if (disposed || denied) return;
      feedback.className = 'ui-feedback ui-feedback-success';
      feedback.textContent = `Copied ${copiedCount} shown messages, including details.`;
    } catch (_error) {
      if (disposed || denied) return;
      feedback.className = 'ui-feedback ui-feedback-error';
      feedback.textContent = 'The browser could not copy these messages. Use Download shown.';
    } finally {
      if (!disposed && !denied) {
        feedback.hidden = false;
        syncState();
      }
    }
  }

  function downloadShown() {
    const text = applicationLogShownText(shown);
    if (!text || denied) return;
    const objectUrl = URL.createObjectURL(new Blob([`${text}\n`], { type: 'text/plain;charset=utf-8' }));
    objectUrls.add(objectUrl);
    const link = node('a');
    link.href = objectUrl;
    link.download = `application-log-${savedLog.value}-shown.txt`;
    link.hidden = true;
    element.append(link);
    link.click();
    link.remove();
    window.setTimeout(() => { URL.revokeObjectURL(objectUrl); objectUrls.delete(objectUrl); }, 0);
  }

  search.addEventListener('input', drawRows);
  level.addEventListener('change', drawRows);
  viewport.addEventListener('scroll', () => {
    if (viewport.scrollHeight - viewport.clientHeight - viewport.scrollTop > 24) follow.checked = false;
  });
  follow.addEventListener('change', () => { if (follow.checked) scrollToNewest(); });
  expand.addEventListener('change', () => {
    rows.querySelectorAll('.application-log-trace').forEach((trace) => { trace.hidden = !expand.checked; });
    rows.querySelectorAll('.ui-disclosure-toggle').forEach((toggle) =>
      toggle.setAttribute('aria-expanded', String(expand.checked)));
  });
  pause.addEventListener('click', () => {
    paused = !paused;
    resumeInterruptedRefresh = false;
    stopTimer();
    cancelRequest();
    syncState();
    if (!paused) void refresh();
  });
  const synchronizeVisibility = () => {
    stopTimer();
    if (document.hidden) {
      resumeInterruptedRefresh ||= Boolean(activeRequest);
      cancelRequest();
    } else {
      const interrupted = resumeInterruptedRefresh;
      resumeInterruptedRefresh = false;
      if (savedLog.value === 'current' ? !paused : interrupted) void refresh();
    }
  };
  document.addEventListener('visibilitychange', synchronizeVisibility);
  refreshButton.addEventListener('click', () => void refresh());
  savedLog.addEventListener('change', () => {
    stopTimer();
    cancelRequest();
    resumeInterruptedRefresh = false;
    snapshot = null;
    disconnected = false;
    unavailable = false;
    retainedNotice = '';
    notice.hidden = errorNotice.hidden = feedback.hidden = true;
    updated.textContent = 'Not updated yet';
    drawRows();
    void refresh();
  });

  function close() {
    if (disposed) return;
    disposed = true;
    stopTimer();
    cancelRequest();
    document.removeEventListener('visibilitychange', synchronizeVisibility);
    objectUrls.forEach((url) => URL.revokeObjectURL(url));
    objectUrls.clear();
    signal?.removeEventListener('abort', close);
  }
  signal?.addEventListener('abort', close, { once: true });
  drawRows();
  if (signal?.aborted) close();
  else void refresh();
  return Object.freeze({ element, close });
}
