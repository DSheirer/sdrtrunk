import { systemName, systemIdentity } from '../core/system-labels.js?v=1';
import { createTableOverflow } from '../core/table-overflow.js?v=1';
import { createDiscoveryRadioReferenceContext, discoveryRadioReferenceResult, discoveryRadioReferenceSystemUrl, ALIAS_LIST_NAME_MAX_LENGTH, discoveryAliasListName, discoveryAliasImportChoice, discoveryAliasImportResults, importDiscoveryAliases } from './discovery-radioreference.js?v=8';

export function spectrumSearchProtocolLabel(candidate, profiles = []) {
  const id = String(candidate?.protocol_id || '');
  const known = { p25phase1: 'P25 Phase 1', p25phase2: 'P25 Phase 2', p25: 'P25', dmr: 'DMR', nxdn: 'NXDN' };
  const label = profiles.find((profile) => profile.id === id)?.label ||
    known[id.toLowerCase().replace(/[^a-z0-9]/g, '')] || id.toUpperCase() || 'Digital';
  const normalized = (value) => String(value).toLowerCase().replace(/[^a-z0-9]/g, '');
  const variant = candidate?.variant;
  const variants = { typec: 'Type-C', typed: 'Type-D', tieriii: 'Tier III',
    hyteratieriii: 'Hytera Tier III', capacitymax: 'Capacity Max', capacityplus: 'Capacity Plus',
    connectplus: 'Connect Plus' };
  const variantLabel = known[normalized(variant)] || variants[normalized(variant)] ||
    String(variant || '').replace(/_/g, ' ');
  if (!variant || normalized(label).includes(normalized(variantLabel))) return label;
  return normalized(variantLabel).startsWith(normalized(label)) ? variantLabel : `${label} ${variantLabel}`;
}

export function spectrumSearchSystemName(candidate) {
  const identity = candidate?.identity || candidate?.trunked_evidence?.identity;
  const wacn = Number(identity?.wacn);
  const system = Number(identity?.system);
  const validIdentity = identity?.wacn != null && identity?.system != null &&
    Number.isInteger(wacn) && wacn >= 0 && wacn <= 0xfffff &&
    Number.isInteger(system) && system >= 0 && system <= 0xfff;
  return systemName(candidate?.system_identity || identity?.system_identity) ||
    systemName({ ...candidate, protocol: candidate?.protocol_id || 'P25',
    radio_system_key: candidate?.radio_system_key || (validIdentity ?
      `p25:${wacn.toString(16).padStart(5, '0')}:${system.toString(16).padStart(3, '0')}` : identity?.radio_system_key || '') });
}


export function spectrumSearchIdentityFacts(candidate, hex) {
  const identity = candidate?.identity || candidate?.trunked_evidence?.identity;
  if (!identity) return [];
  if (identity.wacn != null) return [['WACN', hex(identity.wacn, 5)], ['System ID', hex(identity.system, 3)],
    ['RFSS', identity.rfss], ['Site ID', identity.site]];
  return [['Protocol', spectrumSearchProtocolLabel(candidate)],
    ['Network', identity.network], ['System ID', identity.system], ['Site ID', identity.site],
    ['Model', identity.model], ['Location category', identity.category], ['Integrator', identity.integrator],
    ['RAN', identity.ran], ['Color code', identity.color_code]]
    .filter(([, value]) => value != null && value !== '');
}

export function spectrumSearchMapDraft(control) {
  return [...control.querySelectorAll('.channel-map-row')].map((row) => {
    const inputs = row.querySelectorAll('input');
    return { number: inputs[0]?.value || '', downlink_hz: inputs[1]?.value ? Number(inputs[1].value) * 1_000_000 : null,
      uplink_hz: inputs[2]?.value ? Number(inputs[2].value) * 1_000_000 : 0 };
  });
}

export function spectrumSearchGroupKey(candidate) {
  const identity = candidate?.identity || candidate?.trunked_evidence?.identity || {};
  return candidate.alias_group_id || candidate.radio_system_key || identity.radio_system_key ||
    (identity.wacn != null && identity.system != null ? `p25:${identity.wacn}:${identity.system}` :
      `candidate:${candidate.candidate_id}`);
}

export function openSpectrumSearchWizard(ui, context = {}) {
  const { node, openReadOnlyModal, requestJson, uiActionButton, iconButton, uiSelect, uiSelectFrame, uiPill,
    formField, anchor, href, entityRefHref, channelMHz, hex, channelEditorControl, channelEditorFieldValue, protocols } = ui;
  const path = '/api/v1/admin/spectrum-search';
  const abort = new AbortController();
  const host = node('div', 'spectrum-discovery-workspace spectrum-search-workspace editor-workspace');
  const summary = node('div', 'spectrum-discovery-context');
  const steps = node('ol', 'spectrum-discovery-progress');
  steps.setAttribute('aria-label', 'Find Trunked Systems steps');
  const stage = node('div', 'spectrum-discovery-stage');
  const feedback = node('div', 'ui-feedback');
  feedback.setAttribute('role', 'status');
  const actions = node('footer', 'ui-modal-footer ui-action-row spectrum-discovery-actions');
  host.append(summary, steps, stage, actions);
  let catalog = null;
  let receiverId = '';
  let usedReceiverId = '';
  let selectedBandIds = null;
  const customRange = { minimum: '769', maximum: '775' };
  let lease = null;
  let job = null;
  let releasedJobId = null;
  let retiringJobId = null;
  let jobReleaseInFlight = null;
  const leaseReleasesInFlight = new Map();
  const lateLeases = new Map();
  let receiverFinishPromise = null;
  let pollTimer = null;
  let renewalTimer = null;
  let closed = false;
  const pendingReceiverUses = new Set();
  const trackReceiverUse = async (action) => {
    const pending = action();
    pendingReceiverUses.add(pending);
    try { return await pending; }
    finally { pendingReceiverUses.delete(pending); }
  };
  let busy = false;
  let generation = 0;
  let progressNodes = null;
  let currentStep = '';
  let wantsAliasImport = true;
  let aliasImportResult = null;
  let receiverChooser = null;
  let reviewBindings = [];
  let reviewGroupBindings = [];
  let resultBindings = null;
  let detailBinding = null;
  let disposeBandPicker = () => {};
  const resultOverflows = new Set();
  const disposeResultOverflows = () => {
    resultOverflows.forEach((overflow) => overflow.destroy());
    resultOverflows.clear();
  };
  let refreshedCheckingRun = null;
  let receiverMeasurementRevision = 0;
  const preparedCatalogTuners = new Map();
  const selected = new Set();
  const drafts = new Map();
  const groups = new Map();
  const disabled = new Map();
  const modal = openReadOnlyModal('Find Trunked Systems', host, {
    id: 'spectrum-search', className: 'channel-editor-modal spectrum-search-modal',
    cleanup: () => {
      closed = true;
      generation += 1;
      disposeBandPicker();
      disposeResultOverflows();
      abort.abort();
      window.removeEventListener('pagehide', abandon);
      void releaseForClose();
    }
  });
  if (!modal) return null;
  const current = () => !closed && !abort.signal.aborted;
  const request = async (url, options = {}) => {
    if (modal.ready && !await modal.ready || !current())
      throw new DOMException('Dialog dismissed', 'AbortError');
    return requestJson(url, { page: false, signal: options.method ? null : abort.signal, ...options });
  };
  const jobPath = () => `${path}/${encodeURIComponent(job.job_id)}`;
  const liveJob = () => Boolean(job?.job_id && job.job_id !== releasedJobId && job.job_id !== retiringJobId);
  const browsePath = (id) => `/api/v1/admin/tuners/${encodeURIComponent(id)}/browse`;
  const leaseNoLongerExists = (cause) => [404, 410].includes(Number(cause?.status)) ||
    ['tuner_not_found', 'tuner_browse_expired'].includes(String(cause?.code || ''));
  const leaseRenewalLostOwnership = (cause) => leaseNoLongerExists(cause) ||
    Number(cause?.status) === 409 || String(cause?.code || '') === 'tuner_browse_unavailable';
  const jobNoLongerExists = (cause) => [404, 410].includes(Number(cause?.status)) ||
    ['search_expired', 'spectrum_search_expired'].includes(String(cause?.code || ''));
  const clearRenewal = () => {
    window.clearTimeout(renewalTimer);
    renewalTimer = null;
  };
  const scheduleRenewal = () => {
    clearRenewal();
    if (current() && (lease || liveJob())) renewalTimer = window.setTimeout(() => void renew(), 10000);
  };
  const leaseKey = (value, id) => `${id}:${value.lease_id}`;
  const endOwnedLease = async (previous, id) => {
    const key = leaseKey(previous, id);
    const pending = leaseReleasesInFlight.get(key) || requestJson(browsePath(id), {
        method: 'DELETE', body: { lease_id: previous.lease_id }, page: false, keepalive: true,
        timeoutMs: 3000
      });
    leaseReleasesInFlight.set(key, pending);
    try { await pending; }
    catch (cause) { if (!leaseNoLongerExists(cause)) throw cause; }
    finally { if (leaseReleasesInFlight.get(key) === pending) leaseReleasesInFlight.delete(key); }
  };
  const releaseLease = async ({ bestEffort = false } = {}) => {
    clearRenewal();
    const previous = lease;
    if (!previous?.lease_id) return true;
    try {
      await endOwnedLease(previous, previous.tuner?.id || usedReceiverId);
      if (lease?.lease_id === previous.lease_id) lease = null;
      return true;
    } catch (cause) {
      if (bestEffort) return false;
      if (current() && lease?.lease_id === previous.lease_id) scheduleRenewal();
      throw cause;
    }
  };
  const releaseLateLease = async (acquired, id) => {
    if (lease?.lease_id === acquired.lease_id && usedReceiverId === id) {
      if (!current()) await releaseLease({ bestEffort: true });
      return;
    }
    const key = leaseKey(acquired, id);
    lateLeases.set(key, { acquired, id });
    try {
      await endOwnedLease(acquired, id);
      lateLeases.delete(key);
    } catch (_) {
      // Keep this exact lease for the ordered close cleanup to retry.
    }
  };
  const releaseJob = async ({ bestEffort = false, preserveResults = false } = {}) => {
    window.clearTimeout(pollTimer);
    pollTimer = null;
    const previous = job;
    if (!previous?.job_id) return true;
    const retainOrClear = () => {
      if (job?.job_id !== previous.job_id) return;
      if (preserveResults) releasedJobId = previous.job_id;
      else { job = null; releasedJobId = null; retiringJobId = null; }
    };
    if (previous.job_id === releasedJobId) { retainOrClear(); return true; }
    retiringJobId = previous.job_id;
    const pending = jobReleaseInFlight?.id === previous.job_id ? jobReleaseInFlight.promise :
      requestJson(`${path}/${encodeURIComponent(previous.job_id)}`, {
        method: 'DELETE', page: false, keepalive: true, timeoutMs: 3000
      });
    jobReleaseInFlight = { id: previous.job_id, promise: pending };
    try {
      await pending;
      retainOrClear();
      scheduleRenewal();
      return true;
    } catch (cause) {
      if (jobNoLongerExists(cause)) {
        retainOrClear();
        scheduleRenewal();
        return true;
      }
      if (bestEffort) return false;
      throw cause;
    } finally {
      if (jobReleaseInFlight?.promise === pending) jobReleaseInFlight = null;
    }
  };
  const releaseDelay = (milliseconds) => new Promise((resolve) => window.setTimeout(resolve, milliseconds));
  const releaseForClose = async () => {
    clearRenewal();
    await Promise.allSettled([...pendingReceiverUses]);
    const expiresAt = Math.max(Number(job?.expires_at_ms) || 0,
      Number(lease?.expires_at_epoch_ms || lease?.expires_at_ms) || 0,
      ...[...lateLeases.values()].map(({ acquired }) =>
        Number(acquired.expires_at_epoch_ms || acquired.expires_at_ms) || 0));
    const deadline = Math.max(Date.now() + 10_000, expiresAt) + 500;
    while (job?.job_id || lease?.lease_id || lateLeases.size) {
      const jobReleased = await releaseJob({ bestEffort: true });
      if (jobReleased) {
        await releaseLease({ bestEffort: true });
        for (const { acquired, id } of [...lateLeases.values()]) await releaseLateLease(acquired, id);
      }
      if (!job?.job_id && !lease?.lease_id && !lateLeases.size) break;
      if (Date.now() >= deadline) {
        job = null;
        lease = null;
        lateLeases.clear();
        break;
      }
      await releaseDelay(Math.min(1000, Math.max(0, deadline - Date.now())));
    }
  };
  const abandon = () => {
    abort.abort();
    void releaseForClose();
  };
  window.addEventListener('pagehide', abandon);
  const button = (label, action, primary = false, host = actions) => {
    const control = uiActionButton(label, '', action, `ui-button ui-button-${primary ? 'primary' : 'secondary'}`);
    host.append(control);
    return control;
  };
  const setBusy = (value) => {
    busy = value;
    modal.setBusy(value);
    stage.setAttribute('aria-busy', String(value));
    directory.setBusy(value);
    if (value) host.querySelectorAll('input,select,button').forEach((control) => {
      if (directory.element.contains(control)) return;
      if (!disabled.has(control)) disabled.set(control, control.disabled);
      control.disabled = true;
    });
    else {
      disabled.forEach((wasDisabled, control) => { if (control.isConnected) control.disabled = wasDisabled; });
      disabled.clear();
    }
  };
  const disclosure = (label, ...children) => {
    const details = node('details', 'ui-section-disclosure ui-section-disclosure-flat spectrum-discovery-technical');
    details.append(node('summary', 'ui-section-summary', label), ...children);
    return details;
  };
  const error = (copy, cause) => {
    feedback.hidden = false;
    feedback.replaceChildren(node('p', '', copy));
    if (cause?.message) feedback.append(disclosure('Error details', node('p', 'muted', cause.message)));
  };
  const clearFeedback = () => {
    feedback.replaceChildren();
  };
  const show = (next, index, title) => {
    detailBinding?.modal.close();
    disposeBandPicker();
    disposeBandPicker = () => {};
    disposeResultOverflows();
    progressNodes = null;
    currentStep = next;
    receiverChooser = null;
    reviewBindings = [];
    reviewGroupBindings = [];
    resultBindings = null;
    steps.hidden = next === 'saved';
    const aliasStep = next === 'aliases' || next === 'importing' || next === 'review' &&
      selectedCandidates().some((candidate) => (job.alias_groups || []).some((group) =>
        group.group_id === candidate.alias_group_id && !group.alias_lists?.length));
    const labels = next === 'progress' ? ['Bands', 'Find channels', 'Review & add'] :
      ['Bands', 'Select channels', aliasStep ? 'Review' : 'Review & add', ...(aliasStep ? ['Talkgroups'] : [])];
    steps.dataset.stepCount = String(labels.length);
    steps.replaceChildren(...labels.map((label, position) => {
      const step = node('li');
      step.append(uiPill(position < index ? '✓' : String(position + 1),
        position < index ? 'success' : position === index ? 'blue' : 'neutral'), node('span', '', label));
      if (position === index) step.setAttribute('aria-current', 'step');
      return step;
    }));
    const heading = node('h3', '', title);
    heading.tabIndex = -1;
    stage.replaceChildren(heading, feedback);
    feedback.hidden = false;
    clearFeedback();
    actions.replaceChildren();
    heading.focus({ preventScroll: true });
  };
  const receiver = () => catalog?.tuners?.find((tuner) => tuner.id === receiverId);
  const candidates = () => job?.candidates || [];
  const directory = createDiscoveryRadioReferenceContext(ui, request);
  const recordingWindow = () => {
    const tuner = receiver();
    if (!tuner?.fixed_window && tuner?.source_type !== 'recording') return null;
    const center = Number(tuner.center_frequency_hz ?? tuner.frequency_hz);
    const width = Number(tuner.usable_bandwidth_hz);
    return Number.isFinite(center) && width > 0 ? { minimum_hz: Math.ceil(center - width / 2),
      maximum_hz: Math.floor(center + width / 2) } : null;
  };
  const selectedCandidates = () => candidates().filter((candidate) => selected.has(candidate.candidate_id));
  const pendingDirectory = () => candidates().some((candidate) => candidate.radio_reference?.state === 'pending');
  const scheduleDirectoryPoll = () => {
    if (current() && liveJob() && job.phase === 'complete' && pendingDirectory()) {
      window.clearTimeout(pollTimer);
      pollTimer = window.setTimeout(() => void poll(), 750);
    }
  };
  const refreshReviewDirectory = () => {
    reviewBindings.forEach((binding) => {
      const candidate = candidates().find((value) => value.candidate_id === binding.id);
      if (!candidate) return;
      binding.site.textContent = siteLabel(candidate);
      if (!binding.draft.name_edited) {
        binding.draft.name = candidateName(candidate);
        binding.name.textContent = binding.draft.name;
        if (binding.input) binding.input.value = binding.draft.name;
      }
      const values = candidate.trunked_evidence?.frequency_map || [];
      if (binding.map && !binding.draft.map_draft && JSON.stringify(values) !== binding.mapValues) {
        const updated = binding.createMap(values);
        binding.mapValues = JSON.stringify(values);
        binding.map.replaceWith(updated);
        binding.map = updated;
        binding.editor.map = updated;
      }
    });
    reviewGroupBindings.forEach((binding) => refreshGroupDirectory(binding));
  };
  const identityFacts = (candidate) => spectrumSearchIdentityFacts(candidate, hex);
  const identityText = (candidate) => identityFacts(candidate).map(([label, value]) => `${label} ${value}`).join(' · ') ||
    'Identity unavailable';
  const candidateName = (candidate) => candidate.name || candidate.site_name || `${channelMHz(candidate.frequency_hz)} MHz`;
  const systemLabel = (candidate) => (candidate.radio_reference?.state === 'matched' &&
    candidate.radio_reference.match?.system_name) || spectrumSearchSystemName(candidate) || systemIdentity({ ...candidate,
    radio_system_key: candidate.radio_system_key || candidate.trunked_evidence?.identity?.radio_system_key }) ||
    (candidate.identity ? `P25 ${hex(candidate.identity.wacn, 5)}-${hex(candidate.identity.system, 3)}` :
      `${String(candidate.protocol_id || 'Digital').toUpperCase()} system`);
  const aliasIdentityLabel = (candidate) => candidate?.identity?.wacn != null ?
    `${hex(candidate.identity.wacn, 5)}-${hex(candidate.identity.system, 3)}` : systemLabel(candidate);
  const factList = (entries) => {
    const list = node('dl', 'ui-fact-list');
    entries.forEach(([label, value]) => list.append(node('dt', '', label), node('dd', '', String(value ?? '—'))));
    return list;
  };
  const receiverSummary = (tuner) => {
    const frequency = Number(tuner.center_frequency_hz);
    const width = Number(tuner.usable_bandwidth_hz);
    const frequencyText = Number.isFinite(frequency) && frequency > 0 ?
      `${(frequency / 1_000_000).toFixed(6)} MHz` : 'Frequency unavailable';
    const widthText = Number.isFinite(width) && width > 0 ?
      `${(width / 1_000_000).toFixed(2)} MHz` : 'Width unavailable';
    return `${frequencyText} @ ${widthText}`;
  };
  const updateReceiverMeasurements = (tuner, id) => {
    if (!tuner || tuner.id !== id) return;
    const measurements = {};
    if (Object.hasOwn(tuner, 'frequency_hz')) measurements.center_frequency_hz = tuner.frequency_hz;
    else if (Object.hasOwn(tuner, 'center_frequency_hz')) measurements.center_frequency_hz = tuner.center_frequency_hz;
    if (Object.hasOwn(tuner, 'usable_bandwidth_hz')) measurements.usable_bandwidth_hz = tuner.usable_bandwidth_hz;
    catalog.tuners = (catalog.tuners || []).map((candidate) => {
      if (candidate.id !== id) return candidate;
      if (Object.entries(measurements).some(([key, value]) => !Object.is(candidate[key], value)))
        receiverMeasurementRevision += 1;
      return { ...candidate, ...measurements };
    });
  };
  const updateContext = () => {
    let tuner = receiver();
    const currentCenter = Number(job?.progress?.current_frequency_hz);
    if (tuner && job?.phase === 'scanning' && lease?.lease_id && receiverId === usedReceiverId &&
        Number.isFinite(currentCenter) && currentCenter > 0)
      tuner = { ...tuner, center_frequency_hz: currentCenter };
    receiverChooser?.querySelectorAll('option').forEach((option) => {
      const listed = catalog.tuners.find((candidate) => candidate.id === option.value);
      if (listed) option.textContent = `${listed.name} · ${receiverSummary(listed)}`;
    });
    summary.replaceChildren(node('strong', '', tuner?.name || 'Choose an idle receiver'),
      node('p', 'muted', tuner ? `${tuner.source_type === 'recording' ? 'WAV recording · ' : ''}${receiverSummary(tuner)}` :
        'Find P25, DMR, and NXDN trunked systems.'));
  };
  const refreshCheckingMeasurements = async () => {
    const operation = generation;
    const previousLeaseId = lease?.lease_id;
    const id = usedReceiverId;
    const jobId = job?.job_id;
    const candidateFrequency = Number(job?.progress?.current_frequency_hz);
    const previousMeasurements = receiverMeasurementRevision;
    const run = `${operation}:${jobId}:${candidateFrequency}`;
    if (!current() || !previousLeaseId || receiverId !== id || job?.phase !== 'checking' ||
        !Number.isFinite(candidateFrequency) || candidateFrequency <= 0 || refreshedCheckingRun === run) return;
    refreshedCheckingRun = run;
    try {
      const inventory = await request('/api/v1/admin/tuners', { csrf: false });
      if (!current() || operation !== generation || lease?.lease_id !== previousLeaseId ||
          usedReceiverId !== id || receiverId !== id || job?.job_id !== jobId || job.phase !== 'checking' ||
          Number(job.progress?.current_frequency_hz) !== candidateFrequency ||
          receiverMeasurementRevision !== previousMeasurements) return;
      updateReceiverMeasurements((inventory.tuners || []).find((tuner) => tuner.id === id), id);
      updateContext();
    } catch (_) {
      // The last observed receiver center remains useful if this optional read fails.
    }
  };
  const receiverPreparationState = (tuner) => {
    const channelCount = Math.max(0, Number(tuner.channel_count || 0));
    const lockSetting = (tuner.settings || []).find((setting) => setting.id === 'center_frequency_locked');
    const reasonText = String(tuner.reason || '');
    const needsStop = channelCount > 0 || /channel|serving/i.test(reasonText);
    const locked = tuner.center_frequency_locked === true || lockSetting?.value === true ||
      /lock center|center.*lock|unlock/i.test(reasonText);
    const canPrepare = typeof context.prepareReceiver === 'function' && tuner.takeover_allowed !== false &&
      (needsStop || locked);
    return { tuner, channelCount, reasonText, needsStop, locked, canPrepare };
  };
  const prepareReceiver = (state) => trackReceiverUse(() => prepareReceiverUse(state));
  const prepareReceiverUse = async ({ tuner }) => {
    if (busy) return;
    const operation = generation;
    setBusy(true);
    clearFeedback();
    feedback.hidden = true;
    try {
      const acquired = await context.prepareReceiver(tuner);
      if (!acquired) return;
      if (!current() || operation !== generation) {
        await releaseLateLease(acquired, tuner.id);
        return;
      }
      if (!preparedCatalogTuners.has(tuner.id)) preparedCatalogTuners.set(tuner.id, tuner);
      receiverId = tuner.id;
      usedReceiverId = tuner.id;
      lease = acquired;
      const prepared = { ...tuner, ...(acquired.tuner || {}), eligible: true, reason: null,
        channel_count: 0, center_frequency_locked: false };
      catalog.tuners = (catalog.tuners || []).map((candidate) =>
        candidate.id === tuner.id ? prepared : candidate);
      updateReceiverMeasurements(acquired.tuner, tuner.id);
      scheduleRenewal();
      showBand();
    } catch (cause) {
      error('This receiver could not be prepared. Check its channels and try again.', cause);
    } finally { if (current()) setBusy(false); }
  };
  const receiverCard = (state) => {
    const { tuner, channelCount, reasonText, needsStop, locked, canPrepare } = state;
    const status = channelCount ? `${channelCount} channel${channelCount === 1 ? '' : 's'} running` :
      needsStop ? 'Channels running' : locked ? 'Center locked' : 'Unavailable';
    const row = node('article', 'ui-surface spectrum-search-receiver-card');
    row.setAttribute('role', 'listitem');
    const heading = node('div', 'spectrum-search-receiver-heading');
    const statusPill = uiPill(status, 'warning');
    statusPill.classList.add('spectrum-search-receiver-status');
    heading.append(node('strong', '', tuner.name || 'Receiver'), statusPill);
    const details = node('p', 'muted spectrum-search-receiver-detail', receiverSummary(tuner));
    const copy = node('div', 'spectrum-search-receiver-copy');
    copy.append(heading, details);
    if (!canPrepare && reasonText) copy.append(node('p', 'muted', reasonText));
    row.append(copy);
    if (canPrepare) {
      const prepareLabel = needsStop ? 'Stop channels and use' : 'Unlock and use';
      const prepare = uiActionButton(prepareLabel, '', () => void prepareReceiver(state),
        'ui-button ui-button-primary spectrum-search-prepare-receiver');
      prepare.setAttribute('aria-label', `${prepareLabel}: ${tuner.name || 'Receiver'}`);
      row.append(prepare);
    }
    return row;
  };
  const receiverList = (states) => {
    const list = node('div', 'spectrum-search-receiver-list');
    list.setAttribute('role', 'list');
    states.forEach((state) => list.append(receiverCard(state)));
    return list;
  };
  const renew = async () => {
    if (!current() || (!lease && !liveJob())) return;
    clearRenewal();
    const previous = lease;
    const previousJobId = liveJob() ? job.job_id : null;
    const id = usedReceiverId;
    const operation = generation;
    try {
      if (previous) {
        const renewed = await request(browsePath(id), {
          method: 'POST', body: { lease_id: previous.lease_id }
        });
        if (!current() || lease?.lease_id !== previous.lease_id || operation !== generation || usedReceiverId !== id ||
            (previousJobId && (!liveJob() || job.job_id !== previousJobId))) return;
        lease = renewed;
        updateReceiverMeasurements(renewed.tuner, id);
        updateContext();
      }
      if (!busy && liveJob() && !['scanning', 'checking'].includes(job.phase)) {
        const observed = job;
        const updated = await request(jobPath(), { csrf: false });
        if (current() && !busy && operation === generation && liveJob() && job === observed &&
            job.job_id === updated.job_id) {
          job = updated;
          if (updated.phase !== 'complete' || updated.restart_required) drawJob();
        }
      }
    } catch (cause) {
      if (!current() || operation !== generation || usedReceiverId !== id ||
          (previous && lease?.lease_id !== previous.lease_id) ||
          (previousJobId && (!liveJob() || job.job_id !== previousJobId))) return;
      if (current() && !busy) {
        if (previous && leaseRenewalLostOwnership(cause)) {
          if (lease?.lease_id === previous.lease_id) lease = null;
          showExpired();
          return;
        }
        if (isExpired(cause)) showExpired();
        else error('We couldn’t refresh this search. Check your connection before continuing.', cause);
      }
    }
    if (current() && operation === generation && (lease || liveJob())) scheduleRenewal();
  };
  const showBand = () => {
    const available = (catalog.tuners || []).filter((tuner) => tuner.eligible);
    show('band', 0, available.length ? 'Choose where to look' : 'Choose a receiver');
    if (!available.length) {
      summary.replaceChildren();
      summary.hidden = true;
      feedback.hidden = true;
      const blockedTuners = (catalog.tuners || []).map(receiverPreparationState);
      const allStopping = blockedTuners.length > 0 && blockedTuners.every((state) =>
        state.needsStop && state.canPrepare);
      const anyPreparation = blockedTuners.some((state) => state.canPrepare);
      const anyStopping = blockedTuners.some((state) => state.needsStop && state.canPrepare);
      const unavailable = node('div', 'spectrum-search-unavailable');
      const introduction = node('div', 'ui-notice ui-notice-warning spectrum-search-unavailable-intro');
      introduction.append(node('strong', '', !blockedTuners.length ? 'No receivers are available' :
        allStopping ? 'All receivers are in use' : anyPreparation ? 'A receiver needs to be prepared' :
          'No receiver is ready for a search'),
      node('p', '', !blockedTuners.length ? 'Connect or enable a supported receiver, then refresh this list.' :
        allStopping ? 'Choose one below. VCE will stop its active channels for the search and restart the channels it stopped when you finish. Calls in progress will end and will not resume.' :
          anyPreparation ? (anyStopping ?
            'Choose an action below. VCE restores temporary changes when you finish. Stopping channels may end calls in progress.' :
            'Choose Unlock and use below. VCE will restore Lock center when you finish.') :
            'Check the reason below, then manage receivers or refresh this list.'));
      const manage = node('p', 'ui-field-hint');
      manage.append('Need to change receiver settings? ', anchor('Manage receivers', href('tuners')));
      introduction.append(manage);
      const list = receiverList(blockedTuners);
      unavailable.append(introduction, list);
      stage.append(unavailable);
      button('Close', () => modal.close());
      button('Refresh receivers', () => void load(), !anyPreparation);
      return;
    }
    summary.hidden = false;
    if (!available.some((tuner) => tuner.id === receiverId)) receiverId = catalog.suggested_tuner_id ||
      available.slice().sort((a, b) => b.usable_bandwidth_hz - a.usable_bandwidth_hz)[0].id;
    if (!available.some((tuner) => tuner.id === receiverId)) receiverId = available[0].id;
    const form = node('form', 'spectrum-search-form');
    const chooser = uiSelect(available.map((tuner) => ({ value: tuner.id,
      label: `${tuner.name} · ${receiverSummary(tuner)}` })), receiverId);
    receiverChooser = chooser;
    chooser.setAttribute('aria-label', 'Receiver');
    chooser.addEventListener('change', () => { receiverId = chooser.value; updateContext(); });
    const presets = catalog.presets || [];
    if (selectedBandIds === null) {
      const defaults = presets.filter((preset) => ['700mhz', '800mhz'].includes(preset.id)).map((preset) => preset.id);
      selectedBandIds = new Set(defaults.length ? defaults : [presets[0]?.id || 'custom']);
    }
    const bands = node('fieldset', 'spectrum-search-band-field');
    const bandLabel = node('legend', 'ui-field-label', 'Bands');
    bandLabel.id = 'spectrum-search-band-label';
    const bandTrigger = node('button', 'ui-select spectrum-search-band-trigger');
    bandTrigger.type = 'button';
    bandTrigger.id = 'spectrum-search-band-trigger';
    bandTrigger.setAttribute('aria-labelledby', 'spectrum-search-band-label spectrum-search-band-summary');
    bandTrigger.setAttribute('aria-haspopup', 'dialog');
    bandTrigger.setAttribute('aria-controls', 'spectrum-search-band-options');
    bandTrigger.setAttribute('aria-expanded', 'false');
    bandTrigger.setAttribute('popovertarget', 'spectrum-search-band-options');
    const bandSummary = node('span', 'spectrum-search-band-summary');
    bandSummary.id = 'spectrum-search-band-summary';
    bandTrigger.append(bandSummary);
    const bandMenu = node('div', 'ui-popover spectrum-search-band-popover');
    bandMenu.id = 'spectrum-search-band-options';
    bandMenu.setAttribute('popover', 'auto');
    bandMenu.setAttribute('role', 'dialog');
    bandMenu.setAttribute('aria-labelledby', bandLabel.id);
    bands.append(bandLabel, uiSelectFrame(bandTrigger, 'spectrum-search-band-frame'), bandMenu);
    const custom = node('div', 'channel-editor-grid');
    const minimum = node('input', 'ui-input');
    const maximum = node('input', 'ui-input');
    [minimum, maximum].forEach((input) => { input.type = 'number'; input.step = '0.000001'; });
    minimum.value = customRange.minimum;
    maximum.value = customRange.maximum;
    minimum.addEventListener('input', () => { customRange.minimum = minimum.value; updateBands(); });
    maximum.addEventListener('input', () => { customRange.maximum = maximum.value; updateBands(); });
    custom.append(formField('Start frequency (MHz)', minimum), formField('End frequency (MHz)', maximum));
    const description = node('p', 'ui-field-hint');
    const coverageNotice = node('div', 'ui-notice ui-notice-warning');
    coverageNotice.setAttribute('role', 'status');
    coverageNotice.hidden = true;
    let findSignals;
    const selectedRanges = () => recordingWindow() ? [recordingWindow()] : [
      ...presets.filter((preset) => selectedBandIds.has(preset.id)).flatMap((preset) => preset.ranges),
      ...(selectedBandIds.has('custom') ? [{ minimum_hz: Math.round(Number(minimum.value) * 1_000_000),
        maximum_hz: Math.round(Number(maximum.value) * 1_000_000) }] : [])
    ];
    const selectedBandLabels = () => [...presets, { id: 'custom', label: 'Custom range' }]
      .filter((preset) => selectedBandIds.has(preset.id))
      .map((preset) => preset.label.split(' · ')[0]);
    const configuredRangeFailures = () => {
      const tuner = receiver();
      const outside = (range) => Number.isFinite(range.minimum_hz) && Number.isFinite(range.maximum_hz) &&
        range.minimum_hz > 0 && range.maximum_hz > range.minimum_hz &&
        (range.minimum_hz < Number(tuner?.minimum_frequency_hz) ||
          range.maximum_hz > Number(tuner?.maximum_frequency_hz));
      if (recordingWindow()) return [];
      const invalid = presets.filter((preset) => selectedBandIds.has(preset.id) && preset.ranges.some(outside))
        .map((preset) => preset.label);
      if (selectedBandIds.has('custom') && minimum.value && maximum.value) {
        const range = { minimum_hz: Math.round(Number(minimum.value) * 1_000_000),
          maximum_hz: Math.round(Number(maximum.value) * 1_000_000) };
        if (outside(range)) invalid.push(`Custom range · ${(range.minimum_hz / 1_000_000).toFixed(6)}–${
          (range.maximum_hz / 1_000_000).toFixed(6)} MHz`);
      }
      return invalid;
    };
    const updateBands = () => {
      const fixed = recordingWindow();
      bands.hidden = Boolean(fixed);
      custom.hidden = Boolean(fixed) || !selectedBandIds.has('custom');
      minimum.required = maximum.required = !custom.hidden;
      minimum.disabled = maximum.disabled = custom.hidden;
      const tuner = receiver();
      [minimum, maximum].forEach((input) => {
        input.min = String((tuner?.minimum_frequency_hz || 0) / 1_000_000);
        input.max = String((tuner?.maximum_frequency_hz || 0) / 1_000_000);
      });
      description.textContent = fixed ? `Checks the recording window from ${channelMHz(fixed.minimum_hz)} to ${channelMHz(fixed.maximum_hz)} MHz.` : `Choose one or more bands. Maximum search span: ${(catalog.bounds.maximum_total_hz / 1_000_000).toFixed(0)} MHz.`;
      const labels = selectedBandLabels();
      bandSummary.textContent = labels.length <= 2 ? labels.join(', ') || 'Choose bands' :
        `${labels.slice(0, 2).join(', ')} +${labels.length - 2}`;
      bandTrigger.title = labels.join(', ');
      const invalid = configuredRangeFailures();
      coverageNotice.hidden = !invalid.length;
      coverageNotice.replaceChildren();
      if (invalid.length) {
        coverageNotice.append(node('strong', '', `Configured tuning limits: ${
          (Number(tuner.minimum_frequency_hz) / 1_000_000).toFixed(6)}–${
          (Number(tuner.maximum_frequency_hz) / 1_000_000).toFixed(6)} MHz`),
        node('p', '', `Outside these limits: ${invalid.join('; ')}.`),
        node('p', '', 'Choose another receiver or adjust its configured tuning limits.'));
        coverageNotice.append(anchor('Manage receivers', href('tuners')));
      }
      if (findSignals) findSignals.disabled = Boolean(invalid.length) ||
        (!fixed && !presets.some((preset) => selectedBandIds.has(preset.id)) && custom.hidden);
    };
    [...presets, { id: 'custom', label: 'Custom frequency range' }].forEach((preset) => {
      const choice = node('label', 'ui-choice-card spectrum-search-band-option');
      const check = node('input', 'ui-selection-check');
      check.type = 'checkbox';
      check.value = preset.id;
      check.checked = selectedBandIds.has(preset.id);
      check.addEventListener('change', () => {
        if (check.checked) selectedBandIds.add(preset.id); else selectedBandIds.delete(preset.id);
        updateBands();
      });
      const [label, detail] = preset.id === 'custom' ? ['Custom range', 'Enter a start and end frequency'] :
        preset.label.split(' · ');
      check.setAttribute('aria-label', preset.id === 'custom' ? label : preset.label);
      const copy = node('span', 'spectrum-search-band-option-copy');
      copy.append(node('strong', '', label));
      if (detail) copy.append(node('small', 'ui-field-detail', detail));
      choice.append(check, copy);
      bandMenu.append(choice);
    });
    const positionBandMenu = () => {
      if (!bandMenu.matches(':popover-open')) return;
      const gutter = 8;
      const gap = 6;
      const anchor = bandTrigger.getBoundingClientRect();
      bandMenu.style.minWidth = `${Math.round(anchor.width)}px`;
      bandMenu.style.maxHeight = '';
      const menu = bandMenu.getBoundingClientRect();
      const availableBelow = Math.max(0, window.innerHeight - anchor.bottom - gap - gutter);
      const availableAbove = Math.max(0, anchor.top - gap - gutter);
      const openAbove = availableBelow < Math.min(menu.height, 160) && availableAbove > availableBelow;
      const maxHeight = Math.min(350, openAbove ? availableAbove : availableBelow);
      const top = openAbove ? Math.max(gutter, anchor.top - gap - Math.min(menu.height, maxHeight)) :
        anchor.bottom + gap;
      const left = Math.max(gutter, Math.min(anchor.left, window.innerWidth - menu.width - gutter));
      bandMenu.style.left = `${Math.round(left)}px`;
      bandMenu.style.top = `${Math.round(top)}px`;
      bandMenu.style.maxHeight = `${Math.floor(maxHeight)}px`;
    };
    const bandEvents = new AbortController();
    bandMenu.addEventListener('toggle', (event) => {
      const open = event.newState === 'open';
      bandTrigger.setAttribute('aria-expanded', String(open));
      if (open) {
        positionBandMenu();
        window.requestAnimationFrame(() => {
          if (!current() || !bandMenu.matches(':popover-open') ||
            bandMenu.contains(document.activeElement)) return;
          const choices = [...bandMenu.querySelectorAll('input:not(:disabled)')];
          (choices.find((choice) => choice.checked) || choices[0])?.focus();
        });
      }
    }, { signal: bandEvents.signal });
    bandTrigger.addEventListener('blur', () => window.requestAnimationFrame(() => {
      if (bandMenu.matches(':popover-open') && !bandMenu.contains(document.activeElement) &&
        document.activeElement !== bandTrigger) bandMenu.hidePopover();
    }), { signal: bandEvents.signal });
    bandTrigger.addEventListener('keydown', (event) => {
      if (event.key === 'Escape' && bandMenu.matches(':popover-open')) {
        event.stopPropagation();
        bandMenu.hidePopover();
        return;
      }
      if (!['ArrowDown', 'ArrowUp'].includes(event.key)) return;
      event.preventDefault();
      if (!bandMenu.matches(':popover-open')) bandMenu.showPopover();
      window.requestAnimationFrame(() => {
        const choices = [...bandMenu.querySelectorAll('input:not(:disabled)')];
        (event.key === 'ArrowUp' ? choices.at(-1) : choices[0])?.focus();
      });
    }, { signal: bandEvents.signal });
    bandMenu.addEventListener('keydown', (event) => {
      if (event.key === 'Escape') event.stopPropagation();
    }, { signal: bandEvents.signal });
    bandMenu.addEventListener('focusout', () => window.requestAnimationFrame(() => {
      if (bandMenu.matches(':popover-open') && !bandMenu.contains(document.activeElement) &&
        document.activeElement !== bandTrigger) bandMenu.hidePopover();
    }), { signal: bandEvents.signal });
    window.addEventListener('resize', positionBandMenu, { signal: bandEvents.signal });
    window.addEventListener('scroll', positionBandMenu,
      { signal: bandEvents.signal, capture: true, passive: true });
    disposeBandPicker = () => {
      bandEvents.abort();
      if (bandMenu.matches(':popover-open')) bandMenu.hidePopover();
    };
    chooser.addEventListener('change', updateBands);
    form.append(formField('Receiver', uiSelectFrame(chooser)), bands, custom, description, coverageNotice,
      node('div', 'ui-notice', 'The receiver scans each band, then checks promising signals.'), directory.element);
    void directory.load();
    form.addEventListener('submit', (event) => event.preventDefault());
    stage.append(form);
    updateContext();
    button('Cancel', () => modal.close());
    findSignals = button('Find signals', () => {
      if (!form.reportValidity()) return;
      const ranges = selectedRanges();
      if (!ranges?.length || ranges.some((range) => range.minimum_hz >= range.maximum_hz) ||
        ranges.reduce((total, range) => total + range.maximum_hz - range.minimum_hz, 0) > catalog.bounds.maximum_total_hz) {
        error('Choose a valid band, or enter a smaller range with the end frequency above the start.');
        return;
      }
      if (configuredRangeFailures().length) {
        updateBands();
        return;
      }
      void begin(ranges);
    }, true);
    updateBands();
  };
  const restorePreparedCatalog = () => {
    if (!catalog || !preparedCatalogTuners.size) return;
    catalog.tuners = (catalog.tuners || []).map((tuner) => preparedCatalogTuners.get(tuner.id) || tuner);
    preparedCatalogTuners.clear();
  };
  const refreshReceiverCatalog = async () => {
    const refreshed = await request(`${path}/catalog`, { csrf: false });
    catalog = refreshed;
    preparedCatalogTuners.clear();
  };
  const begin = (ranges) => trackReceiverUse(() => beginSearch(ranges));
  const beginSearch = async (ranges) => {
    const operation = ++generation;
    setBusy(true);
    try {
      await directory.load();
      const directoryStateId = directory.stateId();
      await releaseJob();
      if (!current() || operation !== generation) return;
      const reusingPreparedReceiver = Boolean(lease?.lease_id && usedReceiverId === receiverId);
      if (!reusingPreparedReceiver) await releaseLease();
      if (!current() || operation !== generation) return;
      const id = receiverId;
      usedReceiverId = id;
      const acquired = await request(browsePath(id), { method: 'POST',
        body: reusingPreparedReceiver ? { lease_id: lease.lease_id } : {} });
      if (!current() || operation !== generation) {
        await releaseLateLease(acquired, id);
        return;
      }
      lease = acquired;
      updateReceiverMeasurements(acquired.tuner, id);
      const created = await request(path, { method: 'POST', body: {
        tuner_id: id, browse_lease_id: lease.lease_id, ranges,
        radioreference_state_id: directoryStateId
      }, timeoutMs: 30000 });
      if (!current() || operation !== generation) {
        job = created;
        releasedJobId = null;
        retiringJobId = null;
        await releaseJob({ bestEffort: true });
        return;
      }
      job = created;
      releasedJobId = null;
      retiringJobId = null;
      selected.clear();
      drafts.clear();
      groups.clear();
      modal.setDirty(false);
      scheduleRenewal();
      drawJob();
    } catch (cause) {
      try { await releaseLease(); } catch (releaseCause) {
        if (current() && operation === generation) {
          error('The search did not start, and this receiver could not be released yet. Try again.', releaseCause);
        }
        return;
      }
      if (!current() || operation !== generation) return;
      restorePreparedCatalog();
      try { await refreshReceiverCatalog(); } catch (_) {
        /* The original catalog row is safer than leaving the borrowed receiver shown as idle. */
      }
      if (current() && operation === generation) {
        showBand();
        error(cause.code === 'invalid_request' ?
          cause.message || 'This search is too large for this receiver. Choose a smaller range or a wider receiver.' :
          'We couldn’t start the search. The receiver was restored; check it and try again.', cause);
      }
    } finally { if (current() && operation === generation) setBusy(false); }
  };
  const restart = async () => {
    const operation = ++generation;
    setBusy(true);
    let step = 'job';
    try {
      await releaseJob();
      step = 'receiver';
      await releaseLease();
      step = 'catalog';
      if (!current() || operation !== generation) return;
      await refreshReceiverCatalog();
      if (!current() || operation !== generation) return;
      modal.setDirty(false);
      showBand();
    } catch (cause) {
      if (!current() || operation !== generation) return;
      show('failed', 0, 'Choose where to look');
      error(step === 'job' ?
        'The previous search could not be stopped yet. Try again before starting another search.' :
        step === 'receiver' ?
        (lease?.takeover ?
          'This receiver could not resume its channels yet. Try again before starting another search.' :
          'This receiver could not be released yet. Try again before starting another search.') :
        'We couldn’t refresh the receiver list. Check your connection and try again.', cause);
      button('Close', () => modal.close());
      button('Try again', () => void restart(), true);
    } finally { if (current() && operation === generation) setBusy(false); }
  };
  const drawProgress = () => {
    if (!progressNodes) {
      show('progress', 1, job.phase === 'checking' ? 'Check signals' : 'Find signals');
      const notice = node('div', 'ui-feedback ui-feedback-loading');
      notice.setAttribute('role', 'status');
      const status = node('div', 'spectrum-discovery-status');
      const details = node('div', 'spectrum-discovery-status');
      const heading = node('strong');
      const copy = node('p');
      const count = node('p', 'muted');
      details.append(heading, count, copy);
      notice.append(details);
      status.append(notice);
      stage.append(status, node('p', 'ui-field-hint', 'Nothing is saved during the search. Stop at any time to return to Channels.'));
      button('Stop search', () => modal.close());
      progressNodes = { title: stage.querySelector('h3'), notice, heading, copy, count };
    }
    const checking = job.phase === 'checking';
    progressNodes.title.textContent = checking ? 'Check signals' : 'Find signals';
    const progress = job.progress || {};
    const currentCenter = Number(progress.current_frequency_hz);
    const hasFrequency = Number.isFinite(currentCenter) && currentCenter > 0;
    progressNodes.notice.classList.add('ui-feedback-loading');
    progressNodes.heading.textContent = checking ? (hasFrequency ?
      `Checking ${channelMHz(currentCenter)} MHz` : 'Checking signals') : 'Searching selected bands';
    progressNodes.copy.textContent = checking ? 'Each signal can take up to 30 seconds.' : '';
    progressNodes.copy.hidden = !checking;
    if (!checking && lease?.lease_id && receiverId === usedReceiverId &&
        Number.isFinite(currentCenter) && currentCenter > 0)
      updateReceiverMeasurements({ id: usedReceiverId, frequency_hz: currentCenter }, usedReceiverId);
    updateContext();
    if (checking) void refreshCheckingMeasurements();
    progressNodes.count.textContent = checking ? `${progress.checked || 0} of ${progress.total_signals || 0} signals checked` :
      `${progress.completed || 0} of ${progress.total || 0} band sections searched${hasFrequency ?
        ` · ${channelMHz(currentCenter)} MHz` : ''}`;
    pollTimer = window.setTimeout(() => void poll(), 750);
  };
  const poll = async () => {
    if (!current() || !liveJob()) return;
    const operation = generation;
    const observed = job;
    const directoryRefresh = job.phase === 'complete';
    if (directoryRefresh && busy) { scheduleDirectoryPoll(); return; }
    try {
      const updated = await request(jobPath(), { csrf: false });
      if (!current() || operation !== generation || !liveJob() || job !== observed || busy) return;
      job = updated;
      clearFeedback();
      if (job.restart_required) drawJob();
      else if (directoryRefresh && updated.phase === 'complete' && currentStep === 'review') {
        refreshReviewDirectory();
        scheduleDirectoryPoll();
      } else if (directoryRefresh && updated.phase === 'complete' && currentStep === 'results') {
        refreshResultsDirectory();
        scheduleDirectoryPoll();
      } else if (directoryRefresh && updated.phase === 'complete' && pendingDirectory()) scheduleDirectoryPoll();
      else drawJob();
    } catch (cause) {
      if (!current() || operation !== generation || !liveJob() || job !== observed || busy) return;
      if (isExpired(cause)) { showExpired(); return; }
      if (directoryRefresh) {
        error('Directory names could not be refreshed. You can continue with the on-air details.', cause);
        if (!actions.querySelector('[data-directory-retry]')) {
          const retry = button('Retry directory lookup', () => { retry.remove(); void poll(); });
          retry.dataset.directoryRetry = 'true';
        }
        return;
      }
      if (progressNodes) {
        progressNodes.notice.classList.remove('ui-feedback-loading');
        progressNodes.heading.textContent = 'Last reported progress';
        progressNodes.copy.hidden = true;
      }
      error('We lost the connection during the search. Retry to check its progress, or stop.', cause);
      actions.replaceChildren();
      button('Stop search', () => modal.close());
      button('Retry connection', () => { progressNodes = null; void poll(); }, true);
    }
  };
  const drawJob = () => {
    window.clearTimeout(pollTimer);
    if (job.restart_required) showSaved();
    else if (['scanning', 'checking'].includes(job.phase)) drawProgress();
    else if (job.phase === 'complete') showResults();
    else if (candidates().some((candidate) => candidate.saved)) showSaved();
    else {
      show('failed', 1, 'Search stopped');
      stage.append(node('div', 'ui-notice ui-notice-warning', job.reason || 'The search could not finish. Try again with an idle receiver.'),
        node('p', 'muted', 'Nothing has been added.'));
      button('Close', () => modal.close());
      button('Try another search', () => void restart(), true);
    }
  };
  const groupFor = (candidate) => (job.alias_groups || []).find((group) =>
    group.group_id === candidate.alias_group_id);
  const groupedCandidates = (values) => {
    const ordered = [];
    const byKey = new Map();
    values.forEach((candidate) => {
      const key = spectrumSearchGroupKey(candidate);
      if (!byKey.has(key)) {
        const entry = { key, aliasGroup: groupFor(candidate), candidates: [] };
        byKey.set(key, entry);
        ordered.push(entry);
      }
      byKey.get(key).candidates.push(candidate);
    });
    return ordered;
  };
  const siteLabel = (candidate) => candidate.site_name || (candidate.identity ?
    `RFSS ${hex(candidate.identity.rfss, 2)} · Site ${hex(candidate.identity.site, 2)}` :
      candidate.trunked_evidence?.identity?.site != null ? `Site ${candidate.trunked_evidence.identity.site}` : 'Site unavailable');
  const systemIdentityLabel = (entry) => {
    const candidate = entry.candidates[0];
    const identity = candidate?.identity || candidate?.trunked_evidence?.identity || entry.aliasGroup;
    const count = entry.candidates.length;
    const label = identity?.wacn != null && identity?.system != null ?
      `WACN ${hex(identity.wacn, 5)} · SysID ${hex(identity.system, 3)}` :
      systemIdentity({ ...candidate, radio_system_key: candidate?.radio_system_key || identity?.radio_system_key }) ||
      identityText(candidate);
    return `${label} · ${count} ${count === 1 ? 'site' : 'sites'}`;
  };
  const candidateDescription = (candidate) => [spectrumSearchProtocolLabel(candidate, protocols?.profiles),
    candidate.modulation].filter(Boolean).join(' · ');
  const groupRepresentative = (members) => members.find((candidate) =>
    candidate.radio_reference?.state === 'matched' && candidate.radio_reference.match?.system_name) ||
    members.find((candidate) => candidate.radio_reference?.state === 'pending') || members[0];
  const refreshGroupDirectory = (binding) => {
    const members = binding.candidateIds.map((id) => candidates().find((candidate) => candidate.candidate_id === id))
      .filter(Boolean);
    if (!members.length) return;
    const observed = candidates().filter((candidate) =>
      spectrumSearchGroupKey(candidate) === spectrumSearchGroupKey(members[0]));
    const representative = groupRepresentative(observed);
    const title = systemLabel(representative);
    const url = discoveryRadioReferenceSystemUrl(representative);
    const signature = JSON.stringify([title, url]);
    if (binding.titleSignature !== signature) {
      binding.title.replaceChildren(url ? anchor(title, url) : document.createTextNode(title));
      binding.titleSignature = signature;
    }
    const lookup = representative.radio_reference?.state === 'matched' ? null :
      members.find((candidate) => candidate.radio_reference?.state === 'pending') ||
      members.find((candidate) => candidate.radio_reference);
    const result = lookup ? discoveryRadioReferenceResult(ui, lookup, binding.directoryResult) : null;
    const returnDirectoryFocus = !result && binding.directoryHost.contains(document.activeElement);
    if (result !== binding.directoryResult) binding.directoryHost.replaceChildren(...(result ? [result] : []));
    binding.directoryResult = result;
    binding.directoryHost.hidden = !result;
    if (returnDirectoryFocus) binding.title.querySelector('a')?.focus({ preventScroll: true });
    if (binding.identity) binding.identity.textContent = systemIdentityLabel({ candidates: members });
    if (binding.groupCheck) binding.groupCheck.setAttribute('aria-label', `Select all channels in ${title}`);
    if (binding.draft && !binding.draft.alias_name_edited) {
      binding.draft.new_alias_list_name = discoveryAliasListName(title, binding.draft.new_alias_list_name);
      if (binding.aliasInput) binding.aliasInput.value = binding.draft.new_alias_list_name;
      binding.syncAliasSummary?.();
    }
  };
  const refreshOpenDetails = () => {
    if (!detailBinding) return;
    const candidate = candidates().find((candidate) => candidate.candidate_id === detailBinding.id);
    if (!candidate) return;
    const health = candidate.health || {};
    const identity = identityFacts(candidate).filter(([label]) => label !== 'Protocol');
    const entries = [
      ['System', systemLabel(candidate)], ['Site', siteLabel(candidate)],
      ['Protocol', spectrumSearchProtocolLabel(candidate, protocols?.profiles)]
    ];
    const signal = [
      ['Signal', candidate.strength_dbfs == null ? null : `${Number(candidate.strength_dbfs).toFixed(1)} dBFS`],
      ['Modulation', candidate.modulation],
      ['Decoder quality', health.quality_pct == null ? null : `${Math.round(health.quality_pct)}%`],
      ['Valid control messages', health.valid_control_messages],
      ['Invalid control messages', health.invalid_control_messages],
      ['Last checked', health.checked_at_ms ? new Date(health.checked_at_ms).toLocaleTimeString() : 'During this search']
    ];
    const signature = JSON.stringify([entries, identity, signal]);
    if (detailBinding.signature !== signature) {
      detailBinding.system.replaceChildren(factList(entries));
      detailBinding.identity.replaceChildren(factList(identity));
      detailBinding.identitySection.hidden = identity.length === 0;
      detailBinding.signal.replaceChildren(factList(signal));
      detailBinding.signature = signature;
    }
  };
  const showCandidateDetails = (id) => {
    const candidate = candidates().find((value) => value.candidate_id === id);
    if (!candidate) return;
    const content = node('div', 'ui-editor-sections');
    const section = (label) => {
      const panel = node('fieldset', 'ui-form-section');
      const body = node('div');
      panel.append(node('legend', '', label), body);
      content.append(panel);
      return { panel, body };
    };
    const system = section('Channel');
    const identity = section('On-air identity');
    const signal = section('Signal health');
    const footer = node('footer', 'ui-modal-footer ui-action-row');
    const back = uiActionButton('Back to channels', '', () => details.close(), 'ui-button ui-button-secondary');
    footer.append(back);
    content.append(footer);
    const details = openReadOnlyModal(`Signal details · ${channelMHz(candidate.frequency_hz)} MHz`, content, {
      id: 'spectrum-search-signal', className: 'spectrum-search-detail-modal', stack: 'child',
      cleanup: () => { if (detailBinding?.id === id) detailBinding = null; }
    });
    if (!details) return;
    detailBinding = { id, modal: details, system: system.body, identity: identity.body,
      identitySection: identity.panel, signal: signal.body };
    refreshOpenDetails();
  };
  const refreshResultsDirectory = () => {
    if (!resultBindings) return;
    const latest = new Map(candidates().map((candidate) => [candidate.candidate_id, candidate]));
    const changedStructure = latest.size !== resultBindings.rows.length || resultBindings.rows.some(({ candidate }) => {
      const updated = latest.get(candidate.candidate_id);
      return !updated || spectrumSearchGroupKey(updated) !== spectrumSearchGroupKey(candidate) ||
        updated.selectable !== candidate.selectable || updated.saved !== candidate.saved ||
        updated.known_channel?.configuration_id !== candidate.known_channel?.configuration_id;
    });
    if (changedStructure) {
      selected.forEach((id) => {
        const candidate = latest.get(id);
        if (!candidate || candidate.selectable === false || candidate.known_channel || candidate.saved) selected.delete(id);
      });
      showResults();
      return;
    }
    resultBindings.rows.forEach((binding) => {
      const candidate = latest.get(binding.candidate.candidate_id);
      binding.candidate = candidate;
      binding.label.textContent = candidateDescription(candidate);
      binding.site.textContent = siteLabel(candidate);
      binding.check.setAttribute('aria-label', `Select ${candidateName(candidate)}`);
    });
    resultBindings.sections.forEach((binding) => {
      refreshGroupDirectory(binding);
    });
    refreshOpenDetails();
    resultOverflows.forEach((overflow) => overflow.refresh());
    resultBindings.updateSelection();
  };
  const showResults = () => {
    const found = candidates().length;
    show('results', 1, `${found} ${found === 1 ? 'channel' : 'channels'} found`);
    if (job.truncated_reason) stage.append(node('div', 'ui-notice ui-notice-warning',
      'This search reached its signal limit. Try a smaller range to check the remaining signals.'));
    const toolbar = node('div', 'spectrum-search-results-toolbar');
    const heading = node('div', 'spectrum-search-results-heading');
    heading.append(node('strong', '', 'Grouped by trunked system'), node('span', 'muted'));
    const count = heading.lastChild;
    toolbar.append(heading);
    const rows = [];
    const sections = [];
    const review = button('Review selected', showReview, true);
    const another = button('Search another band', () => void restart());
    actions.prepend(another);
    const updateSelection = () => {
      count.textContent = `${selected.size} selected`;
      review.disabled = selected.size === 0;
      review.textContent = selected.size ? `Review ${selected.size} ${selected.size === 1 ? 'channel' : 'channels'}` :
        'Review selected';
      sections.forEach(({ groupCheck, members }) => {
        const available = members.filter(({ check }) => !check.disabled);
        const chosen = available.filter(({ candidate }) => selected.has(candidate.candidate_id));
        groupCheck.disabled = available.length === 0;
        groupCheck.checked = available.length > 0 && chosen.length === available.length;
        groupCheck.indeterminate = chosen.length > 0 && chosen.length < available.length;
      });
    };
    const selectAll = button('Select all available', () => {
      rows.forEach(({ candidate, check }) => {
        if (!check.disabled) { check.checked = true; selected.add(candidate.candidate_id); }
      });
      updateSelection();
    }, false, toolbar);
    toolbar.append(selectAll);
    const list = node('div', 'spectrum-search-system-list');
    groupedCandidates(candidates()).forEach((entry) => {
      const section = node('section', 'ui-surface spectrum-search-system-group');
      const sectionHeader = node('header', 'spectrum-search-system-header');
      const groupCheck = node('input', 'ui-selection-check');
      groupCheck.type = 'checkbox';
      groupCheck.setAttribute('aria-label', `Select all channels in ${systemLabel(entry.candidates[0])}`);
      const identity = node('div', 'spectrum-search-system-identity');
      const groupTitle = node('strong', '', systemLabel(entry.candidates[0]));
      const groupIdentity = node('span', 'muted', systemIdentityLabel(entry));
      identity.append(groupTitle, groupIdentity);
      const tableActions = node('div', 'ui-section-actions');
      if (entry.candidates.some((candidate) => candidate.identity || candidate.trunked_evidence?.identity?.radio_system_key)) tableActions.append(uiPill('Stable identity', 'success'));
      sectionHeader.append(groupCheck, identity, tableActions);
      const wrapper = node('div', 'ui-table-wrap');
      const table = node('table', 'ui-data-table ui-data-table-quiet ui-mobile-cards spectrum-search-group-table');
      const columns = node('colgroup');
      ['selection', 'channel', 'site', 'signal', 'quality', 'details'].forEach((name) =>
        columns.append(node('col', `spectrum-search-column-${name}`)));
      const head = node('tr');
      ['', 'Channel', 'Site', 'Signal', 'Quality', 'Details'].forEach((label) => {
        const cell = node('th', '', label); cell.scope = 'col';
        if (!label) cell.setAttribute('aria-label', 'Select channels');
        head.append(cell);
      });
      const thead = node('thead'); thead.append(head);
      const body = node('tbody');
      const members = [];
      entry.candidates.forEach((candidate) => {
        const row = node('tr');
        const cells = ['', 'Channel', 'Site', 'Signal', 'Quality', 'Details'].map((label) => {
          const cell = node('td'); cell.dataset.label = label; row.append(cell); return cell;
        });
        const check = node('input', 'ui-selection-check spectrum-search-channel-check');
        check.type = 'checkbox';
        check.setAttribute('aria-label', `Select ${candidateName(candidate)}`);
        check.disabled = candidate.selectable === false || Boolean(candidate.known_channel) || candidate.saved === true;
        check.checked = selected.has(candidate.candidate_id);
        check.addEventListener('change', () => {
          if (check.checked) selected.add(candidate.candidate_id); else selected.delete(candidate.candidate_id);
          updateSelection();
        });
        const channel = node('div', 'identity-summary');
        const label = node('span', 'muted', candidateDescription(candidate));
        channel.append(node('strong', 'identity-summary-primary', `${channelMHz(candidate.frequency_hz)} MHz`),
          label);
        if (candidate.known_channel) {
          const known = candidate.known_channel;
          const link = entityRefHref(known.entity_ref) || (known.configuration_id ?
            href('channel', { configuration_id: known.configuration_id }) : null);
          channel.append(uiPill('Already added'), link ? anchor(known.name || 'Open channel', link) :
            node('span', 'muted', known.name || 'Known channel'));
        } else if (check.disabled) channel.append(node('span', 'muted', candidate.reason || 'Unavailable to add'));
        cells[0].append(check);
        cells[1].append(channel);
        const site = node('span', '', siteLabel(candidate));
        cells[2].append(site);
        const strength = Number.isFinite(Number(candidate.strength_dbfs)) && candidate.strength_dbfs != null ?
          `${Number(candidate.strength_dbfs).toFixed(1)} dBFS` : '—';
        cells[3].append(node('span', '', strength));
        const health = candidate.health || {};
        cells[4].append(node('strong', '', health.quality_pct == null ? '—' : `${Math.round(health.quality_pct)}%`));
        const details = uiActionButton('Details', '', () => showCandidateDetails(candidate.candidate_id),
          'ui-button ui-button-secondary');
        details.setAttribute('aria-label', `Details for ${channelMHz(candidate.frequency_hz)} MHz`);
        cells[5].append(details);
        row.hidden = false;
        const member = { candidate, row, check, label, site };
        members.push(member);
        rows.push(member);
        body.append(row);
      });
      groupCheck.addEventListener('change', () => {
        members.forEach(({ candidate, check }) => {
          if (check.disabled) return;
          check.checked = groupCheck.checked;
          if (groupCheck.checked) selected.add(candidate.candidate_id); else selected.delete(candidate.candidate_id);
        });
        updateSelection();
      });
      table.append(columns, thead, body);
      wrapper.append(table);
      const directoryHost = node('div', 'spectrum-search-system-directory');
      section.append(sectionHeader, directoryHost, wrapper);
      const overflow = createTableOverflow({ wrapper, table, actionsHost: tableActions, iconButton,
        signal: abort.signal, label: `${systemLabel(entry.candidates[0])} search results` });
      resultOverflows.add(overflow);
      const binding = { section, groupCheck, members, title: groupTitle, identity: groupIdentity, directoryHost,
        candidateIds: entry.candidates.map((candidate) => candidate.candidate_id) };
      sections.push(binding);
      refreshGroupDirectory(binding);
      list.append(section);
    });
    selectAll.disabled = rows.every(({ check }) => check.disabled);
    stage.append(toolbar, found ? list : node('div', 'ui-empty-state',
      'No trunked control channels were identified. Try another band or a different receiver.'));
    resultOverflows.forEach((overflow) => overflow.refresh());
    updateSelection();
    resultBindings = { rows, sections, updateSelection };
    scheduleDirectoryPoll();
  };
  const showReview = () => {
    if (job.restart_required) { showSaved(); return; }
    const chosen = selectedCandidates();
    show('review', 2, `Review ${chosen.length} ${chosen.length === 1 ? 'channel' : 'channels'}`);
    const form = node('form', 'spectrum-discovery-review spectrum-search-review-form');
    form.addEventListener('submit', (event) => event.preventDefault());
    const systemList = node('div', 'spectrum-search-review-systems');
    const mapEditors = [];

    groupedCandidates(chosen).forEach((entry) => {
      const representative = groupRepresentative(candidates().filter((candidate) =>
        spectrumSearchGroupKey(candidate) === entry.key));
      const group = entry.aliasGroup;
      const lists = group?.alias_lists || [];
      const draft = group ? groups.get(group.group_id) || {
        alias_list_id: group.suggested_alias_list_id ||
          (lists.length === 1 ? lists[0].id : lists.length ? '' : 0),
        new_alias_list_name: discoveryAliasListName(systemLabel(representative), group.default_new_alias_list_name)
      } : null;
      if (draft) {
        if (lists.length === 1) draft.alias_list_id = lists[0].id;
        else if (lists.length > 1 && !lists.some((aliasList) => aliasList.id === Number(draft.alias_list_id)))
          draft.alias_list_id = '';
        groups.set(group.group_id, draft);
      }
      const requiresAliasChoice = Boolean(draft && lists.length > 1 && !draft.alias_list_id);
      const section = node('section', 'ui-surface spectrum-search-review-system');
      const sectionHeader = node('header', 'spectrum-search-review-system-header');
      const title = node('div', 'spectrum-search-review-system-title');
      const aliasSummary = node('span', 'muted');
      const syncAliasSummary = () => {
        const existing = lists.find((aliasList) => aliasList.id === Number(draft?.alias_list_id));
        aliasSummary.textContent = existing ? `Alias List: ${existing.name} · Existing` : draft ?
          `Alias List: ${draft.new_alias_list_name || 'Name required'}` : 'Alias List unavailable';
      };
      const groupTitle = node('h4', 'ui-record-title', systemLabel(representative));
      title.append(groupTitle, aliasSummary);
      const customizePanel = node('div', 'spectrum-search-review-customize');
      customizePanel.hidden = !(requiresAliasChoice || draft && lists.length === 0);
      const customize = uiActionButton(customizePanel.hidden ? 'Customize' : 'Done', '', () => {
        customizePanel.hidden = !customizePanel.hidden;
        customize.setAttribute('aria-expanded', String(!customizePanel.hidden));
        customize.textContent = customizePanel.hidden ? 'Customize' : 'Done';
      }, 'ui-button ui-button-secondary spectrum-search-customize-action');
      customize.setAttribute('aria-expanded', String(!customizePanel.hidden));
      sectionHeader.append(title, customize);
      const rows = node('div', 'spectrum-search-review-channel-list');
      const channelFields = node('div', 'channel-editor-grid spectrum-search-channel-fields');
      const directoryHost = node('div');
      customizePanel.append(directoryHost);
      const groupBinding = { title: groupTitle, directoryHost, draft, syncAliasSummary,
        candidateIds: entry.candidates.map((candidate) => candidate.candidate_id) };
      reviewGroupBindings.push(groupBinding);
      refreshGroupDirectory(groupBinding);
      entry.candidates.forEach((candidate) => {
        const channelDraft = drafts.get(candidate.candidate_id) || { name: candidateName(candidate) };
        const binding = { id: candidate.candidate_id, draft: channelDraft };
        drafts.set(candidate.candidate_id, channelDraft);
        const row = node('div', 'spectrum-search-review-channel');
        const frequency = node('span'); frequency.dataset.label = 'Frequency';
        frequency.append(node('strong', '', `${channelMHz(candidate.frequency_hz)} MHz`));
        const site = node('span', '', siteLabel(candidate)); site.dataset.label = 'Site';
        const name = node('span', '', channelDraft.name); name.dataset.label = 'Channel name';
        row.append(frequency, site, name);
        Object.assign(binding, { site, name });
        reviewBindings.push(binding);
        if (candidate.saved || candidate.known_channel) row.append(uiPill(candidate.saved ? 'Added' : 'Already added',
          candidate.saved ? 'success' : 'neutral'));
        else {
          const input = node('input', 'ui-input'); input.type = 'text'; input.required = true; input.maxLength = 255;
          input.value = channelDraft.name;
          input.setAttribute('aria-label', `Channel name for ${channelMHz(candidate.frequency_hz)} MHz`);
          input.addEventListener('input', () => {
            channelDraft.name = input.value;
            channelDraft.name_edited = true;
            name.textContent = input.value;
            modal.setDirty(true);
          });
          binding.input = input;
          channelFields.append(formField(`Channel name · ${channelMHz(candidate.frequency_hz)} MHz`, input));
          const profile = protocols?.profiles?.find((profile) => profile.id === candidate.protocol_id);
          const mapField = profile?.sections?.flatMap((section) => section.fields || [])
            .find((field) => field.type === 'frequency_map');
          if (mapField && channelEditorControl && channelEditorFieldValue) {
            const createMap = (values) => {
              const control = channelEditorControl(mapField, profile, {}, { frequency_map: values }, protocols);
              const preserveMap = () => { modal.setDirty(true); channelDraft.map_draft = spectrumSearchMapDraft(control); };
              control.addEventListener('input', preserveMap);
              control.addEventListener('change', preserveMap);
              return control;
            };
            const map = createMap(channelDraft.map_draft || channelDraft.frequency_map || candidate.trunked_evidence?.frequency_map || []);
            const editor = { map, field: mapField, draft: channelDraft };
            mapEditors.push(editor);
            Object.assign(binding, { map, createMap, editor, mapValues: JSON.stringify(candidate.trunked_evidence?.frequency_map || []) });
            const mapWrapper = formField(`${mapField.label} · ${channelMHz(candidate.frequency_hz)} MHz`, map,
              'Enter the verified channel numbers and frequencies. ' +
              'Unmapped channels can be followed only when the system broadcasts their frequencies.');
            mapWrapper.classList.add('channel-wide-field');
            channelFields.append(mapWrapper);
          }
        }
        rows.append(row);
      });
      if (group) customizePanel.append(disclosure('System identity', factList(identityFacts(entry.candidates[0]))));
      if (draft && lists.length > 1) {
        const select = uiSelect(lists.map((aliasList) => ({ value: aliasList.id, label: aliasList.name })),
          draft.alias_list_id, !draft.alias_list_id, 'Choose an Alias List');
        select.required = true;
        select.setAttribute('aria-label', `Alias List for ${aliasIdentityLabel(entry.candidates[0])}`);
        select.addEventListener('change', () => {
          draft.alias_list_id = Number(select.value);
          modal.setDirty(true);
          syncAliasSummary();
        });
        customizePanel.prepend(formField('Alias List', uiSelectFrame(select)));
      } else if (draft && lists.length === 0) {
        const name = node('input', 'ui-input'); name.type = 'text'; name.required = true; name.maxLength = ALIAS_LIST_NAME_MAX_LENGTH;
        name.value = draft.new_alias_list_name;
        groupBinding.aliasInput = name;
        name.setAttribute('aria-label', `New Alias List name for ${aliasIdentityLabel(entry.candidates[0])}`);
        name.addEventListener('input', () => {
          draft.new_alias_list_name = name.value;
          draft.alias_name_edited = true;
          modal.setDirty(true);
          syncAliasSummary();
        });
        customizePanel.prepend(formField('New Alias List name', name,
          'An existing compatible Alias List with this name will be used.'),
        node('p', 'ui-field-hint', 'New Alias Lists use your Default listening list.'));
      }
      if (channelFields.childElementCount) customizePanel.append(channelFields);
      syncAliasSummary();
      section.append(sectionHeader, rows, customizePanel);
      systemList.append(section);
    });

    form.append(systemList);
    stage.append(form);
    button('Back', showResults);
    const newAliasGroups = groupedCandidates(chosen).filter((entry) => entry.aliasGroup &&
      Number(groups.get(entry.aliasGroup.group_id)?.alias_list_id) === 0);
    const addLabel = `Add ${chosen.length} ${chosen.length === 1 ? 'channel' : 'channels'}`;
    button(newAliasGroups.length ? 'Continue' : addLabel, () => {
      if (!form.checkValidity()) {
        form.querySelectorAll('input:invalid, select:invalid, textarea:invalid').forEach((control) => {
          const panel = control.closest('.spectrum-search-review-customize');
          if (!panel) return;
          panel.hidden = false;
          const customize = panel.parentElement.querySelector('.spectrum-search-customize-action');
          customize?.setAttribute('aria-expanded', 'true');
          if (customize) customize.textContent = 'Done';
        });
        form.reportValidity();
        return;
      }
      try {
        mapEditors.forEach(({ map, field, draft }) => { draft.frequency_map = channelEditorFieldValue(map, field); });
      } catch (cause) { error(cause.message || 'Check the channel maps and try again.'); return; }
      const ids = chosen.map((candidate) => candidate.candidate_id);
      if (!newAliasGroups.length) { void save(ids); return; }
      show('aliases', 3, `Do you want to import talkgroups into your Alias ${newAliasGroups.length === 1 ? 'List' : 'Lists'}?`);
      const targets = newAliasGroups.map((entry) => {
        const members = candidates().filter((candidate) => spectrumSearchGroupKey(candidate) === entry.key);
        const representative = groupRepresentative(members);
        const directoryIds = new Set(members.filter((candidate) => candidate.radio_reference?.state === 'matched')
          .map((candidate) => Number(candidate.radio_reference.match?.rr_system_id)).filter((id) => id > 0));
        return { name: groups.get(entry.aliasGroup.group_id).new_alias_list_name, systemName: systemLabel(representative),
          matched: representative.radio_reference?.state === 'matched' && directoryIds.size <= 1 };
      });
      const choice = discoveryAliasImportChoice(ui, targets, wantsAliasImport);
      stage.append(choice.element);
      button('Back', () => { wantsAliasImport = choice.wantsImport(); showReview(); });
      button(addLabel, () => { wantsAliasImport = choice.wantsImport(); void save(ids); }, true);
    }, true);
    scheduleDirectoryPoll();
  };
  const payload = (ids) => ({ revision: job.revision,
    candidates: candidates().filter((candidate) => ids.includes(candidate.candidate_id) && !candidate.saved && !candidate.known_channel).map((candidate) => ({
      candidate_id: candidate.candidate_id, name: drafts.get(candidate.candidate_id)?.name.trim() || candidateName(candidate),
      auto_start: false,
      ...(drafts.get(candidate.candidate_id)?.frequency_map ?
        { frequency_map: drafts.get(candidate.candidate_id).frequency_map } : {})
    })),
    alias_groups: (job.alias_groups || []).filter((group) => candidates().some((candidate) =>
      ids.includes(candidate.candidate_id) && candidate.alias_group_id === group.group_id)).map((group) => ({
      group_id: group.group_id, alias_list_id: Number(groups.get(group.group_id)?.alias_list_id || 0),
      new_alias_list_name: groups.get(group.group_id)?.new_alias_list_name.trim() || null
    }))
  });
  const isExpired = (cause) => [404, 410].includes(cause?.status) ||
    ['search_expired', 'spectrum_search_expired'].includes(cause?.code);
  const surfaceReceiverReleaseFailure = (cause) => {
    const takeover = lease?.takeover === true;
    error(takeover ?
      'This receiver could not resume its channels yet. Try again before closing the search.' :
      'This receiver could not be released yet. Try again before closing the search.', cause);
    if (actions.querySelector('[data-retry-receiver-release]')) return;
    const retry = button(takeover ? 'Try resuming channels' : 'Try releasing receiver',
      () => void retryReceiverRelease(), true);
    retry.dataset.retryReceiverRelease = 'true';
    if (busy) setBusy(true);
  };
  const finishReceiverUse = () => {
    if (receiverFinishPromise) return receiverFinishPromise;
    const pending = trackReceiverUse(async () => {
      try {
        await releaseJob({ preserveResults: true });
        await releaseLease();
        if (current()) {
          actions.querySelector('[data-retry-receiver-release]')?.remove();
          clearFeedback();
        }
        return true;
      } catch (cause) {
        if (current()) surfaceReceiverReleaseFailure(cause);
        return false;
      }
    });
    receiverFinishPromise = pending;
    void pending.finally(() => { if (receiverFinishPromise === pending) receiverFinishPromise = null; });
    return pending;
  };
  const retryReceiverRelease = async () => {
    if (busy) return;
    setBusy(true);
    try { await finishReceiverUse(); }
    finally { if (current()) setBusy(false); }
  };
  const showExpired = () => {
    if (candidates().some((candidate) => candidate.saved)) {
      showSaved();
      error('This search expired. Your added channels are saved; open them to continue.');
      stage.querySelectorAll('button').forEach((control) => { control.disabled = true; });
    } else {
      show('expired', 2, 'This search expired');
      stage.append(node('div', 'ui-notice ui-notice-warning', 'Nothing was added. Start a new search to check the signals again.'));
      button('Close', () => modal.close());
      button('Start a new search', () => void restart(), true);
    }
    void releaseJob().then(() => finishReceiverUse()).catch((cause) => {
      if (current()) error('This search could not be closed yet. Try starting a new search again.', cause);
    });
  };
  const finishReceiverIfComplete = async () => {
    const finished = job?.restart_required || selectedCandidates().every((candidate) =>
      candidate.saved || candidate.known_channel);
    if (!finished) return false;
    return finishReceiverUse();
  };
  const save = (ids) => trackReceiverUse(() => saveChannels(ids));
  const saveChannels = async (ids) => {
    if (job.restart_required) { showSaved(); return; }
    setBusy(true);
    let added = false;
    try {
      const plan = payload(ids);
      if (plan.candidates.length) job = await request(`${jobPath()}/save`, { method: 'POST', body: plan, timeoutMs: 30000 });
      if (!current()) return;
      added = true;
      modal.setDirty(false);
      aliasImportResult = job.alias_import || aliasImportResult;
      showSaved();
      await finishReceiverIfComplete();
      if (!job.restart_required && wantsAliasImport && aliasImportResult?.targets?.length) await runAliasImports();
    } catch (cause) {
      if (!current()) return;
      if (isExpired(cause)) { showExpired(); return; }
      let ledgerConfirmed = false;
      try { job = await request(jobPath(), { csrf: false }); ledgerConfirmed = true; } catch (_) { /* Preserve the last confirmed ledger. */ }
      if (!current()) return;
      if (cause.code === 'channel_saved_restart_required' && !ledgerConfirmed)
        job = { ...job, restart_required: true, ledger_unavailable: true };
      if (job.restart_required || added || candidates().some((candidate) => candidate.saved)) {
        modal.setDirty(false);
        aliasImportResult = job.alias_import || aliasImportResult;
        showSaved();
        await finishReceiverIfComplete();
        if (!job.restart_required && wantsAliasImport && aliasImportResult?.targets?.length) await runAliasImports();
        if (!job.restart_required) error('Some channels were added. Check each result before retrying.', cause);
      } else if (cause.code === 'stale_revision' && job?.phase === 'complete') {
        showReview();
        error('Saved choices changed. Check the Alias Lists and try adding again.', cause);
      } else error('We couldn’t add your channels. Your choices are still here. Try again.', cause);
    } finally { if (current()) setBusy(false); }
  };
  const runAliasImports = async () => {
    if (job.restart_required) { showSaved(); return; }
    setBusy(true);
    try {
      aliasImportResult = await importDiscoveryAliases(request, `${jobPath()}/aliases/import`, aliasImportResult, (result) => {
        aliasImportResult = result;
        if (result.targets.some((target) => target.state === 'restart_required')) job.restart_required = true;
        show('importing', 3, 'Importing talkgroups');
        stage.append(discoveryAliasImportResults(ui, result));
      }, current);
      if (current()) showSaved();
    } finally { if (current()) setBusy(false); }
  };
  const showSaved = () => {
    show('saved', 2, 'Your channel results');
    const results = selectedCandidates();
    if (job.restart_required) stage.append(node('div', 'ui-notice ui-notice-warning',
      `${candidates().some((candidate) => candidate.saved) ? 'Some channels were saved, but VCE could not load the changes.' :
        'VCE needs to restart before changes can be loaded.'} Restart VCE before opening these channels, adding more, or listening. Saved channels do not need to be added again.`));
    const complete = !job.restart_required && results.every((candidate) => candidate.saved || candidate.known_channel);
    if (complete) {
      const added = results.filter((candidate) => candidate.saved).length;
      const existing = results.length - added;
      const messages = [];
      if (added) messages.push(`${added} ${added === 1 ? 'channel' : 'channels'} added.`);
      if (existing) messages.push(`${existing} ${existing === 1 ? 'channel was' : 'channels were'} already in Channels.`);
      stage.append(node('div', 'ui-notice ui-notice-success', messages.join(' ')));
    }
    else results.forEach((candidate) => {
      const row = node('section', 'ui-form-section spectrum-discovery-review');
      const known = !candidate.saved && candidate.known_channel;
      const status = candidate.saved ? 'Added' : known ? 'Already added' :
        job.ledger_unavailable ? 'Status unavailable' : job.restart_required ? 'Not added' : 'Could not add';
      row.append(node('strong', '', drafts.get(candidate.candidate_id)?.name || candidateName(candidate)),
        node('p', 'muted', `${systemLabel(candidate)} · ${channelMHz(candidate.frequency_hz)} MHz`),
        uiPill(status, candidate.saved ? 'success' : known ? 'neutral' : 'warning'));
      row.append(disclosure('System and site identity', factList(identityFacts(candidate))));
      if (!candidate.saved && !known && !job.restart_required) {
        if (candidate.save_error) row.append(node('p', 'muted', candidate.save_error));
        button('Retry', () => void save([candidate.candidate_id]), true, row).setAttribute('aria-label', `Retry adding ${candidateName(candidate)}`);
        button('Review choices', showReview, false, row);
      }
      stage.append(row);
    });
    if (wantsAliasImport && aliasImportResult?.targets?.length) {
      stage.append(discoveryAliasImportResults(ui, aliasImportResult));
      if (!job.restart_required && aliasImportResult.targets.some((target) => target.state === 'failed' || target.state === 'pending'))
        button('Retry talkgroup imports', () => void runAliasImports());
    }
    button('Done', () => modal.close());
    actions.append(anchor('Open Channels', href('channel-setup'), 'ui-button ui-button-primary'));
    if (busy) setBusy(true);
  };
  const load = async () => {
    if (modal.ready && !await modal.ready || !current()) return;
    show('loading', 0, 'Preparing the search…');
    setBusy(true);
    try {
      catalog = await request(`${path}/catalog`, { csrf: false });
      preparedCatalogTuners.clear();
      if (current()) showBand();
    } catch (cause) {
      if (current()) {
        error('We couldn’t load the search options. Check your connection and try again.', cause);
        button('Close', () => modal.close());
        button('Try again', () => void load(), true);
      }
    } finally { if (current()) setBusy(false); }
  };
  void load();
  return modal;
}
