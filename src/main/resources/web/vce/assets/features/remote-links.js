const ROOT = '/api/v1/admin/remote-links';
const POLL_MS = 3000;

// Reuse map: data-workspace, ui-surface-heading, ui-heading-group, ui-surface, ui-facts, ui-metric, ui-status,
// ui-toggle-field, ui-field, ui-select-frame, ui-notice and the shared modal lifecycle
// provide the visual language. This feature owns only Remote Links layout geometry.
export function createRemoteLinksWorkspace(deps) {
  const { node, formField, uiSelectFrame, uiToggleField, uiStatus, iconGlyph,
    openReadOnlyModal, confirmAction, createFormWorkflow, requestJson, modalFooter, metrics, signal } = deps;
  const host = node('div', 'remote-links-page data-workspace');
  const summary = node('div', 'remote-links-summary');
  const message = node('div');
  message.setAttribute('role', 'status');
  const overview = node('div', 'remote-links-overview');
  const senderHeader = node('div', 'remote-links-section-header ui-surface ui-surface-heading');
  const senderHeading = node('div', 'ui-heading-group');
  senderHeading.append(node('h2', '', 'Trusted senders'),
    node('p', 'muted', 'Create credentials for installations allowed to send P25 feeds to this receiver.'));
  const addSender = button('Add trusted sender', () => openCreateSender(), true, 'add-sender');
  senderHeader.append(senderHeading, addSender);
  const senderHost = node('div', 'remote-links-sender-grid');
  const pageMeta = node('footer', 'remote-links-page-meta');
  const freshness = node('span', 'muted');
  pageMeta.append(freshness,
    node('span', 'muted', 'Remote channels use this receiver’s Alias Lists and depend on their sender for live traffic.'));
  host.append(summary, message, overview, senderHeader, senderHost, pageMeta);

  let snapshot = null;
  let loading = false;
  let disposed = false;
  let modalActive = false;
  let timer = null;
  let activationReleaseTimer = null;
  let pointerActivation = false;
  let keyboardActivation = '';
  let pendingSnapshot = null;

  function button(label, action, primary = false, actionKey = '') {
    const control = node('button', `ui-button ui-button-${primary ? 'primary' : 'secondary'}`, label);
    control.type = 'button';
    if (actionKey) control.dataset.remoteAction = actionKey;
    if (action) control.addEventListener('click', action);
    return control;
  }

  const read = () => requestJson(ROOT, { csrf: false, signal });
  const write = (path, method, body, requestSignal = signal) => requestJson(ROOT + path,
    { method, body, signal: requestSignal, timeoutMs: 30000, page: false });
  const statusLabel = (value) => stateKey(value) === 'PENDING' ? 'Setting up' :
    String(value || 'UNKNOWN').replaceAll('_', ' ').toLowerCase()
      .replace(/^./, (letter) => letter.toUpperCase());
  const stateKey = (value) => String(value || 'UNKNOWN').toUpperCase();
  const statusTone = (value) => ['CONNECTED', 'LISTENING', 'READY'].includes(stateKey(value)) ? 'success' :
    ['ERROR', 'MISSING', 'AUTHENTICATION_FAILED', 'UNSUPPORTED'].includes(stateKey(value)) ? 'danger' :
    ['DEGRADED', 'DISCONNECTED'].includes(stateKey(value)) ? 'warning' : 'neutral';
  const status = (value) => uiStatus(statusLabel(value), statusTone(value));
  const age = (milliseconds) => {
    const value = Number(milliseconds);
    if (!Number.isFinite(value) || value <= 0) return 'Never';
    const seconds = Math.max(0, Math.round((Date.now() - value) / 1000));
    if (seconds < 60) return `${seconds}s ago`;
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
    return `${Math.floor(seconds / 86400)}d ago`;
  };
  const frequency = (hz) => Number(hz) > 0 ? `${(Number(hz) / 1_000_000).toFixed(5)} MHz` : 'Unavailable';
  const protocolLabel = (value) => ({ P25_PHASE1: 'P25 Phase 1', P25_PHASE2: 'P25 Phase 2' })[value] ||
    String(value || 'P25').replaceAll('_', ' ');
  const notice = (text, tone = '') => node('div', tone ? `ui-notice ui-notice-${tone}` : 'ui-feedback', text);
  const showError = (target, error) => target.replaceChildren(notice(error?.message || 'The request failed', 'danger'));
  const pathPart = (value) => encodeURIComponent(String(value));
  const aliasLists = () => Array.isArray(snapshot?.alias_lists) ? snapshot.alias_lists : [];

  function heading(title, detail, stateValue = null) {
    const header = node('header', 'remote-links-card-header');
    const identity = node('div', 'remote-links-card-identity');
    identity.append(node('h2', '', title));
    if (detail) identity.append(node('p', 'muted', detail));
    header.append(identity);
    if (stateValue) header.append(status(stateValue));
    return header;
  }

  function facts(items) {
    const list = node('dl', 'ui-facts remote-links-facts');
    items.forEach(([label, value]) => {
      const item = node('div', 'ui-fact');
      item.append(node('dt', '', label), node('dd', '', value == null || value === '' ? '—' : String(value)));
      list.append(item);
    });
    return list;
  }

  function drawListener(listener) {
    const card = node('article', 'remote-links-card ui-surface');
    card.append(heading('Receive remote feeds', listener.enabled ?
      `${listener.bind_address || 'All interfaces'}:${listener.port}` : 'Listener disabled', listener.state));
    const rows = Array.isArray(listener.dependencies) ? listener.dependencies : [];
    if (rows.length) {
      const dependencies = node('div', 'remote-links-dependencies');
      rows.forEach((dependency) => {
        const item = node('div', 'remote-links-dependency');
        item.append(node('span', '', dependency.label || dependency.id || 'Dependency'), status(dependency.state));
        if (dependency.status_message) item.append(node('small', 'muted', dependency.status_message));
        dependencies.append(item);
      });
      card.append(dependencies);
    }
    const actions = node('footer', 'remote-links-card-actions ui-action-row');
    actions.append(button('Configure listener', openListener, false, 'listener'));
    if (listener.bind_address === '127.0.0.1' || listener.bind_address === '::1') {
      card.append(notice('This address accepts connections only from this receiver. Choose its VPN address to receive remote feeds.',
        'warning'));
    } else if (listener.enabled && ['0.0.0.0', '::', '0:0:0:0:0:0:0:0'].includes(listener.bind_address)) {
      card.append(notice('Listening on every interface. Restrict access with a VPN or firewall; authentication does not encrypt traffic.',
        'warning'));
    }
    if (listener.status_message) card.append(notice(listener.status_message,
      stateKey(listener.state) === 'ERROR' ? 'danger' : 'warning'));
    card.append(actions);
    return card;
  }

  function drawSenderConnection(connection) {
    const selected = Array.isArray(connection.exported_channel_configuration_ids) ?
      connection.exported_channel_configuration_ids.length : 0;
    const card = node('article', 'remote-links-card ui-surface');
    const destination = connection.destination_host && connection.destination_port ?
      `${connection.destination_host}:${connection.destination_port}` : 'Not configured';
    const counts = metrics([
      ['Exported systems', selected, undefined, { icon: 'icon-trunked' }]
    ]);
    counts.classList.add('remote-links-connection-metrics');
    const details = facts([
      ['Authentication', connection.credential_configured ? 'Credential saved' : 'Not configured'],
      ['Last connected', age(connection.last_connected_at_ms)]
    ]);
    details.classList.add('remote-links-connection-facts');
    card.append(heading('Send local P25 feeds', destination, connection.state), counts, details);
    if (connection.status_message) card.append(notice(connection.status_message,
      ['ERROR', 'AUTHENTICATION_FAILED'].includes(stateKey(connection.state)) ? 'danger' : 'warning'));
    const actions = node('footer', 'remote-links-card-actions ui-action-row');
    actions.append(button('Configure sender', openSenderConnection, false, 'sender-connection'));
    card.append(actions);
    return card;
  }

  function drawFeed(sender, feed) {
    const card = node('article', 'remote-links-feed-card ui-surface-inset');
    const header = node('header', 'remote-links-feed-header');
    const title = node('div', 'remote-links-feed-title');
    title.append(iconGlyph('icon-cloud'), node('strong', '', feed.display_name || feed.advertised_name || feed.feed_id));
    header.append(title, status(feed.state));
    const managed = Boolean(feed.channel_configuration_id);
    const identity = [protocolLabel(feed.protocol), frequency(feed.frequency_hz)].filter(Boolean).join(' · ');
    const catalog = [feed.system_name, feed.site_name].filter(Boolean).join(' · ');
    const site = [feed.wacn != null ? `WACN ${feed.wacn}` : '', feed.system != null ? `System ID ${feed.system}` : '',
      feed.rfss != null ? `RFSS ${feed.rfss}` : '', feed.site != null ? `Site ${feed.site}` : '']
      .filter(Boolean).join(' · ');
    const detail = node('div', 'remote-links-feed-detail');
    detail.append(node('span', '', identity));
    if (catalog) detail.append(node('span', '', catalog));
    if (site) detail.append(node('span', 'muted', site));
    detail.append(node('span', 'muted', managed ?
      `Managed locally · ${feed.enabled ? 'enabled' : 'disabled'}` :
      stateKey(feed.state) === 'UNSUPPORTED' ? 'This feed is not supported by this version.' :
        stateKey(feed.state) === 'REMOVED' ? 'No longer advertised by the sender.' :
          'Setting up a local channel.'));
    if (feed.lag_milliseconds != null) detail.append(node('span', 'muted',
      `${Math.max(0, Number(feed.lag_milliseconds))}ms link delay · ${feed.sequence_gap_count || 0} sequence gaps`));
    if (feed.status_message) detail.append(node('span', statusTone(feed.state) === 'danger' ?
      'ui-status-danger' : 'muted', feed.status_message));
    card.append(header, detail);
    if (managed) card.append(button('Manage feed', () => openFeed(sender, feed), false,
      `feed:${sender.sender_id}:${feed.feed_id}`));
    return card;
  }

  function drawSender(sender) {
    const card = node('article', 'remote-links-sender-card ui-surface');
    const feeds = Array.isArray(sender.feeds) ? sender.feeds : [];
    card.append(heading(sender.display_name || sender.sender_id, `${feeds.length} advertised feed${feeds.length === 1 ? '' : 's'}`,
      sender.state), facts([
      ['Sender ID', sender.sender_id],
      ['Last seen', age(sender.last_seen_at_ms)],
      ['Preferred P25 Alias List', aliasLists().find((item) => item.alias_list_id === sender.default_alias_list_id)?.name ||
        'Host default']
    ]));
    if (sender.status_message) card.append(notice(sender.status_message,
      stateKey(sender.state) === 'ERROR' ? 'danger' : 'warning'));
    const feedHost = node('div', 'remote-links-feed-grid');
    if (feeds.length) feedHost.append(...feeds.map((feed) => drawFeed(sender, feed)));
    else feedHost.append(node('div', 'ui-feedback', 'No feeds have been advertised by this sender.'));
    const actions = node('footer', 'remote-links-card-actions ui-action-row');
    actions.append(button('Manage sender', () => openSender(sender), false, `sender:${sender.sender_id}`));
    card.append(feedHost, actions);
    return card;
  }

  function draw(value) {
    snapshot = value;
    const senders = Array.isArray(snapshot.senders) ? snapshot.senders : [];
    const connectedCount = senders.filter((sender) => stateKey(sender.state) === 'CONNECTED').length;
    summary.replaceChildren(status(snapshot.listener?.state),
      uiStatus(`${connectedCount} connected sender${connectedCount === 1 ? '' : 's'}`, 'neutral'));
    overview.replaceChildren(drawListener(snapshot.listener || {}),
      drawSenderConnection(snapshot.sender_connection || {}));
    if (senders.length) senderHost.replaceChildren(...senders.map(drawSender));
    else {
      const empty = node('div', 'ui-empty-state remote-links-empty');
      empty.append(iconGlyph('icon-cloud'), node('h2', '', 'No trusted senders'),
        node('p', '', 'Add a sender to create the ID and shared secret used by another VCE installation.'));
      senderHost.replaceChildren(empty);
    }
    freshness.textContent = 'Status updated just now';
  }

  function present(value, force = false) {
    snapshot = value;
    // A press in progress must finish on the same control; ordinary focus does not pause status updates.
    const focused = host.contains(document.activeElement) ? document.activeElement.dataset.remoteAction : '';
    if ((pointerActivation || keyboardActivation) && !force) pendingSnapshot = value;
    else {
      pendingSnapshot = null;
      draw(value);
      if (focused) [...host.querySelectorAll('[data-remote-action]')]
        .find((control) => control.dataset.remoteAction === focused)?.focus();
    }
  }

  function flushPending() {
    if (disposed || modalActive || pointerActivation || keyboardActivation || !pendingSnapshot) return;
    const value = pendingSnapshot;
    pendingSnapshot = null;
    present(value, true);
  }

  host.addEventListener('pointerdown', (event) => {
    if (event.isPrimary && event.button === 0 && event.target.closest('button[data-remote-action]')) {
      window.clearTimeout(activationReleaseTimer);
      pointerActivation = true;
    }
  });
  const pointerUp = () => {
    if (!pointerActivation) return;
    // Click follows pointerup. The timer is a fallback for a release that produces no click.
    activationReleaseTimer = window.setTimeout(() => { pointerActivation = false; flushPending(); }, 0);
  };
  const pointerCancel = () => { pointerActivation = false; flushPending(); };
  document.addEventListener('pointerup', pointerUp);
  document.addEventListener('pointercancel', pointerCancel);
  host.addEventListener('click', () => {
    if (!pointerActivation) return;
    window.clearTimeout(activationReleaseTimer);
    pointerActivation = false;
    flushPending();
  });
  host.addEventListener('keydown', (event) => {
    if (['Enter', ' '].includes(event.key) && event.target.closest('button[data-remote-action]')) {
      window.clearTimeout(activationReleaseTimer);
      keyboardActivation = event.key;
    }
  });
  const keyUp = (event) => {
    if (event.key !== keyboardActivation) return;
    keyboardActivation = '';
    // Space activates on keyup, so let its click finish before replacing controls.
    activationReleaseTimer = window.setTimeout(flushPending, 0);
  };
  document.addEventListener('keyup', keyUp);

  async function refresh(force = false) {
    if (disposed || loading || modalActive) return;
    loading = true;
    try {
      const value = await read();
      if (!disposed) {
        // Keep the editor's opening revision stable until its mutation completes.
        if (modalActive) pendingSnapshot = value;
        else present(value, force);
        message.replaceChildren();
      }
    } catch (error) {
      if (!disposed && error?.name !== 'AbortError') {
        showError(message, error);
        freshness.textContent = 'Status unavailable · retrying';
      }
    } finally { loading = false; }
  }

  async function poll() {
    if (!document.hidden) await refresh();
    if (!disposed) timer = window.setTimeout(poll, POLL_MS);
  }

  function openFormModal(title, form, options = {}) {
    modalActive = true;
    const modal = openReadOnlyModal(title, form, {
      id: options.id || 'remote-links-editor',
      className: `remote-links-modal${options.modalSize ? ` remote-links-modal-${options.modalSize}` : ''}`,
      cleanup: () => {
        modalActive = false;
        options.cleanup?.();
        queueMicrotask(() => { if (!disposed) void refresh(true); });
      }
    });
    if (!modal) modalActive = false;
    else if (modal.ready) void modal.ready.then((shown) => {
      if (shown && modal.dialog.isConnected) modalActive = true;
    });
    return modal;
  }

  function input(type, value = '') {
    const control = node('input', type === 'checkbox' ? 'ui-selection-check' : 'ui-input');
    control.type = type;
    control.value = value ?? '';
    return control;
  }

  function selectAlias(value, allowEmpty = false) {
    const control = node('select', 'ui-select');
    if (allowEmpty) {
      const option = node('option', '', 'Use host default');
      option.value = '';
      control.append(option);
    }
    (snapshot.alias_lists || []).forEach((item) => {
      const option = node('option', '', item.name || `Alias List ${item.alias_list_id}`);
      option.value = String(item.alias_list_id);
      control.append(option);
    });
    control.value = value == null ? '' : String(value);
    return control;
  }

  function submitModal(title, fields, saveLabel, operation, options = {}) {
    const form = node('form', 'remote-links-editor');
    const localMessage = node('div');
    localMessage.setAttribute('role', 'status');
    const body = node('div', `remote-links-fields${options.fieldLayout ? ` remote-links-fields-${options.fieldLayout}` : ''}`);
    body.append(...fields);
    const cancel = button('Cancel');
    const save = button(saveLabel, null, true);
    save.type = 'submit';
    const danger = options.danger || null;
    form.append(body, localMessage, modalFooter(danger, cancel, save));
    const modal = openFormModal(title, form, options);
    if (!modal) return null;
    cancel.addEventListener('click', modal.close);
    const workflow = createFormWorkflow({ form, submit: save, modal,
      renderFeedback: (state, text) => {
        localMessage.setAttribute('role', state === 'error' ? 'alert' : 'status');
        localMessage.hidden = !text;
        if (state === 'error') showError(localMessage, { message: text });
        else localMessage.replaceChildren(...(text ? [notice(text)] : []));
      } });
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      if (modal.state?.isBusy?.()) return;
      await workflow.save(() => operation(form, modal), { onSuccess: async (result) => {
        if (result === false) {
          workflow.setReady(false);
          modal.setDirty(true);
          return;
        }
        if (modal.close()) { if (result?.revision) present(result, true); else await refresh(true); }
      } });
    });
    return { form, modal, message: localMessage, save };
  }

  function openListener() {
    const current = snapshot.listener || {};
    const enabled = uiToggleField('Listen for remote senders', Boolean(current.enabled),
      'Listen for remote senders', 'Accept decoded P25 feeds from trusted senders.');
    const address = input('text', current.bind_address || '0.0.0.0');
    address.required = true; address.maxLength = 255;
    const port = input('number', current.port || 53800);
    port.required = true; port.min = '1'; port.max = '65535'; port.step = '1';
    submitModal('Remote listener', [enabled, formField('Bind address', address,
      'Use a VPN address or restrict access with a firewall. 0.0.0.0 listens on every interface; authentication does not encrypt traffic.'),
    formField('Port', port)],
    'Save listener', () => write('/listener', 'PUT', { revision: snapshot.revision,
      enabled: enabled.querySelector('input').checked, bind_address: address.value.trim(), port: Number(port.value) }),
    { modalSize: 'compact', fieldLayout: 'network' });
  }

  function openSenderConnection() {
    const current = snapshot.sender_connection || {};
    const enabled = uiToggleField('Send selected P25 systems', Boolean(current.enabled),
      'Send selected P25 systems', 'Connects this installation to one trusted host.');
    const destination = input('text', current.destination_host || '');
    destination.maxLength = 255;
    const port = input('number', current.destination_port || 53800);
    port.required = true; port.min = '1'; port.max = '65535'; port.step = '1';
    const senderId = input('text', current.sender_id || '');
    senderId.maxLength = 36; senderId.autocomplete = 'off';
    const secret = input('password', '');
    secret.maxLength = 256; secret.autocomplete = 'new-password';
    secret.placeholder = current.credential_configured ? 'Leave blank to keep saved secret' : 'Paste host-issued secret';
    const choices = node('fieldset', 'remote-links-export-list ui-form-section');
    choices.append(node('legend', '', 'P25 trunked systems to export'));
    const selected = new Set(current.exported_channel_configuration_ids || []);
    const options = snapshot.export_channel_options || [];
    if (!options.length) choices.append(node('p', 'muted', 'No saved P25 trunked channels are eligible for export.'));
    options.forEach((option) => {
      const label = node('label', 'remote-links-export-choice');
      const control = input('checkbox');
      control.value = option.channel_configuration_id;
      control.checked = selected.has(option.channel_configuration_id);
      label.append(control, node('span', '', option.name || option.site_name || option.channel_configuration_id),
        node('small', 'muted', [option.system_name, option.site_name, option.protocol].filter(Boolean).join(' · ')));
      choices.append(label);
    });
    const senderEnabled = enabled.querySelector('input');
    const syncRequired = () => {
      destination.required = senderEnabled.checked;
      senderId.required = senderEnabled.checked;
      secret.required = senderEnabled.checked && !current.credential_configured;
    };
    senderEnabled.addEventListener('change', syncRequired);
    syncRequired();
    submitModal('Outbound remote sender', [enabled, formField('Destination host', destination),
      formField('Destination port', port), formField('Sender ID', senderId,
        'Generated by the destination host.'), formField('Shared secret', secret,
        current.credential_configured ? 'A credential is already saved. Enter a new one only to replace it.' :
          'The destination host shows this secret once.'), choices], 'Save sender', () =>
      write('/sender-connection', 'PUT', { revision: snapshot.revision,
        enabled: enabled.querySelector('input').checked, destination_host: destination.value.trim(),
        destination_port: Number(port.value), sender_id: senderId.value.trim(), secret: secret.value,
        exported_channel_configuration_ids: [...choices.querySelectorAll('input:checked')].map((item) => item.value) }));
  }

  function openCreateSender() {
    const name = input('text');
    name.required = true; name.maxLength = 120; name.placeholder = 'Example: Mountain receiver';
    const editor = submitModal('Add trusted sender', [formField('Sender name', name,
      'Use a name that identifies the remote installation.')], 'Create credential', async () => {
      const result = await write('/senders', 'POST', { revision: snapshot.revision, display_name: name.value.trim() });
      editor.modal.setDirty(true);
      editor.modal.setDiscardMessage('Close this one-time credential? The shared secret will not be shown again.');
      editor.modal.setTitle('Credential created');
      editor.modal.dialog.classList.remove('remote-links-modal-compact');
      const credential = node('div', 'remote-links-credential');
      const reminder = notice('Copy this credential now. The shared secret will not be shown again.', 'warning');
      reminder.setAttribute('role', 'alert');
      reminder.tabIndex = -1;
      credential.append(reminder);
      const copyStatus = node('small', 'ui-field-detail');
      copyStatus.setAttribute('role', 'status');
      copyStatus.setAttribute('aria-live', 'polite');
      [['Sender ID', result.sender_id], ['Shared secret', result.secret]].forEach(([label, value]) => {
        const row = node('div', 'remote-links-credential-row');
        const text = input('text', value);
        text.readOnly = true;
        const copy = button('Copy', async () => {
          try {
            await navigator.clipboard.writeText(value);
            copyStatus.textContent = `${label} copied.`;
            if (label === 'Shared secret') editor.modal.setDirty(false);
          } catch (_) {
            text.focus(); text.select();
            copyStatus.textContent = `Clipboard access failed. ${label} is selected; copy it manually.`;
          }
        });
        copy.prepend(iconGlyph('icon-copy'));
        copy.setAttribute('aria-label', `Copy ${label}`);
        row.append(formField(label, text), copy);
        credential.append(row);
      });
      credential.append(copyStatus);
      const done = button('Done', () => { editor.modal.close(); }, true);
      editor.form.replaceChildren(credential, modalFooter(done));
      editor.modal.setBusy(false);
      reminder.focus();
      return false;
    }, { modalSize: 'compact', fieldLayout: 'single' });
  }

  function openSender(sender) {
    const name = input('text', sender.display_name || '');
    name.required = true; name.maxLength = 120;
    const alias = selectAlias(sender.default_alias_list_id, true);
    const revoke = button('Revoke sender');
    revoke.className = 'ui-button ui-button-danger';
    const editor = submitModal('Trusted sender', [formField('Sender name', name),
      formField('Preferred P25 Alias List', uiSelectFrame(alias),
        'Used for new feeds. If blank, this receiver chooses Default P25 or the first available P25 Alias List.')],
    'Save sender', () => write(`/senders/${pathPart(sender.sender_id)}`, 'PUT', {
      revision: snapshot.revision, display_name: name.value.trim(),
      default_alias_list_id: alias.value ? Number(alias.value) : null
    }), { danger: revoke, fieldLayout: 'single' });
    if (!editor) return;
    let revoking = false;
    revoke.addEventListener('click', async () => {
      if (revoking || editor.modal.state?.isBusy?.()) return;
      revoking = true;
      const disabled = [...editor.form.elements].map(control => [control, control.disabled]);
      disabled.forEach(([control]) => { control.disabled = true; });
      editor.modal.setBusy(true);
      try {
        if (!await confirmAction(`Revoke ${sender.display_name || sender.sender_id}? Existing remote channels will stop receiving.`, {
          title: 'Revoke trusted sender', confirmLabel: 'Revoke sender'
        }) || !editor.modal.dialog.isConnected) return;
        const result = await write(`/senders/${pathPart(sender.sender_id)}`, 'DELETE',
          { revision: snapshot.revision });
        editor.modal.setDirty(false); editor.modal.setBusy(false);
        if (editor.modal.close()) present(result, true);
      } catch (error) {
        if (editor.modal.dialog.isConnected) {
          editor.message.hidden = false;
          editor.message.setAttribute('role', 'alert');
          showError(editor.message, error);
        }
      } finally {
        revoking = false;
        editor.modal.setBusy(false);
        disabled.forEach(([control, wasDisabled]) => { control.disabled = wasDisabled; });
      }
    });
  }

  function openFeed(sender, feed) {
    if (!feed.channel_configuration_id) return;
    const name = input('text', feed.display_name || feed.advertised_name || '');
    name.required = true; name.maxLength = 120;
    const alias = selectAlias(feed.alias_list_id, false);
    alias.required = true;
    const enabled = uiToggleField('Enable this remote channel', Boolean(feed.enabled),
      'Enable this remote channel', 'Disabled feeds remain configured but do not process traffic.');
    const path = `/senders/${pathPart(sender.sender_id)}/feeds/${pathPart(feed.feed_id)}`;
    submitModal('Remote feed', [
      notice(`${protocolLabel(feed.protocol)} · ${frequency(feed.frequency_hz)} · advertised by ${sender.display_name || sender.sender_id}`),
      formField('Local channel name', name), formField('Alias List', uiSelectFrame(alias),
        'Aliases, streaming, recording, and activity use this host’s Alias List.'), enabled
    ], 'Save feed', () => write(path, 'PUT', {
      revision: snapshot.revision, display_name: name.value.trim(), alias_list_id: Number(alias.value),
      enabled: enabled.querySelector('input').checked
    }), { fieldLayout: 'single' });
  }

  function close() {
    disposed = true;
    window.clearTimeout(timer);
    window.clearTimeout(activationReleaseTimer);
    document.removeEventListener('pointerup', pointerUp);
    document.removeEventListener('pointercancel', pointerCancel);
    document.removeEventListener('keyup', keyUp);
  }

  signal?.addEventListener('abort', close, { once: true });
  void poll();
  return { element: host, close };
}
