import { systemLabel, systemName } from './core/system-labels.js?v=1';
import { formatP25RadioIdentifier, p25ServingSystemKey } from './core/radio-labels.js?v=4';
import { formatSourceName } from './core/source-names.js?v=1';

export class WebCallPlayer {
  static MAXIMUM_SEEN_CALL_IDS = 2048;
  static IDLE_CALL_DISPLAY_MS = 5000;
  static MAXIMUM_SCAN_LISTS = 128;
  static MAXIMUM_SELECTED_SCAN_LISTS = 16;
  static MAXIMUM_QUEUED_CALLS = 100;
  static FEED_POLL_INTERVAL_MS = 100;
  static FEED_RETRY_INTERVAL_MS = 2000;
  static MAXIMUM_AVOIDS = 256;

  constructor(ids, { getSourceNameDisplay = () => 'talker_alias' } = {}) {
    this.ui = Object.fromEntries(Object.entries(ids).map(([key, id]) => [key, document.getElementById(id)]));
    this.getSourceNameDisplay = getSourceNameDisplay;
    this.queuedCalls = [];
    this.queuedCount = 0;
    this.avoids = new Map();
    this.lastHeard = null;
    this.lastHeardBuffer = null;
    this.replayingLast = false;
    this.stopAfterReplay = false;
    this.stateObservers = new Set();
    this.actions = {};
    this.holdTarget = null;
    this.heldDisplayCall = null;
    this.current = null;
    this.currentBuffer = null;
    this.currentSourceSampleRate = null;
    this.lastHeardSourceSampleRate = null;
    this.idleDisplayCall = null;
    this.idleDisplayTimer = null;
    this.idleDisplayDeadline = 0;
    this.source = null;
    this.analyserNode = null;
    this.spectrumSamples = null;
    this.gainNode = null;
    this.audioContext = null;
    this.playbackStartedAt = 0;
    this.progressFrame = null;
    this.transportToken = 0;
    this.feedUrl = null;
    this.feedFetch = null;
    this.feedActive = false;
    this.feedController = null;
    this.feedTimer = null;
    this.feedGeneration = 0;
    this.feedCursor = null;
    this.loadToken = 0;
    this.loadController = null;
    this.stopped = true;
    this.paused = false;
    this.volume = 1;
    this.statusValue = this.ui.status?.textContent || '';
    this.preferenceWriter = null;
    this.volumeEditRevision = 0;
    this.volumeEditPending = false;
    this.volumeCommit = null;
    this.preferenceWriteSequence = 0;
    this.pendingPreferenceWrites = new Map();
    this.preferenceIdentity = undefined;
    this.maximumQueued = WebCallPlayer.MAXIMUM_QUEUED_CALLS;
    this.maximumSelectedScanLists = WebCallPlayer.MAXIMUM_SELECTED_SCAN_LISTS;
    this.targetGrouping = true;
    this.targetBurstLimit = 4;
    this.arrivalSequence = 0;
    this.lastPlaybackTargetKey = null;
    this.consecutiveTargetCalls = 0;
    this.seenCallIds = new Set();
    this.seenCallOrder = [];
    this.scanLists = [];
    this.scanListById = new Map();
    this.selectedScanListIds = new Set();
    this.scanListCatalogReady = false;
    this.scanListCatalogState = 'loading';
    this.ui.volume.value = String(this.volume);
    this.bindControls();
    this.render();
  }

  connect(url, feedFetch = null) {
    this.stopFeed();
    this.feedUrl = String(url || '').trim();
    this.feedFetch = typeof feedFetch === 'function' ? feedFetch :
      ((path, options) => fetch(path, { cache: 'no-store', credentials: 'same-origin', ...options })
        .then(async (response) => {
          if (!response.ok) throw Object.assign(new Error(`Call feed returned ${response.status}`),
            { status: response.status });
          return response.json();
        }));
    if (!this.feedUrl) throw new Error('A browser call feed URL is required.');
    return this.connectionHandle();
  }

  setActions(actions = {}) {
    this.actions = actions && typeof actions === 'object' ? actions : {};
  }

  setPreferenceWriter(writer) {
    this.preferenceWriter = typeof writer === 'function' ? writer : null;
  }

  applyPreferences(preferences, options = {}) {
    if (!preferences || typeof preferences !== 'object') return;
    if (Object.hasOwn(options, 'identity') && options.identity !== this.preferenceIdentity) {
      this.preferenceIdentity = options.identity;
      this.volumeEditRevision = (this.volumeEditRevision || 0) + 1;
      this.volumeEditPending = false;
      this.volumeCommit = null;
      this.pendingPreferenceWrites?.clear();
    }
    const volume = Number(preferences.volume);
    if (!this.volumeEditPending && Number.isFinite(volume) && volume >= 0 && volume <= 1) {
      this.volume = volume;
      this.ui.volume.value = String(volume);
      if (this.gainNode) this.gainNode.gain.value = volume;
    }
    if (Array.isArray(preferences.selected_scan_list_ids)) {
      const selected = new Set(preferences.selected_scan_list_ids.map(String));
      const changed = selected.size !== this.selectedScanListIds.size ||
        [...selected].some((id) => !this.selectedScanListIds.has(id));
      if (changed) {
        this.selectedScanListIds = selected;
        this.filterQueueForSelectedLists();
        this.renderScanLists();
        this.synchronizeSubscription();
      }
    }
    if (typeof preferences.target_grouping === 'boolean') {
      this.targetGrouping = preferences.target_grouping;
    }
    const burstLimit = Number(preferences.target_burst_limit);
    if (Number.isInteger(burstLimit) && burstLimit >= 1 && burstLimit <= 20) {
      this.targetBurstLimit = burstLimit;
    }
    this.render();
  }

  writePreferences({ volume = false } = {}) {
    if (!this.preferenceWriter) {
      if (volume) this.volumeEditPending = false;
      return Promise.resolve();
    }
    const selectedScanListIds = [...this.selectedScanListIds].map(Number)
      .filter((id) => Number.isSafeInteger(id) && id > 0).sort((left, right) => left - right);
    const snapshot = {
      volume: this.volume,
      selected_scan_list_ids: selectedScanListIds,
      target_grouping: this.targetGrouping,
      target_burst_limit: this.targetBurstLimit
    };
    const revision = this.volumeEditRevision || 0;
    const writeId = this.preferenceWriteSequence = (this.preferenceWriteSequence || 0) + 1;
    const pending = this.pendingPreferenceWrites ||= new Map();
    pending.set(writeId, revision);
    if (volume) this.volumeCommit = { revision, writeId, settled: false };
    return Promise.resolve().then(() => this.preferenceWriter(snapshot)).catch(() => {}).finally(() => {
      pending.delete(writeId);
      const commit = this.volumeCommit;
      if (commit?.writeId === writeId) commit.settled = true;
      // Only an explicit volume save can finish an edit. Older requests may still
      // deliver a preference snapshot after that save, so retain the local gain
      // until those responses settle too. A newer drag always keeps its guard.
      if (commit?.settled && commit.revision === this.volumeEditRevision &&
          ![...pending.values()].some((olderRevision) => olderRevision < commit.revision)) {
        this.volumeEditPending = false;
      }
    });
  }

  subscribeState(observer) {
    if (typeof observer !== 'function') return () => {};
    this.stateObservers.add(observer);
    observer(this.viewState());
    return () => this.stateObservers.delete(observer);
  }

  notifyStateObservers() {
    if (!this.stateObservers?.size) return;
    const state = this.viewState();
    this.stateObservers.forEach((observer) => {
      try { observer(state); } catch (_) { }
    });
  }

  viewState() {
    return {
      current: this.current,
      displayCall: this.displayCall(),
      currentReady: Boolean(this.current && this.currentBuffer),
      replayingLast: this.replayingLast,
      lastCallReady: Boolean(this.lastHeard && this.lastHeardBuffer),
      stopped: this.stopped,
      paused: this.paused,
      playing: Boolean(this.source) && !this.paused && !this.stopped,
      targetLabel: this.currentTargetLabel(),
      holdTarget: this.holdTarget,
      queuedCount: this.queuedCount,
      queue: this.scheduledQueue(WebCallPlayer.MAXIMUM_QUEUED_CALLS),
      currentTime: this.getPlaybackPosition(),
      duration: Number(this.currentBuffer?.duration) || 0,
      status: this.ui.status?.textContent || '',
      avoids: [...this.avoids.values()].reverse(),
      scanLists: this.scanLists.map((item) => ({ ...item, selected: this.selectedScanListIds.has(item.id) })),
      scanListCatalogReady: this.scanListCatalogReady,
      scanListCatalogState: this.scanListCatalogState,
      maximumSelectedScanLists: this.maximumSelectedScanLists,
      volume: this.volume,
      targetGrouping: this.targetGrouping,
      targetBurstLimit: this.targetBurstLimit
    };
  }

  connectionHandle() {
    return {
      close: () => {
        this.stopFeed();
        this.feedUrl = null;
        this.feedFetch = null;
      }
    };
  }

  activeSelectedScanListIds() {
    return [...this.selectedScanListIds]
      .filter((id) => this.scanListById.get(id)?.enabled)
      .sort((first, second) => Number(first) - Number(second))
      .slice(0, this.maximumSelectedScanLists);
  }

  ensureConnected() {
    if (this.feedActive) return true;
    if (typeof this.feedFetch !== 'function' || !this.feedUrl || !this.scanListCatalogReady ||
        !this.activeSelectedScanListIds().length || this.stopped) return false;
    this.feedActive = true;
    this.feedCursor = null;
    const generation = ++this.feedGeneration;
    void this.pollFeed(generation);
    return true;
  }

  feedRequestUrl() {
    const query = new URLSearchParams();
    this.activeSelectedScanListIds().forEach((id) => query.append('scan_list_id', id));
    if (this.feedCursor !== null) query.set('cursor', this.feedCursor);
    return `${this.feedUrl}?${query}`;
  }

  async requestFeed(signal) {
    const value = await this.feedFetch(this.feedRequestUrl(), { signal });
    const cursor = typeof value?.cursor === 'string' && /^\d+$/.test(value.cursor) ? value.cursor : null;
    if (cursor === null || typeof value?.reset !== 'boolean' || !Array.isArray(value?.calls)) {
      throw new Error('The browser call feed returned an invalid response.');
    }
    return { cursor, reset: value.reset, calls: value.calls };
  }

  scheduleFeedPoll(generation, delay) {
    if (!this.feedActive || this.stopped || generation !== this.feedGeneration) return;
    this.feedTimer = window.setTimeout(() => {
      this.feedTimer = null;
      void this.pollFeed(generation);
    }, delay);
  }

  async pollFeed(generation) {
    if (!this.feedActive || this.stopped || generation !== this.feedGeneration) return;
    const controller = new AbortController();
    this.feedController = controller;
    try {
      const response = await this.requestFeed(controller.signal);
      if (!this.feedActive || this.stopped || generation !== this.feedGeneration) return;
      this.feedCursor = response.cursor;
      response.calls.forEach((call) => this.enqueue(call));
      if (!this.current && !this.queuedCount) this.setStatus('Waiting');
      this.scheduleFeedPoll(generation, WebCallPlayer.FEED_POLL_INTERVAL_MS);
    } catch (error) {
      if (controller.signal.aborted || !this.feedActive || this.stopped || generation !== this.feedGeneration) return;
      this.feedCursor = null;
      this.setStatus('Reconnecting');
      this.scheduleFeedPoll(generation, WebCallPlayer.FEED_RETRY_INTERVAL_MS);
    } finally {
      if (this.feedController === controller) this.feedController = null;
    }
  }

  stopFeed() {
    this.feedActive = false;
    this.feedCursor = null;
    this.feedGeneration++;
    this.feedController?.abort();
    this.feedController = null;
    if (this.feedTimer !== null) window.clearTimeout(this.feedTimer);
    this.feedTimer = null;
  }

  recoverFeed() {
    if (!this.feedActive || this.stopped || this.statusValue !== 'Reconnecting') return false;
    this.stopFeed();
    return this.ensureConnected();
  }

  synchronizeSubscription() {
    if (!this.scanListCatalogReady || !this.activeSelectedScanListIds().length) {
      this.stopFeed();
      if (!this.stopped) {
        this.stopped = true;
        this.paused = false;
        this.transportToken++;
        this.clearQueuedCalls();
        this.stopCurrent();
        this.replayingLast = false;
        this.stopAfterReplay = false;
        if (this.audioContext?.state === 'running') this.audioContext.suspend().catch(() => {});
      }
      this.setStatus(this.scanListCatalogReady ? 'Select a scan list' : 'Loading scan lists');
      return;
    }
    if (this.feedActive) {
      this.stopFeed();
      this.ensureConnected();
    } else if (!this.stopped && !this.stopAfterReplay) this.ensureConnected();
    else this.setStatus('Ready');
  }

  disconnect(status = 'Unavailable') {
    this.stopFeed();
    this.transportToken++;
    this.loadController?.abort();
    this.loadController = null;
    this.stopped = true;
    this.paused = false;
    this.clearQueuedCalls();
    this.avoids.clear();
    this.lastHeard = null;
    this.lastHeardBuffer = null;
    this.lastHeardSourceSampleRate = null;
    this.replayingLast = false;
    this.stopAfterReplay = false;
    this.holdTarget = null;
    this.heldDisplayCall = null;
    this.clearIdleDisplay();
    this.stopCurrent();
    if (this.audioContext?.state === 'running') this.audioContext.suspend().catch(() => {});
    this.setStatus(status);
    this.render();
  }

  setScanListsLoading() {
    this.scanListCatalogReady = false;
    this.scanListCatalogState = 'loading';
    this.stopFeed();
    if (this.ui.scanListStatus) this.ui.scanListStatus.textContent = 'Loading available scan lists…';
    this.renderScanLists();
  }

  setScanListsUnavailable(message = 'Scan lists are unavailable') {
    this.scanListCatalogReady = false;
    this.scanListCatalogState = 'unavailable';
    this.scanLists = [];
    this.scanListById.clear();
    this.stopFeed();
    if (this.ui.scanListStatus) this.ui.scanListStatus.textContent = message;
    this.setStatus(message);
    this.renderScanLists();
  }

  setScanLists(rows) {
    const unique = new Map();
    (Array.isArray(rows) ? rows : []).slice(0, WebCallPlayer.MAXIMUM_SCAN_LISTS).forEach((row) => {
      const id = Number.isSafeInteger(row?.id) && row.id > 0 ? String(row.id) : '';
      const name = typeof row?.name === 'string' ? row.name.trim() : '';
      const description = row?.description === undefined ? '' : row.description;
      if (typeof description !== 'string' || typeof row?.default !== 'boolean') return;
      if (!id || !name || unique.has(id)) return;
      unique.set(id, {
        id,
        name,
        description: description.trim(),
        enabled: true,
        default: row.default
      });
    });
    // The server already applies the administrator-owned scan-list order.
    this.scanLists = [...unique.values()];
    this.scanListById = new Map(this.scanLists.map((item) => [item.id, item]));
    const retainedSelections = new Set([...this.selectedScanListIds]
      .filter((id) => this.scanListById.has(id)));
    const selectionChanged = retainedSelections.size !== this.selectedScanListIds.size;
    if (selectionChanged) this.selectedScanListIds = retainedSelections;
    this.scanListCatalogReady = true;
    this.scanListCatalogState = 'ready';
    this.updateScanListStatus();
    this.filterQueueForSelectedLists();
    this.renderScanLists();
    this.synchronizeSubscription();
    this.render();
    if (selectionChanged) this.writePreferences();
  }

  scanListsReady() {
    return this.scanListCatalogReady;
  }

  setScanListSelected(id, selected) {
    const item = this.scanListById.get(String(id));
    if (!item?.enabled) return;
    const updated = new Set(this.selectedScanListIds);
    if (selected && !updated.has(item.id) && updated.size >= this.maximumSelectedScanLists) {
      if (this.ui.scanListStatus) {
        this.ui.scanListStatus.textContent = `Choose up to ${this.maximumSelectedScanLists} scan lists.`;
      }
      this.renderScanLists();
      return;
    }
    if (selected) updated.add(item.id);
    else updated.delete(item.id);
    if ([...updated].sort().join('|') === [...this.selectedScanListIds].sort().join('|')) return;
    this.selectedScanListIds = updated;
    this.writePreferences();
    this.updateScanListStatus();
    this.filterQueueForSelectedLists();
    this.renderScanLists();
    this.synchronizeSubscription();
    this.render();
  }

  renderScanLists() {
    if (this.ui.scanListSummary) {
      if (!this.scanListCatalogReady) {
        this.ui.scanListSummary.textContent = this.scanListCatalogState === 'loading' ? 'Loading' : 'Unavailable';
        this.ui.scanListSummary.removeAttribute('title');
      } else {
        const selected = this.scanLists.filter((item) => this.selectedScanListIds.has(item.id));
        this.ui.scanListSummary.textContent = String(selected.length);
        this.ui.scanListSummary.title = selected.length ? selected.map((item) => item.name).join(', ') :
          'No scan lists selected';
      }
    }
    if (!this.ui.scanListOptions) return;
    this.ui.scanListOptions.replaceChildren();
    if (!this.scanListCatalogReady) {
      this.ui.scanListOptions.append(this.scanListMessage(this.scanListCatalogState === 'loading' ?
        'Waiting for the receiver…' : 'Scan lists are unavailable.'));
      return;
    }
    if (!this.scanLists.length) {
      this.ui.scanListOptions.append(this.scanListMessage('No scan lists are available.'));
      return;
    }
    const selectionFull = this.activeSelectedScanListIds().length >= this.maximumSelectedScanLists;
    this.scanLists.forEach((item) => {
      const label = document.createElement('label');
      label.className = 'playback-scan-list-option';
      const checkbox = document.createElement('input');
      checkbox.className = 'ui-selection-check ui-selection-check-inverse';
      checkbox.type = 'checkbox';
      checkbox.value = item.id;
      checkbox.checked = this.selectedScanListIds.has(item.id);
      checkbox.disabled = !item.enabled || (selectionFull && !checkbox.checked);
      checkbox.addEventListener('change', () => this.setScanListSelected(item.id, checkbox.checked));
      const copy = document.createElement('span');
      const name = document.createElement('strong');
      name.textContent = item.name;
      copy.append(name);
      if (item.description) {
        const description = document.createElement('small');
        description.textContent = item.description;
        copy.append(description);
      }
      label.append(checkbox, copy);
      this.ui.scanListOptions.append(label);
    });
  }

  updateScanListStatus() {
    if (!this.ui.scanListStatus || !this.scanListCatalogReady) return;
    if (!this.scanLists.length) this.ui.scanListStatus.textContent = 'No scan lists are available.';
    else if (!this.activeSelectedScanListIds().length) {
      this.ui.scanListStatus.textContent = 'Select at least one scan list to receive calls.';
    } else {
      this.ui.scanListStatus.textContent = '';
    }
  }

  scanListMessage(message) {
    const value = document.createElement('div');
    value.className = 'playback-panel-empty muted';
    value.textContent = message;
    return value;
  }

  // The shared HTTP feed supplies normalized completed-call announcements.
  enqueue(call) {
    const normalized = this.normalizeCall(call);
    if (!normalized || !this.callMatchesSelection(normalized)) return;
    if (this.seenCallIds.has(normalized._callId)) return;
    this.rememberCallId(normalized._callId);
    if (!this.isAllowed(normalized)) return;

    while (this.queuedCount >= this.maximumQueued) {
      const dropped = this.dropOldestQueued();
      if (!dropped) break;
    }

    this.insertQueuedCall(normalized);
    if (!this.stopped && !this.paused && !this.current) this.playNext();
    else this.render();
  }

  normalizeCall(value) {
    if (!value || typeof value !== 'object' || typeof value.audio_url !== 'string' ||
        !value.audio_url.trim()) return null;
    const callId = typeof value.call_id === 'string' ? value.call_id.trim() : '';
    const started = value.started_at_ms;
    const completed = value.completed_at_ms;
    if (!callId || !Number.isFinite(started) || started <= 0 || !Number.isFinite(completed) || completed <= 0 ||
        !Array.isArray(value.scan_list_ids)) return null;
    const scanListIds = value.scan_list_ids;
    if (!scanListIds.length || scanListIds.some((id) => !Number.isSafeInteger(id) || id <= 0)) return null;
    const playbackTarget = value.playback_target;
    const playbackTargetKey = typeof playbackTarget?.key === 'string' ? playbackTarget.key.trim() : '';
    const playbackTargetKind = typeof playbackTarget?.kind === 'string' ? playbackTarget.kind.trim() : '';
    if (!playbackTargetKey || !['channel', 'channel_timeslot', 'talkgroup', 'patch_group', 'radio']
      .includes(playbackTargetKind)) return null;
    const call = {
      ...value,
      _callId: callId,
      _startedAtMs: started,
      _arrivalSequence: this.arrivalSequence++,
      _matchedScanListIds: [...new Set(scanListIds.map(String))]
    };
    call._playbackTargetKey = playbackTargetKey;
    return call;
  }

  rememberCallId(id) {
    this.seenCallIds.add(id);
    this.seenCallOrder.push(id);
    while (this.seenCallOrder.length > WebCallPlayer.MAXIMUM_SEEN_CALL_IDS) {
      this.seenCallIds.delete(this.seenCallOrder.shift());
    }
  }

  insertQueuedCall(call) {
    let index = this.queuedCalls.length;
    while (index > 0 && this.compareCalls(call, this.queuedCalls[index - 1]) < 0) index--;
    this.queuedCalls.splice(index, 0, call);
    this.queuedCount = this.queuedCalls.length;
  }

  compareCalls(first, second) {
    return first._startedAtMs - second._startedAtMs ||
      first._arrivalSequence - second._arrivalSequence ||
      first._callId.localeCompare(second._callId);
  }

  nextQueueIndex(queue, lastKey, consecutive) {
    if (!queue.length || !this.targetGrouping || !lastKey) return queue.length ? 0 : -1;
    const same = queue.findIndex((call) => call._playbackTargetKey === lastKey);
    const other = queue.findIndex((call) => call._playbackTargetKey !== lastKey);
    if (same >= 0 && (consecutive < this.targetBurstLimit || other < 0)) return same;
    return other >= 0 ? other : same;
  }

  takeNextCall() {
    const index = this.nextQueueIndex(this.queuedCalls, this.lastPlaybackTargetKey,
      this.consecutiveTargetCalls);
    if (index < 0) return null;
    const [call] = this.queuedCalls.splice(index, 1);
    this.queuedCount = this.queuedCalls.length;
    const key = call._playbackTargetKey;
    if (key === this.lastPlaybackTargetKey) {
      this.consecutiveTargetCalls = Math.min(this.targetBurstLimit,
        this.consecutiveTargetCalls + 1);
    } else {
      this.lastPlaybackTargetKey = key;
      this.consecutiveTargetCalls = 1;
    }
    return call;
  }

  scheduledQueue(limit = 100) {
    const queue = this.queuedCalls.slice();
    const result = [];
    let lastKey = this.lastPlaybackTargetKey;
    let consecutive = this.consecutiveTargetCalls;
    while (result.length < limit) {
      const index = this.nextQueueIndex(queue, lastKey, consecutive);
      if (index < 0) break;
      const [call] = queue.splice(index, 1);
      result.push(call);
      const key = call._playbackTargetKey;
      if (key === lastKey) consecutive = Math.min(this.targetBurstLimit,
        consecutive + 1);
      else {
        lastKey = key;
        consecutive = 1;
      }
    }
    return result;
  }

  dropOldestQueued() {
    const call = this.queuedCalls.shift() || null;
    this.queuedCount = this.queuedCalls.length;
    return call;
  }

  clearQueuedCalls() {
    this.queuedCalls.length = 0;
    this.queuedCount = 0;
  }

  clearQueue() {
    this.clearQueuedCalls();
    this.render();
  }

  filterQueuedCalls(predicate) {
    this.queuedCalls = this.queuedCalls.filter(predicate);
    this.queuedCount = this.queuedCalls.length;
  }

  filterQueueForSelectedLists() {
    this.filterQueuedCalls((call) => this.callMatchesSelection(call));
  }

  callMatchesSelection(call) {
    const selected = new Set(this.activeSelectedScanListIds());
    if (!selected.size) return false;
    return Boolean(call?._matchedScanListIds?.length) &&
      call._matchedScanListIds.some((id) => selected.has(String(id)));
  }

  bindControls() {
    this.ui.play.addEventListener('click', () => this.togglePlayback());
    this.ui.pause?.addEventListener('click', () => this.togglePause());
    this.ui.skip.addEventListener('click', () => this.skip());
    this.ui.replay.addEventListener('click', () => this.replayLastCall());
    this.ui.hold.addEventListener('click', () => this.toggleHold());
    this.ui.avoid.addEventListener('click', () => this.avoidCurrent());
    this.ui.avoidList?.addEventListener('click', () => this.actions.openAvoidList?.(this));
    this.ui.clearQueue?.addEventListener('click', () => this.clearQueue());
    this.ui.volume.addEventListener('input', () => this.changeVolume(false));
    this.ui.volume.addEventListener('change', () => this.writePreferences({ volume: true }));
    const panels = [this.ui.scanListOptions?.closest('details'), this.ui.queueList?.closest('details')]
      .filter(Boolean);
    panels.forEach((panel) => panel.addEventListener('toggle', () => {
      const expanded = panel.closest('.playback-bar')?.classList.contains('scanner-expanded');
      if (panel.open && !expanded) {
        panels.forEach((other) => { if (other !== panel) other.open = false; });
      }
    }));
  }

  async togglePlayback() {
    if (this.stopped && this.scanListCatalogReady && !this.activeSelectedScanListIds().length) {
      const defaultScanList = this.scanLists.find((item) => item.enabled && item.default);
      if (defaultScanList) {
        this.selectedScanListIds = new Set([defaultScanList.id]);
        this.writePreferences();
        this.updateScanListStatus();
        this.filterQueueForSelectedLists();
        this.renderScanLists();
      }
    }

    const token = ++this.transportToken;
    if (!this.stopped) {
      this.stopped = true;
      this.paused = false;
      this.stopFeed();
      this.clearQueuedCalls();
      this.lastPlaybackTargetKey = null;
      this.consecutiveTargetCalls = 0;
      this.stopCurrent();
      this.replayingLast = false;
      this.stopAfterReplay = false;
      if (this.audioContext) await this.audioContext.suspend();
      if (token !== this.transportToken) return;
      this.setStatus('Ready');
    } else {
      this.stopped = false;
      this.paused = false;
      if (!this.ensureConnected()) {
        this.stopped = true;
        this.setStatus('Unavailable');
        this.render();
        return;
      }
      this.ensureAudioContext();
      await this.audioContext.resume();
      if (token !== this.transportToken) return;
      this.setStatus('Waiting');
    }

    this.render();
  }

  async togglePause() {
    if (this.stopped) return;
    const token = ++this.transportToken;
    this.paused = !this.paused;
    this.ensureAudioContext();
    // Suspending the context preserves both the source and its playback clock. The
    // completed-call feed stays active; only browser audio and animation stop.
    try {
      if (this.paused) {
        this.stopProgress();
        this.render();
        await this.audioContext.suspend();
      } else {
        await this.audioContext.resume();
      }
    } catch (_) {
      if (token !== this.transportToken) return;
      // A rejected browser audio transition must not silently start queued calls.
      this.paused = true;
      this.stopProgress();
      this.render();
      return;
    }
    if (token !== this.transportToken) return;
    if (!this.paused) {
      if (this.source) {
        this.setStatus(this.replayingLast ? 'Replaying last call' : 'Listening');
        this.startProgress();
      } else if (this.currentBuffer) {
        this.startCurrent();
      } else if (!this.current) {
        void this.playNext();
      } else {
        this.setStatus('Buffering');
      }
    }
    this.render();
  }

  toggleHold() {
    if (this.replayingLast) return;
    if (this.holdTarget) {
      this.holdTarget = null;
      this.heldDisplayCall = null;
    } else if (this.current && this.currentBuffer) {
      this.holdTarget = this.current._playbackTargetKey;
      this.heldDisplayCall = this.current;
      this.filterQueuedCalls((call) => call._playbackTargetKey === this.holdTarget);
    }

    this.render();
  }

  avoidCurrent() {
    if (!this.current || !this.currentBuffer || this.replayingLast) return;
    const target = this.current._playbackTargetKey;
    const systemScope = this.avoidSystemScope(this.current);
    this.avoids.delete(target);
    this.avoids.set(target, {
      key: target,
      label: this.targetLabel(this.current),
      system_scope: systemScope,
      radio_system_key: systemScope ? this.current.playback_target?.radio_system_key || this.current.radio_system_key : '',
      system_name: systemScope ? systemName(this.current) : ''
    });
    while (this.avoids.size > WebCallPlayer.MAXIMUM_AVOIDS) {
      this.avoids.delete(this.avoids.keys().next().value);
    }
    if (this.holdTarget === target) {
      this.holdTarget = null;
      this.heldDisplayCall = null;
    }
    this.filterQueuedCalls((call) => call._playbackTargetKey !== target);
    this.stopCurrent();
    if (this.stopped || this.paused) this.setStatus('Ready');
    else this.playNext();
  }

  removeAvoid(key) {
    if (!this.avoids.delete(String(key || ''))) return false;
    this.render();
    return true;
  }

  skip() {
    if (this.current) this.stopCurrent();
    else this.takeNextCall();
    if (this.replayingLast) {
      this.replayingLast = false;
      if (this.stopAfterReplay) {
        this.stopped = true;
        this.paused = false;
        this.audioContext?.suspend().catch(() => {});
      }
      this.stopAfterReplay = false;
    }
    if (this.stopped || this.paused) {
      this.setStatus('Ready');
      this.render();
    } else {
      this.playNext();
    }
  }

  async replayLastCall() {
    if (this.paused || !this.lastHeard || !this.lastHeardBuffer) return false;
    const wasStopped = this.stopped;
    const transportToken = ++this.transportToken;
    this.loadToken++;
    this.loadController?.abort();
    this.loadController = null;
    this.stopSource();
    this.clearIdleDisplay();
    this.current = this.lastHeard;
    this.currentBuffer = this.lastHeardBuffer;
    this.currentSourceSampleRate = this.lastHeardSourceSampleRate;
    this.stopped = false;
    this.replayingLast = true;
    this.stopAfterReplay = wasStopped;
    this.ensureAudioContext();
    await this.audioContext.resume();
    if (transportToken !== this.transportToken || !this.replayingLast || !this.currentBuffer) return false;
    this.setStatus('Replaying last call');
    this.startCurrent();
    return true;
  }

  async playNext(completedCall = null) {
    if (this.stopped || this.paused || this.current) return;

    let next = this.takeNextCall();
    while (next && (!this.callMatchesSelection(next) || !this.isAllowed(next))) {
      next = this.takeNextCall();
    }
    if (!next) {
      if (completedCall) this.showIdleDisplay(completedCall);
      this.setStatus('Waiting');
      this.render();
      return;
    }

    this.clearIdleDisplay();
    this.current = next;
    if (this.holdTarget === next._playbackTargetKey) this.heldDisplayCall = next;
    this.currentBuffer = null;
    this.currentSourceSampleRate = null;
    await this.loadCurrent();
  }

  async loadCurrent() {
    if (!this.current) return;
    const requested = this.current;
    const token = ++this.loadToken;
    this.loadController?.abort();
    const loadController = new AbortController();
    this.loadController = loadController;
    let loadTimedOut = false;
    let loadTimeout;
    let sourceSampleRate = null;
    const timeoutFailure = new Promise((_, reject) => {
      loadTimeout = window.setTimeout(() => {
        loadTimedOut = true;
        loadController.abort();
        reject(new Error('Audio loading timed out.'));
      }, 15_000);
    });
    this.setStatus('Buffering');
    this.render();

    try {
      const buffer = await Promise.race([(async () => {
        const response = await fetch(requested.audio_url, {
          cache: 'no-store', credentials: 'same-origin', signal: loadController.signal
        });
        if (!response.ok) throw new Error(`Audio returned ${response.status}`);
        this.ensureAudioContext();
        const data = await response.arrayBuffer();
        sourceSampleRate = WebCallPlayer.waveSampleRate(data);
        return this.audioContext.decodeAudioData(data);
      })(), timeoutFailure]);
      if (token !== this.loadToken || this.current !== requested) return;
      this.currentBuffer = buffer;
      this.currentSourceSampleRate = sourceSampleRate || Number(buffer?.sampleRate) || null;
      if (!this.stopped && !this.paused) this.startCurrent();
      else this.render();
    } catch (error) {
      if (error?.name === 'AbortError' && !loadTimedOut) return;
      if (token === this.loadToken) {
        this.stopCurrent();
        setTimeout(() => {
          if (this.stopped || this.paused) {
            this.setStatus('Ready');
            this.render();
          } else {
            this.playNext();
          }
        }, 150);
      }
    } finally {
      window.clearTimeout(loadTimeout);
      if (this.loadController === loadController) this.loadController = null;
    }
  }

  startCurrent() {
    if (!this.current || !this.currentBuffer || this.stopped || this.paused || this.source) return;
    const token = this.loadToken;
    const source = this.audioContext.createBufferSource();
    source.buffer = this.currentBuffer;
    source.connect(this.analyserNode);
    source.onended = () => {
      if (token !== this.loadToken || source !== this.source) return;
      if (this.replayingLast) {
        const stopAfterReplay = this.stopAfterReplay;
        source.disconnect();
        this.stopProgress();
        this.source = null;
        this.current = null;
        this.currentBuffer = null;
        this.currentSourceSampleRate = null;
        this.replayingLast = false;
        this.stopAfterReplay = false;
        if (stopAfterReplay) {
          this.stopped = true;
          this.paused = false;
          if (this.audioContext?.state === 'running') this.audioContext.suspend().catch(() => {});
          this.setStatus('Ready');
          this.render();
        } else {
          this.playNext();
        }
        return;
      }
      const completed = this.current;
      const completedBuffer = this.currentBuffer;
      const completedSourceSampleRate = this.currentSourceSampleRate;
      source.disconnect();
      this.stopProgress();
      this.source = null;
      this.current = null;
      this.currentBuffer = null;
      this.currentSourceSampleRate = null;
      this.lastHeard = completed;
      this.lastHeardBuffer = completedBuffer;
      this.lastHeardSourceSampleRate = completedSourceSampleRate;
      this.playNext(completed);
    };
    this.source = source;
    this.playbackStartedAt = this.audioContext.currentTime;
    source.start(0);
    this.setStatus('Listening');
    this.render();
    this.startProgress();
  }

  stopSource() {
    const source = this.source;
    this.source = null;
    this.stopProgress();
    if (source) {
      source.onended = null;
      try { source.stop(); } catch (_) { }
      source.disconnect();
    }
  }

  stopCurrent() {
    this.loadToken++;
    this.loadController?.abort();
    this.loadController = null;
    this.stopSource();
    this.current = null;
    this.currentBuffer = null;
    this.currentSourceSampleRate = null;
    this.clearIdleDisplay();
    this.render();
  }

  clearIdleDisplay() {
    if (this.idleDisplayTimer !== null) {
      window.clearTimeout(this.idleDisplayTimer);
      this.idleDisplayTimer = null;
    }
    this.idleDisplayCall = null;
    this.idleDisplayDeadline = 0;
  }

  showIdleDisplay(call) {
    this.clearIdleDisplay();
    if (!call) return;
    this.idleDisplayCall = call;
    this.idleDisplayDeadline = Date.now() + WebCallPlayer.IDLE_CALL_DISPLAY_MS;
    const expire = () => {
      if (this.idleDisplayCall !== call) return;
      const remaining = this.idleDisplayDeadline - Date.now();
      if (remaining > 0) {
        this.idleDisplayTimer = window.setTimeout(expire, remaining);
        return;
      }
      this.clearIdleDisplay();
      this.render();
    };
    this.idleDisplayTimer = window.setTimeout(expire, WebCallPlayer.IDLE_CALL_DISPLAY_MS);
  }

  displayCall() {
    if (this.current) return this.current;
    if (this.holdTarget && this.heldDisplayCall?._playbackTargetKey === this.holdTarget) {
      return this.heldDisplayCall;
    }
    return this.idleDisplayCall && Date.now() < this.idleDisplayDeadline ? this.idleDisplayCall : null;
  }

  getPlaybackPosition() {
    const duration = Number(this.currentBuffer?.duration);
    const position = this.source && this.audioContext ?
      this.audioContext.currentTime - this.playbackStartedAt : 0;
    if (!Number.isFinite(position)) return 0;
    return Number.isFinite(duration) && duration > 0 ? Math.min(duration, Math.max(0, position)) :
      Math.max(0, position);
  }

  startProgress() {
    this.stopProgress();
    const update = () => {
      if (!this.source || this.paused || this.stopped) return;
      this.renderProgress();
      this.progressFrame = window.requestAnimationFrame(update);
    };
    update();
  }

  stopProgress() {
    if (this.progressFrame !== null) {
      window.cancelAnimationFrame(this.progressFrame);
      this.progressFrame = null;
    }
  }

  renderProgress() {
    const duration = Number(this.currentBuffer?.duration);
    const hasDuration = Boolean(this.current) && Number.isFinite(duration) && duration > 0;
    const position = hasDuration ? this.getPlaybackPosition() : 0;
    const progress = hasDuration ? Math.min(1, position / duration) : 0;
    const fadeWindow = hasDuration ? Math.min(1.2, Math.max(0.35, duration * 0.12)) : 0;
    const active = hasDuration && Boolean(this.source) && !this.paused;
    this.ui.progress.style.setProperty('--playback-progress', String(progress));
    this.ui.progress.classList.toggle('active', active);
    this.ui.progress.classList.toggle('ending', active && duration - position <= fadeWindow);
  }

  changeVolume(write = false) {
    this.volumeEditRevision = (this.volumeEditRevision || 0) + 1;
    this.volumeEditPending = true;
    const requested = Number(this.ui.volume.value);
    this.volume = Number.isFinite(requested) ? Math.max(0, Math.min(1, requested)) : 1;
    this.ui.volume.value = String(this.volume);
    if (this.gainNode) this.gainNode.gain.value = this.volume;
    this.renderVolume();
    if (write) this.writePreferences({ volume: true });
    this.notifyStateObservers();
  }

  ensureAudioContext() {
    if (!this.audioContext) {
      const AudioContext = window.AudioContext || window.webkitAudioContext;
      this.audioContext = new AudioContext();
      this.analyserNode = this.audioContext.createAnalyser();
      this.analyserNode.fftSize = 512;
      this.analyserNode.smoothingTimeConstant = 0.5;
      this.gainNode = this.audioContext.createGain();
      this.gainNode.gain.value = this.volume;
      this.analyserNode.connect(this.gainNode);
      this.gainNode.connect(this.audioContext.destination);
    }
  }

  static waveSampleRate(data) {
    if (!(data instanceof ArrayBuffer) || data.byteLength < 20) return null;
    const view = new DataView(data);
    const chunkName = (offset) => String.fromCharCode(
      view.getUint8(offset), view.getUint8(offset + 1), view.getUint8(offset + 2), view.getUint8(offset + 3));
    if (chunkName(0) !== 'RIFF' || chunkName(8) !== 'WAVE') return null;
    let offset = 12;
    while (offset + 8 <= data.byteLength) {
      const size = view.getUint32(offset + 4, true);
      const dataOffset = offset + 8;
      if (chunkName(offset) === 'fmt ' && size >= 16 && dataOffset + 8 <= data.byteLength) {
        const sampleRate = view.getUint32(dataOffset + 4, true);
        return sampleRate > 0 ? sampleRate : null;
      }
      const next = dataOffset + size + (size & 1);
      if (next <= offset || next > data.byteLength) break;
      offset = next;
    }
    return null;
  }

  readAudioSpectrum(levels, maximumFrequencyHz = 8_000) {
    if (!levels?.length) return false;
    levels.fill(0);
    if (this.paused || !this.source || !this.analyserNode || this.audioContext?.state !== 'running') return false;
    const sampleCount = this.analyserNode.frequencyBinCount;
    if (!this.spectrumSamples || this.spectrumSamples.length !== sampleCount) {
      this.spectrumSamples = new Uint8Array(sampleCount);
    }
    this.analyserNode.getByteFrequencyData(this.spectrumSamples);
    const nyquist = Number(this.audioContext.sampleRate) / 2;
    const sourceSampleRate = Number(this.currentSourceSampleRate || this.currentBuffer?.sampleRate);
    const sourceNyquist = Number.isFinite(sourceSampleRate) && sourceSampleRate > 0 ? sourceSampleRate / 2 : nyquist;
    const maximum = Number(maximumFrequencyHz);
    const visibleBins = Number.isFinite(nyquist) && nyquist > 0 && Number.isFinite(maximum) && maximum > 0 ?
      Math.min(sampleCount, Math.max(2,
        Math.ceil(Math.min(maximum, sourceNyquist, nyquist) / nyquist * sampleCount))) : sampleCount;
    for (let bar = 0; bar < levels.length; bar++) {
      const start = Math.max(1, Math.floor(bar * visibleBins / levels.length));
      const end = Math.max(start + 1, Math.floor((bar + 1) * visibleBins / levels.length));
      let peak = 0;
      let total = 0;
      for (let sample = start; sample < Math.min(end, visibleBins); sample++) {
        const value = this.spectrumSamples[sample] / 255;
        peak = Math.max(peak, value);
        total += value;
      }
      const average = total / Math.max(1, Math.min(end, visibleBins) - start);
      const value = Math.max(peak * .72, average);
      levels[bar] = value < .01 ? 0 : Math.min(1, Math.sqrt(value));
    }
    return true;
  }

  renderVolume() {
    const percent = Math.round(this.volume * 100);
    this.ui.volume.setAttribute('aria-valuetext', `${percent} percent`);
    const container = this.ui.volume.closest('.playback-volume');
    container?.style.setProperty('--playback-volume-level', `${percent}%`);
  }

  isAllowed(call) {
    const target = call?._playbackTargetKey || this.targetKey(call);
    return !this.avoids.has(target) && (!this.holdTarget || this.holdTarget === target);
  }

  targetKey(call) {
    return String(call?._playbackTargetKey || call?.playback_target?.key || '').trim();
  }

  canonicalRadioLabel(call, prefix) {
    const identity = call?.[`${prefix}_canonical_identity`];
    if (!formatP25RadioIdentifier(identity)) return '';
    const homeKey = `p25:${Number(identity.wacn).toString(16).padStart(5, '0')}:` +
      Number(identity.system_id).toString(16).padStart(3, '0');
    const homeName = String(call?.[`${prefix}_home_system_name`] || '').trim() ||
      systemName(homeKey);
    return formatP25RadioIdentifier(identity, {
      servingSystemKey: p25ServingSystemKey(call, prefix), homeSystemName: homeName,
      workingId: call?.[`${prefix}_observed_working_id`]
    });
  }

  callLabel(call) {
    const targetId = call.target_id === null || call.target_id === undefined || call.target_id === '' ? '' :
      String(call.target_id);
    const sourceId = call.source_id === null || call.source_id === undefined || call.source_id === '' ? '' :
      String(call.source_id);
    const targetType = this.identifierType(call.target_form, 'ID');
    const sourceType = this.identifierType(call.source_form, 'Radio');
    const targetCanonical = this.canonicalRadioLabel(call, 'target');
    const sourceCanonical = this.canonicalRadioLabel(call, 'source');
    const targetIdentity = targetCanonical || (targetId ? `${targetType} ${targetId}` : '');
    const sourceIdentity = sourceCanonical || (sourceId ? `${sourceType} ${sourceId}` : '');
    const playbackLabel = typeof call.playback_target?.label === 'string' ?
      call.playback_target.label.trim() : '';
    const analogMode = String(call.decoder || call.protocol || '').trim().toUpperCase();
    const analog = ['AM', 'NBFM'].includes(analogMode);
    const target = analog ?
      (playbackLabel || call.channel || 'Saved channel') :
      (call.target_alias ?
        `${call.target_alias}${targetIdentity ? ` · ${targetIdentity}` : ''}` :
        (targetIdentity || call.channel || 'Unknown target'));
    const sourceName = formatSourceName(call.source_alias, call.talker_alias, this.getSourceNameDisplay?.());
    const source = sourceName ?
      `${sourceName}${sourceIdentity ? ` · ${sourceIdentity}` : ''}` : sourceIdentity;
    return `${target}${source ? ` ← ${source}` : ''}`;
  }

  targetLabel(call) {
    if (!call) return '';
    const playbackKind = String(call.playback_target?.kind || '').trim().toLowerCase();
    const canonical = playbackKind === 'radio' || !playbackKind ?
      this.canonicalRadioLabel(call, 'target') : '';
    const alias = String(call.target_alias || '').trim();
    if (canonical) return alias ? `${alias} · ${canonical}` : canonical;
    const playbackLabel = typeof call.playback_target?.label === 'string' ? call.playback_target.label.trim() : '';
    if (playbackLabel) return playbackLabel;
    if (alias) return alias;
    const targetId = call.target_id === null || call.target_id === undefined || call.target_id === '' ? '' :
      String(call.target_id);
    if (targetId) return `${this.identifierType(call.target_form, 'ID')} ${targetId}`;
    return String(call.channel || '').trim() || 'Unknown identity';
  }

  avoidSystemScope(call) {
    const kind = String(call?.playback_target?.kind || '').trim().toLowerCase();
    if (!['talkgroup', 'patch_group', 'radio'].includes(kind)) return '';
    const key = String(call?.playback_target?.radio_system_key || call?.radio_system_key || '').trim();
    return systemLabel({ ...call, radio_system_key: key });
  }

  currentTargetLabel() {
    return this.targetLabel(this.current);
  }

  identifierType(form, fallback) {
    const normalized = String(form || '').toUpperCase();
    const label = {
      PATCH_GROUP: 'Patch',
      RADIO: 'Radio',
      TALKGROUP: 'TGID'
    }[normalized];
    return label || (normalized ? 'ID' : fallback);
  }

  callDetails(call) {
    const details = [];
    const system = systemLabel(call);
    if (system) details.push(system);
    if (call.channel) details.push(String(call.channel));
    const frequency = Number(call.frequency_hz);
    if (Number.isFinite(frequency) && frequency > 0) details.push(`${(frequency / 1000000).toFixed(5)} MHz`);
    const timeslot = Number(call.timeslot);
    if (call.timeslot !== null && call.timeslot !== undefined && call.timeslot !== '' &&
        Number.isFinite(timeslot) && timeslot >= 0) {
      details.push(`Slot ${timeslot}`);
    }
    if (call.decoder) details.push(String(call.decoder));
    if (call.encrypted) details.push('Encrypted');
    const duration = Number(call.duration_ms);
    if (Number.isFinite(duration) && duration > 0) details.push(`${(duration / 1000).toFixed(1)}s`);
    const names = (call._matchedScanListIds || []).map((id) => this.scanListById.get(String(id))?.name)
      .filter(Boolean);
    if (names.length) details.push(names.join(', '));
    return details.join(' · ');
  }

  setStatus(value) {
    this.statusValue = String(value || '');
    this.renderStatus();
    this.notifyStateObservers();
  }

  setVolume(value, persist = true) {
    const volume = Number(value);
    if (!Number.isFinite(volume)) return;
    if (this.ui.volume) this.ui.volume.value = String(Math.max(0, Math.min(1, volume)));
    this.changeVolume(persist);
  }

  renderStatus() {
    if (!this.ui.status) return;
    const status = this.paused ? `Paused — ${this.queuedCount} calls queued` : this.statusValue;
    this.ui.status.textContent = status;
  }

  render() {
    this.renderVolume();
    this.renderStatus();
    const currentReady = Boolean(this.current && this.currentBuffer);
    const lastCallReady = Boolean(this.lastHeard && this.lastHeardBuffer);
    const displayedCall = this.displayCall();
    this.ui.current.replaceChildren();
    if (displayedCall) {
      const label = this.callLabel(displayedCall);
      const primary = document.createElement('strong');
      primary.className = 'playback-current-primary';
      primary.textContent = label;
      this.ui.current.append(primary);
      const details = this.callDetails(displayedCall);
      if (details) {
        const secondary = document.createElement('span');
        secondary.className = 'playback-current-secondary';
        secondary.textContent = details;
        this.ui.current.append(secondary);
      }
      this.ui.current.title = details ? `${label}\n${details}` : label;
    } else {
      const idle = document.createElement('strong');
      idle.className = 'playback-current-primary';
      idle.textContent = 'Idle';
      this.ui.current.append(idle);
      this.ui.current.removeAttribute('title');
    }
    this.ui.queued.textContent = String(this.queuedCount);
    this.ui.queueList.replaceChildren();
    this.scheduledQueue(100).forEach((call, index) => {
      const item = document.createElement('div');
      item.className = 'playback-queue-item';
      const order = document.createElement('span');
      order.className = 'playback-queue-order';
      order.textContent = `${index + 1}.`;
      const copy = document.createElement('span');
      copy.textContent = this.callLabel(call);
      const details = this.callDetails(call);
      if (details) copy.title = details;
      item.append(order, copy);
      this.ui.queueList.append(item);
    });
    if (!this.queuedCount) {
      const empty = document.createElement('div');
      empty.className = 'playback-panel-empty muted';
      empty.textContent = 'No queued calls';
      this.ui.queueList.append(empty);
    }

    const playLabel = this.stopped ? 'Play browser call audio' : 'Stop browser call audio';
    const playIcon = this.ui.play.querySelector('use');
    if (playIcon) playIcon.setAttribute('href', this.stopped ? '#icon-play' : '#icon-stop');
    this.ui.play.classList.toggle('active', !this.stopped);
    this.ui.play.setAttribute('aria-pressed', String(!this.stopped));
    this.ui.play.setAttribute('aria-label', playLabel);
    this.ui.play.title = playLabel;
    if (this.ui.pause) {
      const pauseLabel = this.paused ? 'Resume browser call audio' :
        'Pause audio and keep collecting calls (queue is limited)';
      this.ui.pause.disabled = this.stopped;
      this.ui.pause.classList.toggle('active', this.paused);
      this.ui.pause.setAttribute('aria-pressed', String(this.paused));
      this.ui.pause.setAttribute('aria-label', pauseLabel);
      this.ui.pause.title = pauseLabel;
      this.ui.pause.querySelector('use')?.setAttribute('href', this.paused ? '#icon-play' : '#icon-pause');
    }
    this.ui.replay.disabled = !lastCallReady || this.paused;
    this.ui.replay.setAttribute('aria-label', 'Replay last call');
    this.ui.replay.title = 'Replay last call';
    this.ui.hold.classList.toggle('active', Boolean(this.holdTarget));
    this.ui.hold.disabled = this.replayingLast || (!this.holdTarget && !currentReady);
    this.ui.hold.title = this.holdTarget ? 'Release browser hold' : 'Hold the current target in this browser';
    this.ui.hold.setAttribute('aria-label', this.ui.hold.title);
    this.ui.avoid.disabled = !currentReady || this.replayingLast;
    if (this.ui.avoidList) {
      const avoidListLabel = `View ${this.avoids.size} browser avoid(s)`;
      this.ui.avoidList.setAttribute('aria-label', avoidListLabel);
      this.ui.avoidList.title = avoidListLabel;
    }
    if (this.ui.clearQueue) {
      this.ui.clearQueue.disabled = !this.queuedCount;
      this.ui.clearQueue.title = `Clear ${this.queuedCount} queued browser call(s)`;
      this.ui.clearQueue.setAttribute('aria-label', this.ui.clearQueue.title);
    }
    this.ui.skip.disabled = !this.current && !this.queuedCount;
    this.renderProgress();
    this.notifyStateObservers();
  }
}
