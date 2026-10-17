async function request(path) {
  const response = await fetch(path, { credentials: 'same-origin', cache: 'no-store', headers: { Accept: 'application/json' } });
  const value = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(typeof value.error === 'string' ? value.error : value.error?.message ||
    `Request failed (${response.status})`);
  return value;
}

function bytes(value) {
  const size = Number(value) || 0;
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

export function createEventLogsWorkspace({ node, signal }) {
  const element = node('div', 'data-workspace event-logs-workspace');
  const toolbar = node('div', 'ui-catalog-toolbar');
  const status = node('div');
  status.setAttribute('role', 'status');
  const refresh = node('button', 'ui-button ui-button-secondary', 'Refresh');
  refresh.type = 'button';
  toolbar.append(node('p', 'muted', 'Logs written by the event-log options on each Playlist channel.'), refresh);
  const layout = node('div', 'ui-settings-grid');
  const list = node('section', 'ui-section');
  const viewer = node('section', 'ui-section');
  const title = node('h2', '', 'Select an event log');
  const details = node('p', 'muted', 'The newest portion of the selected file will appear here.');
  const content = node('pre', 'application-log-trace');
  content.style.maxHeight = '65vh';
  content.style.overflow = 'auto';
  viewer.append(title, details, content);
  layout.append(list, viewer);
  element.append(toolbar, status, layout);
  let stopped = false;

  const setError = (error) => {
    status.className = 'ui-notice ui-notice-danger';
    status.textContent = error?.message || 'Event logs could not be loaded.';
  };

  async function open(row) {
    try {
      const value = await request(`/api/v1/event-logs?file=${encodeURIComponent(row.name)}`);
      if (stopped) return;
      title.textContent = value.name;
      details.textContent = `${bytes(value.size)} · ${new Date(value.modified).toLocaleString()}` +
        (value.truncated ? ' · showing newest 1 MB' : '');
      content.textContent = value.content || '';
      const actions = node('div', 'ui-action-row');
      const download = node('a', 'ui-button ui-button-secondary', 'Download complete log');
      download.href = `/api/v1/event-logs?file=${encodeURIComponent(row.name)}&download=true`;
      actions.append(download);
      viewer.querySelector('.ui-action-row')?.remove();
      viewer.insertBefore(actions, content);
      status.className = '';
      status.textContent = '';
    } catch (error) { setError(error); }
  }

  async function load() {
    refresh.disabled = true;
    try {
      const rows = await request('/api/v1/event-logs');
      if (stopped) return;
      const table = node('table', 'ui-table');
      const head = node('thead');
      const heading = node('tr');
      ['File', 'Modified', 'Size', ''].forEach((label) => heading.append(node('th', '', label)));
      head.append(heading);
      const body = node('tbody');
      rows.forEach((row) => {
        const tr = node('tr');
        const action = node('button', 'ui-button ui-button-secondary', 'View');
        action.type = 'button';
        action.addEventListener('click', () => void open(row));
        [row.name, new Date(row.modified).toLocaleString(), bytes(row.size)].forEach((value) =>
          tr.append(node('td', '', value)));
        const cell = node('td');
        cell.append(action);
        tr.append(cell);
        body.append(tr);
      });
      table.append(head, body);
      list.replaceChildren(node('h2', '', 'Saved event logs'), rows.length ? table :
        node('div', 'ui-feedback', 'No event logs have been written yet.'));
      status.className = '';
      status.textContent = '';
    } catch (error) { setError(error); }
    finally { refresh.disabled = false; }
  }

  refresh.addEventListener('click', () => void load());
  signal?.addEventListener('abort', () => { stopped = true; }, { once: true });
  void load();
  return { element, close: () => { stopped = true; } };
}
